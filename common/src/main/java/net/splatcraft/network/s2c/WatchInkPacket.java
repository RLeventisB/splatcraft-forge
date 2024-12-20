package net.splatcraft.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.worldink.ChunkInk;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.RelativeBlockPos;

import java.util.HashMap;
import java.util.Map;

public class WatchInkPacket extends IncrementalChunkBasedPacket
{
    private static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(WatchInkPacket.class);
    private final HashMap<RelativeBlockPos, ChunkInk.BlockEntry> dirty;

    public WatchInkPacket(ChunkPos chunkPos, HashMap<RelativeBlockPos, ChunkInk.BlockEntry> dirty)
    {
        super(chunkPos);
        this.dirty = dirty;
    }

    public static WatchInkPacket decode(RegistryByteBuf buffer)
    {
        ChunkPos pos = buffer.readChunkPos();
        HashMap<RelativeBlockPos, ChunkInk.BlockEntry> dirty = new HashMap<>();
        int size = buffer.readInt();
        for (int i = 0; i < size; i++)
            dirty.put(RelativeBlockPos.fromBuf(buffer), ChunkInk.BlockEntry.readFromBuffer(buffer));

        return new WatchInkPacket(pos, dirty);
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void add(World world, BlockPos pos)
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
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeChunkPos(chunkPos);
        buffer.writeInt(dirty.size());

        for (Map.Entry<RelativeBlockPos, ChunkInk.BlockEntry> pair : dirty.entrySet())
        {
            RelativeBlockPos blockPos = pair.getKey();
            ChunkInk.BlockEntry entry = pair.getValue();
            blockPos.writeBuf(buffer);
            entry.writeToBuffer(buffer);
        }
    }

    @Override
    public void execute()
    {
        ChunkInkHandler.markInkInChunkForUpdate(chunkPos, dirty);
    }
}