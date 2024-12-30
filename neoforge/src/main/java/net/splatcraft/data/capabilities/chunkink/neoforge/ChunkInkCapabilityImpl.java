package net.splatcraft.data.capabilities.chunkink.neoforge;

import net.minecraft.world.chunk.Chunk;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.neoforge.SplatcraftNeoForgeDataAttachments;

public class ChunkInkCapabilityImpl
{
	public static boolean has(Chunk chunk)
	{
		return chunk.hasData(SplatcraftNeoForgeDataAttachments.CHUNK_INK);
	}
	public static boolean hasAndNotEmpty(Chunk chunk)
	{
		return has(chunk) && get(chunk).isntEmpty();
	}
	public static ChunkInk get(Chunk chunk)
	{
		return chunk.getData(SplatcraftNeoForgeDataAttachments.CHUNK_INK);
	}
	public static void set(Chunk chunk, ChunkInk newData)
	{
		chunk.setData(SplatcraftNeoForgeDataAttachments.CHUNK_INK, newData);
	}
	public static void markUpdated(Chunk chunk)
	{
		chunk.setNeedsSaving(true);
	}
}
