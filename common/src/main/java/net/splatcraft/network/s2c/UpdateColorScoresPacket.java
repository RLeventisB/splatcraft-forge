package net.splatcraft.network.s2c;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.crafting.InkVatColorRecipe;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;

import java.util.Arrays;

public class UpdateColorScoresPacket extends PlayS2CPacket
{
    private static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(UpdateColorScoresPacket.class);
    InkColor[] colors;
    boolean add;
    boolean clear;
    public UpdateColorScoresPacket(boolean clear, boolean add, InkColor[] color)
    {
        this.clear = clear;
        colors = color;
        this.add = add;
    }

    public static UpdateColorScoresPacket decode(RegistryByteBuf buffer)
    {
        return new UpdateColorScoresPacket(buffer.readBoolean(), buffer.readBoolean(), buffer.readIntList().stream().map(InkColor::constructOrReuse).toList().toArray(new InkColor[0]));
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void execute()
    {
        if (clear)
        {
            ScoreboardHandler.clearColorCriteria();
            InkVatColorRecipe.getOmniList().clear();
        }

        if (add)
        {
            for (InkColor color : colors)
            {
                ScoreboardHandler.createColorCriterion(color);
            }
        }
        else
        {
            for (InkColor color : colors)
            {
                ScoreboardHandler.removeColorCriterion(color);
            }
        }
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeBoolean(clear);
        buffer.writeBoolean(add);
        buffer.writeIntList((IntList) Arrays.stream(colors).map(InkColor::getColor).toList());
    }
}
