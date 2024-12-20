package net.splatcraft.network.s2c;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.layer.PlayerInkColoredSkinLayer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

public class ReceivePlayerOverlayPacket extends PlayS2CPacket
{
    private static final Id<? extends CustomPayload> ID = new Id<>(Splatcraft.identifierOf("receive_player_overlay_packet"));
    final UUID player;
    final byte[] imageBytes;

    public ReceivePlayerOverlayPacket(UUID player, byte[] imageBytes)
    {
        this.imageBytes = imageBytes;
        this.player = player;
    }

    public static ReceivePlayerOverlayPacket decode(RegistryByteBuf buffer)
    {
        return new ReceivePlayerOverlayPacket(buffer.readUuid(), buffer.readByteArray());
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

    @Environment(EnvType.CLIENT)
    @Override
    public void execute()
    {
        try
        {

            Identifier location = Splatcraft.identifierOf(PlayerInkColoredSkinLayer.PATH + player.toString());
            MinecraftClient.getInstance().getTextureManager().destroyTexture(location);

            if (imageBytes.length > 0)
            {
                NativeImageBackedTexture texture = new NativeImageBackedTexture(net.minecraft.client.texture.NativeImage.read(new ByteArrayInputStream(imageBytes)));
                MinecraftClient.getInstance().getTextureManager().registerTexture(location, texture);
                PlayerInkColoredSkinLayer.TEXTURES.put(player, location);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
