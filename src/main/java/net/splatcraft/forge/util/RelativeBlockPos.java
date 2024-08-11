package net.splatcraft.forge.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;

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
                (byte) SectionPos.sectionRelative(pos.getX()),
                pos.getY(),
                (byte) SectionPos.sectionRelative(pos.getZ())
        );
    }

    /**
     * Instantiates a relative block position from a compound tag (storing also a relative position).
     *
     * @param tag The compound tag to read from
     * @apiNote The tag must contain ints "X", "Y" and "Z" for construction to be successful.
     */
    public static RelativeBlockPos readNBT(CompoundTag tag)
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
    public static RelativeBlockPos fromBuf(FriendlyByteBuf buf)
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
        return new BlockPos(this.offset(pos.x * 16, 0, pos.z * 16));
    }

    /**
     * Writes this position to a compound tag.
     *
     * @param tag The tag to write to
     * @return The modified tag
     */
    public CompoundTag writeNBT(CompoundTag tag)
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
    public FriendlyByteBuf writeBuf(FriendlyByteBuf buf)
    {
        buf.writeByte(getX());
        buf.writeInt(getY());
        buf.writeByte(getZ());

        return buf;
    }
}
