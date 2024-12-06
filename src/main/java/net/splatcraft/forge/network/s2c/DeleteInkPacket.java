package net.splatcraft.forge.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInk;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInkCapability;
import net.splatcraft.forge.util.RelativeBlockPos;

import java.util.ArrayList;
import java.util.List;

public class DeleteInkPacket extends IncrementalChunkBasedPacket
{
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

    public static DeleteInkPacket decode(FriendlyByteBuf buffer)
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
    public void add(Level level, BlockPos pos)
    {
        toDelete.add(pos);
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
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
            ChunkInk chunkInk = ChunkInkCapability.get(level.getChunk(chunkPos.x, chunkPos.z));
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
