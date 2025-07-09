package net.splatcraft.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.data.capabilities.chunkink.ChunkInkCapability;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.structs.RelativeBlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class UpdateInkPacket extends IncrementalChunkBasedPacket
{
	public static final Type<? extends CustomPacketPayload> ID = new Type<>(Splatcraft.identifierOf("update_ink_packet"));
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
	public static UpdateInkPacket decode(RegistryFriendlyByteBuf buffer)
	{
		ChunkPos chunkPos = buffer.readChunkPos();
		int changedBlocks = buffer.readInt();
		HashMap<BlockPos, ChunkInk.BlockEntry> dirty = new HashMap<>(changedBlocks);
		
		for (int i = 0; i < changedBlocks; i++)
		{
			BlockPos pos = buffer.readBlockPos();
			ChunkInk.BlockEntry entry = ChunkInk.BlockEntry.STREAM_CODEC.decode(buffer);
			dirty.put(pos, entry);
		}
		
		return new UpdateInkPacket(chunkPos, dirty);
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void add(Level world, BlockPos pos)
	{
		add(pos, InkBlockUtils.getInkBlock(world, pos));
	}
	public void add(BlockPos pos, ChunkInk.BlockEntry inkBlock)
	{
		if (inkBlock != null)
			dirty.put(pos, inkBlock);
		else
			Splatcraft.LOGGER.warn("Tried adding null ink object"); // lmfao in any given moment if the inkBlock reference becomes null this dies
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeChunkPos(chunkPos);
		buffer.writeInt(dirty.size());
		for (var blockPosTupleEntry : dirty.entrySet())
		{
			BlockPos blockPos = blockPosTupleEntry.getKey();
			
			buffer.writeBlockPos(blockPos);
			ChunkInk.BlockEntry.STREAM_CODEC.encode(buffer, blockPosTupleEntry.getValue());
		}
	}
	@Override
	@OnlyIn(Dist.CLIENT)
	public void execute()
	{
		ClientLevel world = Minecraft.getInstance().level;
		
		if (world != null)
		{
			ChunkInk chunkInk = ChunkInkCapability.get(world, chunkPos);
			
			for (Map.Entry<BlockPos, ChunkInk.BlockEntry> entry : dirty.entrySet())
			{
				BlockPos pos = entry.getKey();
				entry.getValue().apply(chunkInk, RelativeBlockPos.fromAbsolute(pos));
				BlockState state = world.getBlockState(pos);
				world.sendBlockUpdated(pos, state, state, 0);
			}
			world.getChunk(chunkPos.x, chunkPos.z).setUnsaved(true);
		}
	}
}