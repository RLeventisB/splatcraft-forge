package net.splatcraft.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.registries.SplatcraftSounds;

public class PlayerSetSquidC2SPacket extends PlayC2SPacket
{
    private final boolean squid, isCancel;

    public PlayerSetSquidC2SPacket(boolean squid, boolean sendSquidCancel)
    {
        this.squid = squid;
        isCancel = sendSquidCancel;
    }

    public static PlayerSetSquidC2SPacket decode(FriendlyByteBuf buffer)
    {
        byte state = buffer.readByte();
        return new PlayerSetSquidC2SPacket((state & 1) == 1, (state & 2) == 2);
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeByte((squid ? 1 : 0) | (isCancel ? 2 : 0));
    }

    @Override
    public void execute(Player player)
    {
        PlayerInfo target = PlayerInfoCapability.get(player);
        if (squid == target.isSquid())
        {
            throw new IllegalStateException(String.format("Squid state did not change for %s (%s)", player.getGameProfile(), squid));
        }

        target.setIsSquid(squid);
        if (isCancel)
            target.flagSquidCancel();
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), squid ? SplatcraftSounds.squidTransform : SplatcraftSounds.squidRevert, SoundSource.PLAYERS, 0.75F, ((player.getWorld().getRandom().nextFloat() - player.getWorld().getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);

        SplatcraftPacketHandler.sendToTrackers(new PlayerSetSquidS2CPacket(player.getUUID(), squid, isCancel), player);
    }
}