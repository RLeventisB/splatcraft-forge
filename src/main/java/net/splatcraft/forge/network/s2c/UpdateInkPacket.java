package net.splatcraft.forge.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInk;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInkCapability;
import net.splatcraft.forge.util.InkBlockUtils;

import java.util.HashMap;
import java.util.Map;

public class UpdateInkPacket extends IncrementalChunkBasedPacket
{
	protected final HashMap<BlockPos, ChunkInk.BlockEntry> dirty;
	public UpdateInkPacket(ChunkPos chunkPos)
	{
		this(chunkPos, new HashMap<>());
	}
	public UpdateInkPacket(ChunkPos chunkPos, HashMap<BlockPos, ChunkInk.BlockEntry> dirty)
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
		if (inkBlock != null)
			dirty.put(pos, inkBlock);
		else
			Splatcraft.LOGGER.warn("Tried adding null ink object"); // lmfao in any given moment if the inkBlock reference becomes null this dies
	}
	@Override
	public void encode(FriendlyByteBuf buffer)
	{
		buffer.writeChunkPos(chunkPos);
		buffer.writeInt(dirty.size());
		for (var blockPosTupleEntry : dirty.entrySet())
		{
			BlockPos blockPos = blockPosTupleEntry.getKey();
			
			buffer.writeBlockPos(blockPos);
			blockPosTupleEntry.getValue().writeToBuffer(buffer);
		}
	}
	public static UpdateInkPacket decode(FriendlyByteBuf buffer)
	{
		ChunkPos chunkPos = buffer.readChunkPos();
		int changedBlocks = buffer.readInt();
		HashMap<BlockPos, ChunkInk.BlockEntry> dirty = new HashMap<>(changedBlocks);
		
		for (int i = 0; i < changedBlocks; i++)
		{
			BlockPos pos = buffer.readBlockPos();
			ChunkInk.BlockEntry entry = ChunkInk.BlockEntry.readFromBuffer(buffer);
			dirty.put(pos, entry);
		}
		
		return new UpdateInkPacket(chunkPos, dirty);
	}
	@Override
	public void execute()
	{
		ClientLevel level = Minecraft.getInstance().level;
		
		if (level != null)
		{
			ChunkInk chunkInk = ChunkInkCapability.get(level.getChunk(chunkPos.x, chunkPos.z));
			
			for (Map.Entry<BlockPos, ChunkInk.BlockEntry> entry : dirty.entrySet())
			{
				BlockPos pos = entry.getKey();
				entry.getValue().apply(chunkInk, pos);
				BlockState state = level.getBlockState(pos);
				level.sendBlockUpdated(pos, state, state, 0);
			}
		}
	}
}
