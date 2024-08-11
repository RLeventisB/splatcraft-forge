package net.splatcraft.forge.tileentities;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.forge.commands.SuperJumpCommand;
import net.splatcraft.forge.data.Stage;
import net.splatcraft.forge.entities.SpawnShieldEntity;
import net.splatcraft.forge.registries.SplatcraftTileEntities;
import org.jetbrains.annotations.NotNull;

public class SpawnPadTileEntity extends InkColorTileEntity
{
	private UUID spawnShieldUuid;

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
		if(level.isClientSide() || spawnShieldUuid == null)
			return null;

		Entity res = ((ServerLevel) level).getEntity(spawnShieldUuid);
			return (res instanceof SpawnShieldEntity) ? (SpawnShieldEntity) res : null;
	}

	public void setSpawnShield(SpawnShieldEntity shield)
	{
		if(shield == null)
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
		return new Vec3(getBlockPos().getX(), getBlockPos().getY() + SuperJumpCommand.blockHeight(getBlockPos(), level), getBlockPos().getZ() + 0.5);
	}

	@Override
	public void saveAdditional(CompoundTag nbt)
	{
		if(spawnShieldUuid != null)
			nbt.putUUID("SpawnShield", spawnShieldUuid);
		super.saveAdditional(nbt);
	}

	@Override
	public void load(@NotNull CompoundTag nbt)
	{
		super.load(nbt);

		if(nbt.hasUUID("SpawnShield"))
			spawnShieldUuid = nbt.getUUID("SpawnShield");
		updateStages = true;
	}

	private boolean updateStages = false;
	@Override
	public void onLoad()
	{
		//if(updateStages)
		//	addToStages();

		super.onLoad();
	}
}