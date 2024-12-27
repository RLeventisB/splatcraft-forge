package net.splatcraft.entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.registries.SplatcraftEntities;
import net.splatcraft.tileentities.SpawnPadTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

public class SpawnShieldEntity extends Entity implements IColoredEntity
{
	private static final TrackedData<Integer> ACTIVE_TIME = DataTracker.registerData(SpawnShieldEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<InkColor> COLOR = DataTracker.registerData(SpawnShieldEntity.class, CommonUtils.INKCOLORDATAHANDLER);
	private static final TrackedData<Float> SIZE = DataTracker.registerData(SpawnShieldEntity.class, TrackedDataHandlerRegistry.FLOAT);
	public final int MAX_ACTIVE_TIME = 20;
	private BlockPos spawnPadPos;
	public SpawnShieldEntity(EntityType<SpawnShieldEntity> type, World world)
	{
		super(type, world);
		calculateDimensions();
		refreshPosition();
	}
	public SpawnShieldEntity(World world, BlockPos pos, InkColor color)
	{
		this(SplatcraftEntities.SPAWN_SHIELD.get(), world);
		setColor(color);
		setPos(pos.getX() + .5, pos.getY() - 1, pos.getZ() + .5);
		setSpawnPadPos(pos);
	}
	@Override
	public void onTrackedDataSet(TrackedData<?> data)
	{
		if (SIZE.equals(data))
			calculateDimensions();
		
		super.onTrackedDataSet(data);
	}
	@Override
	public void tick()
	{
		super.tick();
		
		if (getWorld().isClient())
			return;
		
		if (!(getSpawnPadPos() != null && getWorld().getBlockEntity(getSpawnPadPos()) instanceof SpawnPadTileEntity spawnPad &&
			spawnPad.isSpawnShield(this)))
		{
			discard();
			return;
		}
		
		if (spawnPad.getInkColor() != getColor())
			setColor(spawnPad.getInkColor());
		
		if (getActiveTime() > 0)
			setActiveTime(getActiveTime() - 1);
		
		for (Entity entity : getWorld().getOtherEntities(this, getBoundingBox(), EntityPredicates.EXCEPT_SPECTATOR))
		{
			if (!(entity.getType().isIn(SplatcraftTags.EntityTypes.BYPASSES_SPAWN_SHIELD) || ColorUtils.colorEquals(getWorld(), getBlockPos(), ColorUtils.getEntityColor(entity), getColor())))
			{
				setActiveTime(MAX_ACTIVE_TIME);
				
				if (entity instanceof AbstractSubWeaponEntity || entity instanceof InkProjectileEntity)
				{
					getWorld().sendEntityStatus(entity, (byte) -1);
					entity.discard();
				}
				else
				{
					if (entity instanceof PlayerEntity player && player.hasVehicle())
						player.stopRiding();
					
					entity.setVelocity(entity.getPos().subtract(getPos().x, getPos().y, getPos().z).normalize().multiply(.5));
					entity.velocityModified = true;
				}
			}
		}
	}
	//prevents shield from being affected by /kill
	@Override
	public void kill()
	{
		if (getSpawnPadPos() == null)
			super.kill();
	}
	@Override
	protected void initDataTracker(DataTracker.Builder builder)
	{
		builder.add(ACTIVE_TIME, 0);
		builder.add(COLOR, ColorUtils.getDefaultColor());
		builder.add(SIZE, 4f);
	}
	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt)
	{
		if (nbt.contains("Size"))
			setSize(nbt.getFloat("Size"));
		if (nbt.contains("Color"))
			setColor(InkColor.getFromNbt(nbt.get("Color")));
		if (nbt.contains("SpawnPadPos"))
			setSpawnPadPos(NbtHelper.toBlockPos(nbt, "SpawnPadPos").get());
	}
	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt)
	{
		nbt.putFloat("Size", getSize());
		nbt.put("Color", getColor().getNbt());
		if (getSpawnPadPos() != null)
			nbt.put("SpawnPadPos", NbtHelper.fromBlockPos(getSpawnPadPos()));
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
	public int getActiveTime()
	{
		return dataTracker.get(ACTIVE_TIME);
	}
	public void setActiveTime(int activeTime)
	{
		dataTracker.set(ACTIVE_TIME, activeTime);
	}
	public float getSize()
	{
		return dataTracker.get(SIZE);
	}
	public void setSize(float size)
	{
		dataTracker.set(SIZE, size);
		refreshPosition();
		calculateDimensions();
	}
	@Override
	public @NotNull EntityDimensions getDimensions(@NotNull EntityPose pose)
	{
		return super.getDimensions(pose).scaled(getSize());
	}
	public BlockPos getSpawnPadPos()
	{
		return spawnPadPos;
	}
	public void setSpawnPadPos(BlockPos spawnPadPos)
	{
		this.spawnPadPos = spawnPadPos;
	}
}
