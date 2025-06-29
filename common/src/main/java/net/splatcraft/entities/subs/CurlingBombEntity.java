package net.splatcraft.entities.subs;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.entities.ObjectCollideListenerEntity;
import net.splatcraft.items.weapons.settings.SubWeaponRecords.CurlingBombDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.mixin.accessors.EntityAccessor;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class CurlingBombEntity extends AbstractSubWeaponEntity<CurlingBombDataRecord> implements ObjectCollideListenerEntity
{
	private static final EntityDataAccessor<Integer> INIT_FUSE_TIME = SynchedEntityData.defineId(CurlingBombEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> COOK_SCALE = SynchedEntityData.defineId(CurlingBombEntity.class, EntityDataSerializers.FLOAT);
	public int fuseTime = 0;
	public int prevFuseTime = 0;
	public float bladeRot = 0;
	public float prevBladeRot = 0;
	private boolean playedActivationSound = false;
	public CurlingBombEntity(EntityType<? extends AbstractSubWeaponEntity<CurlingBombDataRecord>> type, Level world)
	{
		super(type, world);
	}
	@Override
	public float maxUpStep()
	{
		return .7f;
	}
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(INIT_FUSE_TIME, 0);
		builder.define(COOK_SCALE, 0f);
	}
	@Override
	protected Item getDefaultItem()
	{
		return SplatcraftItems.curlingBomb.get();
	}
	@Override
	public void tick()
	{
		SubWeaponSettings<CurlingBombDataRecord> settings = getSettings();

		double spd = getDeltaMovement().horizontalDistance();
		prevBladeRot = bladeRot;
		bladeRot += (float) spd;

		prevFuseTime = fuseTime;
		fuseTime--;

		CurlingBombDataRecord curlingData = settings.subDataRecord;
		boolean slowingDown = fuseTime <= curlingData.warningFrame();
		if (slowingDown && !playedActivationSound)
		{
			level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonating, SoundSource.PLAYERS, 0.8F, 1f);
			playedActivationSound = true;
		}

		if (fuseTime <= 0)
		{
			Vec3 center = getBoundingBox().getCenter();
			explode(curlingData, center);
			return;
		}

		if (!level().isClientSide())
		{
			doTrail(spd > 1.0E-3, settings);
		}

		applyFloorFriction(slowingDown);
		updateRotation();

		if (spd > 0.01 && fuseTime % (int) Math.max(1, (1 - spd) * 10) == 0)
		{
			level().broadcastEntityEvent(this, (byte) 2);
		}

		superTick();

		if (isUnderWater())
		{
			level().broadcastEntityEvent(this, (byte) -1);
			discard();
		}

		processMovement(settings);
	}
	private void processMovement(SubWeaponSettings<CurlingBombDataRecord> settings)
	{
		Vec3 deltaMovement = getDeltaMovement();
		Vec3 collidedDeltaMovement = ((EntityAccessor) this).invokeCollide(deltaMovement);
		if (!Mth.equal(deltaMovement.x, collidedDeltaMovement.x))
			deltaMovement = deltaMovement.with(Direction.Axis.X, reflectVelocity(Direction.Axis.X, deltaMovement));
		if (!Mth.equal(deltaMovement.z, collidedDeltaMovement.z))
			deltaMovement = deltaMovement.with(Direction.Axis.Z, reflectVelocity(Direction.Axis.Z, deltaMovement));

		if (deltaMovement.y != collidedDeltaMovement.y)
		{
			if (deltaMovement.y < collidedDeltaMovement.y && collidedDeltaMovement.y <= 0)
			{
				if (deltaMovement.y < -0.9)
					deltaMovement = deltaMovement.with(Direction.Axis.Y, reflectVelocity(Direction.Axis.Y, deltaMovement));
				else
					deltaMovement = deltaMovement.with(Direction.Axis.Y, 0);
			}
		}

		Pair<EntityHitResult, Direction> hitResult = getEntityHit(position(), (deltaMovement));
		if (hitResult != null)
			deltaMovement = onHitEntity(hitResult.getFirst(), deltaMovement, settings.subDataRecord.bounceOnEntityHit() ? hitResult.getSecond() : null);

		setDeltaMovement(deltaMovement);
		setPos(position().add(collidedDeltaMovement));

		applyGravity();
	}
	private Pair<EntityHitResult, Direction> getEntityHit(Vec3 startPos, Vec3 deltaMovement)
	{
		double minDistance = Double.MAX_VALUE;
		Direction dir = null;
		Entity entity = null;
		Vec3 endVec = startPos.add(deltaMovement);

		for (Entity entity1 : level().getEntities(this, getBoundingBox().expandTowards(deltaMovement).inflate(1.0f), this::canHitEntity))
		{
			AABB aabb = entity1.getBoundingBox().inflate(0.5);
			Optional<Pair<Vec3, Direction>> clipData = aabbClip(aabb, startPos, endVec);
			if (clipData.isPresent())
			{
				double distance = startPos.distanceToSqr(clipData.get().getFirst());
				if (distance < minDistance)
				{
					entity = entity1;
					dir = clipData.get().getSecond();
					minDistance = distance;
				}
			}
		}

		return entity == null ? null : Pair.of(new EntityHitResult(entity), dir);
	}
	@Override
	protected boolean canHitEntity(Entity target)
	{
		return super.canHitEntity(target) && getOwner() != target;
	}
	private Optional<Pair<Vec3, Direction>> aabbClip(AABB aabb, Vec3 from, Vec3 to)
	{
		double[] adouble = new double[]{(double) 1.0F};
		double d0 = to.x - from.x;
		double d1 = to.y - from.y;
		double d2 = to.z - from.z;
		Direction direction = AABB.getDirection(aabb, from, adouble, null, d0, d1, d2);
		if (direction == null)
		{
			return Optional.empty();
		}
		else
		{
			double d3 = adouble[0];
			return Optional.of(Pair.of(from.add(d3 * d0, d3 * d1, d3 * d2), direction));
		}
	}
	private void applyFloorFriction(boolean slowingDown)
	{
		float horizontalFriction = 1f;
		if (onGround())
			horizontalFriction = level().getBlockState(getOnPos()).getBlock().getFriction() / 0.6f;
		if (slowingDown)
		{
			horizontalFriction *= 0.8f;
		}
		horizontalFriction = Mth.clamp(horizontalFriction, 0, 1);
		setDeltaMovement(getDeltaMovement().multiply(horizontalFriction, 1f, horizontalFriction));
	}
	public void explode(CurlingBombDataRecord settings, Vec3 impactPos)
	{
		if (!level().isClientSide())
		{
			InkExplosion.createInkExplosion(getOwner(), impactPos, settings.inkExplosionRange().getValue(getCookProgress()), settings.damageRanges().withShift(-getCookProgress() * settings.maxCookRadiusBonus()), inkType, sourceWeapon, AttackId.NONE);
			InkExplosion.doSplashes(getOwner(), impactPos, settings.inkSplashes(), getColor(), inkType);
			level().broadcastEntityEvent(this, (byte) 1);
			discard();
		}
		level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundSource.PLAYERS, 0.8F, CommonUtils.nextTriangular(level().getRandom(), 0.95F, 0.095F));
	}
	@Override
	public void handleMovement()
	{
		setDeltaMovement(((EntityAccessor) this).invokeCollide(getDeltaMovement()));
		setPos(position().add(getDeltaMovement()));
	}
	@Override
	public float getFriction()
	{
		return 1;
	}
	private void doTrail(boolean fastEnough, SubWeaponSettings<CurlingBombDataRecord> settings)
	{
		float trailWidth = settings.subDataRecord.trailSizeRange().getValue(getCookProgress());
		float trailStep = CommonUtils.calculateStep(trailWidth, 0.7071067811865475f);
		if (fastEnough)
		{
			for (float j = -trailWidth; j <= trailWidth; j += trailStep)
			{
				Vec3 normalized = getDeltaMovement().multiply(1, 0, 1).normalize();
				double sideX = -normalized.z;
				double sideZ = normalized.x;
				for (int i = 1; i <= 2; i++)
				{
					double y = getBlockY() - i;
					BlockPos side = BlockPos.containing(Math.floor(getX() + sideX * j), y, Math.floor(getZ() + sideZ * j));
					if (InkBlockUtils.canInkFromFace(level(), side, Direction.UP))
					{
						BlockInkedResult result = InkBlockUtils.inkBlock(getOwner(), level(), side, getColor(), Direction.UP, inkType, settings.subDataRecord.contactDamage());
						if (result == BlockInkedResult.SUCCESS)
							InkBlockUtils.awardTurfPoints((LivingEntity) getOwner(), sourceWeapon, 1);
						break;
					}
				}
			}
		}
		else
		{
			for (int i = 0; i <= 2; i++)
				if (InkBlockUtils.canInkFromFace(level(), blockPosition().below(i), Direction.UP))
				{
					InkBlockUtils.inkBlock(getOwner(), level(), blockPosition().below(i), getColor(), Direction.UP, inkType, settings.subDataRecord.contactDamage());
					break;
				}
		}
	}
	@Override
	public void handleEntityEvent(byte id)
	{
		super.handleEntityEvent(id);
		float maxDistance = getSettings().subDataRecord.damageRanges().getMaxDistance();
		if (id == 1)
		{
			level().addAlwaysVisibleParticle(new InkExplosionParticleData(getColor(), (maxDistance + getCookProgress()) * 2), getX(), getY(), getZ(), 0, 0, 0);
		}
		if (id == 2)
		{
			level().addParticle(new InkSplashParticleData(getColor(), 1.175f), getX(), getY() + 0.4, getZ(), 0, 0, 0);
		}
	}
	//Ripped and modified from Minestuck's BouncingProjectileEntity class (with permission)
	protected Vec3 onHitEntity(EntityHitResult result, Vec3 velocity, Direction hitDirecion)
	{
		if (result.getEntity() instanceof LivingEntity livingEntity)
		{
			InkDamageUtils.doRollDamage(livingEntity, getSettings().subDataRecord.contactDamage(), getOwner(), this, sourceWeapon);
		}

		if (hitDirecion == null)
			return velocity;

		if (hitDirecion.getAxis() == Direction.Axis.X)
			velocity = new Vec3(-velocity.x, velocity.y, velocity.z);
		if (Math.abs(velocity.y) >= .05 && Math.abs(velocity.y) >= Math.abs(velocity.x) && Math.abs(velocity.y) >= Math.abs(velocity.z))
			velocity = new Vec3(velocity.x, -velocity.y * .5, velocity.z);
		if (Math.abs(velocity.z) >= Math.abs(velocity.y) && Math.abs(velocity.z) >= Math.abs(velocity.x))
			velocity = new Vec3(velocity.x, velocity.y, -velocity.z);
		return velocity;
	}
	protected double reflectVelocity(Direction.Axis axis, Vec3 velocity)
	{
		double velocityX = velocity.x;
		double velocityY = velocity.y;
		double velocityZ = velocity.z;

		return switch (axis)
		{
			case X -> -velocityX;
			case Y -> -velocityY * .5;
			case Z -> -velocityZ;
		};
	}
	public float getFlashIntensity(float partialTicks)
	{
		SubWeaponSettings<CurlingBombDataRecord> settings = getSettings();
		if (settings.subDataRecord == null)
			return 0;

		if (fuseTime <= settings.subDataRecord.warningFrame())
		{
			return settings.subDataRecord.warningFrame() - Mth.lerpInt(partialTicks, prevFuseTime, fuseTime) * 0.85f;
		}
		return 0;
	}
	private boolean canStepUp(Vec3 p_20273_)
	{
		AABB box = getBoundingBox();
		List<VoxelShape> list = level().getEntityCollisions(this, box.expandTowards(p_20273_));
		Vec3 vec3 = p_20273_.lengthSqr() == 0.0D ? p_20273_ : collideBoundingBox(this, p_20273_, box, level(), list);
		boolean flag = p_20273_.x != vec3.x;
		boolean flag1 = p_20273_.y != vec3.y;
		boolean flag2 = p_20273_.z != vec3.z;
		boolean flag3 = onGround() || flag1 && p_20273_.y < 0.0D;
		float stepHeight = maxUpStep();
		if (stepHeight > 0.0F && flag3 && (flag || flag2))
		{
			Vec3 vec31 = collideBoundingBox(this, new Vec3(p_20273_.x, stepHeight, p_20273_.z), box, level(), list);
			Vec3 vec32 = collideBoundingBox(this, new Vec3(0.0D, stepHeight, 0.0D), box.expandTowards(p_20273_.x, 0.0D, p_20273_.z), level(), list);
			if (vec32.y < (double) stepHeight)
			{
				Vec3 vec33 = collideBoundingBox(this, new Vec3(p_20273_.x, 0.0D, p_20273_.z), box.move(vec32), level(), list).add(vec32);
				if (vec33.horizontalDistanceSqr() > vec31.horizontalDistanceSqr())
				{
					vec31 = vec33;
				}
			}

			return vec31.horizontalDistanceSqr() > vec3.horizontalDistanceSqr();
		}

		return false;
	}
	@Override
	public void readAdditionalSaveData(CompoundTag nbt)
	{
		super.readAdditionalSaveData(nbt);
		setInitialFuseTime(nbt.getInt("FuseTime"));
	}
	@Override
	public void addAdditionalSaveData(CompoundTag nbt)
	{
		super.addAdditionalSaveData(nbt);
		nbt.putInt("FuseTime", fuseTime);
	}
	public int getInitialFuseTime()
	{
		return entityData.get(INIT_FUSE_TIME);
	}
	public void setInitialFuseTime(int v)
	{
		entityData.set(INIT_FUSE_TIME, v);
	}
	public float getCookProgress()
	{
		return entityData.get(COOK_SCALE);
	}
	public void setCookScale(float v)
	{
		entityData.set(COOK_SCALE, v);
	}
	@Override
	public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> data)
	{
		if (INIT_FUSE_TIME.equals(data))
			fuseTime = getInitialFuseTime();

		super.onSyncedDataUpdated(data);
	}
	@Override
	public void onCollidedWithObjectEntity(Entity entity)
	{
		explode(getSettings().subDataRecord, getBoundingBox().getCenter());
	}
}
