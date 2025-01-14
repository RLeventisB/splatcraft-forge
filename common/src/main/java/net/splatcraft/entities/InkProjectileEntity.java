package net.splatcraft.entities;

import com.google.common.reflect.TypeToken;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.splatcraft.blocks.ColoredBarrierBlock;
import net.splatcraft.blocks.StageBarrierBlock;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.handlers.DataHandler;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.*;
import net.splatcraft.registries.*;
import net.splatcraft.util.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class InkProjectileEntity extends ThrownItemEntity implements IColoredEntity
{
	private static final TrackedData<String> PROJ_TYPE = DataTracker.registerData(InkProjectileEntity.class, TrackedDataHandlerRegistry.STRING);
	private static final TrackedData<InkColor> COLOR = DataTracker.registerData(InkProjectileEntity.class, CommonUtils.INKCOLORDATAHANDLER);
	private static final TrackedData<Vector2f> PROJ_SIZE = DataTracker.registerData(InkProjectileEntity.class, CommonUtils.VEC2DATAHANDLER);
	private static final TrackedData<Float> GRAVITY = DataTracker.registerData(InkProjectileEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> STRAIGHT_SHOT_TIME = DataTracker.registerData(InkProjectileEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> SPEED = DataTracker.registerData(InkProjectileEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> HORIZONTAL_DRAG = DataTracker.registerData(InkProjectileEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> GRAVITY_SPEED_MULT = DataTracker.registerData(InkProjectileEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<ExtraDataList> EXTRA_DATA = DataTracker.registerData(InkProjectileEntity.class, ExtraSaveData.SERIALIZER);
	private static final TrackedData<Vector3f> SHOOT_DIRECTION = DataTracker.registerData(InkProjectileEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
	public static float MixinTimeDelta;
	public float lifespan = 600;
	public boolean explodes = false, bypassMobDamageMultiplier = false, canPierce = false, persistent = false;
	public ItemStack sourceWeapon = ItemStack.EMPTY;
	public float impactCoverage, dropImpactSize, distanceBetweenDrops;
	public float damageMultiplier = 1;
	public boolean causesHurtCooldown, throwerAirborne;
	public AbstractWeaponSettings<?, ?> damage = ShooterWeaponSettings.DEFAULT;
	public InkBlockUtils.InkType inkType;
	protected float straightShotTime = -1;
	private float accumulatedDrops;
	private AttackId attackId = AttackId.NONE;
	public InkProjectileEntity(EntityType<InkProjectileEntity> type, World world)
	{
		super(type, world);
	}
	public InkProjectileEntity(World world, LivingEntity thrower, InkColor color, InkBlockUtils.InkType inkType, float projectileSize, AbstractWeaponSettings<?, ?> damage, ItemStack sourceWeapon)
	{
		super(SplatcraftEntities.INK_PROJECTILE.get(), thrower, world);
		setColor(color);
		setProjectileSize(projectileSize);
		impactCoverage = projectileSize * 0.85f;
		throwerAirborne = !thrower.isOnGround();
		this.damage = damage;
		this.inkType = inkType;
		this.sourceWeapon = sourceWeapon;
	}
	public InkProjectileEntity(World world, LivingEntity thrower, InkColor color, InkBlockUtils.InkType inkType, float projectileSize, AbstractWeaponSettings<?, ?> damage)
	{
		this(world, thrower, color, inkType, projectileSize, damage, ItemStack.EMPTY);
	}
	public InkProjectileEntity(World world, LivingEntity thrower, ItemStack sourceWeapon, InkBlockUtils.InkType inkType, float projectileSize, AbstractWeaponSettings<?, ?> damage)
	{
		this(world, thrower, ColorUtils.getInkColor(sourceWeapon), inkType, projectileSize, damage, sourceWeapon);
	}
	public static void registerDataAccessors()
	{
		TrackedDataHandlerRegistry.register(SHOOT_DIRECTION.dataType());
		TrackedDataHandlerRegistry.register(PROJ_SIZE.dataType());
		TrackedDataHandlerRegistry.register(EXTRA_DATA.dataType());
	}
	public InkProjectileEntity setChargerStats(float charge, ChargerWeaponSettings.ChargerProjectileDataRecord settings)
	{
		dropImpactSize = settings.inkDropCoverage();
		distanceBetweenDrops = settings.distanceBetweenInkDrops();
		if (distanceBetweenDrops > 0)
			accumulatedDrops = CommonUtils.nextFloat(random, 0, 1);
		float range = charge == 1 ? settings.fullyChargedRange() : settings.minChargeRange() + (settings.maxChargeRange() - settings.minChargeRange()) * charge;
		lifespan = range / settings.size();
		impactCoverage = settings.inkCoverageImpact();
		
		setGravity(0);
		canPierce = charge >= settings.piercesAtCharge();
		setProjectileType(Types.CHARGER);
		return this;
	}
	public InkProjectileEntity setBlasterStats(BlasterWeaponSettings settings)
	{
		setCommonProjectileStats(settings.projectileData);
		explodes = true;
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
	public InkProjectileEntity setSplatlingStats(SplatlingWeaponSettings settings, float charge)
	{
		CommonRecords.ProjectileDataRecord projectileData = charge > 1 ? settings.secondChargeLevelProjectile : settings.firstChargeLevelProjectile;
		
		setCommonProjectileStats(projectileData);
		addExtraData(new ExtraSaveData.ChargeExtraData(charge));
		setProjectileType(Types.SHOOTER);
		return this;
	}
	public InkProjectileEntity setDualieStats(CommonRecords.ProjectileDataRecord settings)
	{
		setCommonProjectileStats(settings);
		setProjectileType(Types.SHOOTER);
		return this;
	}
	public InkProjectileEntity setRollerSwingStats(RollerWeaponSettings settings)
	{
		setProjectileType(Types.ROLLER);
		
		if (throwerAirborne)
		{
			accumulatedDrops = 1;
			return setRollerProjectileStats(settings.flingData.projectileData());
		}
		return setRollerProjectileStats(settings.swingData.projectileData());
	}
	public InkProjectileEntity setCommonProjectileStats(CommonRecords.ProjectileDataRecord settings)
	{
		dropImpactSize = settings.inkDropCoverage();
		distanceBetweenDrops = settings.distanceBetweenInkDrops();
		if (distanceBetweenDrops > 0)
			accumulatedDrops = CommonUtils.nextFloat(random, 0, 1);
		impactCoverage = settings.inkCoverageImpact();
		
		setProjectileVisualSize(settings.visualSize());
		setGravity(settings.gravity());
		setStraightShotTime(settings.straightShotTicks());
		
		lifespan = settings.lifeTicks();
		setHorizontalDrag(settings.horizontalDrag());
		setGravitySpeedMult(settings.delaySpeedMult());
		
		return this;
	}
	public InkProjectileEntity setRollerProjectileStats(RollerWeaponSettings.RollerProjectileDataRecord settings)
	{
		dropImpactSize = settings.inkDropCoverage();
		distanceBetweenDrops = settings.distanceBetweenInkDrops();
		impactCoverage = settings.inkCoverageImpact();
		
		setProjectileVisualSize(settings.visualSize());
		setGravity(settings.gravity());
		setStraightShotTime(settings.straightShotTicks());
		
		lifespan = 600;
		setHorizontalDrag(settings.horizontalDrag());
		setGravitySpeedMult(settings.delaySpeedMult());
		
		addExtraData(new ExtraSaveData.RollerDistanceExtraData(getPos().toVector3f()));
		
		return this;
	}
	@Override
	protected void initDataTracker(DataTracker.Builder builder)
	{
		super.initDataTracker(builder);
		builder.add(PROJ_TYPE, Types.SHOOTER);
		builder.add(COLOR, ColorUtils.getDefaultColor());
		builder.add(PROJ_SIZE, new Vector2f(0.2F, 0.6F));
		builder.add(GRAVITY, 0.175F);
		builder.add(STRAIGHT_SHOT_TIME, 0F);
		builder.add(SPEED, 0f);
		builder.add(HORIZONTAL_DRAG, 1F);
		builder.add(GRAVITY_SPEED_MULT, 1F);
		builder.add(EXTRA_DATA, new ExtraDataList());
		builder.add(SHOOT_DIRECTION, new Vector3f(0, 0, 0));
	}
	@Override
	public void onTrackedDataSet(TrackedData<?> data)
	{
		if (PROJ_SIZE.equals(data))
			calculateDimensions();
		else if (STRAIGHT_SHOT_TIME.equals(data))
			straightShotTime = dataTracker.get(STRAIGHT_SHOT_TIME);
		
		super.onTrackedDataSet(data);
	}
	@Override
	protected @NotNull Item getDefaultItem()
	{
		return SplatcraftItems.splattershot.get();
	}
	@Override
	public void tick()
	{
		tick(SplatcraftGameRules.getIntRuleValue(getWorld(), SplatcraftGameRules.INK_PROJECTILE_FREQUENCY) / 100f);
	}
	public void tick(float timeDelta)
	{
		if (timeDelta > lifespan)
			timeDelta = lifespan;
		
		Vec3d lastPosition = getPos();
		Vec3d velocity = getShootVelocity(timeDelta); // guess we're doing vector3f now
		setVelocity(velocity.x, velocity.y, velocity.z);
		
		MixinTimeDelta = timeDelta;
		super.tick();
		
		straightShotTime -= timeDelta;
		if (isSubmergedInWater())
		{
			discard();
			return;
		}
		
		if (isRemoved())
		{
			Vec3d nextPosition = lastPosition.add(velocity);
			double frame = CommonUtils.getDeltaBetweenVectors(getPos(), lastPosition, nextPosition, 0.5);
			setVelocity(getVelocity().multiply(frame));
			calculateDrops(lastPosition, (float) frame);
			return;
		}
		
		if (!getWorld().isClient() && !persistent && (lifespan -= timeDelta) <= 0)
		{
			ExtraSaveData.ExplosionExtraData explosionData = getExtraDatas().getFirstExtraData(ExtraSaveData.ExplosionExtraData.class);
			if (Objects.equals(getProjectileType(), Types.BLASTER) && explosionData != null)
			{
				InkExplosion.createInkExplosion(getOwner(), getPos(), explosionData.explosionPaint, explosionData.getRadiuses(false, damageMultiplier), inkType, sourceWeapon, AttackId.NONE);
				createDrop(getX(), getY(), getZ(), 0, explosionData.explosionPaint, timeDelta);
				getWorld().sendEntityStatus(this, (byte) 3);
				getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterExplosion, SoundCategory.PLAYERS, 0.8F, CommonUtils.nextTriangular(getWorld().getRandom(), 0.95F, 0.095F));
			}
			else
			{
				InkExplosion.createInkExplosion(getOwner(), getPos(), impactCoverage, 0, 0, inkType, sourceWeapon);
			}
			calculateDrops(lastPosition, timeDelta);
			discard();
		}
		else if (dropImpactSize > 0)
		{
			if (!isInvisible())
			{
				getWorld().sendEntityStatus(this, (byte) 1);
			}
			calculateDrops(lastPosition, timeDelta);
		}
	}
	private void calculateDrops(Vec3d lastPosition, float timeDelta)
	{
		if (distanceBetweenDrops == 0)
		{
			createDrop(getX(), getY(), getZ(), 0, timeDelta);
			return;
		}
		Vec3d deltaMovement = getVelocity();
		float dropsTravelled = (float) deltaMovement.length() / distanceBetweenDrops;
		if (dropsTravelled > 0)
		{
			accumulatedDrops += dropsTravelled;
			while (accumulatedDrops >= 1)
			{
				accumulatedDrops -= 1;
				
				float progress = accumulatedDrops / dropsTravelled;
				Vec3d dropPos = lastPosition.lerp(getPos(), progress);
				
				createDrop(dropPos.x, dropPos.y, dropPos.z, progress, timeDelta);
			}
		}
	}
	public void createDrop(double dropX, double dropY, double dropZ, double extraFrame, float timeDelta)
	{
		createDrop(dropX, dropY, dropZ, extraFrame, dropImpactSize, timeDelta);
	}
	public void createDrop(double dropX, double dropY, double dropZ, double extraFrame, float dropImpactSize, float timeDelta)
	{
		InkDropEntity proj = new InkDropEntity(getWorld(), this, getColor(), inkType, dropImpactSize);
		proj.refreshPositionAfterTeleport(dropX, dropY, dropZ);
		getWorld().spawnEntity(proj);
		proj.tick((float) (extraFrame + timeDelta));
	}
	private Vec3d getShootVelocity(float timeDelta)
	{
		Vector3f shootDirection = getShotDirection();
		float frame = getMaxStraightShotTime() - straightShotTime;
		float[] speedData = getSpeed(frame, getMaxStraightShotTime(), timeDelta);
		Vec3d velocity = new Vec3d(shootDirection.x * speedData[0], shootDirection.y * speedData[0], shootDirection.z * speedData[0]);
		if (speedData[1] <= 0)
			return velocity;
		return velocity.subtract(0, (float) (getGravity() * speedData[1]), 0);
	}
	public float[] getSpeed(float frame, float straightShotFrame, float timeDelta)
	{
		float speed = dataTracker.get(SPEED);
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
		Vec3d motion = getVelocity();
		
		if (!Vec3d.ZERO.equals(motion))
		{
			float pitch = (float) (MathHelper.atan2(motion.y, motion.horizontalLength()) * MathHelper.DEGREES_PER_RADIAN);
			float yaw = (float) (MathHelper.atan2(motion.x, motion.z) * MathHelper.DEGREES_PER_RADIAN);
			if (firstUpdate)
			{
				setPitch(pitch);
				setYaw(yaw);
				prevPitch = pitch;
				prevYaw = yaw;
			}
			else
			{
				setPitch(updateRotation(prevPitch, pitch));
				setYaw(updateRotation(prevYaw, yaw));
			}
		}
	}
	@Override
	public void handleStatus(byte id)
	{
		super.handleStatus(id);
		switch (id)
		{
			case -1 ->
				getWorld().addParticle(new InkExplosionParticleData(getColor(), .5f), getX(), getY(), getZ(), 0, 0, 0);
			case 1 ->
			{
				if (getProjectileType().equals(Types.CHARGER))
					getWorld().addParticle(new InkSplashParticleData(getColor(), getProjectileSize() * 0.4f), getX() - getVelocity().x * 0.25D, getY() + getHeight() * 0.5f - getVelocity().y * 0.25D, getZ() - getVelocity().z * 0.25D, 0, -0.1, 0);
				else
					getWorld().addParticle(new InkSplashParticleData(getColor(), getProjectileSize() * 0.4f), getX() - getVelocity().x * 0.25D, getY() + getHeight() * 0.5f - getVelocity().y * 0.25D, getZ() - getVelocity().z * 0.25D, getVelocity().x, getVelocity().y, getVelocity().z);
			}
			case 2 ->
				getWorld().addParticle(new InkSplashParticleData(getColor(), getProjectileSize() * 2), getX(), getY(), getZ(), 0, 0, 0);
			case 3 ->
				getWorld().addParticle(new InkExplosionParticleData(getColor(), getProjectileSize() * 2), getX(), getY(), getZ(), 0, 0, 0);
		}
	}
	@Override
	protected void onEntityHit(@NotNull EntityHitResult result)
	{
		super.onEntityHit(result);
		
		Vec3d oldPos = getPos();
		if (canPierce)
			setPosition(oldPos);
		
		if (!getWorld().isClient())
		{
			Entity target = result.getEntity();
			float storedCrystalSoundIntensity = lastChimeIntensity;
			
			// idk vector math so i read https://discussions.unity.com/t/inverselerp-for-vector3/177038 for this
			// lol i didnt even use it
			
			Vec3d nextPosition = getPos().add(getVelocity());
			Vec3d impactPos = result.getPos();
			
			lastChimeIntensity = (float) CommonUtils.getDeltaBetweenVectors(impactPos, getPos(), nextPosition, 0.5);
			setPosition(impactPos);
			float dmg = damage.calculateDamage(this, getExtraDatas()) * damageMultiplier;
			lastChimeIntensity = storedCrystalSoundIntensity;
			
			if (target instanceof SpawnShieldEntity && !InkDamageUtils.canDamage(target, this))
			{
				discard();
				getWorld().sendEntityStatus(this, (byte) -1);
			}
			
			if (target instanceof LivingEntity livingTarget)
			{
				if (InkDamageUtils.isSplatted(livingTarget)) return;
				
				boolean didDamage = InkDamageUtils.doDamage(livingTarget, dmg, getOwner(), this, sourceWeapon, SplatcraftDamageTypes.INK_SPLAT, causesHurtCooldown, attackId);
				if (!getWorld().isClient && didDamage)
				{
					ExtraSaveData.ChargeExtraData chargeData = getExtraDatas().getFirstExtraData(ExtraSaveData.ChargeExtraData.class);
					if (Objects.equals(getProjectileType(), Types.CHARGER) && chargeData != null && chargeData.charge >= 1.0f && InkDamageUtils.isSplatted(livingTarget) && dmg > 20 ||
						Objects.equals(getProjectileType(), Types.BLASTER))
					{
						getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterDirect, SoundCategory.PLAYERS, 0.8F, 1);
					}
				}
			}
			
			if (!canPierce)
			{
				ExtraSaveData.ExplosionExtraData explosionData = getExtraDatas().getFirstExtraData(ExtraSaveData.ExplosionExtraData.class);
				if (explodes && explosionData != null)
				{
					InkExplosion.createInkExplosion(getOwner(), impactPos, explosionData.explosionPaint, explosionData.getRadiuses(false, damageMultiplier), inkType, sourceWeapon, explosionData.newAttackId ? AttackId.NONE : attackId);
					getWorld().sendEntityStatus(this, (byte) 3);
					getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterExplosion, SoundCategory.PLAYERS, 0.8F, CommonUtils.nextTriangular(getWorld().getRandom(), 0.95F, 0.095F));
				}
				else
					getWorld().sendEntityStatus(this, (byte) 2);
				
				discard();
			}
		}
	}
	@Override
	protected void onBlockHit(@NotNull BlockHitResult result)
	{
		if (getWorld().isClient())
		{
			super.onBlockHit(result);
			return;
		}
		
		if (InkBlockUtils.canInkPassthrough(getWorld(), result.getBlockPos()))
			return;
		
		if (getWorld().getBlockState(result.getBlockPos()).getBlock() instanceof ColoredBarrierBlock coloredBarrierBlock &&
			coloredBarrierBlock.canAllowThrough(result.getBlockPos(), this))
			return;
		
		super.onBlockHit(result);
		setPosition(result.getPos());
		if (getWorld().getBlockState(result.getBlockPos()).getBlock() instanceof StageBarrierBlock)
			getWorld().sendEntityStatus(this, (byte) -1);
		else
		{
			getWorld().sendEntityStatus(this, (byte) 2);
			Vec3d impactPos = InkExplosion.adjustPosition(result.getPos(), result.getSide().getUnitVector());
			ExtraSaveData.ExplosionExtraData explosionData = getExtraDatas().getFirstExtraData(ExtraSaveData.ExplosionExtraData.class);
			if (explodes && explosionData != null)
			{
				InkExplosion.createInkExplosion(getOwner(), impactPos, explosionData.explosionPaint, explosionData.getRadiuses(true, damageMultiplier), inkType, sourceWeapon, explosionData.newAttackId ? AttackId.NONE : attackId);
				getWorld().sendEntityStatus(this, (byte) 3);
//				getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterExplosion, SoundCategory.PLAYERS, 0.8F, CommonUtils.triangle(getWorld().getRandom(), 0.95F, 0.095F));
			}
			else
			{
				InkExplosion.createInkExplosion(getOwner(), impactPos, impactCoverage, 0, 0, inkType, sourceWeapon);
			}
		}
		if (!getWorld().isClient())
			discard();
	}
	@Override
	public void setVelocity(@NotNull Entity thrower, float pitch, float yaw, float pitchOffset, float velocity, float inaccuracy)
	{
		setVelocity(thrower, pitch, yaw, pitchOffset, velocity, inaccuracy, 0);
	}
	public void setVelocity(Entity thrower, float pitch, float yaw, float pitchOffset, float velocity, float inaccuracy, float partialTicks)
	{
		double f = -Math.sin(yaw * MathHelper.RADIANS_PER_DEGREE) * Math.cos(pitch * MathHelper.RADIANS_PER_DEGREE);
		double f1 = -Math.sin((pitch + pitchOffset) * MathHelper.RADIANS_PER_DEGREE);
		double f2 = Math.cos(yaw * MathHelper.RADIANS_PER_DEGREE) * Math.cos(pitch * MathHelper.RADIANS_PER_DEGREE);
		Vec3d posDiff = new Vec3d(0, 0, 0);
		
		try
		{
			posDiff = thrower.getLerpedPos(partialTicks).subtract(CommonUtils.getOldPosition(thrower, partialTicks));
			if (thrower.isOnGround())
				posDiff.multiply(1, 0, 1);
			posDiff = posDiff.multiply(0.8);
		}
		catch (NullPointerException ignored)
		{
		}
		
		f += posDiff.x;
		f1 += posDiff.y;
		f2 += posDiff.z;
		setVelocity(f, f1, f2, velocity, inaccuracy, partialTicks);
	}
	@Override
	public void setVelocity(double x, double y, double z, float velocity, float inaccuracy)
	{
		setVelocity(x, y, z, velocity, inaccuracy, 0);
	}
	public void setVelocity(double x, double y, double z, float velocity, float inaccuracy, float partialTicks)
	{
		// 1.34f is only to make the acurracy more aproppiate in minecraft since 5 degrees in splatoon isnt as impactful as 5 degrees in minecraft kinda
		float usedInaccuracy = inaccuracy * MathHelper.RADIANS_PER_DEGREE * 1.34f;
		Vec3d vec3 = new Vec3d(x, y, z)
			.rotateY((random.nextFloat() * 2f - 1f) * usedInaccuracy)
			.rotateX((random.nextFloat() * 2f - 1f) * usedInaccuracy * 0.5625f).normalize();
		
		dataTracker.set(SHOOT_DIRECTION, vec3.toVector3f());
		vec3 = vec3.multiply(velocity);
		
		velocityDirty = true;
		
		double d0 = vec3.horizontalLength();
		float yaw = (float) (MathHelper.atan2(vec3.x, vec3.z) * MathHelper.DEGREES_PER_RADIAN);
		float pitch = (float) (MathHelper.atan2(vec3.y, d0) * MathHelper.DEGREES_PER_RADIAN);
		setYaw(yaw);
		setPitch(pitch);
		prevYaw = yaw;
		prevPitch = pitch;
		setVelocity(vec3);
		
		dataTracker.set(SPEED, velocity);
	}
	@Override
	public void onCollision(HitResult result)
	{
		HitResult.Type rayType = result.getType();
		if (rayType == HitResult.Type.ENTITY)
		{
			onEntityHit((EntityHitResult) result);
		}
		else if (rayType == HitResult.Type.BLOCK)
		{
			onBlockHit((BlockHitResult) result);
		}
	}
	@Override
	public boolean canHit(@NotNull Entity entity)
	{
		return entity != getOwner() && entity.canBeHitByProjectile() && InkDamageUtils.canDamage(entity, dataTracker.get(COLOR));
	}
	@Override
	public void readCustomDataFromNbt(@NotNull NbtCompound nbt)
	{
		super.readCustomDataFromNbt(nbt);
		
		if (nbt.contains("Size"))
			setProjectileSize(nbt.getFloat("Size"));
		if (nbt.contains("VisualSize"))
			setProjectileVisualSize(nbt.getFloat("VisualSize"));
		
		impactCoverage = nbt.contains("ImpactCoverage") ? nbt.getFloat("ImpactCoverage") : getProjectileSize() * 0.85f;
		
		if (nbt.contains("Color"))
			setColor(InkColor.getFromNbt(nbt.get("Color")));
		
		dataTracker.set(SPEED, nbt.getFloat("Speed"));
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
		
		NbtList directionTag = nbt.getList("Direction", NbtFloat.FLOAT_TYPE);
		dataTracker.set(SHOOT_DIRECTION, new Vector3f(directionTag.getFloat(0), directionTag.getFloat(1), directionTag.getFloat(2)));
		
		distanceBetweenDrops = nbt.getFloat("TrailFrequency");
		dropImpactSize = nbt.getFloat("TrailSize");
		bypassMobDamageMultiplier = nbt.getBoolean("BypassMobDamageMultiplier");
		canPierce = nbt.getBoolean("CanPierce");
		explodes = nbt.getBoolean("Explodes");
		persistent = nbt.getBoolean("Persistent");
		causesHurtCooldown = nbt.getBoolean("CausesHurtCooldown");
		
		setInvisible(nbt.getBoolean("Invisible"));
		
		String type = nbt.getString("ProjectileType");
		setProjectileType(type.isEmpty() ? Types.DEFAULT : type);
		inkType = InkBlockUtils.InkType.IDENTIFIER_MAP.getOrDefault(Identifier.of(nbt.getString("InkType")), InkBlockUtils.InkType.NORMAL);
		
		sourceWeapon = ItemStack.fromNbtOrEmpty(getRegistryManager(), nbt.getCompound("SourceWeapon"));
		
		AbstractWeaponSettings<?, ?> settings = DataHandler.WeaponStatsListener.SETTINGS.get(Identifier.of(nbt.getString(nbt.getString("Settings"))));
		if (settings != null)
			damage = settings;
		else if (sourceWeapon.getItem() instanceof WeaponBaseItem<?> weapon)
			damage = weapon.getSettings(sourceWeapon);
		if (nbt.contains("AttackId"))
			attackId = AttackId.readNbt(nbt.getCompound("AttackId"));
	}
	@Override
	public void writeCustomDataToNbt(NbtCompound nbt)
	{
		nbt.putFloat("Size", getProjectileSize());
		nbt.putFloat("VisualSize", getProjectileVisualSize());
		nbt.put("Color", getColor().getNbt());
		
		nbt.putFloat("Speed", dataTracker.get(SPEED));
		nbt.putFloat("HorizontalDrag", getHorizontalDrag());
		nbt.putFloat("GravitySpeedMult", getGravitySpeedMult());
		nbt.putFloat("MaxStraightShotTime", getMaxStraightShotTime());
		nbt.putFloat("StraightShotTime", straightShotTime);
		
		NbtList directionTag = new NbtList();
		Vector3f direction = getShotDirection();
		directionTag.add(NbtFloat.of(direction.x));
		directionTag.add(NbtFloat.of(direction.y));
		directionTag.add(NbtFloat.of(direction.z));
		nbt.put("Direction", directionTag);
		
		nbt.putDouble("Gravity", getGravity());
		nbt.putFloat("Lifespan", lifespan);
		nbt.putFloat("TrailSize", dropImpactSize);
		nbt.putFloat("TrailFrequency", distanceBetweenDrops);
		nbt.putBoolean("BypassMobDamageMultiplier", bypassMobDamageMultiplier);
		nbt.putBoolean("CanPierce", canPierce);
		nbt.putBoolean("Explodes", explodes);
		nbt.putBoolean("Persistent", persistent);
		nbt.putBoolean("CausesHurtCooldown", causesHurtCooldown);
		
		nbt.putBoolean("Invisible", isInvisible());
		
		nbt.putString("ProjectileType", getProjectileType());
		nbt.putString("InkType", inkType.getSerializedName());
		nbt.put("SourceWeapon", sourceWeapon.encode(getWorld().getRegistryManager()));
		if (attackId != AttackId.NONE)
			nbt.put("AttackId", attackId.serializeNbt());
		
		super.writeCustomDataToNbt(nbt);
		nbt.remove("Item");
	}
	private @NotNull Float getGravitySpeedMult()
	{
		return dataTracker.get(GRAVITY_SPEED_MULT);
	}
	private void setGravitySpeedMult(float gravitySpeedMult)
	{
		dataTracker.set(GRAVITY_SPEED_MULT, gravitySpeedMult);
	}
	private @NotNull Float getHorizontalDrag()
	{
		return dataTracker.get(HORIZONTAL_DRAG);
	}
	private void setHorizontalDrag(float horizontalDrag)
	{
		dataTracker.set(HORIZONTAL_DRAG, horizontalDrag);
	}
	/**
	 * @return a exposed list to the synched list that manages the extra data sent to the connection or smtinh
	 * @apiNote this list if updated isnt going to be synched!!!!! for this use addExtraData instead, or sendExtraData
	 */
	public ExtraDataList getExtraDatas()
	{
		return dataTracker.get(EXTRA_DATA);
	}
	public void setExtraDataList(ExtraDataList list)
	{
		dataTracker.set(EXTRA_DATA, list);
	}
	public void addExtraData(ExtraSaveData data)
	{
		ExtraDataList list = dataTracker.get(EXTRA_DATA);
		list.add(data);
		dataTracker.set(EXTRA_DATA, list);
	}
	@Override
	public @NotNull ItemStack getStack()
	{
		return sourceWeapon;
	}
	@Override
	public @NotNull EntityDimensions getDimensions(@NotNull EntityPose pose)
	{
		return super.getDimensions(pose).scaled(getProjectileSize() / 2f);
	}
	@Override
	public double getGravity()
	{
		return dataTracker.get(GRAVITY);
	}
	public void setGravity(float gravity)
	{
		dataTracker.set(GRAVITY, gravity);
	}
	public double getStraightShotTime()
	{
		return straightShotTime;
	}
	public void setStraightShotTime(float time)
	{
		dataTracker.set(STRAIGHT_SHOT_TIME, time);
	}
	public float calculateDamageDecay(float baseDamage, float startTick, float decayPerTick, float minDamage)
	{
		// getMaxStraightShotTime() - straightShotTime is just age but it counts the partial ticks too (and time delta!!! yay i hate myself)
		double age = getMaxStraightShotTime() - straightShotTime + lastChimeIntensity;
		
		double diff = age - startTick;
		if (diff < 0)
			return baseDamage;
		return (float) Math.max(minDamage, baseDamage - decayPerTick * diff);
	}
	public float getMaxStraightShotTime()
	{
		return dataTracker.get(STRAIGHT_SHOT_TIME);
	}
	public Vector3f getShotDirection()
	{
		return new Vector3f(dataTracker.get(SHOOT_DIRECTION));
	}
	@Override
	public boolean hasNoGravity()
	{
//        return straightShotTime > 0 || getGravity() == 0 || super.isNoGravity();
		return true;
	}
	public float getProjectileSize()
	{
		return dataTracker.get(PROJ_SIZE).x;
	}
	public void setProjectileSize(float size)
	{
		dataTracker.set(PROJ_SIZE, new Vector2f(size, size * 3));
		refreshPosition();
		calculateDimensions();
	}
	public float getProjectileVisualSize()
	{
		return dataTracker.get(PROJ_SIZE).y;
	}
	public void setProjectileVisualSize(float visualSize)
	{
		dataTracker.set(PROJ_SIZE, new Vector2f(getProjectileSize(), visualSize));
	}
	@Override
	public void remove(@NotNull RemovalReason pReason)
	{
		attackId.projectileRemoved();
		super.remove(pReason);
	}
	@Deprecated //Modify sourceWeapon variable instead
	@Override
	public void setItem(@NotNull ItemStack itemStack)
	{
	}
	@Override
	public InkColor getColor()
	{
		return dataTracker.get(COLOR);
	}
	@Override
	public void setColor(InkColor color)
	{
		dataTracker.set(COLOR, color);
	}
	public String getProjectileType()
	{
		return dataTracker.get(PROJ_TYPE);
	}
	public void setProjectileType(String v)
	{
		dataTracker.set(PROJ_TYPE, v);
	}
	public Random getRandom()
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
			TypeToken<T> token = TypeToken.of(tClass);
			
			for (ExtraSaveData extraData : this)
			{
				if (token.isSupertypeOf(TypeToken.of(extraData.getClass())))
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