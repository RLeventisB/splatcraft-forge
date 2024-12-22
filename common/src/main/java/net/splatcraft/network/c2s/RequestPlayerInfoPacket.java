package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdatePlayerInfoPacket;
import net.splatcraft.util.CommonUtils;

import java.util.UUID;

public class RequestPlayerInfoPacket extends PlayC2SPacket
{
    public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(RequestPlayerInfoPacket.class);
    UUID target;

    public RequestPlayerInfoPacket(PlayerEntity target)
    {
        this.target = target.getUuid();
    }

    private RequestPlayerInfoPacket(UUID target)
    {
        this.target = target;
    }

    public static RequestPlayerInfoPacket decode(RegistryByteBuf buffer)
    {
        return new RequestPlayerInfoPacket(buffer.readUuid());
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeUuid(target);
    }

    @Override
    public void execute(PlayerEntity player)
    {
        ServerPlayerEntity target = (ServerPlayerEntity) player.getWorld().getPlayerByUuid(this.target);
        if (target != null)
        {
            SplatcraftPacketHandler.sendToPlayer(new UpdatePlayerInfoPacket(target), (ServerPlayerEntity) player);
        }
    }
}