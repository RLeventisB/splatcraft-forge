package net.splatcraft.entities.subs;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.entities.ObjectCollideListenerEntity;
import net.splatcraft.items.weapons.settings.SubWeaponRecords.ThrowableExplodingSubDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.mixin.accessors.EntityAccessor;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.structs.AttackId;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkExplosion;

public class SplatBombEntity extends AbstractSubWeaponEntity<ThrowableExplodingSubDataRecord> implements ObjectCollideListenerEntity, IBouncyEntity
{
	public static final int FLASH_DURATION = 10;
	private static final Vec3 REFLECTION_COEFFICIENT = new Vec3(
		-0.86, -0.5, -0.86
	);
	protected int fuseTime = 0;
	protected boolean playedActivationSound = false;
	public SplatBombEntity(EntityType<? extends AbstractSubWeaponEntity<ThrowableExplodingSubDataRecord>> type, Level world)
	{
		super(type, world);
	}
	@Override
	protected Item getDefaultItem()
	{
		return SplatcraftItems.splatBomb.get();
	}
	@Override
	public void tick()
	{
		super.tick();
		
		SubWeaponSettings<ThrowableExplodingSubDataRecord> settings = getSettings();
		
		if (onGround())
		{
			fuseTime++;
		}
		if (fuseTime >= settings.subDataRecord.fuseTime() && isAlive())
		{
			explode(settings, getBoundingBox().getCenter());
			return;
		}
		else if (!playedActivationSound && fuseTime >= settings.subDataRecord.fuseTime() - 18)
		{
			level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonating, SoundSource.PLAYERS, 0.8F, 1f);
			playedActivationSound = true;
		}
	}
	private void explode(SubWeaponSettings<ThrowableExplodingSubDataRecord> settings, Vec3 impactPos)
	{
		if (!level().isClientSide())
		{
			InkExplosion.createInkExplosion(getOwner(), impactPos, settings.subDataRecord.inkSplashRadius(), settings.subDataRecord.damageRanges(), inkType, sourceWeapon, AttackId.NONE);
			level().broadcastEntityEvent(this, (byte) 1);
			discard();
		}
		level().playSound(null, impactPos.x, impactPos.y, impactPos.z, SplatcraftSounds.subDetonate, SoundSource.PLAYERS, 0.8F, CommonUtils.nextTriangular(level().getRandom(), 0.95F, 0.095F));
	}
	@Override
	public Vec3 getFriction()
	{
		double f1 = 0.94;
		if (onGround())
			f1 = level().getBlockState(getOnPos()).getBlock().getFriction() * 1.2f;
		
		f1 = Math.min(0.94, f1);
		
		return new Vec3(f1, 0.94, f1);
	}
	@Override
	public void handleMovement()
	{
		Vec3 oldDeltaMovement = getDeltaMovement();
		
		Pair<Vec3, Vec3> collidedAndNewVelocity = doBounceLogic(this, oldDeltaMovement, this::canHitEntity, false);
		
		setDeltaMovement(collidedAndNewVelocity.getSecond());
		setPos(position().add(collidedAndNewVelocity.getFirst()));
		
		setOnGroundWithMovement(oldDeltaMovement.y < collidedAndNewVelocity.getFirst().y && oldDeltaMovement.y < 0, collidedAndNewVelocity.getFirst());
	}
	@Override
	public double getDefaultGravity()
	{
		return 0.15;
	}
	@Override
	public Vec3 reflectVelocity(Direction.Axis axis, Vec3 newVelocity, Vec3 oldVelocity)
	{
		if (axis == Direction.Axis.Y)
			if (oldVelocity.y < newVelocity.y && newVelocity.y <= 0 && oldVelocity.y <= -0.6 && oldVelocity.horizontalDistanceSqr() < 1.1)
				return oldVelocity.with(Direction.Axis.Y, 0);
		
		return IBouncyEntity.super.reflectVelocity(axis, newVelocity, oldVelocity);
	}
	@Override
	public void handleEntityEvent(byte id)
	{
		super.handleEntityEvent(id);
		if (id == 1)
		{
			level().addAlwaysVisibleParticle(new InkExplosionParticleData(getColor(), getSettings().subDataRecord.damageRanges().getMaxDistance() * 2), getX(), getY(), getZ(), 0, 0, 0);
		}
	}
	//Ripped and modified from Minestuck's BouncingProjectileEntity class (with permission)
	@Override
	public Vec3 reflectionCoefficient(Vec3 velocity)
	{
		return REFLECTION_COEFFICIENT;
	}
	public float getFlashIntensity(float partialTicks)
	{
		SubWeaponSettings<ThrowableExplodingSubDataRecord> settings = getSettings();
		if (settings.subDataRecord == null)
			return 0;
		return Math.max(0, fuseTime - 1 + partialTicks - (settings.subDataRecord.fuseTime() - FLASH_DURATION)) * 0.85f / FLASH_DURATION;
	}
	@Override
	public void onCollidedWithObjectEntity(Entity entity)
	{
		explode(getSettings(), getBoundingBox().getCenter());
	}
	@Override
	public Vec3 collide(Vec3 vec3)
	{
		return ((EntityAccessor) this).invokeCollide(vec3);
	}
}
