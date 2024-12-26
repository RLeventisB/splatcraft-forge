package net.splatcraft.data.capabilities.worldink;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.splatcraft.Splatcraft;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.WatchInkPacket;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

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
	public static boolean hasAndNotEmpty(World world, BlockPos pos)
	{
		return hasAndNotEmpty(world, new ChunkPos(pos));
	}
	public static boolean hasAndNotEmpty(World world, ChunkPos pos)
	{
		if (inkMap.containsKey(world))
		{
			ConcurrentHashMap<Long, ChunkInk> map = inkMap.get(world);
			if (map.containsKey(pos.toLong()))
			{
				return map.get(pos.toLong()).isntEmpty();
			}
		}
		return false;
	}
	public static boolean hasAndNotEmpty(World world, Chunk chunk)
	{
		return hasAndNotEmpty(world, chunk.getPos());
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
	public static void loadChunkData(World world, ChunkPos pos, NbtCompound nbt) throws Exception
	{
		ConcurrentHashMap<Long, ChunkInk> map = inkMap.computeIfAbsent(world, v -> new ConcurrentHashMap<>());
		if (map.containsKey(pos.toLong()))
		{
			throw new Exception("Chunk " + pos + "is already loaded");
		}
		ChunkInk chunkInk = new ChunkInk();
		chunkInk.readNBT(nbt);
		SplatcraftPacketHandler.sendToDim(new WatchInkPacket(pos, chunkInk.getInkInChunk()), world.getRegistryKey());
		map.put(pos.toLong(), chunkInk);
	}
	public static void saveChunkData(World world, ChunkPos pos, NbtCompound nbt, boolean unload)
	{
		ConcurrentHashMap<Long, ChunkInk> map = inkMap.get(world);
		ChunkInk chunkInk = map.get(pos.toLong());
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
		
		if (map == null)
			return;
		
		map.remove(pos.toLong());
		if (map.isEmpty())
		{
			inkMap.remove(world);
		}
	}
	public static void unloadAllChunks(World world)
	{
		if (!inkMap.containsKey(world))
			return;
		
		ConcurrentHashMap<Long, ChunkInk> map = inkMap.get(world);
		for (var entry : map.entrySet())
		{
			map.remove(entry.getKey());
		}
		inkMap.remove(world);
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
		if (hasAndNotEmpty(world, chunk))
		{
			NbtCompound splatcraftData = new NbtCompound();
			saveChunkData(world, chunk.getPos(), splatcraftData, false);
			nbtCompound.put("splatcraft_ink_data", splatcraftData);
		}
	}
}
