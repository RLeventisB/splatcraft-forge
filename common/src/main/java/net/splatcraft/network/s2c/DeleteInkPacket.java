package net.splatcraft.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.data.capabilities.chunkink.ChunkInkCapability;
import net.splatcraft.util.RelativeBlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DeleteInkPacket extends IncrementalChunkBasedPacket
{
	public static final Type<? extends CustomPacketPayload> ID = new Type<>(Splatcraft.identifierOf("delete_ink_packet"));
	public final List<BlockPos> toDelete;
	public DeleteInkPacket(ChunkPos chunkPos)
	{
		super(chunkPos);
		toDelete = new ArrayList<>();
	}
	public DeleteInkPacket(ChunkPos chunkPos, List<BlockPos> toDelete)
	{
		super(chunkPos);
		this.toDelete = toDelete;
	}
	public static DeleteInkPacket decode(RegistryFriendlyByteBuf buffer)
	{
		ChunkPos chunkPos = buffer.readChunkPos();
		int changedBlocks = buffer.readInt();
		List<BlockPos> toDelete = new ArrayList<>(changedBlocks);
		for (int i = 0; i < changedBlocks; i++)
		{
			BlockPos pos = buffer.readBlockPos();
			toDelete.add(pos);
		}
		return new DeleteInkPacket(chunkPos, toDelete);
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void add(Level world, BlockPos pos)
	{
		toDelete.add(pos);
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeChunkPos(chunkPos);
		buffer.writeInt(toDelete.size());
		for (var blockPos : toDelete)
		{
			buffer.writeBlockPos(blockPos);
		}
	}
	@Override
	public void execute()
	{
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null)
		{
			LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
			if (ChunkInkCapability.hasAndNotEmpty(chunk))
			{
				ChunkInk chunkInk = ChunkInkCapability.get(chunk);
				for (BlockPos blockPos : toDelete)
				{
					if (chunkInk.clearBlock(RelativeBlockPos.fromAbsolute(blockPos), true))
					{
						BlockState state = level.getBlockState(blockPos);
						level.sendBlockUpdated(blockPos, state, state, 0);
					}
				}
			}
		}
	}
}
