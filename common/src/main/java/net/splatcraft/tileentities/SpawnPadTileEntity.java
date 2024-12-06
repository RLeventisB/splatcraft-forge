package net.splatcraft.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.level.ServerWorld;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3d;
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

        Entity res = ((ServerWorld) level).getEntity(spawnShieldUuid);
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
            for (Stage stage : Stage.getStagesForPosition(level, new Vec3d(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ())))
                stage.addSpawnPad(this);
    }

    public Vec3d getSuperJumpPos()
    {
        return new Vec3d(getBlockPos().getX(), getBlockPos().getY() + SuperJumpCommand.blockHeight(getBlockPos(), level), getBlockPos().getZ() + 0.5);
    }

    @Override
    public void saveAdditional(NbtCompound nbt)
    {
        if (spawnShieldUuid != null)
            nbt.putUUID("SpawnShield", spawnShieldUuid);
        super.saveAdditional(nbt);
    }

    @Override
    public void load(@NotNull NbtCompound nbt)
    {
        super.load(nbt);

        if (nbt.hasUUID("SpawnShield"))
            spawnShieldUuid = nbt.getUUID("SpawnShield");
        updateStages = true;
    }

    @Override
    public void onLoad()
    {
        //if(updateStages)
        //	addToStages();

        super.onLoad();
    }
}