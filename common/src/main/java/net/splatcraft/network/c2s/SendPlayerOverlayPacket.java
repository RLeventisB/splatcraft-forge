package net.splatcraft.network.c2s;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.handlers.SplatcraftCommonHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.ReceivePlayerOverlayPacket;
import net.splatcraft.util.CommonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

public class SendPlayerOverlayPacket extends PlayC2SPacket
{
    private static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(SendPlayerOverlayPacket.class);
    final UUID player;
    final byte[] imageBytes;
    @Environment(EnvType.CLIENT)
    public SendPlayerOverlayPacket(UUID player, File file) throws IOException
    {
        this.player = player;
        imageBytes = net.minecraft.client.texture.NativeImage.read(new FileInputStream(file)).getBytes();
    }

    public SendPlayerOverlayPacket(UUID player, byte[] imageBytes)
    {
        this.imageBytes = imageBytes;
        this.player = player;
    }

    public static SendPlayerOverlayPacket decode(RegistryByteBuf buffer)
    {
        return new SendPlayerOverlayPacket(buffer.readUuid(), buffer.readByteArray());
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeUuid(player);
        buffer.writeByteArray(imageBytes);
    }

    @Override
    public void execute(PlayerEntity player)
    {
        if (imageBytes.length > 0)
            SplatcraftCommonHandler.COLOR_SKIN_OVERLAY_SERVER_CACHE.put(this.player, imageBytes);
        else SplatcraftCommonHandler.COLOR_SKIN_OVERLAY_SERVER_CACHE.remove(this.player);
        SplatcraftPacketHandler.sendToAll(new ReceivePlayerOverlayPacket(this.player, imageBytes));
    }
}
