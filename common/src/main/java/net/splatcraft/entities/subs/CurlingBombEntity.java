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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.entities.ObjectCollideListenerEntity;
import net.splatcraft.items.weapons.settings.SubWeaponRecords.CurlingBombDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.mixin.accessors.EntityAccessor;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.*;
import net.splatcraft.util.structs.AttackId;
import net.splatcraft.util.structs.BlockInkedResult;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class CurlingBombEntity extends AbstractSubWeaponEntity<CurlingBombDataRecord> implements ObjectCollideListenerEntity, IBouncyEntity
{
	public static final Vec3 REFLECTION_COEFFICIENT = new Vec3(
		-1,
		-0.5,
		-1
	);
	private static final EntityDataAccessor<Integer> INIT_FUSE_TIME = SynchedEntityData.defineId(CurlingBombEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> COOK_SCALE = SynchedEntityData.defineId(CurlingBombEntity.class, EntityDataSerializers.FLOAT);
	public int fuseTime = 0;
	public int prevFuseTime = 0;
	public float bladeRot = 0;
	public float prevBladeRot = 0;
	private boolean playedActivationSound = false;
	private AttackId rollAttackId = AttackId.registerAttack();
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
		
		if (spd > 0.01 && fuseTime % (int) Math.max(1, (1 - spd) * 10) == 0)
		{
			level().broadcastEntityEvent(this, (byte) 2);
		}
		
		super.tick();
	}
	@Override
	public void handleMovement()
	{
		Pair<Vec3, Vec3> collidedAndNewVelocity = doBounceLogic(this, getDeltaMovement(), this::canHitEntity, getSettings().subDataRecord.bounceOnEntityHit());
		
		setDeltaMovement(collidedAndNewVelocity.getSecond());
		setPos(position().add(collidedAndNewVelocity.getFirst()));
	}
	@Override
	public Vec3 reflectVelocity(Direction.Axis axis, Vec3 newVelocity, Vec3 oldVelocity)
	{
		if (axis == Direction.Axis.Y)
			if (oldVelocity.y < newVelocity.y && newVelocity.y <= 0 && oldVelocity.y >= -0.9)
				return oldVelocity.with(Direction.Axis.Y, 0);
		
		return IBouncyEntity.super.reflectVelocity(axis, newVelocity, oldVelocity);
	}
	@Override
	public Vec3 collide(Vec3 vec3)
	{
		return ((EntityAccessor) this).invokeCollide(vec3);
	}
	@Override
	public boolean canHitEntity(@NotNull Entity target)
	{
		return super.canHitEntity(target) && getOwner() != target;
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
	public Vec3 getFriction()
	{
		float horizontalFriction = 1f;
		if (onGround())
			horizontalFriction = level().getBlockState(getOnPos()).getBlock().getFriction() / 0.6f;
		
		CurlingBombDataRecord curlingData = getSettings().subDataRecord;
		boolean slowingDown = fuseTime <= curlingData.warningFrame();
		
		if (slowingDown)
		{
			horizontalFriction *= 0.8f;
		}
		horizontalFriction = Mth.clamp(horizontalFriction, 0, 1);
		return new Vec3(horizontalFriction, 1f, horizontalFriction);
	}
	private void doTrail(boolean fastEnough, SubWeaponSettings<CurlingBombDataRecord> settings)
	{
		float trailWidth = settings.subDataRecord.trailSizeRange().getValue(getCookProgress());
		float trailStep = CommonUtils.calculateStep(trailWidth, Mth.SQRT_OF_TWO / 2f);
		if (fastEnough)
		{
			Vec3 normalized = getDeltaMovement().multiply(1, 0, 1).normalize();
			double sideX = -normalized.z;
			double sideZ = normalized.x;
			for (float j = -trailWidth; j <= trailWidth; j += trailStep)
			{
				Optional<BlockPos> optionalPos = InkBlockUtils.getBlockStandingOnPos(new Vec3(getX() + sideX * j, getY() + 10e-5, getZ() + sideZ * j), level(), 1, this);
				optionalPos.ifPresent(blockPos ->
				{
					if (InkBlockUtils.canInkFromFace(level(), blockPos, Direction.UP))
					{
						BlockInkedResult result = InkBlockUtils.inkBlock(getOwner(), level(), blockPos, getColor(), Direction.UP, inkType, settings.subDataRecord.contactDamage());
						if (result == BlockInkedResult.SUCCESS)
							InkBlockUtils.awardTurfPoints((LivingEntity) getOwner(), sourceWeapon, 1);
					}
				});
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
	public Vec3 onHitEntity(EntityHitResult result, Vec3 velocity, Direction hitDirecion)
	{
		InkDamageUtils.doRollDamage(result.getEntity(), getSettings().subDataRecord.contactDamage(), getOwner(), this, sourceWeapon, rollAttackId);
		
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
	@Override
	public Vec3 reflectionCoefficient(Vec3 velocity)
	{
		return REFLECTION_COEFFICIENT;
	}
	public float getFlashIntensity(float partialTicks)
	{
		SubWeaponSettings<CurlingBombDataRecord> settings = getSettings();
		if (settings.subDataRecord == null)
			return 0;
		
		if (fuseTime <= settings.subDataRecord.warningFrame() && !isItem)
		{
			return settings.subDataRecord.warningFrame() - (fuseTime - partialTicks) * 0.85f;
		}
		return 0;
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
