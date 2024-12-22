package net.splatcraft.data.capabilities.worldink;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.splatcraft.Splatcraft;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ChunkInkCapability
{
	private static final ConcurrentHashMap<World, ConcurrentHashMap<Long, ChunkInk>> inkMap = new ConcurrentHashMap<>();
	public static boolean has(World world, BlockPos pos)
	{
		return has(world, new ChunkPos(pos));
	}
	public static boolean has(World world, ChunkPos pos)
	{
		return inkMap.containsKey(world) && inkMap.get(world).containsKey(pos.toLong());
	}
	public static boolean has(World world, Chunk chunk)
	{
		return has(world, chunk.getPos());
	}
	public static ChunkInk get(World world, BlockPos pos) throws NullPointerException
	{
		return get(world, new ChunkPos(pos));
	}
	public static ChunkInk get(World world, ChunkPos pos) throws NullPointerException
	{
		if (inkMap.containsKey(world))
		{
			ConcurrentHashMap<Long, ChunkInk> innerMap = inkMap.get(world);
			if (innerMap.containsKey(pos.toLong()))
			{
				return innerMap.get(pos.toLong());
			}
			throw new NullPointerException("Couldn't find WorldInk capability!");
		}
		throw new NullPointerException("Couldn't find WorldInk capability!");
	}
	public static ChunkInk get(World world, Chunk chunk) throws NullPointerException
	{
		return get(world, chunk.getPos());
	}
	public static ChunkInk getOrCreate(World world, BlockPos pos)
	{
		return getOrCreate(world, new ChunkPos(pos));
	}
	public static ChunkInk getOrCreate(World world, ChunkPos pos)
	{
		ConcurrentHashMap<Long, ChunkInk> map = inkMap.computeIfAbsent(world, v -> new ConcurrentHashMap<>());
		
		return map.computeIfAbsent(pos.toLong(), t -> new ChunkInk());
	}
	public static ChunkInk getOrCreate(World world, Chunk chunk)
	{
		return getOrCreate(world, chunk.getPos());
	}
	public static void saveAllChunks(World world, boolean unload, Function<Long, NbtCompound> nbtSupplier)
	{
		if (!inkMap.containsKey(world))
			return;
		
		ConcurrentHashMap<Long, ChunkInk> map = inkMap.get(world);
		for (var entry : map.entrySet())
		{
			entry.getValue().writeNBT(nbtSupplier.apply(entry.getKey()));
			if (unload)
			{
				map.remove(entry.getKey());
			}
		}
		if (unload)
		{
			inkMap.remove(world);
		}
	}
	public static void loadChunkData(World world, ChunkPos pos, NbtCompound nbt) throws Exception
	{
		ConcurrentHashMap<Long, ChunkInk> map = inkMap.computeIfAbsent(world, v -> new ConcurrentHashMap<>());
		if (map.containsKey(pos.toLong()))
		{
			throw new Exception("Chunk " + pos + "is already loaded");
		}
		ChunkInk chunkInk = new ChunkInk();
		chunkInk.readNBT(nbt);
		map.put(pos.toLong(), chunkInk);
	}
	public static void saveChunkData(World world, ChunkPos pos, NbtCompound nbt, boolean unload)
	{
		ConcurrentHashMap<Long, ChunkInk> map = inkMap.computeIfAbsent(world, v -> new ConcurrentHashMap<>());
		ChunkInk chunkInk = map.computeIfAbsent(pos.toLong(), v -> new ChunkInk());
		chunkInk.writeNBT(nbt);
		
		if (unload)
		{
			map.remove(pos.toLong());
			if (map.isEmpty())
			{
				inkMap.remove(world);
			}
		}
	}
	public static void unloadChunkData(World world, ChunkPos pos)
	{
		ConcurrentHashMap<Long, ChunkInk> map = inkMap.get(world);
		
		map.remove(pos.toLong());
		if (map.isEmpty())
		{
			inkMap.remove(world);
		}
	}
	public static void onChunkDataRead(Chunk chunk, @Nullable ServerWorld world, NbtCompound nbt)
	{
		if (nbt.contains("splatcraft_ink_data"))
		{
			try
			{
				loadChunkData(world, chunk.getPos(), nbt.getCompound("splatcraft_ink_data"));
			}
			catch (Exception e)
			{
				Splatcraft.LOGGER.error("Error upon loading splatcraft ink data in chunk {}", chunk.getPos());
				Splatcraft.LOGGER.debug(String.valueOf(e));
			}
			
			// just in case!!!
			// nbt.remove("ForgeCaps");
		}
		else if (nbt.contains("ForgeCaps"))
		{
			NbtCompound forgeCaps = nbt.getCompound("ForgeCaps");
			if (forgeCaps.contains("splatcraft:world_ink"))
			{
				try
				{
					loadChunkData(world, chunk.getPos(), forgeCaps.getCompound("splatcraft:world_ink"));
				}
				catch (Exception e)
				{
					Splatcraft.LOGGER.error("Error upon loading splatcraft legacy ink data in chunk {}", chunk.getPos());
					Splatcraft.LOGGER.debug(String.valueOf(e));
				}
			}
		}
	}
	public static void onChunkDataSave(Chunk chunk, ServerWorld world, NbtCompound nbtCompound)
	{
		if (has(world, chunk))
		{
			saveChunkData(world, chunk.getPos(), nbtCompound, false);
		}
	}
}
