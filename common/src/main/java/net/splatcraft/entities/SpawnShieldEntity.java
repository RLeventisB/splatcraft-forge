package net.splatcraft.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.registries.SplatcraftEntities;
import net.splatcraft.tileentities.SpawnPadTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.specials.StingRayAction;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

public class SpawnShieldEntity extends Entity implements IColoredEntity
{
	private static final EntityDataAccessor<Integer> ACTIVE_TIME = SynchedEntityData.defineId(SpawnShieldEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<InkColor> COLOR = SynchedEntityData.defineId(SpawnShieldEntity.class, CommonUtils.INKCOLOR_DATA_HANDLER);
	private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(SpawnShieldEntity.class, EntityDataSerializers.FLOAT);
	public final int MAX_ACTIVE_TIME = 20;
	private BlockPos spawnPadPos;
	public SpawnShieldEntity(EntityType<SpawnShieldEntity> type, Level world)
	{
		super(type, world);
		refreshDimensions();
		reapplyPosition();
	}
	public SpawnShieldEntity(Level world, BlockPos pos, InkColor color)
	{
		this(SplatcraftEntities.SPAWN_SHIELD.get(), world);
		setColor(color);
		setPosRaw(pos.getX() + .5, pos.getY() - 1, pos.getZ() + .5);
		setSpawnPadPos(pos);
		refreshDimensions();
		reapplyPosition();
	}
	@Override
	public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> data)
	{
		if (SIZE.equals(data))
			refreshDimensions();
		
		super.onSyncedDataUpdated(data);
	}
	@Override
	public void tick()
	{
		super.tick();
		
		if (level().isClientSide())
			return;
		
		if (!(getSpawnPadPos() != null && level().getBlockEntity(getSpawnPadPos()) instanceof SpawnPadTileEntity spawnPad &&
			spawnPad.isSpawnShield(this)))
		{
			discard();
			return;
		}
		
		if (spawnPad.getInkColor() != getColor())
			setColor(spawnPad.getInkColor());
		
		if (getActiveTime() > 0)
			setActiveTime(getActiveTime() - 1);
		
		for (Entity entity : level().getEntities(this, getBoundingBox(), EntitySelector.NO_SPECTATORS))
		{
			if (
				(!entity.getType().is(SplatcraftTags.EntityTypes.BYPASSES_SPAWN_SHIELD) &&
					!ColorUtils.colorEquals(level(), blockPosition(), ColorUtils.getEntityColor(entity), getColor()))
					|| (entity instanceof LivingEntity living && EntityAction.hasSpecificEntityAction(living, StingRayAction.class))
			)
			{
				setActiveTime(MAX_ACTIVE_TIME);
				
				// todo: maybe move this to the sub weapon class instead of here??
				if (entity instanceof ObjectCollideListenerEntity listener)
				{
					listener.onCollidedWithObjectEntity(this);
				}
				if (entity instanceof AbstractSubWeaponEntity || entity instanceof InkProjectileEntity)
				{
					level().broadcastEntityEvent(entity, (byte) -1);
					entity.discard();
				}
				else
				{
					if (entity instanceof Player player && player.isPassenger())
						player.stopRiding();
					
					entity.setDeltaMovement(entity.position().subtract(position().x, position().y, position().z).normalize().scale(.5));
					entity.hurtMarked = true;
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
	public boolean isPickable()
	{
		return !isRemoved();
	}
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(ACTIVE_TIME, 0);
		builder.define(COLOR, ColorUtils.getDefaultColor());
		builder.define(SIZE, 4f);
	}
	@Override
	protected void readAdditionalSaveData(CompoundTag nbt)
	{
		if (nbt.contains("Size"))
			setSize(nbt.getFloat("Size"));
		if (nbt.contains("Color"))
			setColor(InkColor.getFromNbt(nbt.get("Color")));
		if (nbt.contains("SpawnPadPos"))
			setSpawnPadPos(BlockPos.CODEC.parse(NbtOps.INSTANCE, nbt.get("SpawnPadPos")).getOrThrow());
	}
	@Override
	protected void addAdditionalSaveData(CompoundTag nbt)
	{
		nbt.putFloat("Size", getSize());
		nbt.put("Color", getColor().getNbt());
		if (getSpawnPadPos() != null)
			nbt.put("SpawnPadPos", BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, getSpawnPadPos()).getOrThrow());
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
	public int getActiveTime()
	{
		return entityData.get(ACTIVE_TIME);
	}
	public void setActiveTime(int activeTime)
	{
		entityData.set(ACTIVE_TIME, activeTime);
	}
	public float getSize()
	{
		return entityData.get(SIZE);
	}
	public void setSize(float size)
	{
		entityData.set(SIZE, size);
		reapplyPosition();
		refreshDimensions();
	}
	@Override
	public @NotNull EntityDimensions getDimensions(@NotNull Pose pose)
	{
		return super.getDimensions(pose).scale(getSize());
	}
	public BlockPos getSpawnPadPos()
	{
		return spawnPadPos;
	}
	public void setSpawnPadPos(BlockPos spawnPadPos)
	{
		this.spawnPadPos = spawnPadPos;
	}
	public static boolean isSpawnShieldPresent(Level level, BlockPos pos, AABB aabb, InkColor spawnShieldColor)
	{
		return !level.
			getEntitiesOfClass(SpawnShieldEntity.class, aabb,
				(shield) -> ColorUtils.colorEquals(level, pos, ColorUtils.getEntityColor(shield), spawnShieldColor)
			).isEmpty();
	}
}
