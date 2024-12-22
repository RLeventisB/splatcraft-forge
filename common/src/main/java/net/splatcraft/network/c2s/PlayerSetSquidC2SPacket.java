package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.sound.SoundCategory;
import net.splatcraft.data.capabilities.playerinfo.EntityInfo;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.CommonUtils;

public class PlayerSetSquidC2SPacket extends PlayC2SPacket
{
    public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(PlayerSetSquidC2SPacket.class);
    private final boolean squid, isCancel;

    public PlayerSetSquidC2SPacket(boolean squid, boolean sendSquidCancel)
    {
        this.squid = squid;
        isCancel = sendSquidCancel;
    }

    public static PlayerSetSquidC2SPacket decode(RegistryByteBuf buffer)
    {
        byte state = buffer.readByte();
        return new PlayerSetSquidC2SPacket((state & 1) == 1, (state & 2) == 2);
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeByte((squid ? 1 : 0) | (isCancel ? 2 : 0));
    }

    @Override
    public void execute(PlayerEntity player)
    {
        EntityInfo target = EntityInfoCapability.get(player);
        if (squid == target.isSquid())
        {
            throw new IllegalStateException(String.format("Squid state did not change for %s (%s)", player.getGameProfile(), squid));
        }

        target.setIsSquid(squid);
        if (isCancel)
            target.flagSquidCancel();
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), squid ? SplatcraftSounds.squidTransform : SplatcraftSounds.squidRevert, SoundCategory.PLAYERS, 0.75F, ((player.getWorld().getRandom().nextFloat() - player.getWorld().getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);

        SplatcraftPacketHandler.sendToTrackers(new PlayerSetSquidS2CPacket(player.getUuid(), squid, isCancel), player);
    }
}