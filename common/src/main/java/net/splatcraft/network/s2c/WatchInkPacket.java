package net.splatcraft.network.s2c;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.structs.ChunkInk;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.structs.RelativeBlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class WatchInkPacket extends IncrementalChunkBasedPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(WatchInkPacket.class);
	private final HashMap<RelativeBlockPos, ChunkInk.BlockEntry> dirty;
	public WatchInkPacket(ChunkPos chunkPos, HashMap<RelativeBlockPos, ChunkInk.BlockEntry> dirty)
	{
		super(chunkPos);
		this.dirty = dirty;
	}
	public static WatchInkPacket decode(RegistryFriendlyByteBuf buffer)
	{
		ChunkPos pos = buffer.readChunkPos();
		HashMap<RelativeBlockPos, ChunkInk.BlockEntry> dirty = new HashMap<>();
		int size = buffer.readInt();
		for (int i = 0; i < size; i++)
			dirty.put(RelativeBlockPos.fromBuf(buffer), ChunkInk.BlockEntry.STREAM_CODEC.decode(buffer));
		
		return new WatchInkPacket(pos, dirty);
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
			dirty.put(RelativeBlockPos.fromAbsolute(pos), inkBlock);
		else
			Splatcraft.LOGGER.warn("Tried adding null ink object");
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeChunkPos(chunkPos);
		buffer.writeInt(dirty.size());
		
		for (Map.Entry<RelativeBlockPos, ChunkInk.BlockEntry> pair : dirty.entrySet())
		{
			RelativeBlockPos blockPos = pair.getKey();
			ChunkInk.BlockEntry entry = pair.getValue();
			blockPos.writeBuf(buffer);
			ChunkInk.BlockEntry.STREAM_CODEC.encode(buffer, entry);
		}
	}
	@Override
	public void execute()
	{
		ChunkInkHandler.markInkInChunkForUpdate(chunkPos, dirty);
	}
}