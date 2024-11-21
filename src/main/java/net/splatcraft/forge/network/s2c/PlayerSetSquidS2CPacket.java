package net.splatcraft.forge.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;

import java.util.UUID;

public class PlayerSetSquidS2CPacket extends PlayS2CPacket
{
    UUID target;
    private final boolean squid, isCancel;

    public PlayerSetSquidS2CPacket(UUID player, boolean squid)
    {
        this(player, squid, false);
    }

    public PlayerSetSquidS2CPacket(UUID player, boolean squid, boolean isSquidCancel)
    {
        this.squid = squid;
        this.target = player;
        isCancel = isSquidCancel;
    }

    public static PlayerSetSquidS2CPacket decode(FriendlyByteBuf buffer)
    {
        UUID player = buffer.readUUID();
        byte state = buffer.readByte();
        return new PlayerSetSquidS2CPacket(player, (state & 1) == 1, (state & 2) == 2);
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeUUID(target);
        buffer.writeByte((squid ? 1 : 0) | (isCancel ? 2 : 0));
    }

    @Override
    public void execute()
    {
        Player player = Minecraft.getInstance().level.getPlayerByUUID(this.target);
        if (player == null)
        {
            return;
        }
        PlayerInfo target = PlayerInfoCapability.get(player);
        target.setIsSquid(squid);
        if (isCancel)
            target.flagSquidCancel();
    }
}
