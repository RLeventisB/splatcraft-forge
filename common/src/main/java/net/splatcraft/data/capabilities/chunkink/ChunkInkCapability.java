package net.splatcraft.data.capabilities.chunkink;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.splatcraft.platform.Services;

public class ChunkInkCapability
{
	public static boolean has(Level world, BlockPos pos)
	{
		return has(world.getChunk(pos));
	}
	public static boolean has(Level world, ChunkPos pos)
	{
		return has(world.getChunk(pos.x, pos.z));
	}
	public static boolean has(ChunkAccess chunk)
	{
		return Services.PLATFORM.hasChunkInk(chunk);
	}
	public static boolean hasAndNotEmpty(Level world, BlockPos pos)
	{
		return hasAndNotEmpty(world.getChunk(pos));
	}
	public static boolean hasAndNotEmpty(Level world, ChunkPos pos)
	{
		return hasAndNotEmpty(world.getChunk(pos.x, pos.z));
	}
	public static boolean hasAndNotEmpty(ChunkAccess chunk)
	{
		return Services.PLATFORM.hasAndIsNotEmptyChunkInk(chunk);
	}
	public static ChunkInk get(Level world, BlockPos pos)
	{
		return get(world.getChunk(pos));
	}
	public static ChunkInk get(Level world, ChunkPos pos)
	{
		return get(world.getChunk(pos.x, pos.z));
	}
	public static ChunkInk get(ChunkAccess chunk)
	{
		return Services.PLATFORM.getChunkInk(chunk);
	}
	public static void set(Level world, BlockPos pos, ChunkInk newData)
	{
		set(world.getChunk(pos), newData);
	}
	public static void set(Level world, ChunkPos pos, ChunkInk newData)
	{
		set(world.getChunk(pos.x, pos.z), newData);
	}
	public static void set(ChunkAccess chunk, ChunkInk newData)
	{
		Services.PLATFORM.setChunkInk(chunk, newData);
	}
}
