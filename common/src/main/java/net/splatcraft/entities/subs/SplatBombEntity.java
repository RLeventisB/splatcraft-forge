package net.splatcraft.entities.subs;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.item.Item;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.AttackId;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkExplosion;

public class SplatBombEntity extends AbstractSubWeaponEntity
{
    public static final int FLASH_DURATION = 10;
    protected int fuseTime = 0;
    protected int prevFuseTime = 0;

    public SplatBombEntity(EntityType<? extends AbstractSubWeaponEntity> type, World world)
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
        SubWeaponSettings settings = getSettings();

        if (!isOnGround() || squaredDistanceTo(getVelocity()) > (double) 1.0E-5F)
        {
            float f1 = 0.98F;
            if (isOnGround())
                f1 = getWorld().getBlockState(CommonUtils.createBlockPos(getX(), getY() - 1.0D, getZ())).getBlock().getSlipperiness();

            f1 = (float) Math.min(0.98, f1 * 1.5f);

            setVelocity(getVelocity().multiply(f1, 0.98D, f1));
        }

        if (isOnGround())
            fuseTime++;
        if (fuseTime >= settings.subDataRecord.fuseTime())
        {
            InkExplosion.createInkExplosion(getOwner(), getBoundingBox().getCenter(), settings.subDataRecord.inkSplashRadius(), settings.subDataRecord.damageRanges(), inkType, sourceWeapon, AttackId.NONE);
            getWorld().sendEntityStatus(this, (byte) 1);
            getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundCategory.PLAYERS, 0.8F, CommonUtils.triangle(getWorld().getRandom(), 0.95F, 0.095F));
            if (!getWorld().isClient())
                discard();
            return;
        }

        move(MovementType.SELF, getVelocity().multiply(0, 1, 0));
        setPos(getX() + getVelocity().x, getY(), getZ() + getVelocity().z);
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
        if (absVelocityY >= .05 && absVelocityY >= absVelocityX && absVelocityY >= absVelocityZ)
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
        if (Math.abs(velocityY) >= 0.05 && (blockFace == Direction.DOWN || blockFace == Direction.UP))
            setVelocity(velocityX, -velocityY * .5, velocityZ);
        if (blockFace == Direction.NORTH || blockFace == Direction.SOUTH)
            setVelocity(velocityX, velocityY, -velocityZ);
    }

    @Override
    protected boolean handleMovement()
    {
        return false;
    }

    public float getFlashIntensity(float partialTicks)
    {
        SubWeaponSettings settings = getSettings();
        return Math.max(0, MathHelper.lerp(partialTicks, prevFuseTime, fuseTime) - (settings.subDataRecord.fuseTime() - FLASH_DURATION)) * 0.85f / FLASH_DURATION;
    }
}
