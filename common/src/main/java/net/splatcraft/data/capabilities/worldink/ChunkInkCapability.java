package net.splatcraft.data.capabilities.worldink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChunkInkCapability implements ICapabilityProvider, INBTSerializable<NbtCompound>
{
    public static Capability<ChunkInk> CAPABILITY = CapabilityManager.get(new CapabilityToken<>()
    {
    });
    private ChunkInk worldInk = null;
    private final LazyOptional<ChunkInk> opt = LazyOptional.of(() ->
        worldInk == null ? (worldInk = new ChunkInk()) : worldInk);

    public static ChunkInk get(Level level, BlockPos pos) throws NullPointerException
    {
        return get(level.getChunkAt(pos));
    }

    public static ChunkInk get(Level level, ChunkPos pos) throws NullPointerException
    {
        return get(level.getChunk(pos.x, pos.z));
    }

    public static ChunkInk get(LevelChunk chunk) throws NullPointerException
    {
        return chunk.getCapability(CAPABILITY).orElseThrow(() -> new NullPointerException("Couldn't find WorldInk capability!"));
    }

    public static ChunkInk getOrNull(Level level, BlockPos pos)
    {
        return getOrNull(level.getChunkAt(pos));
    }

    public static ChunkInk getOrNull(LevelChunk chunk)
    {
        return chunk.getCapability(CAPABILITY).resolve().orElse(null);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
    {
        return cap == CAPABILITY ? opt.cast() : LazyOptional.empty();
    }

    @Override
    public NbtCompound serializeNBT()
    {
        return opt.orElse(null).writeNBT(new NbtCompound());
    }

    @Override
    public void deserializeNBT(NbtCompound nbt)
    {
        opt.orElse(null).readNBT(nbt);
    }
}
