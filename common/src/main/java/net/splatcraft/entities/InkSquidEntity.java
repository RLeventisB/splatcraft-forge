package net.splatcraft.entities;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.splatcraft.client.particles.SquidSoulParticleData;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

public class InkSquidEntity extends PathAwareEntity implements IColoredEntity
{
    private static final TrackedData<InkColor> COLOR = DataTracker.registerData(InkSquidEntity.class, CommonUtils.INKCOLORDATAHANDLER);

    public InkSquidEntity(EntityType<? extends PathAwareEntity> type, World world)
    {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder setCustomAttributes()
    {
        return createLivingAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 20)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23D)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder)
    {
        super.initDataTracker(builder);
        builder.add(COLOR, ColorUtils.getDefaultColor());
    }

    @Override
    protected void initGoals()
    {
        goalSelector.add(6, new WanderAroundFarGoal(this, 0.6D));
        goalSelector.add(8, new LookAroundGoal(this));
        goalSelector.add(11, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
    }

    @Override
    public void onDeath(DamageSource damageSource)
    {
        getWorld().sendEntityStatus(this, (byte) 60);
        super.onDeath(damageSource);
    }

    @Override
    public void handleStatus(byte id)
    {
        if (id == 60)
        {
            getEntityWorld().addParticle(new SquidSoulParticleData(getColor()), getX(), getY(), getZ(), 0, 1, 0);
        }
        else
        {
            super.handleStatus(id);
        }
    }

    @Override
    public int getXpToDrop()
    {
        return 0;
    }

    @Override
    public boolean shouldDropXp()
    {
        return false;
    }

    @Override
    public void tick()
    {
        super.tick();

        BlockPos pos = getVelocityAffectingPos();

        if (getWorld().getBlockState(pos).getBlock() == SplatcraftBlocks.inkwell.get() && getWorld().getBlockEntity(pos) instanceof InkColorTileEntity te)
        {
            if (te.getInkColor() != getColor())
            {
                setColor(te.getInkColor());
            }
        }
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        playSound(SoundEvents.BLOCK_HONEY_BLOCK_FALL, 0.15F, 1.0F);
    }

    @Override
    public void readCustomDataFromNbt(@NotNull NbtCompound nbt)
    {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Color"))
            setColor(InkColor.getFromNbt(nbt));
        else
            setColor(ColorUtils.getRandomStarterColor());
    }

    @Override
    public void writeCustomDataToNbt(@NotNull NbtCompound nbt)
    {
        super.writeCustomDataToNbt(nbt);
        nbt.put("Color", getColor().getNbt());
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

    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer)
    {
        return false;
    }
}
