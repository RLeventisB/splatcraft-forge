package net.splatcraft.forge.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.forge.util.ClientUtils;
import net.splatcraft.forge.util.ColorUtils;

import java.util.ArrayList;

public class SendScanTurfResultsPacket extends PlayS2CPacket
{
    Integer[] colors;
    Float[] scores;
    int length;

    public SendScanTurfResultsPacket(Integer[] colors, Float[] scores)
    {
        this.colors = colors;
        this.scores = scores;
        this.length = Math.min(colors.length, scores.length);
    }

    public static SendScanTurfResultsPacket decode(FriendlyByteBuf buffer)
    {
        ArrayList<Integer> colorList = new ArrayList<>();
        ArrayList<Float> scoreList = new ArrayList<>();
        int length = buffer.readInt();
        for (int i = 0; i < length; i++)
        {
            colorList.add(buffer.readInt());
            scoreList.add(buffer.readFloat());
        }

        return new SendScanTurfResultsPacket(colorList.toArray(new Integer[0]), scoreList.toArray(new Float[0]));
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeInt(length);
        for (int i = 0; i < length; i++)
        {
            buffer.writeInt(colors[i]);
            buffer.writeFloat(scores[i]);
        }
    }

    @Override
    public void execute()
    {
        Player player = ClientUtils.getClientPlayer();
        int winner = -1;
        float winnerScore = -1;

        for (int i = 0; i < colors.length; i++)
        {
            player.displayClientMessage(new TranslatableComponent("status.scan_turf.score", ColorUtils.getFormatedColorName(colors[i], false), String.format("%.1f", scores[i])), false);
            if (winnerScore < scores[i])
            {
                winnerScore = scores[i];
                winner = colors[i];
            }
        }

        if (winner != -1)
        {
            player.displayClientMessage(new TranslatableComponent("status.scan_turf.winner", ColorUtils.getFormatedColorName(winner, false)), false);
        }

    }

}
