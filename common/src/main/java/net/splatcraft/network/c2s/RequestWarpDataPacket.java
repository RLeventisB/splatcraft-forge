package net.splatcraft.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendStageWarpDataToPadPacket;
import net.splatcraft.util.CommonUtils;

public class RequestWarpDataPacket extends PlayC2SPacket
{
    public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(RequestWarpDataPacket.class);

    public static RequestWarpDataPacket decode(FriendlyByteBuf buf)
    {
        return new RequestWarpDataPacket();
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return ID;
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer)
    {

    }

    @Override
    public void execute(Player player)
    {
        SplatcraftPacketHandler.sendToPlayer(SendStageWarpDataToPadPacket.compile(player), (ServerPlayer) player);
    }
}
