package net.splatcraft.data.capabilities.chunkink;

import com.google.common.collect.Iterators;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.structs.InkColor;
import net.splatcraft.util.structs.RelativeBlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
	public static final Codec<ChunkInk> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		Codec.unboundedMap(Codec.STRING.xmap(RelativeBlockPos::fromString, RelativeBlockPos::toString), BlockEntry.CODEC).fieldOf("ink_map").forGetter(v -> v.INK_MAP)
	).apply(inst, ChunkInk::new));
	// mental note for how this works
	// this stores whether a block is "permanent" or is inked
	// a block is inked when any of their faces isnt null
	// when removing ink of a block, the InkEntry becomes null, and the INK_MAP unregisters the block if it has no inked faces and isnt permanent
	private final HashMap<RelativeBlockPos, BlockEntry> INK_MAP;
	public ChunkInk(Map<RelativeBlockPos, BlockEntry> map)
	{
		INK_MAP = new HashMap<>(map);
	}
	public ChunkInk()
	{
		INK_MAP = new HashMap<>();
	}
	public boolean isInkedAny(RelativeBlockPos pos)
	{
		BlockEntry entry = getInk(pos);
		return entry != null && entry.isInkedAny();
	}
	public boolean isInked(RelativeBlockPos pos, Direction direction)
	{
		return isInked(pos, direction.get3DDataValue());
	}
	public boolean isInked(RelativeBlockPos pos, int index)
	{
		BlockEntry entry = getInk(pos);
		return entry != null && entry.isInked(index);
	}
	public void ink(RelativeBlockPos pos, Direction direction, InkColor color, InkBlockUtils.InkType type)
	{
		ink(pos, direction.get3DDataValue(), color, type);
	}
	public void ink(RelativeBlockPos pos, int index, InkColor color, InkBlockUtils.InkType type)
	{
		BlockEntry entry = INK_MAP.computeIfAbsent(pos, v -> new BlockEntry());
		entry.paint(index, color, type);
	}
	public boolean clearInk(RelativeBlockPos pos, Direction direction)
	{
		return clearInk(pos, direction, false);
	}
	public boolean clearInk(RelativeBlockPos pos, int index)
	{
		return clearInk(pos, index, false);
	}
	public boolean clearInk(RelativeBlockPos pos, Direction direction, boolean removeInmutable)
	{
		return clearInk(pos, direction.get3DDataValue(), removeInmutable);
	}
	public boolean clearInk(RelativeBlockPos pos, int index, boolean removeInmutable)
	{
		BlockEntry entry = getInk(pos);
		if (entry == null || (entry.immutable && !removeInmutable))
			return false;
		if (!entry.clear(index) && !entry.immutable)
			INK_MAP.remove(pos);
		
		return true;
	}
	/**
	 * @param pos the block to remove
	 * @return true if the block existed and was removed, false if there wasnt a block or the block was permanent
	 */
	public boolean clearBlock(RelativeBlockPos pos)
	{
		return clearBlock(pos, false);
	}
	/**
	 * @param pos             the block to remove
	 * @param removePermanent whether remove permanent blocks too
	 * @return true if the block existed and was removed, false if there wasnt a block or the block was permanent and removePermanent was false
	 */
	public boolean clearBlock(RelativeBlockPos pos, boolean removePermanent)
	{
		BlockEntry entry = getInk(pos);
		if (entry == null || (entry.immutable && !removePermanent))
			return false;
		return INK_MAP.remove(pos) != null;
	}
	public HashMap<RelativeBlockPos, BlockEntry> getInkInChunk()
	{
		return INK_MAP;
	}
	@Nullable
	public BlockEntry getInk(RelativeBlockPos pos)
	{
		return INK_MAP.get(pos);
	}
	public void readLegacyNBT(CompoundTag nbt)
	{
		INK_MAP.clear();
		boolean oldFormat = nbt.contains("PermanentInk"); // old format is referred to before this fork btw
		
		if (oldFormat)
		{
			INK_MAP.clear();
			for (Tag tag : nbt.getList("Ink", Tag.TAG_COMPOUND))
			{
				CompoundTag element = (CompoundTag) tag;
				RelativeBlockPos pos = RelativeBlockPos.readNBT(element.getCompound("Pos"));
				InkColor color = InkColor.constructOrReuse(element.getInt("Color"));
				InkBlockUtils.InkType inkType = InkBlockUtils.InkType.IDENTIFIER_MAP.get(ResourceLocation.parse(element.getString("Type")));
				
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
			
			for (Tag tag : nbt.getList("PermanentInk", CompoundTag.TAG_COMPOUND))
			{
				CompoundTag element = (CompoundTag) tag;
				RelativeBlockPos pos = RelativeBlockPos.readNBT(element.getCompound("Pos"));
				InkColor color = InkColor.constructOrReuse(element.getInt("Color"));
				InkBlockUtils.InkType inkType = InkBlockUtils.InkType.IDENTIFIER_MAP.get(ResourceLocation.parse(element.getString("Type")));
				
				BlockEntry entry = getInk(pos);
				
				if (entry != null)
				{
					entry.immutable = true;
					if (entry.color(0) != color)// in the case where the permanent ink doesnt have the same color as the actual ink
					{
						for (byte i = 0; i < 6; i++)
						{
							entry.paint(i, color, inkType);
						}
					}
				}
			}
		}
		else
		{
			for (Tag tag : nbt.getList("Ink", CompoundTag.TAG_COMPOUND))
			{
				CompoundTag element = (CompoundTag) tag;
				boolean isPermanent = element.getBoolean("IsPermanent");
				RelativeBlockPos pos = RelativeBlockPos.readNBT(element.getCompound("Pos"));
				if (element.contains("Faces"))
				{
					Byte[] activeIndices = BlockEntry.getIndicesFromActiveFlag(element.getByte("Faces"));
					for (Byte activeIndex : activeIndices)
					{
						Direction direction = Direction.from3DDataValue(activeIndex);
						
						ink(pos,
							activeIndex,
							InkColor.constructOrReuse(element.getInt("Color" + direction.name())),
							InkBlockUtils.InkType.IDENTIFIER_MAP.get(ResourceLocation.parse(element.getString("Type" + direction.name())))
						);
						if (isPermanent)
							markInmutable(pos);
					}
				}
			}
		}
	}
	public boolean isntEmpty()
	{
		return !INK_MAP.isEmpty();
	}
	public void markInmutable(RelativeBlockPos pos)
	{
		BlockEntry entry = INK_MAP.getOrDefault(pos, new BlockEntry());
		entry.immutable = true;
		INK_MAP.put(pos, entry);
	}
	public void markMutable(RelativeBlockPos pos)
	{
		BlockEntry entry = INK_MAP.getOrDefault(pos, new BlockEntry());
		entry.immutable = false;
		INK_MAP.put(pos, entry);
	}
	public static final class BlockEntry
	{
		// man why isn't there a codec that supports arrays + null elements in arrays :(
		public static final Codec<BlockEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			InkEntry.CODEC.lenientOptionalFieldOf("down_entry").forGetter(v -> Optional.ofNullable(v.entries[0])),
			InkEntry.CODEC.lenientOptionalFieldOf("up_entry").forGetter(v -> Optional.ofNullable(v.entries[1])),
			InkEntry.CODEC.lenientOptionalFieldOf("north_entry").forGetter(v -> Optional.ofNullable(v.entries[2])),
			InkEntry.CODEC.lenientOptionalFieldOf("south_entry").forGetter(v -> Optional.ofNullable(v.entries[3])),
			InkEntry.CODEC.lenientOptionalFieldOf("west_entry").forGetter(v -> Optional.ofNullable(v.entries[4])),
			InkEntry.CODEC.lenientOptionalFieldOf("east_entry").forGetter(v -> Optional.ofNullable(v.entries[5])),
			Codec.BOOL.fieldOf("immutable").forGetter(v -> v.immutable)
		).apply(inst, BlockEntry::new));
		public static final StreamCodec<ByteBuf, BlockEntry> STREAM_CODEC = StreamCodec.of(
			(buf, entry) ->
			{
				// format for the state byte:
				// if entry is completely empty, state = 0
				// else:
				// first - sixth bit: state of the face
				// seventh bit: whether the block is permanent/static
				
				if (!entry.isInkedAny())
				{
					buf.writeByte(0);
					return;
				}
				
				buf.writeByte(entry.getActiveFlag() | (entry.immutable ? 64 : 0));
				for (byte i = 0; i < 6; i++)
				{
					if (entry.isInked(i))
					{
						InkEntry.STREAM_CODEC.encode(buf, entry.get(i));
					}
				}
			},
			(buf) ->
			{
				BlockEntry entry = new BlockEntry();
				byte state = buf.readByte();
				if (state != 0)
				{
					entry.immutable = (state & 64) == 64;
					state &= 0b00111111;
					for (byte i : getIndicesFromActiveFlag(state))
					{
						entry.entries[i] = InkEntry.STREAM_CODEC.decode(buf);
					}
				}
				
				return entry;
			}
		);
		public final InkEntry[] entries = new InkEntry[6];
		public boolean immutable;
		public BlockEntry()
		{
			immutable = false;
		}
		public BlockEntry(
			Optional<InkEntry> entry1,
			Optional<InkEntry> entry2,
			Optional<InkEntry> entry3,
			Optional<InkEntry> entry4,
			Optional<InkEntry> entry5,
			Optional<InkEntry> entry6,
			boolean immutable)
		{
			entries[0] = entry1.orElse(null);
			entries[1] = entry2.orElse(null);
			entries[2] = entry3.orElse(null);
			entries[3] = entry4.orElse(null);
			entries[4] = entry5.orElse(null);
			entries[5] = entry6.orElse(null);
			this.immutable = immutable;
		}
		public static Byte[] getIndicesFromActiveFlag(byte flag)
		{
			return Iterators.toArray(new IndexIterator(flag), Byte.class);
		}
		public static Boolean[] getStateFromActiveFlag(byte flag)
		{
			return Iterators.toArray(new StateIterator(flag), Boolean.class);
		}
		public InkEntry get(int index)
		{
			return entries[index];
		}
		public InkColor color(int index)
		{
			return get(index).color();
		}
		public InkBlockUtils.InkType type(int index)
		{
			return get(index).type();
		}
		public BlockEntry paint(int index, InkColor color, InkBlockUtils.InkType type)
		{
			entries[index] = new InkEntry(color, type);
			return this;
		}
		public BlockEntry setImmutable(boolean immutable)
		{
			this.immutable = immutable;
			return this;
		}
		/**
		 * @param index the index to clear the block
		 * @return whether the block has any faces colored left
		 */
		public boolean clear(int index)
		{
			entries[index] = null;
			return isInkedAny();
		}
		public boolean isInkedAny()
		{
			for (InkEntry v : entries)
			{
				if (v != null)
				{
					return true;
				}
			}
			return false;
		}
		public boolean isInked(int index)
		{
			return entries[index] != null;
		}
		public byte getActiveFlag()
		{
			byte flag = 0;
			for (byte i = 0; i < 6; i++)
				if (isInked(i))
					flag |= 1 << i;
			return flag;
		}
		public Byte[] getActiveIndices()
		{
			return Iterators.toArray(new IndexIterator(getActiveFlag()), Byte.class);
		}
		public void apply(ChunkInk worldInk, RelativeBlockPos pos)
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
			if (immutable) worldInk.markInmutable(pos);
			else worldInk.markMutable(pos);
		}
		public static abstract class AbstractEntryIterator<T> implements Iterator<T>
		{
			protected final byte activeFlag;
			protected byte index = 0;
			public AbstractEntryIterator(byte activeFlag)
			{
				this.activeFlag = activeFlag;
				while (index < 6 && !isActive(index))
				{
					index++;
				}
			}
			@Override
			public boolean hasNext()
			{
				return index < 6;
			}
			public void advanceUntilValid()
			{
				do
				{
					index++;
				}
				while (index < 6 && !isActive(index));
			}
			public boolean isActive(byte bit)
			{
				return (activeFlag & (1 << bit)) != 0;
			}
			@Override
			public boolean equals(Object obj)
			{
				if (obj.getClass() != getClass())
					return false;
				
				AbstractEntryIterator iterator = (AbstractEntryIterator) obj;
				return activeFlag == iterator.activeFlag && index == iterator.index;
			}
			@Override
			public int hashCode()
			{
				return Objects.hash(activeFlag, index);
			}
			@Override
			public String toString()
			{
				return getClass().getTypeName() + "[" +
					"activeFlag=" + activeFlag + ", " +
					"index=" + index + ']';
			}
		}
		public static final class DirectionIterator extends AbstractEntryIterator<Direction>
		{
			public DirectionIterator(byte activeFlag)
			{
				super(activeFlag);
			}
			@Override
			public Direction next()
			{
				Direction direction = Direction.from3DDataValue(index);
				advanceUntilValid();
				return direction;
			}
		}
		public static final class IndexIterator extends AbstractEntryIterator<Byte>
		{
			public IndexIterator(byte activeFlag)
			{
				super(activeFlag);
			}
			@Override
			public Byte next()
			{
				byte lastIndex = index;
				advanceUntilValid();
				return lastIndex;
			}
		}
		public static final class StateIterator extends AbstractEntryIterator<Boolean>
		{
			public StateIterator(byte activeFlag)
			{
				super(activeFlag);
			}
			@Override
			public Boolean next()
			{
				boolean state = isActive(index);
				index++;
				return state;
			}
		}
		public record EntryIterable<T>(AbstractEntryIterator<T> iterator) implements Iterable<T>
		{
			@Override
			public @NotNull AbstractEntryIterator<T> iterator()
			{
				return iterator;
			}
		}
	}
	public record InkEntry(InkColor color, InkBlockUtils.InkType type)
	{
		public static final Codec<InkEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			InkColor.RAW_INT_CODEC.fieldOf("color").forGetter(InkEntry::color),
			InkBlockUtils.InkType.CODEC.fieldOf("type").forGetter(InkEntry::type)
		).apply(inst, InkEntry::new));
		public static final StreamCodec<ByteBuf, InkEntry> STREAM_CODEC = StreamCodec.composite(
			InkColor.PACKET_CODEC, InkEntry::color,
			InkBlockUtils.InkType.STREAM_CODEC, InkEntry::type,
			InkEntry::new
		);
	}
}
