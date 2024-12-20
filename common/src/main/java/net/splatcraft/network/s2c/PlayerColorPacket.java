package net.splatcraft.network.s2c;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.Splatcraft;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;

import java.util.UUID;

public class PlayerColorPacket extends PlayS2CPacket
{
    private static final Id<? extends CustomPayload> ID = new Id<>(Splatcraft.identifierOf("player_color_packet"));
    private final InkColor color;
    UUID target;
    String playerName;

    public PlayerColorPacket(UUID player, String name, InkColor color)
    {
        this.color = color;
        target = player;
        playerName = name;
    }

    public PlayerColorPacket(PlayerEntity player, InkColor color)
    {
        this(player.getUuid(), player.getDisplayName().getString(), color);
    }

    public static PlayerColorPacket decode(RegistryByteBuf buffer)
    {
        int color = buffer.readInt();
        String name = buffer.readString();
        UUID player = buffer.readUuid();
        return new PlayerColorPacket(player, name, InkColor.constructOrReuse(color));
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeInt(color.getColor());
        buffer.writeString(playerName);
        buffer.writeUuid(target);
    }

    @Override
    public void execute()
    {
        PlayerEntity player = MinecraftClient.getInstance().world.getPlayerByUuid(target);
        if (player != null)
            ColorUtils.setPlayerColor(player, color, false);
        ClientUtils.setClientPlayerColor(target, color);
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }
}
