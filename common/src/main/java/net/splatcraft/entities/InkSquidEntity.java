package net.splatcraft.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.World;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.client.particles.SquidSoulParticleData;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.ColorUtils;
import org.jetbrains.annotations.NotNull;

public class InkSquidEntity extends PathAwareEntity implements IColoredEntity
{
    private static final TrackedData<Integer> COLOR = DataTracker.registerData(InkSquidEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public InkSquidEntity(EntityType<? extends PathAwareEntity> type, World world)
    {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder setCustomAttributes()
    {
        return MobEntity.createLivingAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 20)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23D)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder)
    {
        super.initDataTracker(builder);
        builder.add(COLOR, ColorUtils.DEFAULT);
    }

    @Override
    protected void initGoals()
    {
        super.initGoals();
    }

    protected void initCustomGoals()
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
    public void handleStatus(byte status)
    {
        super.handleStatus(status);
    }

    public void handleEntityEvent(byte id)
    {
        if (id == 60)
        {
            getEntityWorld().addParticle(new SquidSoulParticleData(getColor()), this.getX(), this.getY(), this.getZ(), 0, 1, 0);
        }
        else
        {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public int getExperienceReward()
    {
        return 0;
    }

    @Override
    public boolean shouldDropExperience()
    {
        return false;
    }

    @Override
    public void tick()
    {
        super.tick();

        BlockPos pos = getBlockPosBelowThatAffectsMyMovement();

        if (level().getBlockState(pos).getBlock() == SplatcraftBlocks.inkwell.get() && level().getBlockEntity(pos) instanceof InkColorTileEntity te)
        {
            if (te.getColor() != getColor())
            {
                setColor(te.getColor());
            }
        }
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        playSound(SoundEvents.HONEY_BLOCK_FALL, 0.15F, 1.0F);
    }

    @Override
    public void readAdditionalSaveData(@NotNull NbtCompound nbt)
    {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("Color"))
            setColor(ColorUtils.getColorFromNbt(nbt));
        else
            setColor(ColorUtils.getRandomStarterColor());
    }

    @Override
    public void addAdditionalSaveData(@NotNull NbtCompound nbt)
    {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Color", getColor());
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

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer)
    {
        return false;
    }
}
