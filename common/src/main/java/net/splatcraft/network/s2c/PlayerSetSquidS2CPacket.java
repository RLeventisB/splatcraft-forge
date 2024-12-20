package net.splatcraft.network.s2c;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.playerinfo.EntityInfo;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;

import java.util.UUID;

public class PlayerSetSquidS2CPacket extends PlayS2CPacket
{
    private static final Id<? extends CustomPayload> ID = new Id<>(Splatcraft.identifierOf("player_set_squid_s2c_packet"));
    private final boolean squid, isCancel;
    UUID target;

    public PlayerSetSquidS2CPacket(UUID player, boolean squid)
    {
        this(player, squid, false);
    }

    public PlayerSetSquidS2CPacket(UUID player, boolean squid, boolean isSquidCancel)
    {
        this.squid = squid;
        target = player;
        isCancel = isSquidCancel;
    }

    public static PlayerSetSquidS2CPacket decode(RegistryByteBuf buffer)
    {
        UUID player = buffer.readUuid();
        byte state = buffer.readByte();
        return new PlayerSetSquidS2CPacket(player, (state & 1) == 1, (state & 2) == 2);
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
        buffer.writeByte((squid ? 1 : 0) | (isCancel ? 2 : 0));
    }

    @Override
    public void execute()
    {
        PlayerEntity player = MinecraftClient.getInstance().world.getPlayerByUuid(target);
        if (player == null)
        {
            return;
        }
        EntityInfo target = EntityInfoCapability.get(player);
        target.setIsSquid(squid);
        if (isCancel)
            target.flagSquidCancel();
    }
}
