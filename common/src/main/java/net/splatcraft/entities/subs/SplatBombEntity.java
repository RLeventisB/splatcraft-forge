package net.splatcraft.entities.subs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.entities.ObjectCollideListenerEntity;
import net.splatcraft.items.weapons.settings.SubWeaponRecords.ThrowableExplodingSubDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.AttackId;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkExplosion;
import org.jetbrains.annotations.NotNull;

public class SplatBombEntity extends AbstractSubWeaponEntity<ThrowableExplodingSubDataRecord> implements ObjectCollideListenerEntity
{
	public static final int FLASH_DURATION = 10;
	protected int fuseTime = 0;
	protected int prevFuseTime = 0;
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

		prevFuseTime = fuseTime;
		SubWeaponSettings<ThrowableExplodingSubDataRecord> settings = getSettings();

		if (!onGround() || distanceToSqr(getDeltaMovement()) > (double) 1.0E-5F)
		{
			float f1 = 0.98F;
			if (onGround())
				f1 = level().getBlockState(BlockPos.containing(getX(), getY() - 1.0D, getZ())).getBlock().getFriction();

			f1 = (float) Math.min(0.98, f1 * 1.5f);

			setDeltaMovement(getDeltaMovement().multiply(f1, 0.98D, f1));
		}

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

		move(MoverType.SELF, getDeltaMovement());
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
	public void handleMovement()
	{
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
	protected void onHitEntity(@NotNull EntityHitResult result)
	{
		super.onHitEntity(result);

		double velocityX = getDeltaMovement().x * 0.3;
		double velocityY = getDeltaMovement().y;
		double velocityZ = getDeltaMovement().z * 0.3;
		double absVelocityX = Math.abs(velocityX);
		double absVelocityY = Math.abs(velocityY);
		double absVelocityZ = Math.abs(velocityZ);

		if (absVelocityX >= absVelocityY && absVelocityX >= absVelocityZ)
			setDeltaMovement(-velocityX, velocityY, velocityZ);
		if (absVelocityY >= .02 && absVelocityY >= absVelocityX && absVelocityY >= absVelocityZ)
			setDeltaMovement(velocityX, -velocityY * .5, velocityZ);
		if (absVelocityZ >= absVelocityY && absVelocityZ >= absVelocityX)
			setDeltaMovement(velocityX, velocityY, -velocityZ);
	}
	@Override
	protected void onHitBlock(BlockHitResult result)
	{
		if (level().getBlockState(result.getBlockPos()).getCollisionShape(level(), result.getBlockPos()).bounds().maxY - (getY() - getBlockY()) <= 0)
			return;

		double velocityX = getDeltaMovement().x;
		double velocityY = getDeltaMovement().y;
		double velocityZ = getDeltaMovement().z;

		Direction blockFace = result.getDirection();

		if (blockFace == Direction.EAST || blockFace == Direction.WEST)
			setDeltaMovement(-velocityX, velocityY, velocityZ);
		if ((blockFace == Direction.DOWN || blockFace == Direction.UP) && Math.abs(velocityY) >= 1.2)
			setDeltaMovement(velocityX, -velocityY * .3, velocityZ);
		if (blockFace == Direction.NORTH || blockFace == Direction.SOUTH)
			setDeltaMovement(velocityX, velocityY, -velocityZ);
	}
	public float getFlashIntensity(float partialTicks)
	{
		SubWeaponSettings<ThrowableExplodingSubDataRecord> settings = getSettings();
		if (settings.subDataRecord == null)
			return 0;
		return Math.max(0, Mth.lerpInt(partialTicks, prevFuseTime, fuseTime) - (settings.subDataRecord.fuseTime() - FLASH_DURATION)) * 0.85f / FLASH_DURATION;
	}
	@Override
	public void onCollidedWithObjectEntity(Entity entity)
	{
		explode(getSettings(), getBoundingBox().getCenter());
	}
}
