package net.splatcraft.entities.subs;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.entities.IColoredEntity;
import net.splatcraft.entities.InkDropEntity;
import net.splatcraft.entities.ObjectCollideListenerEntity;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.mixin.accessors.EntityAccessor;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkDamageUtils;
import net.splatcraft.util.InkExplosion;
import net.splatcraft.util.structs.AttackId;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static net.splatcraft.items.weapons.settings.SubWeaponRecords.TorpedoDataRecord;

public class TorpedoEntity extends AbstractSubWeaponEntity<TorpedoDataRecord> implements ObjectCollideListenerEntity, IBouncyEntity, IColoredEntity
{
	private static final Vec3 REFLECTION_COEFFICIENT = new Vec3(
		-0.9, -0.3, -0.9
	);
	private static final EntityDataAccessor<Optional<Vec3>> TARGET_POSITION = SynchedEntityData.defineId(TorpedoEntity.class, CommonUtils.OPTIONAL_VEC3_DATA_HANDLER);
	private static final EntityDataAccessor<Integer> SEARCH_DELAY = SynchedEntityData.defineId(TorpedoEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> COUNTER = SynchedEntityData.defineId(TorpedoEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(TorpedoEntity.class, EntityDataSerializers.FLOAT);
	private boolean playedActivationSound;
	public TorpedoEntity(EntityType<? extends AbstractSubWeaponEntity<TorpedoDataRecord>> type, Level world)
	{
		super(type, world);
	}
	@Override
	public void tick()
	{
		searchEnemies();
		
		SubWeaponSettings<TorpedoDataRecord> settings = getSettings();
		int counter = entityData.get(COUNTER);
		if (isLockedIn())
		{
			if (counter == 0) // start moving
			{
				Vec3 movementVector = getTargetPosition().get()
					.subtract(position())
					.normalize()
					.scale(settings.subDataRecord.moveSpeed()
					);
				setDeltaMovement(movementVector);
			}
			if (counter > -1)
				counter--;
			else
				// walkDist is for horizontal movement but might as well use one of the 4033 unused default variables from the entity class
				walkDist += (float) getDeltaMovement().length();
		}
		else
		{
			if (onGround())
			{
				counter++;
			}
			
			if (counter >= settings.subDataRecord.fuseTime() && isAlive())
			{
				explode(settings, getBoundingBox().getCenter());
				return;
			}
			else if (onGround() && !playedActivationSound && counter >= settings.subDataRecord.fuseTime() - 18)
			{
				level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonating, SoundSource.PLAYERS, 0.8F, 1f);
				playedActivationSound = true;
			}
		}
		if (!level().isClientSide())
			entityData.set(COUNTER, counter);
		
		int searchDelay = getSearchDelay();
		if (searchDelay > 0)
			setSearchDelay(searchDelay - 1);
		
		super.tick();
	}
	@Override
	public double getDefaultGravity()
	{
		return 0.13;
	}
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(TARGET_POSITION, Optional.empty());
		builder.define(SEARCH_DELAY, 0);
		builder.define(COUNTER, 0);
		builder.define(HEALTH, 0f);
		
		super.defineSynchedData(builder);
	}
	private void searchEnemies()
	{
		if (isLockedIn() || getSearchDelay() > 0 || level().isClientSide() || onGround())
			return;
		
		Vector2f searchRange = getSearchRange();
		AABB area = AABB.ofSize(getBoundingBox().getCenter(), searchRange.x * 2, searchRange.y * 2, searchRange.x * 2);
		List<LivingEntity> nearbyEntities = level().getEntitiesOfClass(LivingEntity.class, area,
			entity ->
				InkDamageUtils.canDamage(entity, getColor()) &&
					isInDistance(entity.getBoundingBox(), searchRange) &&
					entity.hasLineOfSight(this) &&
					!InkDamageUtils.isSplatted(entity)
		);
		if (nearbyEntities.isEmpty())
		{
			setSearchDelay(1);
			return;
		}
		
		final Comparator<LivingEntity> comparator = Comparator.comparingDouble(v -> v.distanceToSqr(this));
		
		LivingEntity closestEntity = nearbyEntities
			.stream().min(comparator).get();
		
		setDeltaMovement(0, 0, 0);
		syncPacketPositionCodec(getX(), getY(), getZ());
		if (level().getChunkSource() instanceof ServerChunkCache serverChunkManager)
		{
			serverChunkManager.chunkMap.entityMap.get(getId()).broadcast(new ClientboundTeleportEntityPacket(this));
		}
		
		// todo: fix bug where sometimes if the torpedo transforms inside of an entity, it doesnt collide with it
		
		entityData.set(TARGET_POSITION, Optional.of(closestEntity.getBoundingBox().getCenter()));
		entityData.set(COUNTER, getSettings().subDataRecord.movementDelay());
	}
	private boolean isInDistance(AABB box, Vector2f searchRange)
	{
		Vec3 closest = CommonUtils.limitTo(box, position());
		double dX = getX() - closest.x;
		double dY = getY() - closest.y;
		double dZ = getZ() - closest.z;
		Vector2f squared = searchRange.mul(searchRange, new Vector2f());
		return (dX * dX + dZ * dZ) / squared.y + (dY * dY) / squared.x <= 1;
	}
	@Override
	public void handleMovement()
	{
		Vec3 oldDeltaMovement = getDeltaMovement();
		
		Pair<Vec3, Vec3> collidedAndNewVelocity = doBounceLogic(this, getDeltaMovement(), this::canHitEntity, false);
		
		setDeltaMovement(collidedAndNewVelocity.getSecond());
		setPos(position().add(collidedAndNewVelocity.getFirst()));
		
		if (collidedAndNewVelocity.getFirst().subtract(collidedAndNewVelocity.getSecond()).lengthSqr() > 10e-3)
		{
			setSearchDelay(5);
		}
		if (isLockedIn())
		{
			setOnGround(false);
		}
		else
		{
			setOnGroundWithMovement(oldDeltaMovement.y < collidedAndNewVelocity.getFirst().y && oldDeltaMovement.y < 0, collidedAndNewVelocity.getFirst());
		}
	}
	@Override
	protected void applyGravity()
	{
		if (isLockedIn())
			return;
		
		super.applyGravity();
	}
	@Override
	protected boolean canHitEntity(Entity target)
	{
		return isLockedIn() && super.canHitEntity(target);
	}
	protected void onHitEntity(@NotNull EntityHitResult result)
	{
		super.onHitEntity(result);
		if (!isLockedIn())
			return;
		
		explode(getSettings(), result.getLocation());
		summonDroplets(getSettings(), result.getLocation());
	}
	private void summonDroplets(SubWeaponSettings<TorpedoDataRecord> settings, Vec3 location)
	{
		// hello this is InkExplosion.doSplashes but the constructor is different
		Entity owner = getOwner();
		AttackId dropletAttackId = AttackId.registerAttack();
		SubWeaponSettings.SplashAroundDataRecord splashData = settings.subDataRecord.dropletData();
		InkColor color = getColor();
		if (getOwner() == null)
			return;
		
		Function<Integer, Float> yawGetter = splashData.distributeEvenly() ?
			(count) -> (float) count / splashData.splashCount() :
			(count) -> level().getRandom().nextFloat();
		
		for (int i = 0; i < splashData.splashCount(); i++)
		{
			float yaw = yawGetter.apply(i) * Mth.TWO_PI;
			float pitch = -splashData.splashPitchRange().getValue(random.nextFloat()) * Mth.DEG_TO_RAD;
			float speed = splashData.splashVelocityRange().getValue(random.nextFloat());
			InkDropEntity drop = new InkDropEntity(level(), location, owner, color, inkType, splashData.splashPaintRadius(), sourceWeapon);
			drop.setExplosionData(settings.subDataRecord.dropletDamageRange(), dropletAttackId);
			float f = -Mth.sin(yaw) * Mth.cos(pitch);
			float g = -Mth.sin(pitch);
			float h = Mth.cos(yaw) * Mth.cos(pitch);
			drop.shoot(f, g, h, speed, 0);
			
			level().addFreshEntity(drop);
		}
	}
	@Override
	protected void onHitBlock(@NotNull BlockHitResult result)
	{
		if (!isLockedIn())
			return;
		
		SubWeaponSettings<TorpedoDataRecord> settings = getSettings();
		Vec3 impactPos = InkExplosion.adjustPosition(result.getLocation(), result.getDirection(), this);
		explode(settings, impactPos);
	}
	public void explode(SubWeaponSettings<TorpedoDataRecord> settings, Vec3 impactPos)
	{
		if (!level().isClientSide())
		{
			InkExplosion.createInkExplosion(getOwner(), impactPos, settings.subDataRecord.mainInkSplashRadius(), settings.subDataRecord.mainExplosionDamageRange(), inkType, sourceWeapon, AttackId.NONE);
			level().broadcastEntityEvent(this, (byte) 1);
			discard();
		}
		level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundSource.PLAYERS, 0.8F, CommonUtils.nextTriangular(level().getRandom(), 0.95F, 0.095F));
	}
	@Override
	public void handleEntityEvent(byte id)
	{
		super.handleEntityEvent(id);
		if (id == 1)
		{
			level().addAlwaysVisibleParticle(new InkExplosionParticleData(getColor(), getSettings().subDataRecord.mainExplosionDamageRange().getMaxKey() * 2), getX(), getY(), getZ(), 0, 0, 0);
		}
	}
	@Override
	public void updateRotation()
	{
		Vec3 vec3 = getDeltaMovement();
		float xRot = getXRot() + (float) vec3.length() / 3f;
		float yRot = (float) (Mth.atan2(vec3.x, vec3.z) * Mth.RAD_TO_DEG);
		if (tickCount == 1)
		{
			setYRot(yRot);
			setXRot(xRot);
			
			xRotO = xRot;
			yRotO = yRot;
		}
		else
		{
			if (isLockedIn())
			{
				int counter = entityData.get(COUNTER);
				if (counter > 0)
				{
					vec3 = getTargetPosition().get().subtract(position());
					if (vec3.lengthSqr() > 0)
					{
						xRot = (float) Mth.atan2(vec3.y, vec3.horizontalDistance()) * Mth.RAD_TO_DEG;
						yRot = (float) (Mth.atan2(vec3.x, vec3.z) * Mth.RAD_TO_DEG);
						
						setYRot(lerpRotation(yRotO, yRot));
						setXRot(lerpRotation(xRotO, xRot));
					}
				}
			}
			else
			{
				setYRot(lerpRotation(yRotO, yRot));
				setXRot(xRot);
			}
		}
	}
	@Override
	public Vec3 getFriction()
	{
		if (isLockedIn())
			return CommonUtils.createVec3(1f);
		
		float horizontalFriction = 0.95f;
		if (onGround())
			horizontalFriction = level().getBlockState(getOnPos()).getBlock().getFriction();
		
		horizontalFriction = Mth.clamp(horizontalFriction, 0, 1);
		return new Vec3(horizontalFriction, 0.95f, horizontalFriction);
	}
	// there are better ways to name this method but i can think of them
	public boolean isLockedIn()
	{
		return getTargetPosition().isPresent();
	}
	private Optional<Vec3> getTargetPosition()
	{
		return entityData.get(TARGET_POSITION);
	}
	public int getSearchDelay()
	{
		return entityData.get(SEARCH_DELAY);
	}
	public void setSearchDelay(int searchDelay)
	{
		if (!level().isClientSide())
			entityData.set(SEARCH_DELAY, searchDelay);
	}
	private Vector2f getSearchRange()
	{
		return getSettings().subDataRecord.searchRange();
	}
	@Override
	protected Item getDefaultItem()
	{
		return SplatcraftItems.torpedo.get();
	}
	@Override
	public void onCollidedWithObjectEntity(Entity entity)
	{
		explode(getSettings(), getBoundingBox().getCenter());
	}
	@Override
	public Vec3 reflectVelocity(Direction.Axis axis, Vec3 newVelocity, Vec3 oldVelocity)
	{
		if (axis == Direction.Axis.Y)
			if (oldVelocity.y < newVelocity.y && newVelocity.y <= 0 && oldVelocity.y <= -0.7 && oldVelocity.horizontalDistanceSqr() < 0.9)
				return oldVelocity.with(Direction.Axis.Y, 0);
		
		return IBouncyEntity.super.reflectVelocity(axis, newVelocity, oldVelocity);
	}
	@Override
	public boolean didCollisionOnAxis(Direction.Axis axis, Vec3 newVelocity, Vec3 oldVelocity)
	{
		return !isLockedIn() && IBouncyEntity.super.didCollisionOnAxis(axis, newVelocity, oldVelocity);
	}
	@Override
	public Vec3 reflectionCoefficient(Vec3 velocity)
	{
		return REFLECTION_COEFFICIENT;
	}
	@Override
	public void addAdditionalSaveData(CompoundTag nbt)
	{
		getTargetPosition().ifPresent(targetPos ->
			Vec3.CODEC.encodeStart(NbtOps.INSTANCE, targetPos).result().ifPresent(tag -> nbt.put("TargetPosition", tag)));
		nbt.putInt("Counter", entityData.get(COUNTER));
		nbt.putFloat("Health", entityData.get(HEALTH));
		nbt.putInt("SearchDelay", getSearchDelay());
		
		super.addAdditionalSaveData(nbt);
	}
	@Override
	public void readAdditionalSaveData(CompoundTag nbt)
	{
		if (nbt.contains("TargetPosition"))
		{
			Vec3.CODEC.parse(NbtOps.INSTANCE, nbt.get("TargetPosition")).result().ifPresent(targetPos ->
				entityData.set(TARGET_POSITION, Optional.of(targetPos))
			);
		}
		else
			entityData.set(TARGET_POSITION, Optional.empty());
		
		entityData.set(COUNTER, nbt.getInt("Counter"));
		entityData.set(HEALTH, nbt.getFloat("Health"));
		setSearchDelay(nbt.getInt("SearchDelay"));
		
		super.readAdditionalSaveData(nbt);
	}
	@Override
	public Vec3 collide(Vec3 vec3)
	{
		return ((EntityAccessor) this).invokeCollide(vec3);
	}
	@Override
	public boolean onEntityInked(DamageSource source, float damage, InkColor color)
	{
		if (isLockedIn())
		{
			setHealth(getHealth() - damage);
			if (getHealth() <= 0)
			{
				kill();
			}
			return true;
		}
		return false;
	}
	@Override
	public boolean canBeHitByProjectile()
	{
		return isLockedIn();
	}
	public float getHealth()
	{
		return entityData.get(HEALTH);
	}
	public void setHealth(float health)
	{
		entityData.set(HEALTH, health);
	}
	public int getCounter()
	{
		return entityData.get(COUNTER);
	}
}
