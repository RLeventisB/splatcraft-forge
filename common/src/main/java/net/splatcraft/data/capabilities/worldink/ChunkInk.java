package net.splatcraft.data.capabilities.worldink;

import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.RelativeBlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    // mental note for how this works
    // this stores whether a block is "permanent" or is inked
    // a block is inked when any of their faces isnt null
    // when removing ink of a block, the InkEntry becomes null, and the INK_MAP unregisters the block if it has no inked faces and isnt permanent
    private final HashMap<RelativeBlockPos, BlockEntry> INK_MAP = new HashMap<>();

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

    public void ink(RelativeBlockPos pos, Direction direction, int color, InkBlockUtils.InkType type)
    {
        ink(pos, direction.get3DDataValue(), color, type);
    }

    public void ink(RelativeBlockPos pos, int index, int color, InkBlockUtils.InkType type)
    {
        BlockEntry entry = INK_MAP.getOrDefault(pos, new BlockEntry());
        entry.paint(index, color, type);
        INK_MAP.put(pos, entry);
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
        if (entry == null || (entry.inmutable && !removeInmutable))
            return false;
        if (!entry.clear(index) && !entry.inmutable)
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
        if (entry == null || (entry.inmutable && !removePermanent))
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

    public NbtCompound writeNBT(NbtCompound nbt)
    {
        ListTag inkMapList = new ListTag();

        for (Map.Entry<RelativeBlockPos, BlockEntry> pair : INK_MAP.entrySet())
        {
            RelativeBlockPos pos = pair.getKey();
            BlockEntry entry = pair.getValue();
            NbtCompound element = new NbtCompound();
            element.put("Pos", pos.writeNBT(new NbtCompound()));
            element.putBoolean("IsPermanent", entry.inmutable);
            if (!entry.isInkedAny())
                continue;

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

    public void readNBT(NbtCompound nbt)
    {
        INK_MAP.clear();
        boolean oldFormat = nbt.contains("PermanentInk");

        if (oldFormat)
        {
            INK_MAP.clear();
            for (Tag tag : nbt.getList("Ink", Tag.TAG_COMPOUND))
            {
                NbtCompound element = (NbtCompound) tag;
                RelativeBlockPos pos = RelativeBlockPos.readNBT(element.getCompound("Pos"));
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
                NbtCompound element = (NbtCompound) tag;
                RelativeBlockPos pos = RelativeBlockPos.readNBT(element.getCompound("Pos"));
                int color = element.getInt("Color");
                InkBlockUtils.InkType inkType = InkBlockUtils.InkType.values.get(new ResourceLocation(element.getString("Type")));

                BlockEntry entry = getInk(pos);

                if (entry != null)
                {
                    entry.inmutable = true;
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
            for (Tag tag : nbt.getList("Ink", Tag.TAG_COMPOUND))
            {
                NbtCompound element = (NbtCompound) tag;
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
                            element.getInt("Color" + direction.name()),
                            InkBlockUtils.InkType.values.get(new ResourceLocation(element.getString("Type" + direction.name())))
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
        entry.inmutable = true;
        INK_MAP.put(pos, entry);
    }

    public void markMutable(RelativeBlockPos pos)
    {
        BlockEntry entry = INK_MAP.getOrDefault(pos, new BlockEntry());
        entry.inmutable = false;
        INK_MAP.put(pos, entry);
    }

    public static final class BlockEntry
    {
        public final InkEntry[] entries = new InkEntry[6];
        public boolean inmutable;

        public BlockEntry()
        {
            this.inmutable = false;
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
            return new Boolean[]{(flag & 1) == 1, (flag & 2) == 2, (flag & 4) == 4, (flag & 8) == 8, (flag & 16) == 16, (flag & 32) == 32};
        }

        public static BlockEntry readFromBuffer(FriendlyByteBuf buffer)
        {
            BlockEntry entry = new BlockEntry();
            byte state = buffer.readByte();
            entry.inmutable = (state & 128) == 128;
            if ((state & 1) == 1)
            {
                Boolean[] inked = getStateFromActiveFlag((byte) (state >> 1));
                for (int i = 0; i < 6; i++)
                {
                    if (inked[i])
                    {
                        int color = buffer.readInt();
                        InkBlockUtils.InkType type = InkBlockUtils.InkType.fromId(buffer.readByte());
                        entry.paint(i, color, type);
                    }
                    else
                    {
                        entry.clear(i);
                    }
                }
            }

            return entry;
        }

        public InkEntry get(int index)
        {
            return entries[index];
        }

        public int color(int index)
        {
            return get(index).color();
        }

        public InkBlockUtils.InkType type(int index)
        {
            return get(index).type();
        }

        public BlockEntry paint(int index, int color, InkBlockUtils.InkType type)
        {
            entries[index] = new InkEntry(color, type);
            return this;
        }

        public BlockEntry setInmutable(boolean inmutable)
        {
            this.inmutable = inmutable;
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

        public void writeToBuffer(FriendlyByteBuf buffer)
        {
            // format:
            // first bit = whether there's any ink in the block
            // second - seventh bit: state of the face
            // eigth bit: whether the block is permanent/static
            buffer.writeByte((isInkedAny() ? 1 : 0) | (getActiveFlag() << 1) | (inmutable ? 128 : 0));
            for (byte i = 0; i < 6; i++)
            {
                if (isInked(i))
                {
                    buffer.writeInt(color(i));
                    buffer.writeByte(type(i).getId());
                }
            }
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
            if (inmutable) worldInk.markInmutable(pos);
            else worldInk.markMutable(pos);
        }
    }

    public record InkEntry(int color, InkBlockUtils.InkType type)
    {
    }
}
