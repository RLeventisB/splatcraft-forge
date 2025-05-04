package net.splatcraft.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.Stage;
import net.splatcraft.entities.SpawnShieldEntity;
import net.splatcraft.registries.SplatcraftTileEntities;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SpawnPadTileEntity extends InkColorTileEntity
{
	private UUID spawnShieldUuid;
	private boolean updateStages = false;
	public SpawnPadTileEntity(BlockPos pos, BlockState state)
	{
		super(SplatcraftTileEntities.spawnPadTileEntity.get(), pos, state);
	}
	public boolean isSpawnShield(SpawnShieldEntity otherShield)
	{
		return spawnShieldUuid != null && otherShield != null && spawnShieldUuid.equals(otherShield.getUUID());
	}
	public SpawnShieldEntity getSpawnShield()
	{
		if (level.isClientSide() || spawnShieldUuid == null)
			return null;

		Entity res = ((ServerLevel) level).getEntity(spawnShieldUuid);
		return (res instanceof SpawnShieldEntity) ? (SpawnShieldEntity) res : null;
	}
	public void setSpawnShield(SpawnShieldEntity shield)
	{
		if (shield == null)
			spawnShieldUuid = null;
		else spawnShieldUuid = shield.getUUID();
	}
	public void addToStages()
	{
		if (!level.isClientSide())
			for (Stage stage : Stage.getStagesForPosition(level, new Vec3(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ())))
				stage.addSpawnPad(this);
	}
	public Vec3 getSuperJumpPos()
	{
		BlockPos pos = getBlockPos();
		return new Vec3(pos.getX() + 0.5, pos.getY() + SuperJumpCommand.blockHeight(pos, level), pos.getZ() + 0.5);
	}
	@Override
	public void saveAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider wrapperLookup)
	{
		if (spawnShieldUuid != null)
			nbt.putUUID("SpawnShield", spawnShieldUuid);
		super.saveAdditional(nbt, wrapperLookup);
	}
	@Override
	public void loadAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider lookup)
	{
		super.loadAdditional(nbt, lookup);

		if (nbt.hasUUID("SpawnShield"))
			spawnShieldUuid = nbt.getUUID("SpawnShield");
		updateStages = true;
	}
}