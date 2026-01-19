package net.splatcraft.entities;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.blocks.ColoredBarrierBlock;
import net.splatcraft.blocks.IColoredBlock;
import net.splatcraft.blocks.StageBarrierBlock;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.items.weapons.settings.*;
import net.splatcraft.items.weapons.settings.CommonRecords.ProjectileSizeRecord;
import net.splatcraft.items.weapons.settings.RollerWeaponSettings.RollerProjectileDataRecord;
import net.splatcraft.network.s2c.SendPlayerHitPacket;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.registries.SplatcraftEntities;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.tileentities.InkProjectileListener;
import net.splatcraft.util.*;
import net.splatcraft.util.structs.AttackId;
import net.splatcraft.util.structs.DamageCalculator;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import oshi.util.tuples.Quartet;
import oshi.util.tuples.Quintet;
import oshi.util.tuples.Triplet;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class InkProjectileEntity extends ThrowableProjectile implements IColoredEntity, ISetVelocityExtension
{
	private static final EntityDataAccessor<String> PROJ_TYPE = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<InkColor> COLOR = SynchedEntityData.defineId(InkProjectileEntity.class, CommonUtils.INKCOLOR_DATA_HANDLER);
	private static final EntityDataAccessor<Float> WORLD_HITBOX_LENGTH = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> HITBOX_RADIUS = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> VISUAL_SIZE = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> GRAVITY = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> STRAIGHT_SHOT_TIME = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> HORIZONTAL_DRAG = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> GRAVITY_SPEED_MULT = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<ExtraDataList> EXTRA_DATA = SynchedEntityData.defineId(InkProjectileEntity.class, ExtraSaveData.SERIALIZER);
	private static final EntityDataAccessor<Vector3f> SHOOT_DIRECTION = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.VECTOR3);
	private static final byte BARRIER_DENY = -1;
	private static final byte DROP_PARTICLE = 1;
	private static final byte PROJECTILE_IMPACT = 2;
	private static final byte BLAST_PARTICLE = 3;
	public HashSet<Integer> hitEnemies = new HashSet<>(2);
	public float lifespan = 600;
	public boolean explodes = false, bypassMobDamageMultiplier = false, canPierce = false, persistent = false, explodesOnExpire = false;
	public ItemStack sourceWeapon = ItemStack.EMPTY;
	public float impactCoverage, dropImpactSize, distanceBetweenDrops;
	public float damageMultiplier = 1;
	public boolean causesHurtCooldown;
	public DamageCalculator damage = DamageCalculator.empty();
	public InkBlockUtils.InkType inkType;
	public float accumulatedDrops;
	protected float straightShotTime = -1;
	private AttackId attackId = AttackId.NONE;
	public InkProjectileEntity(EntityType<InkProjectileEntity> type, Level world)
	{
		super(type, world);
	}
	public InkProjectileEntity(Level world, LivingEntity thrower, InkColor color, InkBlockUtils.InkType inkType, ProjectileSizeRecord size, DamageCalculator damage, ItemStack sourceWeapon)
	{
		super(SplatcraftEntities.INK_PROJECTILE.get(), thrower, world);
		setColor(color);
		setProjectileSize(size);
		impactCoverage = getProjectileHitboxRadius() * 0.85f;
		this.damage = damage;
		this.inkType = inkType;
		this.sourceWeapon = sourceWeapon;
	}
	public InkProjectileEntity(Level world, LivingEntity thrower, InkColor color, InkBlockUtils.InkType inkType, ProjectileSizeRecord size, DamageCalculator damage)
	{
		this(world, thrower, color, inkType, size, damage, ItemStack.EMPTY);
	}
	public InkProjectileEntity(Level world, LivingEntity thrower, ItemStack sourceWeapon, InkBlockUtils.InkType inkType, ProjectileSizeRecord size, DamageCalculator damage)
	{
		this(world, thrower, ColorUtils.getInkColor(sourceWeapon), inkType, size, damage, sourceWeapon);
	}
	public InkProjectileEntity setChargerStats(float charge, ChargerWeaponSettings.ChargerProjectileDataRecord settings)
	{
		dropImpactSize = settings.inkDropCoverage().getValue(charge);
		distanceBetweenDrops = settings.distanceBetweenInkDrops().getValue(charge);
		if (distanceBetweenDrops > 0)
			accumulatedDrops = CommonUtils.nextFloat(random, 0, 1);
		lifespan = settings.range().getValue(charge) / settings.speed().getValue(charge);
		impactCoverage = settings.inkCoverageImpact().getValue(charge);

		setGravity(0);
		canPierce = charge >= settings.piercesAtCharge();
		setProjectileType(Types.CHARGER);
		return this;
	}
	public InkProjectileEntity setBlasterStats(BlasterWeaponSettings settings)
	{
		setCommonProjectileStats(settings.projectileData);
		explodes = true;
		explodesOnExpire = true;
		setProjectileType(Types.BLASTER);
		return this;
	}
	public InkProjectileEntity setSlosherStats(CommonRecords.ProjectileDataRecord settings)
	{
		setCommonProjectileStats(settings);
		setProjectileType(Types.ROLLER);
		return this;
	}
	public InkProjectileEntity setShooterStats(ShooterWeaponSettings settings)
	{
		setCommonProjectileStats(settings.projectileData);
		setProjectileType(Types.SHOOTER);
		return this;
	}
	public InkProjectileEntity setSplatlingStats(SplatlingWeaponSettings<?> settings, float dataIndex)
	{
		CommonRecords.ProjectileDataRecord projectileData = settings.interpolateData(dataIndex).getFirst();

		setCommonProjectileStats(projectileData);
		setProjectileType(Types.SHOOTER);
		return this;
	}
	public InkProjectileEntity setDualieStats(CommonRecords.ProjectileDataRecord settings)
	{
		setCommonProjectileStats(settings);
		setProjectileType(Types.SHOOTER);
		return this;
	}
	public InkProjectileEntity setBrushSwingStats(RollerWeaponSettings settings, boolean weak)
	{
		setProjectileType(Types.ROLLER);

		return setRollerProjectileStats(settings.swingData.projectileData(), weak, true);
	}
	public InkProjectileEntity setRollerSwingStats(RollerWeaponSettings settings, boolean airborne, boolean weak)
	{
		setProjectileType(Types.ROLLER);

		if (airborne)
		{
			return setRollerProjectileStats(settings.flingData.projectileData(), weak, false);
		}
		return setRollerProjectileStats(settings.swingData.projectileData(), weak, false);
	}
	public InkProjectileEntity setCommonProjectileStats(CommonRecords.ProjectileDataRecord settings)
	{
		dropImpactSize = settings.inkDropCoverage();
		distanceBetweenDrops = settings.distanceBetweenInkDrops();
		if (distanceBetweenDrops > 0)
			accumulatedDrops = CommonUtils.nextFloat(random, 0, 1);
		impactCoverage = settings.inkCoverageImpact();

		setProjectileSize(settings.size());
		setGravity(settings.gravity());
		setStraightShotTime(settings.straightShotTicks());

		lifespan = settings.lifeTicks();
		setHorizontalDrag(settings.horizontalDrag());
		setGravitySpeedMult(settings.delaySpeedMult());

		return this;
	}
	public InkProjectileEntity setRollerProjectileStats(RollerProjectileDataRecord settings, boolean weak, boolean fromBrush)
	{
		dropImpactSize = settings.inkDropCoverage();
		distanceBetweenDrops = settings.distanceBetweenInkDrops();
		impactCoverage = settings.inkCoverageImpact();

		setProjectileSize(settings.size());
		setGravity(settings.gravity());
		setStraightShotTime(settings.straightShotTicks());

		lifespan = 600;
		setHorizontalDrag(settings.horizontalDrag());
		setGravitySpeedMult(settings.delaySpeedMult());

		if (fromBrush && weak)
		{
			dropImpactSize *= 0.8f;
			impactCoverage *= 0.8f;
			setStraightShotTime(settings.straightShotTicks() * 0.6f);
			setProjectileVisualSize(getProjectileVisualSize() * 0.6f);
		}

		return this;
	}
	@Override
	protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder)
	{
		builder.define(PROJ_TYPE, Types.SHOOTER);
		builder.define(COLOR, ColorUtils.getDefaultColor());
		builder.define(WORLD_HITBOX_LENGTH, 0.2f);
		builder.define(HITBOX_RADIUS, 0.2f);
		builder.define(VISUAL_SIZE, 0.6f);
		builder.define(GRAVITY, 0.175F);
		builder.define(STRAIGHT_SHOT_TIME, 0F);
		builder.define(SPEED, 0f);
		builder.define(HORIZONTAL_DRAG, 1F);
		builder.define(GRAVITY_SPEED_MULT, 1F);
		builder.define(EXTRA_DATA, new ExtraDataList());
		builder.define(SHOOT_DIRECTION, new Vector3f(0, 0, 0));
	}
	@Override
	public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> data)
	{
		if (WORLD_HITBOX_LENGTH.equals(data))
			refreshDimensions();
		else if (STRAIGHT_SHOT_TIME.equals(data))
			straightShotTime = entityData.get(STRAIGHT_SHOT_TIME);

		super.onSyncedDataUpdated(data);
	}
	@Override
	public void tick()
	{
		tick(1);
	}
	public void tick(float timeDelta)
	{
		if (timeDelta > lifespan)
			timeDelta = lifespan;

		Vec3 lastPosition = position();
		Vec3 velocity = getShootVelocity(timeDelta);
		setDeltaMovement(velocity.x, velocity.y, velocity.z);

		velocity = processCollisions(velocity, Math.min(1f, lifespan));

		checkInsideBlocks();
		double nextX = getX() + velocity.x;
		double nextY = getY() + velocity.y;
		double nextZ = getZ() + velocity.z;
		updateRotation();
		if (isInWater())
		{
			for (int i = 0; i < 4; i++)
			{
				level().addParticle(ParticleTypes.BUBBLE, nextX - velocity.x * 0.25, nextY - velocity.y * 0.25, nextZ - velocity.z * 0.25, velocity.x, velocity.y, velocity.z);
			}

			discard();
			return;
		}

		applyGravity();
		setPos(nextX, nextY, nextZ);

		straightShotTime -= timeDelta;

		if (level().isClientSide())
			return;

		lifespan -= timeDelta;
		if (!persistent && lifespan <= 0 && !isRemoved())
		{
			ExtraSaveData.ExplosionExtraData explosionData = getExtraDatas().getFirstExtraData(ExtraSaveData.ExplosionExtraData.class);
			if (Objects.equals(getProjectileType(), Types.BLASTER) && explosionData != null && explodesOnExpire)
			{
				InkExplosion.createInkExplosionWithSound(getOwner(), position(), explosionData.explosionPaint, explosionData.getRadiuses(false, damageMultiplier), inkType, sourceWeapon, AttackId.NONE, position().toVector3f());
				createDrop(getX(), getY(), getZ(), 0, explosionData.explosionPaint);
				level().broadcastEntityEvent(this, BLAST_PARTICLE);
				level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterExplosion, SoundSource.PLAYERS, 0.8F, CommonUtils.nextTriangular(level().getRandom(), 0.95F, 0.095F));
			}
			else
			{
				InkExplosion.createInkExplosion(getOwner(), position(), impactCoverage, inkType, sourceWeapon);
			}
			calculateDrops(lastPosition);
			discard();
		}
		else if (dropImpactSize > 0)
		{
			if (!isInvisible())
			{
				level().broadcastEntityEvent(this, DROP_PARTICLE);
			}
			calculateDrops(lastPosition);
		}
	}
	private @NotNull Vec3 processCollisions(Vec3 velocity, double currentCoefficient)
	{
		// oh no

		AABB searchAABB = getBoundingBox().expandTowards(velocity);
		Vec3 reversedPosition = position().reverse();
		float radiusSqrd = Mth.square(getProjectileHitboxRadius());

		List<Entity> possibleEntityCollisions = level().getEntities(this, searchAABB, this::canHitEntity);

		List<Entity> possibleShieldingEntityCollisions = possibleEntityCollisions.stream().filter(v -> v instanceof ShieldingEntity).toList();

		Quintet<Entity, Double, Vec3, Vec3, Vec3> limitCollision = processLimitingCollision(possibleShieldingEntityCollisions, searchAABB, velocity, reversedPosition, radiusSqrd);

		DoubleUnaryOperator orthogonalCoefficientOperator;

		if (limitCollision != null)
		{
			// resize the search box since we will collide with an ShieldingEntity / WorldBorder and will not travel the entire velocity vector
			// esto hace nan cuando coeficiente es 0 !
			double factor = DoubleMath.fuzzyEquals(limitCollision.getB(), 0, 10e-7) ? 1 : currentCoefficient / limitCollision.getB();
			orthogonalCoefficientOperator = (v) -> Math.clamp(v, 0, limitCollision.getB() * factor);

			velocity = limitCollision.getC();
			currentCoefficient = limitCollision.getB();

			searchAABB = getBoundingBox().expandTowards(velocity);
		}
		else
		{
			orthogonalCoefficientOperator = (v) -> Math.clamp(v, 0, 1);
		}

		AABB finalSearchAABB = searchAABB;
		if (limitCollision != null)
			possibleEntityCollisions = possibleEntityCollisions.stream().filter(v -> v.getBoundingBox().intersects(finalSearchAABB)).toList();

		List<Pair<BlockPos, VoxelShape>> possibleWorldCollisions = ImmutableList.copyOf(() -> new BlockCollisions<>(level(), this, finalSearchAABB, false, (blockPos, shape) -> Pair.of(blockPos.immutable(), shape)));
		Optional<Quintet<BlockPos, Vec3, Vec3, Boolean, Double>> collisionData = CollisionUtils.findFirstBlock(position(), velocity, CommonUtils.createVec3(getProjectileWorldHitboxLength()), possibleWorldCollisions, orthogonalCoefficientOperator);

		if (collisionData.isPresent())
		{
			Quintet<BlockPos, Vec3, Vec3, Boolean, Double> collision = collisionData.get();

			onHitBlock(collision.getA(), collision.getB(), velocity.normalize().scale(-1), collision.getE().floatValue(), collision.getD());

			if (currentCoefficient > collision.getE())
			{
				// resize the search box AGAIN since we will collide with a block and will not travel the entire velocity vector, that was previously resized

				double factor = DoubleMath.fuzzyEquals(collision.getE(), 0, 10e-7) ? 10e-5 : currentCoefficient / collision.getE();

				orthogonalCoefficientOperator = (v) -> Math.clamp(v, 0, collision.getE() * factor);

				currentCoefficient = collision.getE();
				velocity = velocity.scale(factor);

				searchAABB = getBoundingBox().expandTowards(velocity);
			}
		}

		// todo: maybe recalculate the entities since the searchAABB was modified
		List<Entity> possibleNonShieldingEntityCollisions = possibleEntityCollisions.stream().filter(v -> !(v instanceof ShieldingEntity)).toList();

		return processEntityCollisions(
			possibleNonShieldingEntityCollisions,
			velocity,
			limitCollision != null ? new Quartet<>(limitCollision.getA(), limitCollision.getD(), limitCollision.getE(), limitCollision.getB()) : null,
			orthogonalCoefficientOperator);
	}
	private @Nullable Quintet<Entity, Double, Vec3, Vec3, Vec3> processLimitingCollision(List<Entity> possibleShieldingEntityCollisions, AABB searchAABB, Vec3 finalVelocity, Vec3 reversedPosition, float radiusSqrd)
	{
		// method that searches for shielding entities and the world border to collide with, since colliding with these should stop all the other collisions

		List<Pair<AABB, Entity>> possibleInitialCollisions = new ArrayList<>();
		for (Entity entity : possibleShieldingEntityCollisions)
		{
			if (!(entity instanceof ShieldingEntity shieldingEntity)) continue;

			// todo: give more complex collisions to ShieldingEntity, like multiple bounding boxes
			possibleInitialCollisions.add(Pair.of(entity.getBoundingBox(), entity));
		}

		WorldBorder worldBorder = level().getWorldBorder();
		if (worldBorder.isInsideCloseToBorder(this, searchAABB))
			possibleInitialCollisions.addAll(level().getWorldBorder().getCollisionShape().toAabbs().stream().map(v -> Pair.of(v, (Entity) null)).toList());

		Triplet<Integer, Double, Triplet<Vec3, Vec3, Vec3>> collisionData = CollisionUtils.findFirstToCollide(possibleInitialCollisions, pair ->
		{
			AABB aabb = pair.getFirst();
			Triplet<Vec3, Vec3, Double> segmentDistance = CollisionUtils.calculateOrthogonalPoint(finalVelocity, aabb.move(reversedPosition), v -> Math.clamp(v, 0, 1));

			if (segmentDistance.getB().distanceToSqr(segmentDistance.getA()) > radiusSqrd) return null;

			return Pair.of(segmentDistance.getC(), new Triplet<>(finalVelocity.scale(segmentDistance.getC()), segmentDistance.getA(), segmentDistance.getB()));
		});

		if (collisionData == null) return null;

		return CommonUtils.merge(possibleInitialCollisions.get(collisionData.getA()).getSecond(), collisionData.getB(), collisionData.getC());
	}
	private Vec3 processEntityCollisions(List<Entity> possibleNonShieldingEntityCollisions, Vec3 velocity, Quartet<Entity, Vec3, Vec3, Double> finalShieldingEntity, DoubleUnaryOperator orthogonalCoefficientOperator)
	{
		AtomicReference<Vec3> velocityReference = new AtomicReference<>(velocity);
		List<Quartet<Entity, Vec3, Vec3, Double>> collisions = new ArrayList<>();
		if (canPierce)
		{
			collisions.addAll(CollisionUtils.findEntityCollisions(
				position(),
				velocity,
				getProjectileHitboxRadius(),
				possibleNonShieldingEntityCollisions,
				orthogonalCoefficientOperator)
			);

			if (finalShieldingEntity != null)
			{
				collisions.add(finalShieldingEntity);
			}
		}
		else
		{
			CollisionUtils.findFirstEntityCollisions(
				position(),
				velocity,
				getProjectileHitboxRadius(),
				possibleNonShieldingEntityCollisions,
				orthogonalCoefficientOperator
			).ifPresent(e ->
			{
				velocityReference.set(e.getB().subtract(position()));
				collisions.add(e);
			});
		}

		for (Quartet<Entity, Vec3, Vec3, Double> entity : collisions)
		{
			onHitEntity(entity.getA(), entity.getB(), entity.getC(), entity.getD().floatValue());
		}

		SendPlayerHitPacket.releaseHitPositions(getOwner());

		return velocityReference.get();
	}
	private void calculateDrops(Vec3 lastPosition)
	{
		calculateDrops(lastPosition, position());
	}
	private void calculateDrops(Vec3 lastPosition, Vec3 currentPosition)
	{
		calculateDrops(lastPosition, currentPosition, (float) getDeltaMovement().length());
	}
	private void calculateDrops(Vec3 lastPosition, Vec3 currentPosition, float unitsTravelled)
	{
		calculateDrops(lastPosition, currentPosition, unitsTravelled, false);
	}
	private void calculateDrops(Vec3 lastPosition, Vec3 currentPosition, float unitsTravelled, boolean doRayCheck)
	{
		if (distanceBetweenDrops < 0)
			return;

		if (distanceBetweenDrops == 0)
		{
			createDrop(getX(), getY(), getZ(), 0, 0);
			return;
		}
		float dropsTravelled = unitsTravelled / distanceBetweenDrops;
		if (dropsTravelled > 0)
		{
			accumulatedDrops += dropsTravelled;

			while (accumulatedDrops >= 1)
			{
				accumulatedDrops -= 1;

				float progress = accumulatedDrops / dropsTravelled;

				Vec3 dropPos = currentPosition.lerp(lastPosition, progress);

				// todo: fix this and not depend on world.noCollision
				if (doRayCheck && !level().noCollision(AABB.ofSize(dropPos, 1, 1, 1)))
					break;

				createDrop(dropPos.x, dropPos.y, dropPos.z, progress, dropImpactSize);
			}
		}
	}
	public void createDrop(double dropX, double dropY, double dropZ, float extraFrame, float dropImpactSize)
	{
		InkDropEntity proj = new InkDropEntity(level(), this, getColor(), inkType, dropImpactSize, sourceWeapon);
		proj.moveTo(dropX, dropY, dropZ);
		level().addFreshEntity(proj);
		proj.tick(extraFrame);
	}
	private Vec3 getShootVelocity(float timeDelta)
	{
		Vector3f shootDirection = getShotDirection();
		float frame = getMaxStraightShotTime() - straightShotTime;
		float[] speedData = getSpeed(frame, getMaxStraightShotTime(), timeDelta);
		Vec3 velocity = new Vec3(shootDirection.x * speedData[0], shootDirection.y * speedData[0], shootDirection.z * speedData[0]);
		if (speedData[1] <= 0)
			return velocity;
		return velocity.subtract(0, (float) (getDefaultGravity() * speedData[1]), 0);
	}
	public float[] getSpeed(float frame, float straightShotFrame, float timeDelta)
	{
		float speed = entityData.get(SPEED);
		float fallenFrames = frame - straightShotFrame;
		float fallenFramesNext = fallenFrames + timeDelta;
		if (timeDelta == 0)
			return new float[] {0, fallenFramesNext};

		if (fallenFramesNext < 0) // not close to falling
		{
			return new float[] {speed * timeDelta, fallenFramesNext};
		}
		else if (fallenFramesNext >= timeDelta) // already falling
		{
			speed *= getHorizontalDrag() * getGravitySpeedMult() * (float) Math.pow(getHorizontalDrag(), fallenFrames);
			return new float[] {speed * timeDelta, fallenFramesNext};
		}
		float straightFraction = -fallenFrames;
		return new float[] {(speed * straightFraction + speed * getHorizontalDrag() * getGravitySpeedMult() * (float) Math.pow(getHorizontalDrag(), fallenFrames) * fallenFramesNext), fallenFramesNext};
	}
	@Override
	public void updateRotation()
	{
		Vec3 motion = getDeltaMovement();

		if (!Vec3.ZERO.equals(motion))
		{
			float pitch = (float) (Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG);
			float yaw = (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
			if (tickCount == 1)
			{
				setXRot(pitch);
				setYRot(yaw);
				xRotO = pitch;
				yRotO = yaw;
			}
			else
			{
				setXRot(lerpRotation(xRotO, pitch));
				setYRot(lerpRotation(yRotO, yaw));
			}
		}
	}
	@Override
	public void handleEntityEvent(byte id)
	{
		super.handleEntityEvent(id);
		switch (id)
		{
			case BARRIER_DENY ->
				level().addParticle(new InkExplosionParticleData(getColor(), .5f), getX(), getY(), getZ(), 0, 0, 0);
			case DROP_PARTICLE ->
			{
				Vector3f velocity = getShotDirection().mul(entityData.get(SPEED) / 2f);
				if (getProjectileType().equals(Types.CHARGER))
					level().addParticle(new InkSplashParticleData(getColor(), getProjectileWorldHitboxLength() * 0.8f), getX() - velocity.x * 0.25D, getY() + getBbHeight() * 0.5f - velocity.y * 0.25D, getZ() - velocity.z * 0.25D, 0, -0.1, 0);
				else
					level().addParticle(new InkSplashParticleData(getColor(), getProjectileWorldHitboxLength() * 0.8f), getX() - velocity.x * 0.25D, getY() + getBbHeight() * 0.5f - velocity.y * 0.25D, getZ() - velocity.z * 0.25D, velocity.x, velocity.y, velocity.z);
			}
			case PROJECTILE_IMPACT ->
				level().addParticle(new InkSplashParticleData(getColor(), getProjectileWorldHitboxLength() * 2), getX(), getY(), getZ(), 0, 0, 0);
			case BLAST_PARTICLE ->
				level().addParticle(new InkExplosionParticleData(getColor(), getProjectileWorldHitboxLength() * 2), getX(), getY(), getZ(), 0, 0, 0);
		}
	}
	@Deprecated
	@Override
	public void onHit(@NotNull HitResult result)
	{
		return; // use the other method

/*
		HitResult.Type rayType = result.getType();
		if (rayType == HitResult.Type.ENTITY)
		{
			onHitEntity((EntityHitResult) result);
		}
		else if (rayType == HitResult.Type.BLOCK)
		{
			onHitBlock((BlockHitResult) result);
		}

		if (canPierce)
		{
			HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
			if (hitresult.getType() == HitResult.Type.ENTITY)
			{
				hitTargetOrDeflectSelf(hitresult);
			}
		}
		SendPlayerHitPacket.releaseHitPositions(getOwner());
*/
	}
	@Override
	public boolean canHitEntity(@NotNull Entity entity)
	{
		return entity != getOwner() && entity.canBeHitByProjectile() && InkDamageUtils.canDamage(entity, entityData.get(COLOR)) && !hitEnemies.contains(entity.getId());
	}
	protected void onHitEntity(Entity target, Vec3 closestPointInsideBox, Vec3 collisionNormal, float framesAdvanced)
	{
		if (level().isClientSide())
			return;

		float dmg = calculateDamage(framesAdvanced);

		if (target instanceof SpawnShieldEntity && !InkDamageUtils.canDamage(target, this))
		{
			discard();
			level().broadcastEntityEvent(this, BARRIER_DENY);
		}

		Entity owner = getOwner();

		if (InkDamageUtils.isSplatted(target)) return;

		boolean didDamage = InkDamageUtils.doDamage(target, dmg, owner, this, sourceWeapon, SplatcraftDamageTypes.INK_SPLAT, causesHurtCooldown, attackId);
		if (didDamage)
		{
			hitEnemies.add(target.getId());

			ExtraSaveData.ChargeExtraData chargeData = getExtraDatas().getFirstExtraData(ExtraSaveData.ChargeExtraData.class);
			if (Objects.equals(getProjectileType(), Types.CHARGER) && chargeData != null && chargeData.charge >= 1.0f && InkDamageUtils.isSplatted(target) && dmg > 20 ||
			    Objects.equals(getProjectileType(), Types.BLASTER))
			{
				accumulateHitPacket(closestPointInsideBox, SplatcraftSounds.shotDirectHit, 1.2f);
			}
			else
			{
				accumulateHitPacket(closestPointInsideBox, SplatcraftSounds.shotHit, 1f);
			}
		}

		if (!canPierce)
		{
			ExtraSaveData.ExplosionExtraData explosionData = getExtraDatas().getFirstExtraData(ExtraSaveData.ExplosionExtraData.class);
			if (explodes && explosionData != null)
			{
				InkExplosion.createInkExplosionWithSound(
					owner, closestPointInsideBox, explosionData.explosionPaint,
					explosionData.getRadiuses(false, damageMultiplier),
					inkType, sourceWeapon,
					explosionData.newAttackId ? AttackId.NONE : attackId, closestPointInsideBox.toVector3f());

				level().broadcastEntityEvent(this, BLAST_PARTICLE);
				level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterExplosion, SoundSource.PLAYERS, 0.8F, CommonUtils.nextTriangular(level().getRandom(), 0.95F, 0.095F));
			}
			else
				level().broadcastEntityEvent(this, PROJECTILE_IMPACT);

			discard();
		}
	}
	private void onHitBlock(BlockPos collidedPos, Vec3 closestPointToBlock, Vec3 collisionNormal, float framesAdvanced, boolean inside)
	{
		if (InkBlockUtils.canInkPassthrough(level(), collidedPos))
			return;

		BlockState state = level().getBlockState(collidedPos);
		if (state.getBlock() instanceof ColoredBarrierBlock coloredBarrierBlock &&
		    coloredBarrierBlock.canAllowThrough(collidedPos, this))
			return;

		if (state.getBlock() instanceof IColoredBlock coloredBlock)
		{
			coloredBlock.inkBlock(level(), collidedPos, getColor(), calculateDamage(framesAdvanced), inkType);
		}
		if (level().getBlockEntity(collidedPos) instanceof InkProjectileListener listener)
			listener.onCollide(this, collidedPos, closestPointToBlock, collisionNormal);

		Direction closestDirection = Direction.getNearest(collisionNormal.x, collisionNormal.y, collisionNormal.z);
		BlockHitResult result = new BlockHitResult(closestPointToBlock, closestDirection, collidedPos, inside);
		super.onHitBlock(result);
		if (level().isClientSide())
		{
			return;
		}

		Vec3 nextPosition = position().add(getDeltaMovement());
		calculateDrops(position(), nextPosition, (float) (framesAdvanced * getDeltaMovement().length()), true);

		if (level().getBlockState(collidedPos).getBlock() instanceof StageBarrierBlock)
			level().broadcastEntityEvent(this, BARRIER_DENY);
		else
		{
			level().broadcastEntityEvent(this, PROJECTILE_IMPACT);
			Vec3 impactPos = closestPointToBlock.add(collisionNormal.scale(0.01));
			
			ExtraSaveData.ImpactSoundExtraData soundData = getExtraDatas().getFirstExtraData(ExtraSaveData.ImpactSoundExtraData.class);
			if (soundData != null)
			{
				level().playSound(null, getX(), getY(), getZ(), soundData.sound, SoundSource.PLAYERS, 1f, 1f);
			}

			ExtraSaveData.ExplosionExtraData explosionData = getExtraDatas().getFirstExtraData(ExtraSaveData.ExplosionExtraData.class);
			if (explodes && explosionData != null)
			{
				InkExplosion.createInkExplosionWithSound(getOwner(), impactPos, explosionData.explosionPaint, explosionData.getRadiuses(true, damageMultiplier), inkType, sourceWeapon, explosionData.newAttackId ? AttackId.NONE : attackId, impactPos.toVector3f());
				level().broadcastEntityEvent(this, BLAST_PARTICLE);
			}
			else
			{
				InkExplosion.createInkExplosion(getOwner(), impactPos, impactCoverage, inkType, sourceWeapon);
			}
		}

		discard();
	}
	private void accumulateHitPacket(Vec3 impactPos, SoundEvent shotSound, float scale)
	{
		SendPlayerHitPacket.accumulateHitPositions(impactPos.toVector3f(), shotSound, scale);
	}
	private float calculateDamage(float partialTick)
	{
		float storedCrystalSoundIntensity = crystalSoundIntensity;

		// idk vector math so i read https://discussions.unity.com/t/inverselerp-for-vector3/177038 for this
		// lol i didnt even use it

		Vec3 nextPosition = position().add(getDeltaMovement());

		crystalSoundIntensity = partialTick;

		float dmg = damage.calculateDamage(this, getExtraDatas()) * damageMultiplier;
		crystalSoundIntensity = storedCrystalSoundIntensity;
		return dmg;
	}
	@Override
	public void shootFromRotation(@NotNull Entity shooter, float pitch, float yaw, float roll, float speed, float divergence)
	{
		ISetVelocityExtension.super.setDeltaMovement(shooter, pitch, yaw, roll, speed, divergence);
	}
	@Override
	public void shoot(double x, double y, double z, float power, float uncertainty)
	{
		ISetVelocityExtension.super.setDeltaMovement(x, y, z, power, uncertainty);
	}
	@Override
	public Vec3 calculateShotDirection(double x, double y, double z, float inaccuracy)
	{
		if (inaccuracy == 0)
			return new Vec3(x, y, z).normalize();

		float xRand = 0, yRand = 0;
		if (random.nextBoolean())
		{
			xRand = random.nextFloat() * 2f - 1;
			yRand = random.nextBoolean() ? 1 : -1;
		}
		else
		{
			xRand = random.nextBoolean() ? 1 : -1;
			yRand = random.nextFloat() * 2f - 1;
		}
		float usedInaccuracy = inaccuracy * Mth.DEG_TO_RAD;
		return new Vec3(x, y, z)
			.yRot(xRand * usedInaccuracy)
			.xRot(yRand * usedInaccuracy * VERTICAL_RATIO).normalize();
	}
	@Override
	public void onShotDirectionCalculated(Vec3 shotDirection)
	{
		entityData.set(SHOOT_DIRECTION, shotDirection.toVector3f());
	}
	@Override
	public void onVelocityCalculated(Vec3 direction, float speed)
	{
		hasImpulse = true;

		double d0 = direction.horizontalDistance();
		float yaw = (float) (Mth.atan2(direction.x, direction.z) * Mth.RAD_TO_DEG);
		float pitch = (float) (Mth.atan2(direction.y, d0) * Mth.RAD_TO_DEG);
		setYRot(yaw);
		setXRot(pitch);
		yRotO = yaw;
		xRotO = pitch;
		setDeltaMovement(direction);

		entityData.set(SPEED, speed);
	}
	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag nbt)
	{
		super.readAdditionalSaveData(nbt);

		if (nbt.contains("TerrainSize"))
			setProjectileWorldHitboxLength(nbt.getFloat("TerrainSize"));
		if (nbt.contains("CollisionSize"))
			setProjectileHitboxRadius(nbt.getFloat("CollisionSize"));
		if (nbt.contains("VisualSize"))
			setProjectileVisualSize(nbt.getFloat("VisualSize"));

		impactCoverage = nbt.contains("ImpactCoverage") ? nbt.getFloat("ImpactCoverage") : getProjectileWorldHitboxLength() * 0.85f;

		if (nbt.contains("Color"))
			setColor(InkColor.getFromNbt(nbt.get("Color")));

		entityData.set(SPEED, nbt.getFloat("Speed"));
		setHorizontalDrag(nbt.getFloat("HorizontalDrag"));
		setGravitySpeedMult(nbt.getFloat("GravitySpeedMult"));

		if (nbt.contains("Gravity"))
			setGravity(nbt.getFloat("Gravity"));
		if (nbt.contains("Lifespan"))
			lifespan = nbt.getInt("Lifespan");
		if (nbt.contains("MaxStraightShotTime"))
			setStraightShotTime(nbt.getFloat("StraightShotTime"));
		if (nbt.contains("StraightShotTime"))
			straightShotTime = nbt.getFloat("StraightShotTime");

		ExtraCodecs.VECTOR3F.parse(NbtOps.INSTANCE, nbt.get("Direction")).result().ifPresent(direction ->
		{
			entityData.set(SHOOT_DIRECTION, direction);
		});

		distanceBetweenDrops = nbt.getFloat("TrailFrequency");
		dropImpactSize = nbt.getFloat("TrailSize");
		bypassMobDamageMultiplier = nbt.getBoolean("BypassMobDamageMultiplier");
		canPierce = nbt.getBoolean("CanPierce");
		explodes = nbt.getBoolean("Explodes");
		explodesOnExpire = nbt.getBoolean("ExplodesOnExpire");
		persistent = nbt.getBoolean("Persistent");
		causesHurtCooldown = nbt.getBoolean("CausesHurtCooldown");

		setInvisible(nbt.getBoolean("Invisible"));

		String type = nbt.getString("ProjectileType");
		setProjectileType(type.isEmpty() ? Types.DEFAULT : type);
		inkType = InkBlockUtils.InkType.CODEC.parse(NbtOps.INSTANCE, nbt.get("InkType")).result().orElse(InkBlockUtils.InkType.NORMAL);

		sourceWeapon = ItemStack.parseOptional(registryAccess(), nbt.getCompound("SourceWeapon"));

		if (nbt.contains("DamageCalculator"))
		{
			DamageCalculator.parseDamageCalculator(NbtOps.INSTANCE, nbt.get("DamageCalculator")).ifSuccess(
				damage -> this.damage = damage
			);
		}

		if (nbt.contains("AttackId"))
			attackId = AttackId.parseAttackId(NbtOps.INSTANCE, nbt.getCompound("AttackId"));
	}
	@Override
	public void addAdditionalSaveData(CompoundTag nbt)
	{
		nbt.putFloat("TerrainSize", getProjectileWorldHitboxLength());
		nbt.putFloat("CollisionSize", getProjectileHitboxRadius());
		nbt.putFloat("VisualSize", getProjectileVisualSize());
		nbt.put("Color", getColor().getNbt());

		nbt.putFloat("Speed", entityData.get(SPEED));
		nbt.putFloat("HorizontalDrag", getHorizontalDrag());
		nbt.putFloat("GravitySpeedMult", getGravitySpeedMult());
		nbt.putFloat("MaxStraightShotTime", getMaxStraightShotTime());
		nbt.putFloat("StraightShotTime", straightShotTime);

		ExtraCodecs.VECTOR3F.encodeStart(NbtOps.INSTANCE, getShotDirection()).result().ifPresent(tag -> nbt.put("Direction", tag));

		nbt.putDouble("Gravity", getDefaultGravity());
		nbt.putFloat("Lifespan", lifespan);
		nbt.putFloat("TrailSize", dropImpactSize);
		nbt.putFloat("TrailFrequency", distanceBetweenDrops);
		nbt.putBoolean("BypassMobDamageMultiplier", bypassMobDamageMultiplier);
		nbt.putBoolean("CanPierce", canPierce);
		nbt.putBoolean("Explodes", explodes);
		nbt.putBoolean("ExplodesOnExpire", explodesOnExpire);
		nbt.putBoolean("Persistent", persistent);
		nbt.putBoolean("CausesHurtCooldown", causesHurtCooldown);

		nbt.putBoolean("Invisible", isInvisible());

		nbt.putString("ProjectileType", getProjectileType());
		nbt.putString("InkType", inkType.name());
		nbt.put("SourceWeapon", sourceWeapon.saveOptional(level().registryAccess()));
		SplatcraftComponents.getOptional(sourceWeapon, SplatcraftComponents.WEAPON_SETTING_ID).ifPresent(setting ->
		{
			ResourceLocation.CODEC.encodeStart(NbtOps.INSTANCE, setting).ifSuccess(tag ->
			{
				nbt.put("Settings", tag);
			});
		});

		DamageCalculator.encodeDamageCalculator(NbtOps.INSTANCE, damage).ifSuccess(
			tag -> nbt.put("DamageCalculator", tag)
		);

		if (attackId != AttackId.NONE)
			nbt.put("AttackId", AttackId.encodeAttackId(NbtOps.INSTANCE, attackId));

		super.addAdditionalSaveData(nbt);
	}
	private @NotNull Float getGravitySpeedMult()
	{
		return entityData.get(GRAVITY_SPEED_MULT);
	}
	private void setGravitySpeedMult(float gravitySpeedMult)
	{
		entityData.set(GRAVITY_SPEED_MULT, gravitySpeedMult);
	}
	private @NotNull Float getHorizontalDrag()
	{
		return entityData.get(HORIZONTAL_DRAG);
	}
	private void setHorizontalDrag(float horizontalDrag)
	{
		entityData.set(HORIZONTAL_DRAG, horizontalDrag);
	}
	/**
	 * @return a exposed list to the synched list that manages the extra data sent to the connection or smtinh
	 * @apiNote this list if updated isnt going to be synched!!!!! for this use {@link #addExtraData(ExtraSaveData)} instead, or {@link #setExtraDataList(ExtraDataList)}
	 */
	public ExtraDataList getExtraDatas()
	{
		return entityData.get(EXTRA_DATA);
	}
	public void setExtraDataList(ExtraDataList list)
	{
		entityData.set(EXTRA_DATA, list);
	}
	public void addExtraData(ExtraSaveData data)
	{
		ExtraDataList list = entityData.get(EXTRA_DATA);
		list.add(data);
		entityData.set(EXTRA_DATA, list);
	}
	public @NotNull ItemStack getItem()
	{
		return sourceWeapon;
	}
	@Override
	public @NotNull EntityDimensions getDimensions(@NotNull Pose pose)
	{
		return super.getDimensions(pose).scale(getProjectileWorldHitboxLength());
	}
	@Override
	protected @NotNull AABB makeBoundingBox()
	{
		return dimensions.makeBoundingBox(getX(), getY() - getBbHeight() / 2, getZ());
	}
	@Override
	public double getDefaultGravity()
	{
		return entityData.get(GRAVITY);
	}
	public void setGravity(float gravity)
	{
		entityData.set(GRAVITY, gravity);
	}
	public double getStraightShotTime()
	{
		return straightShotTime;
	}
	public void setStraightShotTime(float time)
	{
		entityData.set(STRAIGHT_SHOT_TIME, time);
	}
	public float calculateDamageDecay(float baseDamage, float startTick, float decayPerTick, float minDamage)
	{
		// getMaxStraightShotTime() - straightShotTime is just age but it counts the partial ticks too (and time delta!!! yay i hate myself)
		double age = getMaxStraightShotTime() - straightShotTime + crystalSoundIntensity;

		double diff = age - startTick;
		if (diff < 0)
			return baseDamage;
		return (float) Math.max(minDamage, baseDamage - decayPerTick * diff);
	}
	public float getMaxStraightShotTime()
	{
		return entityData.get(STRAIGHT_SHOT_TIME);
	}
	public Vector3f getShotDirection()
	{
		return new Vector3f(entityData.get(SHOOT_DIRECTION));
	}
	@Override
	public boolean isNoGravity()
	{
		return true;
	}
	public void setProjectileSize(CommonRecords.ProjectileSizeRecord size)
	{
		setProjectileWorldHitboxLength(size.worldHitboxLength());
		setProjectileVisualSize(size.visualSize() * 3);
		setProjectileHitboxRadius(size.hitboxRadius());
	}
	public void setProjectileOverallSize(float size)
	{
		setProjectileWorldHitboxLength(size);
		setProjectileVisualSize(size * 3);
		setProjectileHitboxRadius(size);
	}
	public float getProjectileWorldHitboxLength()
	{
		return entityData.get(WORLD_HITBOX_LENGTH);
	}
	public void setProjectileWorldHitboxLength(float length)
	{
		entityData.set(WORLD_HITBOX_LENGTH, length);
		reapplyPosition();
		refreshDimensions();
	}
	public float getProjectileVisualSize()
	{
		return entityData.get(VISUAL_SIZE);
	}
	public void setProjectileVisualSize(float visualSize)
	{
		entityData.set(VISUAL_SIZE, visualSize);
	}
	public float getProjectileHitboxRadius()
	{
		return entityData.get(HITBOX_RADIUS);
	}
	public void setProjectileHitboxRadius(float size)
	{
		entityData.set(HITBOX_RADIUS, size);
	}
	@Override
	public void remove(@NotNull RemovalReason pReason)
	{
		if (!level().isClientSide())
			attackId.projectileRemoved();
		super.remove(pReason);
	}
	@Override
	public InkColor getColor()
	{
		return entityData.get(COLOR);
	}
	@Override
	public void setColor(InkColor color)
	{
		entityData.set(COLOR, color);
	}
	public String getProjectileType()
	{
		return entityData.get(PROJ_TYPE);
	}
	public void setProjectileType(String v)
	{
		entityData.set(PROJ_TYPE, v);
	}
	public @NotNull RandomSource getRandom()
	{
		return random;
	}
	public void setAttackId(AttackId attackId)
	{
		this.attackId = attackId;
	}
	public static class Types
	{
		public static final String DEFAULT = "default";
		public static final String SHOOTER = "shooter";
		public static final String CHARGER = "charger";
		public static final String ROLLER = "roller";
		public static final String BLASTER = "blaster";
	}
	public static final class ExtraDataList extends ArrayList<ExtraSaveData>
	{
		public ExtraDataList(int count)
		{
			super(count);
		}
		public ExtraDataList()
		{
			super();
		}
		public ExtraDataList(Collection<ExtraSaveData> collection)
		{
			super(collection);
		}
		public <T extends ExtraSaveData> T getFirstExtraData(Class<T> tClass)
		{
			for (ExtraSaveData extraData : this)
			{
				if (tClass.isAssignableFrom(extraData.getClass()))
					return (T) extraData;
			}
			return null;
		}
		public ExtraDataList cloneList()
		{
			ExtraSaveData[] array = toArray(new ExtraSaveData[size()]);
			return new ExtraDataList(Arrays.stream(array).toList());
		}
	}
}