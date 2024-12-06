package net.splatcraft.entities.subs;

import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
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

    public SplatBombEntity(EntityType<? extends AbstractSubWeaponEntity> type, Level level)
    {
        super(type, level);
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

        if (!this.onGround() || distanceToSqr(this.getDeltaMovement()) > (double) 1.0E-5F)
        {
            float f1 = 0.98F;
            if (this.onGround())
                f1 = this.getWorld().getBlockState(CommonUtils.createBlockPos(this.getX(), this.getY() - 1.0D, this.getZ())).getFriction(level(), CommonUtils.createBlockPos(this.getX(), this.getY() - 1.0D, this.getZ()), this);

            f1 = (float) Math.min(0.98, f1 * 1.5f);

            this.setDeltaMovement(this.getDeltaMovement().multiply(f1, 0.98D, f1));
        }

        if (onGround())
            fuseTime++;
        if (fuseTime >= settings.subDataRecord.fuseTime())
        {
            InkExplosion.createInkExplosion(getOwner(), getBoundingBox().getCenter(), settings.subDataRecord.inkSplashRadius(), settings.subDataRecord.damageRanges(), inkType, sourceWeapon, AttackId.NONE);
            level().broadcastEntityEvent(this, (byte) 1);
            level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundSource.PLAYERS, 0.8F, CommonUtils.triangle(level().getRandom(), 0.95F, 0.095F));
            if (!level().isClientSide())
                discard();
            return;
        }

        this.move(MoverType.SELF, this.getDeltaMovement().multiply(0, 1, 0));
        setPos(getX() + getDeltaMovement().x(), getY(), getZ() + getDeltaMovement().z);
    }

    @Override
    public void handleEntityEvent(byte id)
    {
        super.handleEntityEvent(id);
        if (id == 1)
        {
            level().addAlwaysVisibleParticle(new InkExplosionParticleData(getColor(), getSettings().subDataRecord.damageRanges().getMaxDistance() * 2), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    //Ripped and modified from Minestuck's BouncingProjectileEntity class (with permission)
    @Override
    protected void onHitEntity(EntityHitResult result)
    {
        super.onHitEntity(result);

        double velocityX = this.getDeltaMovement().x * 0.3;
        double velocityY = this.getDeltaMovement().y;
        double velocityZ = this.getDeltaMovement().z * 0.3;
        double absVelocityX = Math.abs(velocityX);
        double absVelocityY = Math.abs(velocityY);
        double absVelocityZ = Math.abs(velocityZ);

        if (absVelocityX >= absVelocityY && absVelocityX >= absVelocityZ)
            this.setDeltaMovement(-velocityX, velocityY, velocityZ);
        if (absVelocityY >= .05 && absVelocityY >= absVelocityX && absVelocityY >= absVelocityZ)
            this.setDeltaMovement(velocityX, -velocityY * .5, velocityZ);
        if (absVelocityZ >= absVelocityY && absVelocityZ >= absVelocityX)
            this.setDeltaMovement(velocityX, velocityY, -velocityZ);
    }

    @Override
    protected void onBlockHit(BlockHitResult result)
    {
        if (level().getBlockState(result.getBlockPos()).getCollisionShape(level(), result.getBlockPos()).bounds().maxY - (position().y() - blockPosition().getY()) <= 0)
            return;

        double velocityX = this.getDeltaMovement().x;
        double velocityY = this.getDeltaMovement().y;
        double velocityZ = this.getDeltaMovement().z;

        Direction blockFace = result.getDirection();

        if (blockFace == Direction.EAST || blockFace == Direction.WEST)
            this.setDeltaMovement(-velocityX, velocityY, velocityZ);
        if (Math.abs(velocityY) >= 0.05 && (blockFace == Direction.DOWN || blockFace == Direction.UP))
            this.setDeltaMovement(velocityX, -velocityY * .5, velocityZ);
        if (blockFace == Direction.NORTH || blockFace == Direction.SOUTH)
            this.setDeltaMovement(velocityX, velocityY, -velocityZ);
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
