package net.splatcraft.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendStageWarpDataToPadPacket;

public class RequestWarpDataPacket extends PlayC2SPacket
{
    public static RequestWarpDataPacket decode(FriendlyByteBuf buf)
    {
        return new RequestWarpDataPacket();
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {

    }

    @Override
    public void execute(Player player)
    {
        SplatcraftPacketHandler.sendToPlayer(SendStageWarpDataToPadPacket.compile(player), (ServerPlayer) player);
    }
}
