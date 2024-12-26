package net.splatcraft.network.s2c;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.worldink.ChunkInk;
import net.splatcraft.data.capabilities.worldink.ChunkInkCapability;
import net.splatcraft.util.RelativeBlockPos;

import java.util.ArrayList;
import java.util.List;

public class DeleteInkPacket extends IncrementalChunkBasedPacket
{
	public static final Id<? extends CustomPayload> ID = new Id<>(Splatcraft.identifierOf("delete_ink_packet"));
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
	public static DeleteInkPacket decode(RegistryByteBuf buffer)
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
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
	@Override
	public void add(World world, BlockPos pos)
	{
		toDelete.add(pos);
	}
	@Override
	public void encode(RegistryByteBuf buffer)
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
		ClientWorld level = MinecraftClient.getInstance().world;
		if (level != null)
		{
			WorldChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
			if (ChunkInkCapability.hasAndNotEmpty(level, chunk))
			{
				ChunkInk chunkInk = ChunkInkCapability.get(level, chunk);
				for (BlockPos blockPos : toDelete)
				{
					if (chunkInk.clearBlock(RelativeBlockPos.fromAbsolute(blockPos), true))
					{
						BlockState state = level.getBlockState(blockPos);
						level.updateListeners(blockPos, state, state, 0);
					}
				}
			}
		}
	}
}
