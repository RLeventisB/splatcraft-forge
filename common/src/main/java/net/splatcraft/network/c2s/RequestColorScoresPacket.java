package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateColorScoresPacket;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;

public class RequestColorScoresPacket extends PlayC2SPacket
{
    private static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(RequestColorScoresPacket.class);

    public RequestColorScoresPacket()
    {

    }

    public static RequestColorScoresPacket decode(RegistryByteBuf buffer)
    {
        return new RequestColorScoresPacket();
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void execute(PlayerEntity player)
    {
        // mfw i have to construct a java array (painful)
        InkColor[] colors = new InkColor[ScoreboardHandler.getCriteriaKeySet().size()];
        int i = 0;
        for (InkColor c : ScoreboardHandler.getCriteriaKeySet())
        {
            colors[i++] = c;
        }
        SplatcraftPacketHandler.sendToPlayer(new UpdateColorScoresPacket(true, true, colors), (ServerPlayerEntity) player);
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {

    }
}
