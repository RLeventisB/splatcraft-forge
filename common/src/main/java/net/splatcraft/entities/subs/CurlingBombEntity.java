package net.splatcraft.entities.subs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3d;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CurlingBombEntity extends AbstractSubWeaponEntity
{
    public static final int FLASH_DURATION = 20;
    private static final EntityDataAccessor<Integer> INIT_FUSE_TIME = SynchedEntityData.defineId(CurlingBombEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> COOK_SCALE = SynchedEntityData.defineId(CurlingBombEntity.class, EntityDataSerializers.FLOAT);
    public int fuseTime = 0;
    public int prevFuseTime = 0;
    public float bladeRot = 0;
    public float prevBladeRot = 0;
    private boolean playedActivationSound = false;

    public CurlingBombEntity(EntityType<? extends AbstractSubWeaponEntity> type, Level level)
    {
        super(type, level);
        setMaxUpStep(.7f);
    }

    public static void onItemUseTick(Level level, LivingEntity entity, ItemStack stack, int useTime)
    {
        NbtCompound data = stack.getTag().getCompound("EntityData");
        data.putInt("CookTime", stack.getItem().getUseDuration(stack) - useTime);

        stack.getTag().put("EntityData", data);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(INIT_FUSE_TIME, 0);
        entityData.define(COOK_SCALE, 0f);
    }

    @Override
    protected Item getDefaultItem()
    {
        return SplatcraftItems.curlingBomb.get();
    }

    @Override
    public void readItemData(NbtCompound nbt)
    {
        if (nbt.contains("CookTime"))
        {
            if (getSettings().subDataRecord.curlingData().cookTime() > 0)
                setCookScale(Math.min(4, (nbt.getInt("CookTime") / (float) getSettings().subDataRecord.curlingData().cookTime())));
            setInitialFuseTime(nbt.getInt("CookTime"));
            prevFuseTime = getInitialFuseTime();
        }
    }

    @Override
    public void tick()
    {
        super.tick();

        SubWeaponSettings settings = getSettings();

        double spd = getDeltaMovement().multiply(1, 0, 1).length();
        prevBladeRot = bladeRot;
        bladeRot += (float) spd;

        prevFuseTime = fuseTime;
        fuseTime++;

        boolean playAlertAnim = fuseTime == 30;

        if (fuseTime >= settings.subDataRecord.fuseTime() - FLASH_DURATION && !playedActivationSound)
        {
            level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonating, SoundSource.PLAYERS, 0.8F, 1f);
            playedActivationSound = true;
        }

        if (!level().isClientSide())
        {
            if (spd > 1.0E-3)
            {
                for (float j = -0.15f; j <= 0.15f; j += 0.3f)
                {
                    Vec3d normalized = getDeltaMovement().multiply(1, 0, 1).normalize();
                    double sideX = -normalized.z;
                    double sideZ = normalized.x;
                    for (int i = 0; i <= 2; i++)
                    {
                        BlockPos side = CommonUtils.createBlockPos(Math.floor(getX() + sideX * j), getBlockY() - i, Math.floor(getZ() + sideZ * j));
                        if (InkBlockUtils.canInkFromFace(level(), side, Direction.UP))
                        {
                            InkBlockUtils.playerInkBlock(getOwner() instanceof Player player ? player : null, level(), side, getColor(), Direction.UP, inkType, settings.subDataRecord.curlingData().contactDamage());
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
                        InkBlockUtils.playerInkBlock(getOwner() instanceof Player player ? player : null, level(), blockPosition().below(i), getColor(), Direction.UP, inkType, settings.subDataRecord.curlingData().contactDamage());
                        break;
                    }
            }
        }
        if (!onGround() || distanceToSqr(getDeltaMovement()) > 1.0E-5)
        {
            float f1 = 0.98F;
            if (onGround())
                f1 = this.getWorld().getBlockState(CommonUtils.createBlockPos(getX(), getY() - 1.0D, getZ())).getFriction(level(), CommonUtils.createBlockPos(getX(), getY() - 1.0D, getZ()), this);

            f1 = (float) Math.min(0.98, f1 * 3f) * Math.min(1, 2 * (1 - fuseTime / (float) settings.subDataRecord.fuseTime()));

            setDeltaMovement(getDeltaMovement().multiply(f1, 0.98D, f1));
        }
        if (fuseTime >= settings.subDataRecord.fuseTime())
        {
            DamageRangesRecord radiuses = settings.subDataRecord.damageRanges().cloneWithMultiplier(getCookScale(), getCookScale());
            InkExplosion.createInkExplosion(getOwner(), getBoundingBox().getCenter(), settings.subDataRecord.inkSplashRadius() * getCookScale(), radiuses, inkType, sourceWeapon, AttackId.NONE);
            level().broadcastEntityEvent(this, (byte) 1);
            level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundSource.PLAYERS, 0.8F, CommonUtils.triangle(level().getRandom(), 0.95F, 0.095F));
            if (!level().isClientSide())
                discard();
            return;
        }
        else if (spd > 0.01 && fuseTime % (int) Math.max(1, (1 - spd) * 10) == 0)
        {
            level().broadcastEntityEvent(this, (byte) 2);
        }
        move(MoverType.SELF, getDeltaMovement().multiply(0, 1, 0));

        Vec3d vec = getDeltaMovement().multiply(1, 0, 1);
        vec = position().add(collide(vec));

        setPos(vec.x, vec.y, vec.z);
    }

    @Override
    public void handleEntityEvent(byte id)
    {
        super.handleEntityEvent(id);
        if (id == 1)
        {
            level().addAlwaysVisibleParticle(new InkExplosionParticleData(getColor(), (getSettings().subDataRecord.damageRanges().getMaxDistance() + getCookScale()) * 2), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
        if (id == 2)
        {
            level().addParticle(new InkSplashParticleData(getColor(), getSettings().subDataRecord.damageRanges().getMaxDistance() * 1.15f), this.getX(), this.getY() + 0.4, this.getZ(), 0, 0, 0);
        }
    }

    //Ripped and modified from Minestuck's BouncingProjectileEntity class (with permission)
    @Override
    protected void onHitEntity(EntityHitResult result)
    {
        if (result.getEntity() instanceof LivingEntity livingEntity)
        {
            InkDamageUtils.doRollDamage(livingEntity, getSettings().subDataRecord.curlingData().contactDamage(), getOwner(), this, sourceWeapon);
        }

        double velocityX = this.getDeltaMovement().x;
        double velocityY = this.getDeltaMovement().y;
        double velocityZ = this.getDeltaMovement().z;
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
        if (canStepUp(getDeltaMovement()))
            return;

        double velocityX = this.getDeltaMovement().x;
        double velocityY = this.getDeltaMovement().y;
        double velocityZ = this.getDeltaMovement().z;

        Direction blockFace = result.getDirection();

        if (level().getBlockState(result.getBlockPos()).getCollisionShape(level(), result.getBlockPos()).bounds().maxY - (position().y() - blockPosition().getY()) < .7f)
            return;

        if (blockFace == Direction.EAST || blockFace == Direction.WEST)
            this.setDeltaMovement(-velocityX, velocityY, velocityZ);
        if (Math.abs(velocityY) >= 0.05 && (blockFace == Direction.DOWN))
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

    private boolean canStepUp(Vec3d p_20273_)
    {
        AABB aabb = this.getBoundingBox();
        List<VoxelShape> list = this.getWorld().getEntityCollisions(this, aabb.expandTowards(p_20273_));
        Vec3d vec3 = p_20273_.lengthSqr() == 0.0D ? p_20273_ : collideBoundingBox(this, p_20273_, aabb, this.getWorld(), list);
        boolean flag = p_20273_.x != vec3.x;
        boolean flag1 = p_20273_.y != vec3.y;
        boolean flag2 = p_20273_.z != vec3.z;
        boolean flag3 = this.onGround() || flag1 && p_20273_.y < 0.0D;
        float stepHeight = getStepHeight();
        if (stepHeight > 0.0F && flag3 && (flag || flag2))
        {
            Vec3d vec31 = collideBoundingBox(this, new Vec3d(p_20273_.x, stepHeight, p_20273_.z), aabb, this.getWorld(), list);
            Vec3d vec32 = collideBoundingBox(this, new Vec3d(0.0D, stepHeight, 0.0D), aabb.expandTowards(p_20273_.x, 0.0D, p_20273_.z), this.getWorld(), list);
            if (vec32.y < (double) stepHeight)
            {
                Vec3d vec33 = collideBoundingBox(this, new Vec3d(p_20273_.x, 0.0D, p_20273_.z), aabb.move(vec32), this.getWorld(), list).add(vec32);
                if (vec33.horizontalDistanceSqr() > vec31.horizontalDistanceSqr())
                {
                    vec31 = vec33;
                }
            }

            return vec31.horizontalDistanceSqr() > vec3.horizontalDistanceSqr();
        }

        return false;
    }

    private Vec3d collide(Vec3d p_20273_)
    {
        AABB aabb = this.getBoundingBox();
        List<VoxelShape> list = this.getWorld().getEntityCollisions(this, aabb.expandTowards(p_20273_));
        Vec3d vec3 = p_20273_.lengthSqr() == 0.0D ? p_20273_ : collideBoundingBox(this, p_20273_, aabb, this.getWorld(), list);
        boolean flag = p_20273_.x != vec3.x;
        boolean flag1 = p_20273_.y != vec3.y;
        boolean flag2 = p_20273_.z != vec3.z;
        boolean flag3 = this.onGround() || flag1 && p_20273_.y < 0.0D;
        float stepHeight = getStepHeight();
        if (stepHeight > 0.0F && flag3 && (flag || flag2))
        {
            Vec3d vec31 = collideBoundingBox(this, new Vec3d(p_20273_.x, stepHeight, p_20273_.z), aabb, this.getWorld(), list);
            Vec3d vec32 = collideBoundingBox(this, new Vec3d(0.0D, stepHeight, 0.0D), aabb.expandTowards(p_20273_.x, 0.0D, p_20273_.z), this.getWorld(), list);
            if (vec32.y < (double) stepHeight)
            {
                Vec3d vec33 = collideBoundingBox(this, new Vec3d(p_20273_.x, 0.0D, p_20273_.z), aabb.move(vec32), this.getWorld(), list).add(vec32);
                if (vec33.horizontalDistanceSqr() > vec31.horizontalDistanceSqr())
                {
                    vec31 = vec33;
                }
            }

            if (vec31.horizontalDistanceSqr() > vec3.horizontalDistanceSqr())
            {
                return vec31.add(collideBoundingBox(this, new Vec3d(0.0D, -vec31.y + p_20273_.y, 0.0D), aabb.move(vec31), this.getWorld(), list));
            }
        }

        return vec3;
    }

    @Override
    public void readAdditionalSaveData(NbtCompound nbt)
    {
        super.readAdditionalSaveData(nbt);
        setInitialFuseTime(nbt.getInt("FuseTime"));
    }

    @Override
    public void addAdditionalSaveData(NbtCompound nbt)
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

    public float getCookScale()
    {
        return entityData.get(COOK_SCALE);
    }

    public void setCookScale(float v)
    {
        entityData.set(COOK_SCALE, v);
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> dataParameter)
    {
        super.onSyncedDataUpdated(dataParameter);

        if (INIT_FUSE_TIME.equals(dataParameter))
        {
            fuseTime = getInitialFuseTime();
        }
    }
}
