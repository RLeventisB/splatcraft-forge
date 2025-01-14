package net.splatcraft.entities.subs;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.item.Item;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.entities.ObjectCollideListenerEntity;
import net.splatcraft.items.weapons.settings.SubWeaponRecords.ThrowableExplodingSubDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.AttackId;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkExplosion;

public class SplatBombEntity extends AbstractSubWeaponEntity<ThrowableExplodingSubDataRecord> implements ObjectCollideListenerEntity
{
	public static final int FLASH_DURATION = 10;
	protected int fuseTime = 0;
	protected int prevFuseTime = 0;
	protected boolean playedActivationSound = false;
	public SplatBombEntity(EntityType<? extends AbstractSubWeaponEntity<ThrowableExplodingSubDataRecord>> type, World world)
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
		
		if (!isOnGround() || squaredDistanceTo(getVelocity()) > (double) 1.0E-5F)
		{
			float f1 = 0.98F;
			if (isOnGround())
				f1 = getWorld().getBlockState(CommonUtils.createBlockPos(getX(), getY() - 1.0D, getZ())).getBlock().getSlipperiness();
			
			f1 = (float) Math.min(0.98, f1 * 1.5f);
			
			setVelocity(getVelocity().multiply(f1, 0.98D, f1));
		}
		
		if (isOnGround())
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
			getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonating, SoundCategory.PLAYERS, 0.8F, 1f);
			playedActivationSound = true;
		}
		
		move(MovementType.SELF, getVelocity());
	}
	private void explode(SubWeaponSettings<ThrowableExplodingSubDataRecord> settings, Vec3d impactPos)
	{
		if (!getWorld().isClient())
		{
			InkExplosion.createInkExplosion(getOwner(), impactPos, settings.subDataRecord.inkSplashRadius(), settings.subDataRecord.damageRanges(), inkType, sourceWeapon, AttackId.NONE);
			getWorld().sendEntityStatus(this, (byte) 1);
			discard();
		}
		getWorld().playSound(null, impactPos.x, impactPos.y, impactPos.z, SplatcraftSounds.subDetonate, SoundCategory.PLAYERS, 0.8F, CommonUtils.nextTriangular(getWorld().getRandom(), 0.95F, 0.095F));
	}
	@Override
	public void handleMovement()
	{
	}
	@Override
	public void handleStatus(byte id)
	{
		super.handleStatus(id);
		if (id == 1)
		{
			getWorld().addImportantParticle(new InkExplosionParticleData(getColor(), getSettings().subDataRecord.damageRanges().getMaxDistance() * 2), getX(), getY(), getZ(), 0, 0, 0);
		}
	}
	//Ripped and modified from Minestuck's BouncingProjectileEntity class (with permission)
	@Override
	protected void onEntityHit(EntityHitResult result)
	{
		super.onEntityHit(result);
		
		double velocityX = getVelocity().x * 0.3;
		double velocityY = getVelocity().y;
		double velocityZ = getVelocity().z * 0.3;
		double absVelocityX = Math.abs(velocityX);
		double absVelocityY = Math.abs(velocityY);
		double absVelocityZ = Math.abs(velocityZ);
		
		if (absVelocityX >= absVelocityY && absVelocityX >= absVelocityZ)
			setVelocity(-velocityX, velocityY, velocityZ);
		if (absVelocityY >= .02 && absVelocityY >= absVelocityX && absVelocityY >= absVelocityZ)
			setVelocity(velocityX, -velocityY * .5, velocityZ);
		if (absVelocityZ >= absVelocityY && absVelocityZ >= absVelocityX)
			setVelocity(velocityX, velocityY, -velocityZ);
	}
	@Override
	protected void onBlockHit(BlockHitResult result)
	{
		if (getWorld().getBlockState(result.getBlockPos()).getCollisionShape(getWorld(), result.getBlockPos()).getBoundingBox().maxY - (getBlockY() - getBlockY()) <= 0)
			return;
		
		double velocityX = getVelocity().x;
		double velocityY = getVelocity().y;
		double velocityZ = getVelocity().z;
		
		Direction blockFace = result.getSide();
		
		if (blockFace == Direction.EAST || blockFace == Direction.WEST)
			setVelocity(-velocityX, velocityY, velocityZ);
		if (blockFace == Direction.DOWN)
			setVelocity(velocityX, -velocityY * .3, velocityZ);
		if (blockFace == Direction.NORTH || blockFace == Direction.SOUTH)
			setVelocity(velocityX, velocityY, -velocityZ);
	}
	public float getFlashIntensity(float partialTicks)
	{
		SubWeaponSettings<ThrowableExplodingSubDataRecord> settings = getSettings();
		if (settings.subDataRecord == null)
			return 0;
		return Math.max(0, MathHelper.lerp(partialTicks, prevFuseTime, fuseTime) - (settings.subDataRecord.fuseTime() - FLASH_DURATION)) * 0.85f / FLASH_DURATION;
	}
	@Override
	public void onCollidedWithObjectEntity(Entity entity)
	{
		explode(getSettings(), getBoundingBox().getCenter());
	}
}
