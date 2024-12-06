package net.splatcraft.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateColorScoresPacket;

public class RequestColorScoresPacket extends PlayC2SPacket
{
    public RequestColorScoresPacket()
    {

    }

    public static RequestColorScoresPacket decode(FriendlyByteBuf buffer)
    {
        return new RequestColorScoresPacket();
    }

    @Override
    public void execute(Player player)
    {
        int[] colors = new int[ScoreboardHandler.getCriteriaKeySet().size()];
        int i = 0;
        for (int c : ScoreboardHandler.getCriteriaKeySet())
        {
            colors[i++] = c;
        }
        SplatcraftPacketHandler.sendToPlayer(new UpdateColorScoresPacket(true, true, colors), (ServerPlayer) player);
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {

    }
}
