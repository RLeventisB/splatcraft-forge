package net.splatcraft.forge.entities;

import com.google.common.reflect.TypeToken;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import net.splatcraft.forge.VectorUtils;
import net.splatcraft.forge.blocks.ColoredBarrierBlock;
import net.splatcraft.forge.blocks.StageBarrierBlock;
import net.splatcraft.forge.client.particles.InkExplosionParticleData;
import net.splatcraft.forge.client.particles.InkSplashParticleData;
import net.splatcraft.forge.handlers.DataHandler;
import net.splatcraft.forge.items.weapons.WeaponBaseItem;
import net.splatcraft.forge.items.weapons.settings.*;
import net.splatcraft.forge.registries.*;
import net.splatcraft.forge.util.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class InkProjectileEntity extends ThrowableItemProjectile implements IColoredEntity
{
    private static final EntityDataAccessor<String> PROJ_TYPE = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vec2> PROJ_SIZE = SynchedEntityData.defineId(InkProjectileEntity.class, CommonUtils.VEC2SERIALIZER);
    private static final EntityDataAccessor<Float> GRAVITY = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STRAIGHT_SHOT_TIME = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HORIZONTAL_DRAG = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GRAVITY_SPEED_MULT = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<ExtraDataList> EXTRA_DATA = SynchedEntityData.defineId(InkProjectileEntity.class, ExtraSaveData.SERIALIZER);
    private static final EntityDataAccessor<Vec3> SHOOT_DIRECTION = SynchedEntityData.defineId(InkProjectileEntity.class, CommonUtils.VEC3SERIALIZER);

    protected double straightShotTime = -1;
    public float lifespan = 600;
    public boolean explodes = false, bypassMobDamageMultiplier = false, canPierce = false, persistent = false;
    public ItemStack sourceWeapon = ItemStack.EMPTY;
    public float impactCoverage, dropImpactSize, distanceBetweenDrops;
    public float damageMultiplier = 1;
    public boolean causesHurtCooldown, throwerAirborne;
    public AbstractWeaponSettings<?, ?> damage = ShooterWeaponSettings.DEFAULT;
    public InkBlockUtils.InkType inkType;
    private float accumulatedDrops;
    private AttackId attackId = AttackId.NONE;

    public InkProjectileEntity(EntityType<InkProjectileEntity> type, Level level)
    {
        super(type, level);
    }

    public InkProjectileEntity(Level level, LivingEntity thrower, int color, InkBlockUtils.InkType inkType, float projectileSize, AbstractWeaponSettings<?, ?> damage, ItemStack sourceWeapon)
    {
        super(SplatcraftEntities.INK_PROJECTILE.get(), thrower, level);
        setColor(color);
        setProjectileSize(projectileSize);
        this.impactCoverage = projectileSize * 0.85f;
        this.throwerAirborne = !thrower.onGround();
        this.damage = damage;
        this.inkType = inkType;
        this.sourceWeapon = sourceWeapon;
    }

    public InkProjectileEntity(Level level, LivingEntity thrower, int color, InkBlockUtils.InkType inkType, float projectileSize, AbstractWeaponSettings<?, ?> damage)
    {
        this(level, thrower, color, inkType, projectileSize, damage, ItemStack.EMPTY);
    }

    public InkProjectileEntity(Level level, LivingEntity thrower, ItemStack sourceWeapon, InkBlockUtils.InkType inkType, float projectileSize, AbstractWeaponSettings<?, ?> damage)
    {
        this(level, thrower, ColorUtils.getInkColor(sourceWeapon), inkType, projectileSize, damage, sourceWeapon);
    }

    public static void registerDataAccessors()
    {
        EntityDataSerializers.registerSerializer(SHOOT_DIRECTION.getSerializer());
        EntityDataSerializers.registerSerializer(PROJ_SIZE.getSerializer());
        EntityDataSerializers.registerSerializer(EXTRA_DATA.getSerializer());
    }

    public InkProjectileEntity setChargerStats(float charge, ChargerWeaponSettings.ChargerProjectileDataRecord settings)
    {
        dropImpactSize = settings.inkDropCoverage();
        distanceBetweenDrops = settings.distanceBetweenInkDrops();
        if (distanceBetweenDrops > 0)
            accumulatedDrops = CommonUtils.nextFloat(random, 0, 1);
        float range = charge == 1 ? settings.fullyChargedRange() : settings.minChargeRange() + (settings.maxChargeRange() - settings.minChargeRange()) * charge;
        lifespan = range / settings.size();
        impactCoverage = settings.inkCoverageImpact();

        setGravity(0);
        this.canPierce = charge >= settings.piercesAtCharge();
        setProjectileType(Types.CHARGER);
        return this;
    }

    public InkProjectileEntity setBlasterStats(BlasterWeaponSettings settings)
    {
        setCommonProjectileStats(settings.projectileData);
        explodes = true;
        setProjectileType(Types.BLASTER);
        return this;
    }

    public InkProjectileEntity setSlosherStats(CommonRecords.ProjectileDataRecord settings)
    {
        setCommonProjectileStats(settings);
        setProjectileType(Types.ROLLER);
        return this;
    }

    public InkProjectileEntity setShooterStats(ShooterWeaponSettings settings)
    {
        setCommonProjectileStats(settings.projectileData);
        setProjectileType(Types.SHOOTER);
        return this;
    }

    public InkProjectileEntity setSplatlingStats(SplatlingWeaponSettings settings, float charge)
    {
        CommonRecords.ProjectileDataRecord projectileData = charge > 1 ? settings.secondChargeLevelProjectile : settings.firstChargeLevelProjectile;

        setCommonProjectileStats(projectileData);
        addExtraData(new ExtraSaveData.ChargeExtraData(charge));
        setProjectileType(Types.SHOOTER);
        return this;
    }

    public InkProjectileEntity setDualieStats(CommonRecords.ProjectileDataRecord settings)
    {
        setCommonProjectileStats(settings);
        setProjectileType(Types.SHOOTER);
        return this;
    }

    public InkProjectileEntity setRollerSwingStats(RollerWeaponSettings settings)
    {
        setProjectileType(Types.ROLLER);

        if (throwerAirborne)
        {
            accumulatedDrops = 1;
            return setRollerProjectileStats(settings.flingData.projectileData());
        }
        return setRollerProjectileStats(settings.swingData.projectileData());
    }

    public InkProjectileEntity setCommonProjectileStats(CommonRecords.ProjectileDataRecord settings)
    {
        dropImpactSize = settings.inkDropCoverage();
        distanceBetweenDrops = settings.distanceBetweenInkDrops();
        if (distanceBetweenDrops > 0)
            accumulatedDrops = CommonUtils.nextFloat(random, 0, 1);
        impactCoverage = settings.inkCoverageImpact();

        setProjectileVisualSize(settings.visualSize());
        setGravity(settings.gravity());
        setStraightShotTime(settings.straightShotTicks());

        lifespan = settings.lifeTicks();
        setHorizontalDrag(settings.horizontalDrag());
        setGravitySpeedMult(settings.delaySpeedMult());

        return this;
    }

    public InkProjectileEntity setRollerProjectileStats(RollerWeaponSettings.RollerProjectileDataRecord settings)
    {
        dropImpactSize = settings.inkDropCoverage();
        distanceBetweenDrops = settings.distanceBetweenInkDrops();
        impactCoverage = settings.inkCoverageImpact();

        setProjectileVisualSize(settings.visualSize());
        setGravity(settings.gravity());
        setStraightShotTime(settings.straightShotTicks());

        lifespan = 600;
        setHorizontalDrag(settings.horizontalDrag());
        setGravitySpeedMult(settings.delaySpeedMult());

        addExtraData(new ExtraSaveData.RollerDistanceExtraData(position().toVector3f()));

        return this;
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(PROJ_TYPE, Types.SHOOTER);
        entityData.define(COLOR, ColorUtils.DEFAULT);
        entityData.define(PROJ_SIZE, new Vec2(0.2F, 0.6F));
        entityData.define(GRAVITY, 0.175F);
        entityData.define(STRAIGHT_SHOT_TIME, 0F);
        entityData.define(SPEED, 0f);
        entityData.define(HORIZONTAL_DRAG, 1F);
        entityData.define(GRAVITY_SPEED_MULT, 1F);
        entityData.define(SHOOT_DIRECTION, new Vec3(0, 0, 0));
        entityData.define(EXTRA_DATA, new ExtraDataList());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> dataParameter)
    {
        if (dataParameter.equals(PROJ_SIZE))
            refreshDimensions();
        else if (dataParameter.equals(STRAIGHT_SHOT_TIME))
            straightShotTime = entityData.get(STRAIGHT_SHOT_TIME);

        super.onSyncedDataUpdated(dataParameter);
    }

    @Override
    protected @NotNull Item getDefaultItem()
    {
        return SplatcraftItems.splattershot.get();
    }

    @Override
    public void tick()
    {
        tick(SplatcraftGameRules.getIntRuleValue(level(), SplatcraftGameRules.INK_PROJECTILE_FREQUENCY) / 100f);
    }

    public void tick(float timeDelta)
    {
        if (timeDelta > lifespan)
            timeDelta = lifespan;

        Vec3 lastPosition = position();
        Vec3 velocity = getShootVelocity(timeDelta);
        setDeltaMovement(velocity);
        straightShotTime -= timeDelta;

        super.tick();

        if (isInWater())
        {
            discard();
            return;
        }

        if (isRemoved())
        {
            Vec3 nextPosition = lastPosition.add(velocity);
            double frame = (
                (
                    Mth.inverseLerp(getX(), lastPosition.x, nextPosition.x) +
                        Mth.inverseLerp(getY(), lastPosition.y, nextPosition.y) +
                        Mth.inverseLerp(getZ(), lastPosition.z, nextPosition.z)
                ) / 3);
            setDeltaMovement(getDeltaMovement().scale(frame));
            calculateDrops(lastPosition, (float) frame);
            return;
        }

        if (!level().isClientSide() && !persistent && (lifespan -= timeDelta) <= 0)
        {
            ExtraSaveData.ExplosionExtraData explosionData = getExtraDatas().getFirstExtraData(ExtraSaveData.ExplosionExtraData.class);
            if (Objects.equals(getProjectileType(), Types.BLASTER) && explosionData != null)
            {
                InkExplosion.createInkExplosion(getOwner(), position(), explosionData.explosionPaint, explosionData.getRadiuses(false, damageMultiplier), inkType, sourceWeapon, AttackId.NONE);
                createDrop(getX(), getY(), getZ(), 0, explosionData.explosionPaint, timeDelta);
                level().broadcastEntityEvent(this, (byte) 3);
                level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterExplosion, SoundSource.PLAYERS, 0.8F, CommonUtils.triangle(level().getRandom(), 0.95F, 0.095F));
            }
            else
            {
                InkExplosion.createInkExplosion(getOwner(), position(), impactCoverage, 0, 0, inkType, sourceWeapon);
            }
            calculateDrops(lastPosition, timeDelta);
            discard();
        }
        else if (dropImpactSize > 0)
        {
            if (!isInvisible())
            {
                level().broadcastEntityEvent(this, (byte) 1);
            }
            calculateDrops(lastPosition, timeDelta);
        }
    }

    // we NEED the HitResult actual position or else my stupid obsession with partial frame damage falloff will make me perish
    // yes this breaks like 40 fundamentals like encapsulation and mixins but uhhhhhhhhhhhhhhhhhhhhhhhhhhh
    // wait! this can be done with mixins god i am so fucking dumb
    /*public static void superTick(Projectile entity, float timeDelta)
    {
        if (!entity.hasBeenShot)
        {
            entity.gameEvent(GameEvent.PROJECTILE_SHOOT, entity.getOwner());
            entity.hasBeenShot = true;
        }

        if (!entity.leftOwner)
        {
            entity.leftOwner = entity.checkLeftOwner();
        }

        entity.baseTick();

        HitResult hitresult = getHitResultOnMoveVector(entity);
        boolean teleported = false;
        if (hitresult.getType() == HitResult.Type.BLOCK)
        {
            BlockPos blockpos = ((BlockHitResult) hitresult).getBlockPos();
            BlockState blockstate = entity.level().getBlockState(blockpos);
            if (blockstate.is(Blocks.NETHER_PORTAL))
            {
                entity.handleInsidePortal(blockpos);
                teleported = true;
            }
            else if (blockstate.is(Blocks.END_GATEWAY))
            {
                BlockEntity blockentity = entity.level().getBlockEntity(blockpos);
                if (blockentity instanceof TheEndGatewayBlockEntity gatewayBlock && TheEndGatewayBlockEntity.canEntityTeleport(entity))
                {
                    TheEndGatewayBlockEntity.teleportEntity(entity.level(), blockpos, blockstate, entity, gatewayBlock);
                }

                teleported = true;
            }
        }

        if (hitresult.getType() != HitResult.Type.MISS && !teleported && !ForgeEventFactory.onProjectileImpact(entity, hitresult) && !entity.level().isClientSide())
        {
            entity.onHit(hitresult);
        }

        entity.checkInsideBlocks();
        Vec3 deltaMovement = entity.getDeltaMovement();
        double newX = entity.getX() + deltaMovement.x * timeDelta;
        double newY = entity.getY() + deltaMovement.y * timeDelta;
        double newZ = entity.getZ() + deltaMovement.z * timeDelta;

        entity.updateRotation();
        entity.setPos(newX, newY, newZ);
    }

    public static @NotNull HitResult getHitResultOnMoveVector(Projectile entity)
    {
        Vec3 movement = entity.getDeltaMovement();
        Level level = entity.level();
        Vec3 startPos = entity.position();
        Vec3 nextPos = startPos.add(movement);

        HitResult hitresult = level.clip(new ClipContext(startPos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hitresult.getType() != HitResult.Type.MISS)
        {
            nextPos = hitresult.getLocation();
        }

        HitResult hitresult1 = getEntityHitResult(level, entity, startPos, nextPos, entity.getBoundingBox().expandTowards(movement).inflate(1.0), entity::canHitEntity);
        if (hitresult1 != null)
        {
            hitresult = hitresult1;
        }

        return hitresult;
    }

    private static EntityHitResult getEntityHitResult(Level pLevel, Entity projectile, Vec3 pStartVec, Vec3 pEndVec, AABB boundingBox, Predicate<Entity> filter)
    {
        double d0 = Double.MAX_VALUE;
        Entity entity = null;
        Vec3 hitPos = null;

        for (Entity entity1 : pLevel.getEntities(projectile, boundingBox, filter))
        {
            AABB aabb = entity1.getBoundingBox().inflate(0.3f);
            Optional<Vec3> optional = aabb.clip(pStartVec, pEndVec);
            if (optional.isPresent())
            {
                double d1 = pStartVec.distanceToSqr(optional.get());
                if (d1 < d0)
                {
                    entity = entity1;
                    d0 = d1;
                    hitPos = optional.get();
                }
            }
        }

        return entity == null ? null : new EntityHitResult(entity, hitPos);
    }*/

    private void calculateDrops(Vec3 lastPosition, float timeDelta)
    {
        if (distanceBetweenDrops == 0)
        {
            createDrop(getX(), getY(), getZ(), 0, timeDelta);
            return;
        }
        Vec3 deltaMovement = getDeltaMovement();
        float dropsTravelled = (float) deltaMovement.length() / distanceBetweenDrops;
        if (dropsTravelled > 0)
        {
            accumulatedDrops += dropsTravelled;
            while (accumulatedDrops >= 1)
            {
                accumulatedDrops -= 1;

                double progress = accumulatedDrops / dropsTravelled;
                Vec3 dropPos = VectorUtils.lerp(progress, position(), lastPosition);

                createDrop(dropPos.x(), dropPos.y(), dropPos.z(), progress, timeDelta);
            }
        }
    }

    public void createDrop(double dropX, double dropY, double dropZ, double extraFrame, float timeDelta)
    {
        createDrop(dropX, dropY, dropZ, extraFrame, dropImpactSize, timeDelta);
    }

    public void createDrop(double dropX, double dropY, double dropZ, double extraFrame, float dropImpactSize, float timeDelta)
    {
        InkDropEntity proj = new InkDropEntity(level(), this, getColor(), inkType, dropImpactSize, sourceWeapon);
        proj.moveTo(dropX, dropY, dropZ);
        level().addFreshEntity(proj);
        proj.tick((float) (extraFrame + timeDelta));
    }

    private Vec3 getShootVelocity(float timeDelta)
    {
        Vec3 shootDirection = entityData.get(SHOOT_DIRECTION);
        double frame = getMaxStraightShotTime() - straightShotTime;
        double[] speedData = getSpeed(frame, getMaxStraightShotTime(), timeDelta);
        Vec3 velocity = shootDirection.scale(speedData[0]);
        if (speedData[1] <= 0)
            return velocity;
        return velocity.subtract(0, getGravity() * speedData[1] * timeDelta, 0);
    }

    public double[] getSpeed(double frame, float straightShotFrame, float timeDelta)
    {
        double speed = entityData.get(SPEED);
        double fallenFrames = frame - straightShotFrame;
        double fallenFramesNext = fallenFrames + timeDelta;

        if (fallenFramesNext < 0) // not close to falling
        {
            return new double[]{speed * timeDelta, fallenFramesNext};
        }
        else if (fallenFramesNext >= timeDelta) // already falling
        {
            speed *= getHorizontalDrag() * getGravitySpeedMult() * Math.pow(getHorizontalDrag(), fallenFrames);
            return new double[]{speed * timeDelta, fallenFramesNext};
        }
        double straightFraction = timeDelta - fallenFramesNext;
        return new double[]{(speed * straightFraction + speed * getHorizontalDrag() * getGravitySpeedMult() * Math.pow(getHorizontalDrag(), fallenFrames) * fallenFramesNext), fallenFramesNext};
    }

    @Override
    public void updateRotation()
    {
        Vec3 motion = getDeltaMovement();

        if (!Vec3.ZERO.equals(motion))
        {
            this.setXRot(lerpRotation(this.xRotO, (float) (Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG)));
            this.setYRot(lerpRotation(this.yRotO, (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG)));
        }
    }

    @Override
    public void handleEntityEvent(byte id)
    {
        super.handleEntityEvent(id);
        switch (id)
        {
            case -1 ->
                level().addParticle(new InkExplosionParticleData(getColor(), .5f), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            case 1 ->
            {
                if (getProjectileType().equals(Types.CHARGER))
                    level().addParticle(new InkSplashParticleData(getColor(), getProjectileSize() * 0.4f), this.getX() - this.getDeltaMovement().x() * 0.25D, this.getY() + getBbHeight() * 0.5f - this.getDeltaMovement().y() * 0.25D, this.getZ() - this.getDeltaMovement().z() * 0.25D, 0, -0.1, 0);
                else
                    level().addParticle(new InkSplashParticleData(getColor(), getProjectileSize() * 0.4f), this.getX() - this.getDeltaMovement().x() * 0.25D, this.getY() + getBbHeight() * 0.5f - this.getDeltaMovement().y() * 0.25D, this.getZ() - this.getDeltaMovement().z() * 0.25D, this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z());
            }
            case 2 ->
                level().addParticle(new InkSplashParticleData(getColor(), getProjectileSize() * 2), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            case 3 ->
                level().addParticle(new InkExplosionParticleData(getColor(), getProjectileSize() * 2), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result)
    {
        super.onHitEntity(result);

        Entity target = result.getEntity();
        float storedCrystalSoundIntensity = crystalSoundIntensity;

        // idk vector math so i read https://discussions.unity.com/t/inverselerp-for-vector3/177038 for this
        // lol i didnt even use it

        Vec3 nextPosition = position().add(getDeltaMovement());
        Vec3 impactPos = result.getLocation();
        Vec3 oldPos = position();

        List<Double> values = new ArrayList<>();
        values.add(Mth.inverseLerp(impactPos.x, getX(), nextPosition.x));
        values.add(Mth.inverseLerp(impactPos.y, getY(), nextPosition.y));
        values.add(Mth.inverseLerp(impactPos.z, getZ(), nextPosition.z));
        values.removeIf(d -> !Double.isFinite(d));
        crystalSoundIntensity = (float) values.stream().mapToDouble(v -> v).average().orElse(0.5);
        setPos(impactPos);
        float dmg = damage.calculateDamage(this, getExtraDatas()) * damageMultiplier;
        if (canPierce)
            setPos(oldPos);
        crystalSoundIntensity = storedCrystalSoundIntensity;

        if (!level().isClientSide() && target instanceof SpawnShieldEntity && !InkDamageUtils.canDamage(target, this))
        {
            discard();
            level().broadcastEntityEvent(this, (byte) -1);
        }

        if (target instanceof LivingEntity livingTarget)
        {
            if (InkDamageUtils.isSplatted(livingTarget)) return;

            boolean didDamage = InkDamageUtils.doDamage(livingTarget, dmg, getOwner(), this, sourceWeapon, SplatcraftDamageTypes.INK_SPLAT, causesHurtCooldown, attackId);
            if (!level().isClientSide && didDamage)
            {
                ExtraSaveData.ChargeExtraData chargeData = getExtraDatas().getFirstExtraData(ExtraSaveData.ChargeExtraData.class);
                if (Objects.equals(getProjectileType(), Types.CHARGER) && chargeData != null && chargeData.charge >= 1.0f && InkDamageUtils.isSplatted(livingTarget) && dmg > 20 ||
                    Objects.equals(getProjectileType(), Types.BLASTER))
                {
                    level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterDirect, SoundSource.PLAYERS, 0.8F, 1);
                }
            }
        }
        if (!canPierce)
        {
            ExtraSaveData.ExplosionExtraData explosionData = getExtraDatas().getFirstExtraData(ExtraSaveData.ExplosionExtraData.class);
            if (explodes && explosionData != null)
            {
                InkExplosion.createInkExplosion(getOwner(), impactPos, explosionData.explosionPaint, explosionData.getRadiuses(false, damageMultiplier), inkType, sourceWeapon, explosionData.newAttackId ? AttackId.NONE : attackId);
                level().broadcastEntityEvent(this, (byte) 3);
                level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterExplosion, SoundSource.PLAYERS, 0.8F, CommonUtils.triangle(level().getRandom(), 0.95F, 0.095F));
            }
            else
                level().broadcastEntityEvent(this, (byte) 2);

            if (!level().isClientSide())
                discard();
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result)
    {
        if (level().isClientSide())
        {
            super.onHitBlock(result);
            return;
        }

        if (InkBlockUtils.canInkPassthrough(level(), result.getBlockPos()))
            return;

        if (level().getBlockState(result.getBlockPos()).getBlock() instanceof ColoredBarrierBlock coloredBarrierBlock &&
            coloredBarrierBlock.canAllowThrough(result.getBlockPos(), this))
            return;

        super.onHitBlock(result);
        setPos(result.getLocation());
        if (level().getBlockState(result.getBlockPos()).getBlock() instanceof StageBarrierBlock)
            level().broadcastEntityEvent(this, (byte) -1);
        else
        {
            level().broadcastEntityEvent(this, (byte) 2);
            Vec3 impactPos = InkExplosion.adjustPosition(result.getLocation(), result.getDirection().getNormal());
            ExtraSaveData.ExplosionExtraData explosionData = getExtraDatas().getFirstExtraData(ExtraSaveData.ExplosionExtraData.class);
            if (explodes && explosionData != null)
            {
                InkExplosion.createInkExplosion(getOwner(), impactPos, explosionData.explosionPaint, explosionData.getRadiuses(true, damageMultiplier), inkType, sourceWeapon, explosionData.newAttackId ? AttackId.NONE : attackId);
                level().broadcastEntityEvent(this, (byte) 3);
//				level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterExplosion, SoundSource.PLAYERS, 0.8F, CommonUtils.triangle(level().getRandom(), 0.95F, 0.095F));
            }
            else
            {
                InkExplosion.createInkExplosion(getOwner(), impactPos, impactCoverage, 0, 0, inkType, sourceWeapon);
            }
        }
        if (!level().isClientSide())
            this.discard();
    }

    @Override
    public void shootFromRotation(@NotNull Entity thrower, float pitch, float yaw, float pitchOffset, float velocity, float inaccuracy)
    {
        shootFromRotation(thrower, pitch, yaw, pitchOffset, velocity, inaccuracy, 0);
    }

    public void shootFromRotation(Entity thrower, float pitch, float yaw, float pitchOffset, float velocity, float inaccuracy, float partialTicks)
    {
        double f = -Math.sin(yaw * Mth.DEG_TO_RAD) * Math.cos(pitch * Mth.DEG_TO_RAD);
        double f1 = -Math.sin((pitch + pitchOffset) * Mth.DEG_TO_RAD);
        double f2 = Math.cos(yaw * Mth.DEG_TO_RAD) * Math.cos(pitch * Mth.DEG_TO_RAD);
        Vec3 posDiff = new Vec3(0, 0, 0);

        try
        {
            posDiff = thrower.position().subtract(CommonUtils.getOldPosition(thrower));
            if (thrower.onGround())
                posDiff.multiply(1, 0, 1);
            posDiff = posDiff.scale(0.8);
        }
        catch (NullPointerException ignored)
        {
        }

        f += posDiff.x;
        f1 += posDiff.y;
        f2 += posDiff.z;
        this.shoot(f, f1, f2, velocity, inaccuracy, partialTicks);
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy)
    {
        shoot(x, y, z, velocity, inaccuracy, 0);
    }

    public void shoot(double x, double y, double z, float velocity, float inaccuracy, float partialTicks)
    {
        // 1.34f is only to make the acurracy more aproppiate in minecraft since 5 degrees in splatoon isnt as impactful as 5 degrees in minecraft kinda
        float usedInaccuracy = inaccuracy * Mth.DEG_TO_RAD * 1.34f;
        Vec3 vec3 = new Vec3(x, y, z)
            .yRot((this.random.nextFloat() * 2f - 1f) * usedInaccuracy)
            .xRot((this.random.nextFloat() * 2f - 1f) * usedInaccuracy * 0.5625f);

        entityData.set(SHOOT_DIRECTION, vec3);
        vec3 = vec3.scale(velocity);

        double d0 = vec3.horizontalDistance();
        this.setYRot((float) (Mth.atan2(vec3.x, vec3.z) * Mth.RAD_TO_DEG));
        this.setXRot((float) (Mth.atan2(vec3.y, d0) * Mth.RAD_TO_DEG));
        this.yRotO = this.getViewYRot(partialTicks);
        this.xRotO = this.getViewXRot(partialTicks);
        setDeltaMovement(vec3);

        entityData.set(SPEED, velocity);
    }

    @Override
    public void onHit(HitResult result)
    {
        HitResult.Type rayType = result.getType();
        if (rayType == HitResult.Type.ENTITY)
        {
            this.onHitEntity((EntityHitResult) result);
        }
        else if (rayType == HitResult.Type.BLOCK)
        {
            onHitBlock((BlockHitResult) result);
        }
    }

    @Override
    public boolean canHitEntity(@NotNull Entity entity)
    {
        return entity != getOwner() && entity.canBeHitByProjectile() && InkDamageUtils.canDamage(entity, entityData.get(COLOR));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag nbt)
    {
        super.readAdditionalSaveData(nbt);

        if (nbt.contains("Size"))
            setProjectileSize(nbt.getFloat("Size"));
        if (nbt.contains("VisualSize"))
            setProjectileVisualSize(nbt.getFloat("VisualSize"));

        impactCoverage = nbt.contains("ImpactCoverage") ? nbt.getFloat("ImpactCoverage") : getProjectileSize() * 0.85f;

        if (nbt.contains("Color"))
            setColor(ColorUtils.getColorFromNbt(nbt));

        entityData.set(SPEED, nbt.getFloat("Speed"));
        setHorizontalDrag(nbt.getFloat("HorizontalDrag"));
        setGravitySpeedMult(nbt.getFloat("GravitySpeedMult"));

        if (nbt.contains("Gravity"))
            setGravity(nbt.getFloat("Gravity"));
        if (nbt.contains("Lifespan"))
            lifespan = nbt.getInt("Lifespan");
        if (nbt.contains("MaxStraightShotTime"))
            setStraightShotTime(nbt.getFloat("StraightShotTime"));
        if (nbt.contains("StraightShotTime"))
            straightShotTime = nbt.getDouble("StraightShotTime");

        ListTag directionTag = nbt.getList("Direction", DoubleTag.TAG_DOUBLE);
        entityData.set(SHOOT_DIRECTION, new Vec3(directionTag.getDouble(0), directionTag.getDouble(1), directionTag.getDouble(2)));

        distanceBetweenDrops = nbt.getFloat("TrailFrequency");
        dropImpactSize = nbt.getFloat("TrailSize");
        bypassMobDamageMultiplier = nbt.getBoolean("BypassMobDamageMultiplier");
        canPierce = nbt.getBoolean("CanPierce");
        explodes = nbt.getBoolean("Explodes");
        persistent = nbt.getBoolean("Persistent");
        causesHurtCooldown = nbt.getBoolean("CausesHurtCooldown");

        setInvisible(nbt.getBoolean("Invisible"));

        String type = nbt.getString("ProjectileType");
        setProjectileType(type.isEmpty() ? Types.DEFAULT : type);
        inkType = InkBlockUtils.InkType.values.getOrDefault(new ResourceLocation(nbt.getString("InkType")), InkBlockUtils.InkType.NORMAL);

        sourceWeapon = ItemStack.of(nbt.getCompound("SourceWeapon"));

        AbstractWeaponSettings<?, ?> settings = DataHandler.WeaponStatsListener.SETTINGS.get(new ResourceLocation(nbt.getString(nbt.getString("Settings"))));
        if (settings != null)
            damage = settings;
        else if (sourceWeapon.getItem() instanceof WeaponBaseItem<?> weapon)
            damage = weapon.getSettings(sourceWeapon);
        if (nbt.contains("AttackId"))
            attackId = AttackId.readNbt(nbt.getCompound("AttackId"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt)
    {
        nbt.putFloat("Size", getProjectileSize());
        nbt.putFloat("VisualSize", getProjectileVisualSize());
        nbt.putInt("Color", getColor());

        nbt.putFloat("Speed", entityData.get(SPEED));
        nbt.putFloat("HorizontalDrag", getHorizontalDrag());
        nbt.putFloat("GravitySpeedMult", getGravitySpeedMult());
        nbt.putFloat("MaxStraightShotTime", getMaxStraightShotTime());
        nbt.putDouble("StraightShotTime", straightShotTime);

        ListTag directionTag = new ListTag();
        Vec3 direction = getShotDirection();
        directionTag.add(DoubleTag.valueOf(direction.x));
        directionTag.add(DoubleTag.valueOf(direction.y));
        directionTag.add(DoubleTag.valueOf(direction.z));
        nbt.put("Direction", directionTag);

        nbt.putFloat("Gravity", getGravity());
        nbt.putFloat("Lifespan", lifespan);
        nbt.putFloat("TrailSize", dropImpactSize);
        nbt.putFloat("TrailFrequency", distanceBetweenDrops);
        nbt.putBoolean("BypassMobDamageMultiplier", bypassMobDamageMultiplier);
        nbt.putBoolean("CanPierce", canPierce);
        nbt.putBoolean("Explodes", explodes);
        nbt.putBoolean("Persistent", persistent);
        nbt.putBoolean("CausesHurtCooldown", causesHurtCooldown);

        nbt.putBoolean("Invisible", isInvisible());

        nbt.putString("ProjectileType", getProjectileType());
        nbt.putString("InkType", inkType.getSerializedName());
        nbt.put("SourceWeapon", sourceWeapon.save(new CompoundTag()));
        if (attackId != AttackId.NONE)
            nbt.put("AttackId", attackId.serializeNbt());

        super.addAdditionalSaveData(nbt);
        nbt.remove("Item");
    }

    private @NotNull Float getGravitySpeedMult()
    {
        return entityData.get(GRAVITY_SPEED_MULT);
    }

    private void setGravitySpeedMult(float gravitySpeedMult)
    {
        entityData.set(GRAVITY_SPEED_MULT, gravitySpeedMult);
    }

    private @NotNull Float getHorizontalDrag()
    {
        return entityData.get(HORIZONTAL_DRAG);
    }

    private void setHorizontalDrag(float horizontalDrag)
    {
        entityData.set(HORIZONTAL_DRAG, horizontalDrag);
    }

    /**
     * @return a exposed list to the synched list that manages the extra data sent to the connection or smtinh
     * @apiNote this list if updated isnt going to be synched!!!!! for this use addExtraData instead, or sendExtraData
     */
    public ExtraDataList getExtraDatas()
    {
        return entityData.get(EXTRA_DATA);
    }

    public void setExtraDataList(ExtraDataList list)
    {
        entityData.set(EXTRA_DATA, list);
    }

    public void addExtraData(ExtraSaveData data)
    {
        ExtraDataList list = entityData.get(EXTRA_DATA);
        list.add(data);
        entityData.set(EXTRA_DATA, list);
    }

    @Deprecated //Modify sourceWeapon variable instead
    @Override
    public void setItem(@NotNull ItemStack itemStack)
    {
    }

    @Override
    protected @NotNull ItemStack getItemRaw()
    {
        return sourceWeapon;
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket()
    {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose)
    {
        return super.getDimensions(pose).scale(getProjectileSize() / 2f);
    }

    @Override
    public float getGravity()
    {
        return entityData.get(GRAVITY);
    }

    public void setGravity(float gravity)
    {
        entityData.set(GRAVITY, gravity);
    }

    public double getStraightShotTime()
    {
        return straightShotTime;
    }

    public float calculateDamageDecay(float baseDamage, float startTick, float decayPerTick, float minDamage)
    {
        // getMaxStraightShotTime() - straightShotTime is just tickCount but it counts the partial ticks too (and time delta!!! yay i hate myself)
        double tickCount = getMaxStraightShotTime() - straightShotTime + crystalSoundIntensity;

        double diff = tickCount - startTick;
        if (diff < 0)
            return baseDamage;
        return (float) Math.max(minDamage, baseDamage - decayPerTick * diff);
    }

    public float getMaxStraightShotTime()
    {
        return entityData.get(STRAIGHT_SHOT_TIME);
    }

    public void setStraightShotTime(float time)
    {
        entityData.set(STRAIGHT_SHOT_TIME, time);
    }

    public Vec3 getShotDirection()
    {
        return entityData.get(SHOOT_DIRECTION);
    }

    @Override
    public boolean isNoGravity()
    {
        return straightShotTime > 0 || getGravity() == 0 || super.isNoGravity();
    }

    public float getProjectileSize()
    {
        return entityData.get(PROJ_SIZE).x;
    }

    public float getProjectileVisualSize()
    {
        return entityData.get(PROJ_SIZE).y;
    }

    public void setProjectileVisualSize(float visualSize)
    {
        entityData.set(PROJ_SIZE, new Vec2(getProjectileSize(), visualSize));
    }

    public void setProjectileSize(float size)
    {
        entityData.set(PROJ_SIZE, new Vec2(size, size * 3));
        reapplyPosition();
        refreshDimensions();
    }

    @Override
    public void remove(@NotNull RemovalReason pReason)
    {
        attackId.projectileRemoved();
        super.remove(pReason);
    }

    @Override
    public @NotNull ItemStack getItem()
    {
        return ItemStack.EMPTY;
    }

    @Override
    public int getColor()
    {
        return entityData.get(COLOR);
    }

    @Override
    public void setColor(int color)
    {
        entityData.set(COLOR, color);
    }

    public String getProjectileType()
    {
        return entityData.get(PROJ_TYPE);
    }

    public void setProjectileType(String v)
    {
        entityData.set(PROJ_TYPE, v);
    }

    public RandomSource getRandom()
    {
        return random;
    }

    public void setAttackId(AttackId attackId)
    {
        this.attackId = attackId;
    }

    public static class Types
    {
        public static final String DEFAULT = "default";
        public static final String SHOOTER = "shooter";
        public static final String CHARGER = "charger";
        public static final String ROLLER = "roller";
        public static final String BLASTER = "blaster";
    }

    public static final class ExtraDataList extends ArrayList<ExtraSaveData>
    {
        public ExtraDataList(int count)
        {
            super(count);
        }

        public ExtraDataList()
        {
            super();
        }

        public ExtraDataList(Collection<ExtraSaveData> collection)
        {
            super(collection);
        }

        public <T extends ExtraSaveData> T getFirstExtraData(Class<T> tClass)
        {
            TypeToken<T> token = TypeToken.of(tClass);

            for (ExtraSaveData extraData : this)
            {
                if (token.isSupertypeOf(TypeToken.of(extraData.getClass())))
                    return (T) extraData;
            }
            return null;
        }

        public ExtraDataList cloneList()
        {
            ExtraSaveData[] array = toArray(new ExtraSaveData[size()]);
            return new ExtraDataList(Arrays.stream(array).toList());
        }
    }
}