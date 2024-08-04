package net.splatcraft.forge.data.capabilities.worldink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.forge.util.InkBlockUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/*  TODO
	make old inked blocks decay instantly
	piston push interactions
	fix rendering bugs (See WorldInkHandler.Render comment)
	finish Rubidium support
	add Embeddium support
	add Oculus support
	screw OptiFine
 */
public class ChunkInk
{
	private final HashMap<BlockPos, BlockEntry> INK_MAP = new HashMap<>();
	public boolean isInkedAny(BlockPos pos)
	{
		return INK_MAP.containsKey(localizeBlockPos(pos));
	}
	public boolean isInked(BlockPos pos, Direction direction)
	{
		return isInked(pos, direction.get3DDataValue());
	}
	public boolean isInked(BlockPos pos, int index)
	{
		BlockEntry entry = INK_MAP.get(localizeBlockPos(pos));
		return entry != null && entry.isInked(index);
	}
	public void ink(BlockPos pos, Direction direction, int color, InkBlockUtils.InkType type)
	{
		ink(pos, direction.get3DDataValue(), color, type);
	}
	public void ink(BlockPos pos, Direction direction, int color, InkBlockUtils.InkType type, boolean permanent)
	{
		ink(pos, direction.get3DDataValue(), color, type, permanent);
	}
	public void ink(BlockPos pos, int index, int color, InkBlockUtils.InkType type)
	{
		pos = localizeBlockPos(pos);
		BlockEntry entry = INK_MAP.getOrDefault(pos, new BlockEntry());
		entry.setColor(index, color).setType(index, type);
		INK_MAP.put(pos, entry);
	}
	public void ink(BlockPos pos, int index, int color, InkBlockUtils.InkType type, boolean permanent)
	{
		pos = localizeBlockPos(pos);
		BlockEntry entry = INK_MAP.getOrDefault(pos, new BlockEntry());
		entry.setColor(index, color).setType(index, type).setPermanent(permanent);
		INK_MAP.put(pos, entry);
	}
	public boolean clearInk(BlockPos pos, Direction direction)
	{
		return clearInk(pos, direction, false);
	}
	public boolean clearInk(BlockPos pos, Direction direction, boolean removePermanent)
	{
		return clearInk(pos, direction.get3DDataValue(), removePermanent);
	}
	public boolean clearInk(BlockPos pos, int index)
	{
		return clearInk(pos, index, false);
	}
	public boolean clearInk(BlockPos pos, int index, boolean removePermanent)
	{
		pos = localizeBlockPos(pos);
		BlockEntry entry = getInk(pos);
		if (!isInkedAny(pos))
			return false;
		if (!entry.permanent || removePermanent)
		{
			if (!entry.clear(index))
				INK_MAP.remove(pos);
		}
		
		return true;
	}
	public boolean clearBlock(BlockPos pos)
	{
		return clearBlock(pos, false);
	}
	public boolean clearBlock(BlockPos pos, boolean removePermanent)
	{
		BlockEntry entry = getInk(pos);
		if (entry == null || (entry.permanent && !removePermanent))
			return false;
		INK_MAP.remove(localizeBlockPos(pos));
		return true;
	}
	public HashMap<BlockPos, BlockEntry> getInkInChunk()
	{
		return INK_MAP;
	}
	@Nullable
	public BlockEntry getInk(BlockPos pos)
	{
		return INK_MAP.get(localizeBlockPos(pos));
	}
	public CompoundTag writeNBT(CompoundTag nbt)
	{
		ListTag inkMapList = new ListTag();
		
		for (Map.Entry<BlockPos, BlockEntry> pair : INK_MAP.entrySet())
		{
			BlockPos pos = pair.getKey();
			BlockEntry entry = pair.getValue();
			if (!entry.isInkedAny())
				continue;
			CompoundTag element = new CompoundTag();
			element.put("Pos", NbtUtils.writeBlockPos(pos));
			element.putBoolean("IsPermanent", entry.permanent);
			element.putByte("Faces", entry.getActiveFlag());
			for (byte index : entry.getActiveIndices())
			{
				InkEntry inkEntry = entry.get(index);
				Direction direction = Direction.from3DDataValue(index);
				element.putInt("Color" + direction.name(), inkEntry.color());
				element.putString("Type" + direction.name(), inkEntry.type().getName().toString());
			}
			
			inkMapList.add(element);
		}
		
		nbt.put("Ink", inkMapList);
		
		return nbt;
	}
	protected BlockPos localizeBlockPos(BlockPos pos)
	{
		return new BlockPos(Math.floorMod(pos.getX(), 16), pos.getY(), Math.floorMod(pos.getZ(), 16));
	}
	public void readNBT(CompoundTag nbt)
	{
		INK_MAP.clear();
		boolean oldFormat = nbt.contains("PermanentInk");
		
		if (oldFormat)
		{
			INK_MAP.clear();
			for (Tag tag : nbt.getList("Ink", Tag.TAG_COMPOUND))
			{
				CompoundTag element = (CompoundTag) tag;
				BlockPos pos = NbtUtils.readBlockPos(element.getCompound("Pos"));
				int color = element.getInt("Color");
				InkBlockUtils.InkType inkType = InkBlockUtils.InkType.values.get(new ResourceLocation(element.getString("Type")));
				
				for (byte i = 0; i < 6; i++)
				{
					ink(
						pos,
						i,
						color,
						inkType
					);
				}
			}
			
			for (Tag tag : nbt.getList("PermanentInk", Tag.TAG_COMPOUND))
			{
				CompoundTag element = (CompoundTag) tag;
				BlockPos pos = NbtUtils.readBlockPos(element.getCompound("Pos"));
				int color = element.getInt("Color");
				InkBlockUtils.InkType inkType = InkBlockUtils.InkType.values.get(new ResourceLocation(element.getString("Type")));
				
				BlockEntry entry = getInk(pos);
				
				if (entry != null)
				{
					entry.permanent = true;
					if (entry.color(0) != color)
					{
						for (byte i = 0; i < 6; i++)
						{
							entry.setColor(i, color);
						}
					}
					if (entry.type(0) != inkType)
					{
						for (byte i = 0; i < 6; i++)
						{
							entry.setType(i, inkType);
						}
					}
				}
			}
		}
		else
		{
			for (Tag tag : nbt.getList("Ink", Tag.TAG_COMPOUND))
			{
				CompoundTag element = (CompoundTag) tag;
				boolean isPermanent = element.getBoolean("IsPermanent");
				BlockPos pos = NbtUtils.readBlockPos(element.getCompound("Pos"));
				Byte[] activeIndices = BlockEntry.getIndicesFromActiveFlag(element.getByte("Faces"));
				for (Byte activeIndex : activeIndices)
				{
					Direction direction = Direction.from3DDataValue(activeIndex);
					
					ink(pos,
						activeIndex,
						element.getInt("Color" + direction.name()),
						InkBlockUtils.InkType.values.get(new ResourceLocation(element.getString("Type" + direction.name()))),
						isPermanent);
				}
			}
		}
	}
	public boolean isntEmpty()
	{
		return !INK_MAP.isEmpty();
	}
	public static final class BlockEntry
	{
		public final InkEntry[] entries = new InkEntry[6];
		public boolean permanent;
		public BlockEntry(boolean permanent)
		{
			this.permanent = permanent;
		}
		public BlockEntry()
		{
			this.permanent = false;
		}
		public InkEntry get(int index)
		{
			return get(index, true);
		}
		public InkEntry get(int index, boolean make)
		{
			InkEntry entry = entries[index];
			if (entry == null && make)
			{
				entry = new InkEntry(-1, InkBlockUtils.InkType.NORMAL);
				entries[index] = entry;
			}
			return entry;
		}
		public InkEntry get(Direction direction)
		{
			return get(direction.get3DDataValue(), true);
		}
		public InkEntry get(Direction direction, boolean make)
		{
			return get(direction.get3DDataValue(), make);
		}
		public int color(int index)
		{
			return get(index).color();
		}
		public int color(Direction direction)
		{
			return get(direction).color();
		}
		public InkBlockUtils.InkType type(int index)
		{
			return get(index).type();
		}
		public InkBlockUtils.InkType type(Direction direction)
		{
			return get(direction).type();
		}
		public BlockEntry setColor(int index, int color)
		{
			get(index).color = color;
			return this;
		}
		public BlockEntry setColor(Direction direction, int color)
		{
			get(direction).color = color;
			return this;
		}
		public BlockEntry setType(int index, InkBlockUtils.InkType type)
		{
			get(index).type = type;
			return this;
		}
		public BlockEntry setType(Direction direction, InkBlockUtils.InkType type)
		{
			get(direction).type = type;
			return this;
		}
		public BlockEntry setPermanent(boolean permanent)
		{
			this.permanent = permanent;
			return this;
		}
		/**
		 * @param direction the direction to clear the block
		 * @return whether the block has any faces colored
		 */
		public boolean clear(Direction direction)
		{
			return clear(direction.get3DDataValue());
		}
		/**
		 * @param index the index to clear the block
		 * @return whether the block has any faces colored
		 */
		public boolean clear(int index)
		{
			if (entries[index] != null)
				get(index).clear();
			return isInkedAny();
		}
		public void reset()
		{
			for (byte i = 0; i < 6; i++)
			{
				if (entries[i] != null)
					get(i).clear();
			}
			permanent = false;
		}
		public boolean isInkedAny()
		{
			for (InkEntry v : entries)
			{
				if (v != null && v.color != -1)
				{
					return true;
				}
			}
			return false;
		}
		public boolean isInked(int index)
		{
			InkEntry entry = entries[index];
			return entry != null && entry.isInked();
		}
		public boolean isInked(Direction direction)
		{
			return isInked(direction.get3DDataValue());
		}
		public byte getActiveFlag()
		{
			return (byte) ((isInked(0) ? 1 : 0) | (isInked(1) ? 2 : 0) | (isInked(2) ? 4 : 0) | (isInked(3) ? 8 : 0) | (isInked(4) ? 16 : 0) | (isInked(5) ? 32 : 0));
		}
		public Byte[] getActiveIndices()
		{
			ArrayList<Byte> list = new ArrayList<>(6);
			for (byte i = 0; i < 6; i++)
			{
				if (isInked(i))
					list.add(i);
			}
			return list.toArray(new Byte[0]);
		}
		public static Byte[] getIndicesFromActiveFlag(byte flag)
		{
			ArrayList<Byte> list = new ArrayList<>(6);
			if ((flag & 1) == 1)
				list.add((byte) 0);
			if ((flag & 2) == 2)
				list.add((byte) 1);
			if ((flag & 4) == 4)
				list.add((byte) 2);
			if ((flag & 8) == 8)
				list.add((byte) 3);
			if ((flag & 16) == 16)
				list.add((byte) 4);
			if ((flag & 32) == 32)
				list.add((byte) 5);
			list.trimToSize();
			return list.toArray(new Byte[0]);
		}
		public static Boolean[] getStateFromActiveFlag(byte flag)
		{
			return new Boolean[] {(flag & 1) == 1, (flag & 2) == 2, (flag & 4) == 4, (flag & 8) == 8, (flag & 16) == 16, (flag & 32) == 32};
		}
		public void writeToBuffer(FriendlyByteBuf buffer)
		{
			if (!isInkedAny())
			{
				buffer.writeByte(0);
				return;
			}
			
			buffer.writeByte(1 | (getActiveFlag() << 1) | (permanent ? 128 : 0));
			for (byte i = 0; i < 6; i++)
			{
				if (isInked(i))
				{
					buffer.writeInt(color(i));
					buffer.writeByte(type(i).getId());
				}
			}
		}
		public static BlockEntry readFromBuffer(FriendlyByteBuf buffer)
		{
			BlockEntry entry = new BlockEntry();
			byte state = buffer.readByte();
			if ((state & 1) == 1)
			{
				entry.permanent = (state & 128) == 128;
				Boolean[] inked = getStateFromActiveFlag((byte) (state >> 1));
				for (int i = 0; i < 6; i++)
				{
					if (inked[i])
					{
						int color = buffer.readInt();
						InkBlockUtils.InkType type = InkBlockUtils.InkType.fromId(buffer.readByte());
						entry.setColor(i, color).setType(i, type);
					}
					else
					{
						entry.clear(i);
					}
				}
			}
			else
			{
				entry.reset();
			}
			return entry;
		}
		public void apply(ChunkInk worldInk, BlockPos pos)
		{
			if (!isInkedAny())
			{
				worldInk.clearBlock(pos);
				return;
			}
			for (byte i = 0; i < 6; i++)
			{
				if (isInked(i))
				{
					worldInk.ink(pos, i, color(i), type(i));
				}
				else
					worldInk.clearInk(pos, i);
			}
		}
	}
	public static final class InkEntry
	{
		private int color;
		private InkBlockUtils.InkType type;
		public InkEntry(int color, InkBlockUtils.InkType type)
		{
			this.color = color;
			this.type = type;
		}
		@Override
		public boolean equals(Object o)
		{
			return this == o || (o instanceof InkEntry entry && color == entry.color && Objects.equals(type, entry.type));
		}
		@Override
		public int hashCode()
		{
			return Objects.hash(color, type);
		}
		public int color()
		{
			return color;
		}
		public InkBlockUtils.InkType type()
		{
			return type;
		}
		public boolean isInked()
		{
			return color != -1;
		}
		public void clear()
		{
			color = -1;
			type = InkBlockUtils.InkType.NORMAL;
		}
		@Override
		public String toString()
		{
			return "InkEntry[" +
				"color=" + color + ", " +
				"type=" + type + ']';
		}
	}
	public record InkFace(InkEntry entry, Direction face)
	{
	}
}
