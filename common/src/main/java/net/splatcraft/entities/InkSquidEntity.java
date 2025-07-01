package net.splatcraft.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.client.particles.SquidSoulParticleData;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

public class InkSquidEntity extends PathfinderMob implements IColoredEntity
{
	private static final EntityDataAccessor<InkColor> COLOR = SynchedEntityData.defineId(InkSquidEntity.class, CommonUtils.INKCOLOR_DATA_HANDLER);
	public InkSquidEntity(EntityType<? extends PathfinderMob> type, Level world)
	{
		super(type, world);
	}
	public static AttributeSupplier.Builder setCustomAttributes()
	{
		return createLivingAttributes()
			.add(Attributes.MAX_HEALTH, 20)
			.add(Attributes.MOVEMENT_SPEED, 0.23D)
			.add(Attributes.FOLLOW_RANGE, 16);
	}
	@Override
	protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(COLOR, ColorUtils.getDefaultColor());
	}
	@Override
	protected void registerGoals()
	{
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 10.0F));
	}
	@Override
	public void die(@NotNull DamageSource damageSource)
	{
		level().broadcastEntityEvent(this, (byte) 60);
		super.die(damageSource);
	}
	@Override
	public void handleEntityEvent(byte id)
	{
		if (id == 60)
		{
			getCommandSenderWorld().addParticle(new SquidSoulParticleData(getColor()), getX(), getY(), getZ(), 0, 1, 0);
		}
		else
		{
			super.handleEntityEvent(id);
		}
	}
	@Override
	public int getBaseExperienceReward()
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
			if (te.getInkColor() != getColor())
			{
				setColor(te.getInkColor());
			}
		}
	}
	@Override
	protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state)
	{
		playSound(SoundEvents.HONEY_BLOCK_FALL, 0.15F, 1.0F);
	}
	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag nbt)
	{
		super.readAdditionalSaveData(nbt);
		if (nbt.contains("Color"))
			setColor(InkColor.getFromNbt(nbt));
		else
			setColor(ColorUtils.getRandomStarterColor());
	}
	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag nbt)
	{
		super.addAdditionalSaveData(nbt);
		nbt.put("Color", getColor().getNbt());
	}
	@Override
	public InkColor getColor()
	{
		return entityData.get(COLOR);
	}
	@Override
	public void setColor(InkColor color)
	{
		entityData.set(COLOR, color);
	}
	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer)
	{
		return false;
	}
}
