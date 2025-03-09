package net.splatcraft.data.capabilities.chunkink.neoforge;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.neoforge.SplatcraftNeoForgeDataAttachments;

public class ChunkInkCapabilityImpl
{
	public static boolean has(ChunkAccess chunk)
	{
		return chunk.hasData(SplatcraftNeoForgeDataAttachments.CHUNK_INK);
	}
	public static boolean hasAndNotEmpty(ChunkAccess chunk)
	{
		return has(chunk) && get(chunk).isntEmpty();
	}
	public static ChunkInk get(ChunkAccess chunk)
	{
		return chunk.getData(SplatcraftNeoForgeDataAttachments.CHUNK_INK);
	}
	public static void set(ChunkAccess chunk, ChunkInk newData)
	{
		chunk.setData(SplatcraftNeoForgeDataAttachments.CHUNK_INK, newData);
	}
	public static void markUpdated(ChunkAccess chunk)
	{
		chunk.setUnsaved(true);
	}
}
