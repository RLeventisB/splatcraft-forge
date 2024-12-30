package net.splatcraft.data.capabilities.chunkink;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.splatcraft.Splatcraft;
import org.jetbrains.annotations.Nullable;

public class ChunkInkCapability
{
	public static boolean has(World world, BlockPos pos)
	{
		return has(world.getChunk(pos));
	}
	public static boolean has(World world, ChunkPos pos)
	{
		return has(world.getChunk(pos.x, pos.z));
	}
	@ExpectPlatform
	public static boolean has(Chunk chunk)
	{
		throw new AssertionError();
	}
	public static boolean hasAndNotEmpty(World world, BlockPos pos)
	{
		return hasAndNotEmpty(world.getChunk(pos));
	}
	public static boolean hasAndNotEmpty(World world, ChunkPos pos)
	{
		return hasAndNotEmpty(world.getChunk(pos.x, pos.z));
	}
	@ExpectPlatform
	public static boolean hasAndNotEmpty(Chunk chunk)
	{
		throw new AssertionError();
	}
	public static ChunkInk get(World world, BlockPos pos)
	{
		return get(world.getChunk(pos));
	}
	public static ChunkInk get(World world, ChunkPos pos)
	{
		return get(world.getChunk(pos.x, pos.z));
	}
	@ExpectPlatform
	public static ChunkInk get(Chunk chunk)
	{
		throw new AssertionError();
	}
	public static void set(World world, BlockPos pos, ChunkInk newData)
	{
		set(world.getChunk(pos), newData);
	}
	public static void set(World world, ChunkPos pos, ChunkInk newData)
	{
		set(world.getChunk(pos.x, pos.z), newData);
	}
	@ExpectPlatform
	public static void set(Chunk chunk, ChunkInk newData)
	{
		throw new AssertionError();
	}
	public static void markUpdated(World world, BlockPos pos)
	{
		markUpdated(world.getChunk(pos));
	}
	public static void markUpdated(World world, ChunkPos pos)
	{
		markUpdated(world.getChunk(pos.x, pos.z));
	}
	@ExpectPlatform
	public static void markUpdated(Chunk chunk)
	{
		throw new AssertionError();
	}
	public static void tryReadLegacyData(Chunk chunk, @Nullable ServerWorld world, NbtCompound nbt)
	{
		if (nbt.contains("ForgeCaps"))
		{
			NbtCompound forgeCaps = nbt.getCompound("ForgeCaps");
			if (forgeCaps.contains("splatcraft:world_ink"))
			{
				try
				{
					ChunkInk chunkInk = new ChunkInk();
					chunkInk.readLegacyNBT(forgeCaps.getCompound("splatcraft:world_ink"));
					set(chunk, chunkInk);
				}
				catch (Exception e)
				{
					Splatcraft.LOGGER.error("Error upon loading splatcraft legacy ink data in chunk {}", chunk.getPos());
					Splatcraft.LOGGER.debug(String.valueOf(e));
				}
			}
		}
	}
}
