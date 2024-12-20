package net.splatcraft.tileentities;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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

        return spawnShieldUuid != null && otherShield != null && spawnShieldUuid.equals(otherShield.getUuid());
    }

    public SpawnShieldEntity getSpawnShield()
    {
        if (world.isClient() || spawnShieldUuid == null)
            return null;

        Entity res = ((ServerWorld) world).getEntity(spawnShieldUuid);
        return (res instanceof SpawnShieldEntity) ? (SpawnShieldEntity) res : null;
    }

    public void setSpawnShield(SpawnShieldEntity shield)
    {
        if (shield == null)
            spawnShieldUuid = null;
        else spawnShieldUuid = shield.getUuid();
    }

    public void addToStages()
    {
        if (!world.isClient())
            for (Stage stage : Stage.getStagesForPosition(world, new Vec3d(getPos().getX(), getPos().getY(), getPos().getZ())))
                stage.addSpawnPad(this);
    }

    public Vec3d getSuperJumpPos()
    {
        return new Vec3d(getPos().getX(), getPos().getY() + SuperJumpCommand.blockHeight(getPos(), world), getPos().getZ() + 0.5);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup wrapperLookup)
    {
        if (spawnShieldUuid != null)
            nbt.putUuid("SpawnShield", spawnShieldUuid);
        super.writeNbt(nbt, wrapperLookup);
    }

    @Override
    public void readNbt(@NotNull NbtCompound nbt, RegistryWrapper.WrapperLookup lookup)
    {
        super.readNbt(nbt, lookup);

        if (nbt.containsUuid("SpawnShield"))
            spawnShieldUuid = nbt.getUuid("SpawnShield");
        updateStages = true;
    }
}