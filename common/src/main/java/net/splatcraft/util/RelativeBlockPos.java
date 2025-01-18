package net.splatcraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
	public static final Codec<RelativeBlockPos> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		Codec.BYTE.fieldOf("x").forGetter(v -> (byte) v.getX()),
		Codec.INT.fieldOf("y").forGetter(Vec3i::getY),
		Codec.BYTE.fieldOf("z").forGetter(v -> (byte) v.getZ())
	).apply(inst, RelativeBlockPos::new));
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
			(byte) ChunkSectionPos.getLocalCoord(pos.getX()),
			pos.getY(),
			(byte) ChunkSectionPos.getLocalCoord(pos.getZ())
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
	public static RelativeBlockPos fromString(String string)
	{
		String[] coords = string.split(",");
		return new RelativeBlockPos(Byte.decode(coords[0]), Integer.decode(coords[1]), Byte.decode(coords[2]));
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
	@Override
	public String toString()
	{
		return getX() + "," + getY() + "," + getZ(); // do not add spaces to the strings it will make the codecs unhappy
	}
}
