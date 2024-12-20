package net.splatcraft.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3i;

/**
 * A block position whose X and Z are relative to a chunk position. Y is kept absolute.
 */
public class RelativeBlockPos extends Vec3i
{
    private RelativeBlockPos(byte x, int y, byte z)
    {
        super(x, y, z);
    }

    /**
     * Instantiates a relative block position from an absolute block position.
     *
     * @param pos The absolute block position to convert from
     */
    public static RelativeBlockPos fromAbsolute(BlockPos pos)
    {
        return new RelativeBlockPos(
            (byte) ChunkSectionPos.getSectionCoord(pos.getX()),
            pos.getY(),
            (byte) ChunkSectionPos.getSectionCoord(pos.getZ())
        );
    }

    /**
     * Instantiates a relative block position from a compound tag (storing also a relative position).
     *
     * @param tag The compound tag to read from
     * @apiNote The tag must contain ints "X", "Y" and "Z" for construction to be successful.
     */
    public static RelativeBlockPos readNBT(NbtCompound tag)
    {
        return new RelativeBlockPos(
            tag.getByte("X"),
            tag.getByte("Y"),
            tag.getByte("Z")
        );
    }

    /**
     * Instantiates a relative block position from a packet buffer.
     *
     * @param buf The packet buffer to read from.
     * @apiNote The buffer must have three integers in a row for construction to be successful.
     */
    public static RelativeBlockPos fromBuf(PacketByteBuf buf)
    {
        return new RelativeBlockPos(
            buf.readByte(),
            buf.readInt(),
            buf.readByte()
        );
    }

    /**
     * Instantiates an absolute block position from this relative block position using a chunk position.
     *
     * @param pos The chunk position to use
     * @return The absolute block position
     */
    public BlockPos toAbsolute(ChunkPos pos)
    {
        return new BlockPos(add(pos.x * 16, 0, pos.z * 16));
    }

    /**
     * Writes this position to a compound tag.
     *
     * @param tag The tag to write to
     * @return The modified tag
     */
    public NbtCompound writeNBT(NbtCompound tag)
    {
        tag.putByte("X", (byte) getX());
        tag.putInt("Y", getY());
        tag.putByte("Z", (byte) getZ());

        return tag;
    }

    /**
     * Writes this position to a packet buffer.
     *
     * @param buf The packet buffer to write to
     * @return The modified buffer
     */
    public PacketByteBuf writeBuf(PacketByteBuf buf)
    {
        buf.writeByte(getX());
        buf.writeInt(getY());
        buf.writeByte(getZ());

        return buf;
    }
}
