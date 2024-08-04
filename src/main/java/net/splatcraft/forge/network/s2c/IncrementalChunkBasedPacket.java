package net.splatcraft.forge.network.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public abstract class IncrementalChunkBasedPacket extends PlayS2CPacket
{
	protected final ChunkPos chunkPos;
	public IncrementalChunkBasedPacket(ChunkPos pos)
	{
		chunkPos = pos;
	}
	public abstract void add(Level level, BlockPos pos);
}
