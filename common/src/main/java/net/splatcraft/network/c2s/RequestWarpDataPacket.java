package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendStageWarpDataToPadPacket;
import net.splatcraft.util.CommonUtils;

public class RequestWarpDataPacket extends PlayC2SPacket
{
    public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(RequestWarpDataPacket.class);

    public static RequestWarpDataPacket decode(PacketByteBuf buf)
    {
        return new RequestWarpDataPacket();
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {

    }

    @Override
    public void execute(PlayerEntity player)
    {
        SplatcraftPacketHandler.sendToPlayer(SendStageWarpDataToPadPacket.compile(player), (ServerPlayerEntity) player);
    }
}
