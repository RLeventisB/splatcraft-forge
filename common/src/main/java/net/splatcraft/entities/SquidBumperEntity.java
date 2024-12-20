package net.splatcraft.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Arm;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateInkOverlayPacket;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class SquidBumperEntity extends LivingEntity implements IColoredEntity
{
    public static final float maxInkHealth = 20.0F;
    public static final int maxRespawnTime = 60;
    private static final TrackedData<Boolean> IMMORTAL = DataTracker.registerData(SquidBumperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<InkColor> COLOR = DataTracker.registerData(SquidBumperEntity.class, CommonUtils.INKCOLORDATAHANDLER);
    private static final TrackedData<Integer> RESPAWN_TIME = DataTracker.registerData(SquidBumperEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SPLAT_HEALTH = DataTracker.registerData(SquidBumperEntity.class, TrackedDataHandlerRegistry.FLOAT);
    public boolean inkproof = false;
    /**
     * After punching the stand, the cooldown before you can punch it again without breaking it.
     */
    public long punchCooldown;
    public long hurtCooldown;
    public int prevRespawnTime = 0;

    public SquidBumperEntity(EntityType<? extends LivingEntity> type, World world)
    {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder setCustomAttributes()
    {
        return createLivingAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 20).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0D);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder)
    {
        builder.add(COLOR, ColorUtils.getDefaultColor());
        builder.add(SPLAT_HEALTH, maxInkHealth);
        builder.add(RESPAWN_TIME, maxRespawnTime);
        builder.add(IMMORTAL, false);
    }

    @Override
    public void tick()
    {
        super.tick();

        hurtCooldown = Math.max(hurtCooldown - 1, 0);

        prevRespawnTime = getRespawnTime();
        if (getRespawnTime() > 1)
            setRespawnTime(getRespawnTime() - 1);

        if (getRespawnTime() == 20 && getInkHealth() <= 0)
        {
            getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.squidBumperRespawning, getSoundCategory(), 1, 1);
        }
        else if (getRespawnTime() == 1)
            respawn();

        BlockPos pos = getVelocityAffectingPos();

        if (getWorld().getBlockState(pos).getBlock() == SplatcraftBlocks.inkwell.get() && getWorld().getBlockEntity(pos) instanceof InkColorTileEntity te)
        {
            if (te.getInkColor() != getColor())
                setColor(te.getInkColor());
        }
    }

    @Override
    public boolean canHit()
    {
        return getInkHealth() > 0;
    }

    @Override
    public boolean onEntityInked(DamageSource source, float damage, InkColor color)
    {
        if (hurtCooldown <= 0 && getInkHealth() > 0 && !inkproof)
        {
            ink(damage, color);
            if (getInkHealth() <= 0 && !isImmortal())
            {
                getWorld().sendEntityStatus(this, (byte) 34);
            }
            return true;
        }
        return false;
    }

    /**
     * Called when the entity is attacked.
     */
    @Override
    public boolean damage(@NotNull DamageSource source, float amount)
    {
        if (!getWorld().isClient() && isAlive())
        {
            if (source.isOf(DamageTypes.OUT_OF_WORLD))
            {
                discard();
                return false;
            }
            else if (!isInvulnerableTo(source))
            {
                if (source.isOf(DamageTypes.EXPLOSION))
                {
                    dropBumper();
                    discard();
                    return false;
                }
                else if (source.isOf(DamageTypes.IN_FIRE))
                {
                    if (isOnFire())
                    {
                        damageBumper(source, 0.15F);
                    }
                    else
                    {
                        setOnFireFor(5);
                    }

                    return false;
                }
                else if (source.isOf(DamageTypes.ON_FIRE) && getHealth() > 0.5F)
                {
                    damageBumper(source, 4.0F);
                    return false;
                }
                else
                {
                    if (
                        (source.getSource() instanceof PersistentProjectileEntity projectileEntity &&
                            projectileEntity.getPierceLevel() > 0 &&
                            source.getName().equals("player")) ||
                            (
                                source.getAttacker() instanceof PlayerEntity player && player.getAbilities().allowModifyWorld
                            ))
                    {

                    }
                    boolean flag1 = source.getSource() instanceof PersistentProjectileEntity projectileEntity && projectileEntity.getPierceLevel() > 0;
                    if (!"player".equals(source.getName()) && !(source.getSource() instanceof PersistentProjectileEntity))
                    {
                        return false;
                    }
                    else if (source.getAttacker() instanceof PlayerEntity player && !player.getAbilities().allowModifyWorld)
                    {
                        return false;
                    }
                    else if (source.isSourceCreativePlayer())
                    {
                        playBrokenSound();
                        playParticles();
                        discard();
                        return flag1;
                    }
                    else
                    {
                        long i = getWorld().getTime();
                        if (i - punchCooldown > 5L && !(source.getSource() instanceof PersistentProjectileEntity projectileEntity))
                        {
                            getWorld().sendEntityStatus(this, (byte) 32);
                            punchCooldown = i;
                        }
                        else
                        {
                            dropBumper();
                            playParticles();
                            discard();
                        }

                        return true;
                    }
                }
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    private void playParticles()
    {
        if (getWorld() instanceof ServerWorld serverLevel)
        {
            serverLevel.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.WHITE_WOOL.getDefaultState()), getX(), getEyePos().y, getZ(), 10, getWidth() / 4.0F, getHeight() / 4.0F, getWidth() / 4.0F, 0.05D);
        }
    }

    private void playPopParticles()
    {
        for (int i = 0; i < 10; i++)
        {
            getWorld().addParticle(new InkSplashParticleData(getColor(), 2), getX(), getY() + getHeight() * 0.5, getZ(), random.nextDouble() * 0.5 - 0.25, random.nextDouble() * 0.5 - 0.25, random.nextDouble() * 0.5 - 0.25);
        }
        getWorld().addParticle(new InkExplosionParticleData(getColor(), 2), getX(), getY() + getHeight() * 0.5, getZ(), 0, 0, 0);
    }

    private void playHealParticles()
    {
        getWorld().addParticle(new InkSplashParticleData(InkOverlayCapability.get(this).getColor(), 2), getX(), getY() + getHeight() * 0.5, getZ(), 0, 0, 0);
    }

    private void playBrokenSound()
    {
        getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.squidBumperBreak, getSoundCategory(), 1.0F, 1.0F);
    }

    private void damageBumper(DamageSource source, float dmg)
    {
        float f = getHealth();
        f -= dmg;
        if (f <= 0.5F)
        {
            dropBumper();
            discard();
        }
        else
        {
            setHealth(f);
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id)
    {
        switch (id)
        {
            case 31:
                if (getWorld().isClient())
                {
                    hurtCooldown = getWorld().getTime();
                    getWorld().playSound(getX(), getY(), getZ(), SplatcraftSounds.squidBumperInk, getSoundCategory(), 0.3F, 1.0F, false);
                }
                break;
            case 32:
                if (getWorld().isClient())
                {
                    getWorld().playSound(getX(), getY(), getZ(), SplatcraftSounds.squidBumperHit, getSoundCategory(), 0.3F, 1.0F, false);
                    punchCooldown = getWorld().getTime();
                }
                break;
            case 34:
                if (getWorld().isClient())
                {
                    getWorld().playSound(getX(), getY(), getZ(), SplatcraftSounds.squidBumperPop, getSoundCategory(), 0.5F, 20.0F, false);
                    InkOverlayCapability.get(this).setAmount(0);
                    playPopParticles();
                }
                break;

            default:
                super.handleStatus(id);
        }
    }

    @Override
    protected boolean isImmobile()
    {
        return true;
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public void pushAway(@NotNull Entity entityIn)
    {
        if (getInkHealth() <= 0)
            return;

        if (!isConnectedThroughVehicle(entityIn))
        {
            if (!entityIn.noClip && !noClip)
            {
                double d0 = entityIn.getX() - getX();
                double d1 = entityIn.getZ() - getZ();
                double d2 = MathHelper.absMax(d0, d1);

                if (d2 >= 0.009999999776482582D)
                {
                    d2 = Math.sqrt(d2);
                    d0 = d0 / d2;
                    d1 = d1 / d2;
                    double d3 = 1.0D / d2;

                    if (d3 > 1.0D)
                    {
                        d3 = 1.0D;
                    }

                    d0 = d0 * d3;
                    d1 = d1 * d3;
                    d0 = d0 * 0.05000000074505806D;
                    d1 = d1 * 0.05000000074505806D;
                    d0 *= 3;
                    d1 *= 3;

                    if (!entityIn.hasPassengers())
                    {
                        entityIn.addVelocity(d0, 0.0D, d1);
                    }
                }
            }
        }
    }

    @Override
    public void addVelocity(double p_233627_1_, double p_233627_2_, double p_233627_4_)
    {
    }

    public void dropBumper()
    {
        CommonUtils.blockDrop(getWorld(), getBlockPos(), ColorUtils.setColorLocked(ColorUtils.setInkColor(new ItemStack(SplatcraftItems.squidBumper.get()), getColor()), true));
    }

    @Override
    protected void drop(ServerWorld world, DamageSource damageSource)
    {
        super.drop(world, damageSource);
    }

    @Override
    public @NotNull Iterable<ItemStack> getArmorItems()
    {
        return Collections.EMPTY_LIST;
    }

    @Override
    public @NotNull ItemStack getEquippedStack(@NotNull EquipmentSlot slotIn)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public void equipStack(@NotNull EquipmentSlot slotIn, @NotNull ItemStack stack)
    {

    }

    @Override
    public @NotNull Arm getMainArm()
    {
        return Arm.RIGHT;
    }

    @Override
    public void readCustomDataFromNbt(@NotNull NbtCompound nbt)
    {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Color"))
            setColor(InkColor.getFromNbt(nbt));
        else setColor(ColorUtils.getRandomStarterColor());

        if (nbt.contains("Inkproof"))
            inkproof = nbt.getBoolean("Inkproof");

        if (nbt.contains("InkHealth"))
            setInkHealth(nbt.getFloat("InkHealth"));
        if (nbt.contains("RegenTicks"))
            setRespawnTime(nbt.getInt("RegenTicks"));
        if (nbt.contains("Immortal"))
            setImmortal(nbt.getBoolean("Immortal"));
    }

    @Override
    public void writeCustomDataToNbt(@NotNull NbtCompound nbt)
    {
        super.writeCustomDataToNbt(nbt);
        nbt.put("Color", getColor().getNbt());
        nbt.putBoolean("Inkproof", inkproof);

        nbt.putFloat("InkHealth", getInkHealth());
        nbt.putInt("RegenTicks", getRespawnTime());
        nbt.putBoolean("Immortal", isImmortal());
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

    public float getInkHealth()
    {
        return dataTracker.get(SPLAT_HEALTH);
    }

    public void setInkHealth(float value)
    {
        dataTracker.set(SPLAT_HEALTH, value);
    }

    public int getRespawnTime()
    {
        return dataTracker.get(RESPAWN_TIME);
    }

    public void setRespawnTime(int value)
    {
        dataTracker.set(RESPAWN_TIME, value);
    }

    public boolean isImmortal()
    {
        return dataTracker.get(IMMORTAL);
    }

    public void setImmortal(boolean immortal)
    {
        dataTracker.set(IMMORTAL, immortal);
    }

    public float getBumperScale(float partialTicks)
    {
        return getInkHealth() <= 0 ? (10 - Math.min(MathHelper.lerp(partialTicks, prevRespawnTime, getRespawnTime()), 10)) / 10f : 1;
    }

    public void ink(float damage, InkColor color)
    {
        getWorld().sendEntityStatus(this, (byte) 31);
        setRespawnTime(maxRespawnTime);
        hurtCooldown = timeUntilRegen;

        if (dataTracker.get(IMMORTAL))
        {
            setInkHealth(maxInkHealth - damage);
        }
        else
        {
            setInkHealth(getInkHealth() - damage);
            if (!getWorld().isClient())
                if (!isSubmergedInWater() && InkOverlayCapability.hasCapability(this))
                {
                    InkOverlayInfo info = InkOverlayCapability.get(this);

                    if (getInkHealth() > 0)
                    {
                        if (info.getAmount() < maxInkHealth * 1.5)
                            info.addAmount(damage);
                    }
                    else info.setAmount(0);

                    info.setColor(color);
                    SplatcraftPacketHandler.sendToTrackers(new UpdateInkOverlayPacket(this, info), this);
                }
        }
    }

    public void respawn()
    {
        if (getInkHealth() <= 0)
            getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.squidBumperReady, getSoundCategory(), 1, 1);
        setInkHealth(maxInkHealth);
        setRespawnTime(0);

        InkOverlayCapability.get(this).setAmount(0);
    }
}