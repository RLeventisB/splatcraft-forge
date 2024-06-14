package net.splatcraft.forge.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.splatcraft.forge.blocks.ColoredBarrierBlock;
import net.splatcraft.forge.blocks.StageBarrierBlock;
import net.splatcraft.forge.client.particles.InkExplosionParticleData;
import net.splatcraft.forge.client.particles.InkSplashParticleData;
import net.splatcraft.forge.handlers.WeaponHandler;
import net.splatcraft.forge.registries.SplatcraftEntities;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.InkExplosion;
import org.jetbrains.annotations.NotNull;

public class InkDropEntity extends ThrowableItemProjectile implements IColoredEntity {

    private static final EntityDataAccessor<Integer> DROP_COLOR = SynchedEntityData.defineId(InkDropEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DROP_SIZE = SynchedEntityData.defineId(InkDropEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GRAVITY = SynchedEntityData.defineId(InkDropEntity.class, EntityDataSerializers.FLOAT);
    public int lifespan = 600;
    public ItemStack sourceWeapon = ItemStack.EMPTY;
    public float impactCoverage;
    public InkBlockUtils.InkType inkType;

    public InkDropEntity(EntityType<InkDropEntity> type, Level level) {
        super(type, level);
    }

    public InkDropEntity(Level level, InkProjectileEntity projectile, int color, InkBlockUtils.InkType inkType, float splatSize, ItemStack sourceWeapon) {
        super(SplatcraftEntities.INK_DROP.get(), level);
        setPos(projectile.getX(), projectile.getY(), projectile.getZ());
        setOwner(projectile.getOwner());
        setColor(color);
        setProjectileSize(0.3f);
        this.impactCoverage = splatSize;
        this.inkType = inkType;
        this.sourceWeapon = sourceWeapon;
    }

    public InkDropEntity(Level level, InkProjectileEntity projectile, int color, InkBlockUtils.InkType inkType, float splatSize) {
        this(level, projectile, color, inkType, splatSize, ItemStack.EMPTY);
    }

    public InkDropEntity(Level level, InkProjectileEntity projectile, ItemStack sourceWeapon, InkBlockUtils.InkType inkType, float splatSize) {
        this(level, projectile, ColorUtils.getInkColor(sourceWeapon), inkType, splatSize, sourceWeapon);
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(DROP_COLOR, ColorUtils.DEFAULT);
        entityData.define(DROP_SIZE, 0.3f);
        entityData.define(GRAVITY, 0.075f);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> dataParameter) {
        if (dataParameter.equals(DROP_SIZE))
            refreshDimensions();

        super.onSyncedDataUpdated(dataParameter);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return SplatcraftItems.splattershot.get();
    }

    @Override
    public void tick()
    {
        super.tick();
        Vec3 vel = getDeltaMovement();
        setDeltaMovement(vel.x * 0.9, vel.y, vel.z * 0.9);

        if (isInWater()) {
            discard();
            return;
        }

        if(isRemoved())
            return;

        if (!level.isClientSide && lifespan-- <= 0)
        {
            discard();
        }
    }

    @Override
    protected void updateRotation()
    {
        Vec3 motion = getDeltaMovement();

        if(!Vec3.ZERO.equals(motion))
        {
            this.setXRot(lerpRotation(this.xRotO, (float) (Mth.atan2(motion.y, motion.horizontalDistance()) * (double) (180F / (float) Math.PI))));
            this.setYRot(lerpRotation(this.yRotO, (float) (Mth.atan2(motion.x, motion.z) * (double) (180F / (float) Math.PI))));
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);

        switch (id) {
            case -1 ->
                    level.addParticle(new InkExplosionParticleData(getColor(), .5f), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            case 1 ->
                    level.addParticle(new InkSplashParticleData(getColor(), getProjectileSize()), this.getX() - this.getDeltaMovement().x() * 0.25D, this.getY() + getBbHeight() * 0.5f - this.getDeltaMovement().y() * 0.25D, this.getZ() - this.getDeltaMovement().z() * 0.25D, this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z());
            case 2 ->
                    level.addParticle(new InkSplashParticleData(getColor(), getProjectileSize() * 2), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            case 3 ->
                    level.addParticle(new InkExplosionParticleData(getColor(), getProjectileSize() * 2), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }

    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (InkBlockUtils.canInkPassthrough(level, result.getBlockPos()))
            return;

        if (level.getBlockState(result.getBlockPos()).getBlock() instanceof ColoredBarrierBlock &&
                ((ColoredBarrierBlock) level.getBlockState(result.getBlockPos()).getBlock()).canAllowThrough(result.getBlockPos(), this))
            return;

        super.onHitBlock(result);

        InkExplosion.createInkExplosion(getOwner(), blockPosition(), impactCoverage, 0, 0, true, inkType, sourceWeapon);
        if (level.getBlockState(result.getBlockPos()).getBlock() instanceof StageBarrierBlock)
            level.broadcastEntityEvent(this, (byte) -1);
        else
            level.broadcastEntityEvent(this, (byte) 2);
        if (!level.isClientSide)
        {
            this.discard();
        }
    }

    @Override
    public void shootFromRotation(@NotNull Entity thrower, float pitch, float yaw, float pitchOffset, float velocity, float inaccuracy)
    {
        super.shootFromRotation(thrower, pitch, yaw, pitchOffset, velocity, inaccuracy);
        InkExplosion.createInkExplosion(getOwner(), thrower.blockPosition(), 0.75f, 0, 0, true, inkType, sourceWeapon);

        Vec3 posDiff = new Vec3(0, 0, 0);

        if (thrower instanceof Player player)
        {
            if(WeaponHandler.playerHasPrevPos(player))
                posDiff = thrower.position().subtract(WeaponHandler.getPlayerPrevPos(player));
            if (thrower.isOnGround())
                posDiff.multiply(1, 0, 1);
        }


        moveTo(getX() + posDiff.x(), getY() + posDiff.y(), getZ() + posDiff.z());
        Vec3 throwerSpeed = thrower.getDeltaMovement();
        Vec3 speed = getDeltaMovement()
                .subtract(throwerSpeed.x, thrower.isOnGround() ? 0.0 : throwerSpeed.y, throwerSpeed.z)
                .add(Math.min(2.5, throwerSpeed.x * 0.8), 0.0, Math.min(2.5, throwerSpeed.z * 0.8))
                .add(posDiff.multiply(0.8, 0.8, 0.8));
        setDeltaMovement(speed);
    }


    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy)
    {
        Vec3 vec3 = (new Vec3(x, y, z)).normalize().add(this.random.nextGaussian() * 0.0075 * inaccuracy, this.random.nextGaussian() * 0.0075D * inaccuracy, this.random.nextGaussian() * 0.0075 * inaccuracy);

        double d0 = vec3.horizontalDistance();
        this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * (180.0 / Math.PI)));
        this.setXRot((float)(Mth.atan2(vec3.y, d0) * (180.0 / Math.PI)));
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        if (result instanceof EntityHitResult entityHitResult)
        {
            this.onHitEntity(entityHitResult);
        }
        else if (result instanceof BlockHitResult blockHitResult)
        {
            onHitBlock(blockHitResult);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag nbt)
    {
        super.readAdditionalSaveData(nbt);

        if (nbt.contains("Size"))
            setProjectileSize(nbt.getFloat("Size"));

        ListTag directionTag = nbt.getList("DeltaMotion", DoubleTag.TAG_DOUBLE);
        setDeltaMovement(new Vec3(directionTag.getDouble(0),directionTag.getDouble(1), directionTag.getDouble(2)));

        impactCoverage = nbt.getFloat("ImpactCoverage");

        setColor(nbt.getInt("DropColor"));

        if (nbt.contains("Gravity"))
            setGravity(nbt.getFloat("Gravity"));
        if (nbt.contains("Lifespan"))
            lifespan = nbt.getInt("Lifespan");

        setInvisible(nbt.getBoolean("Invisible"));

        inkType = InkBlockUtils.InkType.values.getOrDefault(new ResourceLocation(nbt.getString("InkType")), InkBlockUtils.InkType.NORMAL);

        sourceWeapon = ItemStack.of(nbt.getCompound("SourceWeapon"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putFloat("Size", getProjectileSize());
        ListTag directionTag = new ListTag();
        Vec3 direction = getDeltaMovement();
        directionTag.add(DoubleTag.valueOf(direction.x));
        directionTag.add(DoubleTag.valueOf(direction.y));
        directionTag.add(DoubleTag.valueOf(direction.z));
        nbt.put("DeltaMotion", directionTag);

        nbt.putFloat("ImpactCoverage", impactCoverage);
        nbt.putInt("DropColor", getColor());

        nbt.putFloat("Gravity", getGravity());
        nbt.putInt("Lifespan", lifespan);

        nbt.putBoolean("Invisible", isInvisible());

        nbt.putString("InkType", inkType.getSerializedName());
        nbt.put("SourceWeapon", sourceWeapon.save(new CompoundTag()));

        super.addAdditionalSaveData(nbt);
        nbt.remove("Item");
    }

    @Deprecated() //Modify sourceWeapon variable instead
    @Override
    public void setItem(ItemStack itemStack) {}

    @Override
    protected ItemStack getItemRaw() {
        return sourceWeapon;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(getProjectileSize() / 2f);
    }
    public float getProjectileSize() {
        return entityData.get(DROP_SIZE);
    }

    public void setProjectileSize(float size) {
        entityData.set(DROP_SIZE, size);
        reapplyPosition();
        refreshDimensions();
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.EMPTY;
    }
    @Override
    public float getGravity() {
        return entityData.get(GRAVITY);
    }

    public void setGravity(float gravity) {
        entityData.set(GRAVITY, gravity);
    }

    @Override
    public int getColor() {
        return entityData.get(DROP_COLOR);
    }

    @Override
    public void setColor(int color) {
        entityData.set(DROP_COLOR, color);
    }
}
