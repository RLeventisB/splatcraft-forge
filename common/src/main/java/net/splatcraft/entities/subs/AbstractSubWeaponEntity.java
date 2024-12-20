package net.splatcraft.entities.subs;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.entities.IColoredEntity;
import net.splatcraft.items.weapons.SubWeaponItem;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class AbstractSubWeaponEntity extends Entity implements IColoredEntity
{
    protected static final RegistryKey<DamageType> SPLASH_DAMAGE_TYPE = SplatcraftDamageTypes.INK_SPLAT;
    private static final TrackedData<InkColor> COLOR = DataTracker.registerData(AbstractSubWeaponEntity.class, CommonUtils.INKCOLORDATAHANDLER);
    private static final TrackedData<ItemStack> DATA_ITEM_STACK = DataTracker.registerData(AbstractSubWeaponEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    public boolean isItem = false;
    public boolean bypassMobDamageMultiplier = false;
    public InkBlockUtils.InkType inkType;
    public ItemStack sourceWeapon = ItemStack.EMPTY;
    private UUID ownerUUID;
    private int ownerNetworkId;
    private boolean leftOwner;

    //	@Deprecated //use AbstractWeaponEntity.create
//	no
    public AbstractSubWeaponEntity(EntityType<? extends AbstractSubWeaponEntity> type, World world)
    {
        super(type, world);
    }

    public static <A extends AbstractSubWeaponEntity> A create(EntityType<A> type, World world, @NotNull LivingEntity thrower, ItemStack sourceWeapon)
    {
        return create(type, world, thrower, ColorUtils.getInkColor(sourceWeapon), InkBlockUtils.getInkType(thrower), sourceWeapon);
    }

    public static <A extends AbstractSubWeaponEntity> A create(EntityType<A> type, World world, LivingEntity thrower, InkColor color, InkBlockUtils.InkType inkType, ItemStack sourceWeapon)
    {
        A result = create(type, world, thrower.getX(), thrower.getEyeY() - 0.1, thrower.getZ(), color, inkType, sourceWeapon);
        result.setOwner(thrower);

        return result;
    }

    public static <A extends AbstractSubWeaponEntity> A create(EntityType<A> type, World world, double x, double y, double z, InkColor color, InkBlockUtils.InkType inkType, ItemStack sourceWeapon)
    {
        A result = type.create(world);
        result.setPos(x, y, z);
        result.setColor(color);
        result.inkType = inkType;
        result.sourceWeapon = sourceWeapon;
        result.setItem(sourceWeapon);

        result.readItemData(sourceWeapon.get(SplatcraftComponents.SUB_WEAPON_DATA));

        return result;
    }

    public void readItemData(NbtCompound nbt)
    {

    }

    @Override
    public void tick()
    {
        if (!leftOwner)
        {
            leftOwner = checkLeftOwner();
        }

        super.tick();

        if (isSubmergedInWater())
        {
            getWorld().sendEntityStatus(this, (byte) -1);
            discard();
        }

        Vec3d raytraceOffset = new Vec3d(getWidth() / 2f * Math.signum(getVelocity().x), getHeight() * Math.max(0, Math.signum(getVelocity().y)), getWidth() / 2f * Math.signum(getVelocity().z));

        setVelocity(getVelocity().add(raytraceOffset));
        HitResult raytraceresult = ProjectileUtil.getCollision(this, this::canHitEntity);
        setVelocity(getVelocity().subtract(raytraceOffset));

        // todo: adapt this code better
        boolean flag = false;
        if (raytraceresult.getType() == HitResult.Type.BLOCK)
        {
            BlockHitResult blockHitResult = (BlockHitResult) raytraceresult;
            onBlockHit(blockHitResult);
            BlockPos blockPos = blockHitResult.getBlockPos();
            getWorld().emitGameEvent(GameEvent.PROJECTILE_LAND, blockPos, GameEvent.Emitter.of(this, getWorld().getBlockState(blockPos)));
        }

        if (raytraceresult.getType() != HitResult.Type.MISS && !flag)
        {
            onHit(raytraceresult);
        }

        checkBlockCollision();
        Vec3d deltaMovement = getVelocity();

        Vec3d newPos = new Vec3d(getX() + getVelocity().x, getY() + getVelocity().y, getZ() + getVelocity().z);

        double newX = getX() + deltaMovement.x;
        double newY = getY() + deltaMovement.y;
        double newZ = getZ() + deltaMovement.z;
        updateRotation();
        float f = 0.941192F;
        if (isSubmergedInWater())
        {
            for (int i = 0; i < 4; ++i)
            {
                getWorld().addParticle(ParticleTypes.BUBBLE, newX - deltaMovement.x * 0.25D, newY - deltaMovement.y * 0.25D, newZ - deltaMovement.z * 0.25D, deltaMovement.x, deltaMovement.y, deltaMovement.z);
            }

            f = 0.8F;
        }

        setVelocity(deltaMovement.multiply(f));
        if (!hasNoGravity())
        {
            Vec3d vector3d1 = getVelocity();
            setVelocity(vector3d1.x, vector3d1.y - getGravity(), vector3d1.z);
        }

        if (handleMovement())
            setPos(newPos.x, newPos.y, newPos.z);
    }

    @Override
    public void handleStatus(byte id)
    {
        super.handleStatus(id);

        if (id == -1)
        {
            getWorld().addParticle(new InkExplosionParticleData(getColor(), .5f), getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    public void shoot(Entity thrower, float pitch, float yaw, float pitchOffset, float velocity, float inaccuracy)
    {
        shootFromRotation(thrower, pitch, yaw, pitchOffset, velocity, inaccuracy);

        Vec3d posDiff = new Vec3d(0, 0, 0);

        if (thrower instanceof PlayerEntity player)
        {
            try
            {
                posDiff = thrower.getPos().subtract(player.getLerpedPos(0));
                if (thrower.isOnGround())
                    posDiff.multiply(1, 0, 1);
            }
            catch (NullPointerException ignored)
            {
            }
        }

        refreshPositionAfterTeleport(getX() + posDiff.x, getY() + posDiff.y, getZ() + posDiff.z);
        setVelocity(getVelocity().add(posDiff.multiply(0.8, 0.8, 0.8)));
    }

    public double getGravity()
    {
        return 0.09;
    }

    protected boolean handleMovement()
    {
        return true;
    }

    public SubWeaponSettings getSettings()
    {
        if (getItemRaw().getItem() instanceof SubWeaponItem sub)
        {
            return sub.getSettings(getItemRaw());
        }
        return SubWeaponSettings.DEFAULT;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt)
    {
        nbt.put("Color", getColor().getNbt());
        nbt.putBoolean("BypassMobDamageMultiplier", bypassMobDamageMultiplier);
        nbt.putString("InkType", inkType.getSerializedName());
        nbt.put("SourceWeapon", sourceWeapon.encode(getRegistryManager()));

        if (ownerUUID != null)
            nbt.putUuid("Owner", ownerUUID);

        if (leftOwner)
            nbt.putBoolean("LeftOwner", true);

        ItemStack itemstack = getItemRaw();
        if (!itemstack.isEmpty())
            nbt.put("Item", itemstack.encode(getWorld().getRegistryManager()));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt)
    {
        if (nbt.contains("Color"))
            setColor(InkColor.getFromNbt(nbt.get("Color")));
        bypassMobDamageMultiplier = nbt.getBoolean("DypassMobDamageMultiplier");
        inkType = InkBlockUtils.InkType.values.getOrDefault(Identifier.of(nbt.getString("InkType")), InkBlockUtils.InkType.NORMAL);
        sourceWeapon = ItemStack.CODEC.decode(NbtOps.INSTANCE, nbt.getCompound("SourceWeapon")).getOrThrow().getFirst();

        if (nbt.contains("Owner"))
            ownerUUID = nbt.getUuid("Owner");

        leftOwner = nbt.getBoolean("LeftOwner");

        ItemStack itemstack = ItemStack.fromNbtOrEmpty(getRegistryManager(), nbt.getCompound("Item"));
        setItem(itemstack);
    }

    protected void onBlockHit(BlockHitResult result)
    {
    }

    protected void onEntityHit(EntityHitResult result)
    {
    }

    protected void onHit(HitResult result)
    {
        HitResult.Type rayType = result.getType();
        if (rayType == HitResult.Type.ENTITY)
        {
            onEntityHit((EntityHitResult) result);
        }
        else if (rayType == HitResult.Type.BLOCK)
        {
            onBlockHit((BlockHitResult) result);
        }
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder)
    {
        builder.add(DATA_ITEM_STACK, ItemStack.EMPTY);
        builder.add(COLOR, ColorUtils.getDefaultColor());
    }

    @Override
    public InkColor getColor()
    {
        return dataTracker.get(COLOR);
    }

    @Override
    public void setColor(InkColor color)
    {
        dataTracker.set(COLOR, color);
    }

    protected abstract Item getDefaultItem();

    protected ItemStack getItemRaw()
    {
        return getDataTracker().get(DATA_ITEM_STACK);
    }

    public ItemStack getItem()
    {
        ItemStack itemstack = getItemRaw();
        return itemstack.isEmpty() ? new ItemStack(getDefaultItem()) : itemstack;
    }

    public void setItem(ItemStack item)
    {
        if (item.getItem() != getDefaultItem())
        {
            getDataTracker().set(DATA_ITEM_STACK, Util.make(item.copy(), (itemStack) ->
                itemStack.setCount(1)));
        }
    }

    @Nullable
    public Entity getOwner()
    {
        if (ownerUUID != null && getWorld() instanceof ServerWorld serverLevel)
        {
            return serverLevel.getEntity(ownerUUID);
        }
        else
        {
            return ownerNetworkId != 0 ? getWorld().getEntityById(ownerNetworkId) : null;
        }
    }

    public void setOwner(@Nullable Entity p_212361_1_)
    {
        if (p_212361_1_ != null)
        {
            ownerUUID = p_212361_1_.getUuid();
            ownerNetworkId = p_212361_1_.getId();
        }
    }

    private boolean checkLeftOwner()
    {
        Entity entity = getOwner();
        if (entity != null)
        {
            for (Entity entity1 : getWorld().getOtherEntities(this, getBoundingBox().stretch(getVelocity()).expand(1.0D), (entity1) ->
                !entity1.isSpectator() && entity1.canHit()))
            {
                if (entity1.getRootVehicle() == entity.getRootVehicle())
                {
                    return false;
                }
            }
        }

        return true;
    }

    public void shoot(double x, double y, double z, float speed, float inaccuracy)
    {
        Vec3d vec3 = (new Vec3d(x, y, z)).normalize().add(random.nextGaussian() * (double) 0.0075F * (double) inaccuracy, random.nextGaussian() * (double) 0.0075F * (double) inaccuracy, random.nextGaussian() * (double) 0.0075F * (double) inaccuracy).multiply(speed);
        setVelocity(vec3);
        double d0 = vec3.horizontalLength();
        setYaw((float) (MathHelper.atan2(vec3.x, vec3.z) * MathHelper.DEGREES_PER_RADIAN));
        setPitch((float) (MathHelper.atan2(vec3.y, d0) * MathHelper.DEGREES_PER_RADIAN));
        prevYaw = getYaw();
        prevPitch = getPitch();
    }

    public void shootFromRotation(Entity p_234612_1_, float p_234612_2_, float p_234612_3_, float p_234612_4_, float p_234612_5_, float p_234612_6_)
    {
        float f = -MathHelper.sin(p_234612_3_ * (MathHelper.RADIANS_PER_DEGREE)) * MathHelper.cos(p_234612_2_ * (MathHelper.RADIANS_PER_DEGREE));
        float f1 = -MathHelper.sin((p_234612_2_ + p_234612_4_) * (MathHelper.RADIANS_PER_DEGREE));
        float f2 = MathHelper.cos(p_234612_3_ * (MathHelper.RADIANS_PER_DEGREE)) * MathHelper.cos(p_234612_2_ * (MathHelper.RADIANS_PER_DEGREE));
        shoot(f, f1, f2, p_234612_5_, p_234612_6_);
        Vec3d vector3d = p_234612_1_.getVelocity();
        setVelocity(getVelocity().add(vector3d.x, p_234612_1_.isOnGround() ? 0.0D : vector3d.y, vector3d.z));
    }

    @Environment(EnvType.CLIENT)
    public void lerpMotion(double x, double y, double z)
    {
        setVelocity(x, y, z);
        if (prevPitch == 0.0F && prevYaw == 0.0F)
        {
            float f = MathHelper.sqrt((float) (x * x + z * z));
            setPitch((float) (MathHelper.atan2(y, f) * MathHelper.DEGREES_PER_RADIAN));
            setYaw((float) (MathHelper.atan2(x, z) * MathHelper.DEGREES_PER_RADIAN));
            prevPitch = getPitch();
            prevYaw = getYaw();
            refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), getPitch());
        }
    }

    public boolean canHitEntity(Entity entity)
    {
        if (!entity.isSpectator() && entity.isAlive() && entity.canHit())
        {
            Entity owner = getOwner();
            return owner == null || leftOwner || !owner.isConnectedThroughVehicle(entity);
        }
        else
        {
            return false;
        }
    }

    public void updateRotation()
    {
        Vec3d vec3 = getVelocity();
        double d0 = vec3.horizontalLength();
        setPitch(CommonUtils.lerpRotation(0.2f, prevPitch, (float) (MathHelper.atan2(vec3.y, d0) * MathHelper.DEGREES_PER_RADIAN)));
        setYaw(CommonUtils.lerpRotation(0.2f, prevYaw, (float) (MathHelper.atan2(vec3.x, vec3.z) * MathHelper.DEGREES_PER_RADIAN)));
    }
}
