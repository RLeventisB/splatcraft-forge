package net.splatcraft.network.s2c;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.splatcraft.Splatcraft;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;

import java.util.ArrayList;

public class SendScanTurfResultsPacket extends PlayS2CPacket
{
    private static final Id<? extends CustomPayload> ID = new Id<>(Splatcraft.identifierOf("send_scarf_turf_results_packet"));
    InkColor[] colors;
    Float[] scores;
    int length;
    public SendScanTurfResultsPacket(InkColor[] colors, Float[] scores)
    {
        this.colors = colors;
        this.scores = scores;
        length = Math.min(colors.length, scores.length);
    }

    public static SendScanTurfResultsPacket decode(RegistryByteBuf buffer)
    {
        ArrayList<InkColor> colorList = new ArrayList<>();
        ArrayList<Float> scoreList = new ArrayList<>();
        int length = buffer.readInt();
        for (int i = 0; i < length; i++)
        {
            colorList.add(InkColor.constructOrReuse(buffer.readInt()));
            scoreList.add(buffer.readFloat());
        }

        return new SendScanTurfResultsPacket(colorList.toArray(new InkColor[0]), scoreList.toArray(new Float[0]));
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeInt(length);
        for (int i = 0; i < length; i++)
        {
            buffer.writeInt(colors[i].getColor());
            buffer.writeFloat(scores[i]);
        }
    }

    @Override
    public void execute()
    {
        PlayerEntity player = ClientUtils.getClientPlayer();
        InkColor winner = InkColor.INVALID;
        float winnerScore = -1;

        for (int i = 0; i < colors.length; i++)
        {
            player.sendMessage(Text.translatable("status.scan_turf.score", ColorUtils.getFormatedColorName(colors[i], false), String.format("%.1f", scores[i])), false);
            if (winnerScore < scores[i])
            {
                winnerScore = scores[i];
                winner = colors[i];
            }
        }

        if (winner.isValid())
        {
            player.sendMessage(Text.translatable("status.scan_turf.winner", ColorUtils.getFormatedColorName(winner, false)), false);
        }
    }
}
