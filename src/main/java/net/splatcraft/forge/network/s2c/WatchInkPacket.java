package net.splatcraft.forge.network.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInk;
import net.splatcraft.forge.handlers.ChunkInkHandler;
import net.splatcraft.forge.util.InkBlockUtils;

import java.util.HashMap;
import java.util.Map;

public class WatchInkPacket extends IncrementalChunkBasedPacket
{
	private final HashMap<BlockPos, ChunkInk.BlockEntry> dirty;
	public WatchInkPacket(ChunkPos chunkPos, HashMap<BlockPos, ChunkInk.BlockEntry> dirty)
	{
		super(chunkPos);
		this.dirty = dirty;
	}
	@Override
	public void add(Level level, BlockPos pos)
	{
		add(pos, InkBlockUtils.getInkBlock(level, pos));
	}
	public void add(BlockPos pos, ChunkInk.BlockEntry inkBlock)
	{
		dirty.put(pos, inkBlock);
	}
	@Override
	public void encode(FriendlyByteBuf buffer)
	{
		buffer.writeChunkPos(chunkPos);
		buffer.writeInt(dirty.size());
		
		for (Map.Entry<BlockPos, ChunkInk.BlockEntry> pair : dirty.entrySet())
		{
			BlockPos blockPos = pair.getKey();
			ChunkInk.BlockEntry entry = pair.getValue();
			if (entry == null)
				entry = new ChunkInk.BlockEntry();
			
			buffer.writeBlockPos(blockPos);
			entry.writeToBuffer(buffer);
		}
	}
	public static WatchInkPacket decode(FriendlyByteBuf buffer)
	{
		ChunkPos pos = buffer.readChunkPos();
		HashMap<BlockPos, ChunkInk.BlockEntry> dirty = new HashMap<>();
		int size = buffer.readInt();
		for (int i = 0; i < size; i++)
			dirty.put(buffer.readBlockPos(), ChunkInk.BlockEntry.readFromBuffer(buffer));
		
		return new WatchInkPacket(pos, dirty);
	}
	@Override
	public void execute()
	{
		ChunkInkHandler.markInkInChunkForUpdate(chunkPos, dirty);
	}
}
