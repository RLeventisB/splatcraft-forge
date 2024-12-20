package net.splatcraft.network.s2c;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public abstract class IncrementalChunkBasedPacket extends PlayS2CPacket
{
    protected final ChunkPos chunkPos;

    public IncrementalChunkBasedPacket(ChunkPos pos)
    {
        chunkPos = pos;
    }

    public abstract void add(World world, BlockPos pos);
}
