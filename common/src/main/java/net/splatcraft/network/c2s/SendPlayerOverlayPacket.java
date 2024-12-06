package net.splatcraft.network.c2s;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.splatcraft.handlers.SplatcraftCommonHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.ReceivePlayerOverlayPacket;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

public class SendPlayerOverlayPacket extends PlayC2SPacket
{
    final UUID player;
    final byte[] imageBytes;

    @OnlyIn(Dist.CLIENT)
    public SendPlayerOverlayPacket(UUID player, File file) throws IOException
    {
        this.player = player;
        imageBytes = NativeImage.read(new FileInputStream(file)).asByteArray();
    }

    public SendPlayerOverlayPacket(UUID player, byte[] imageBytes)
    {
        this.imageBytes = imageBytes;
        this.player = player;
    }

    public static SendPlayerOverlayPacket decode(FriendlyByteBuf buffer)
    {
        return new SendPlayerOverlayPacket(buffer.readUUID(), buffer.readByteArray());
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeUUID(player);
        buffer.writeByteArray(imageBytes);
    }

    @Override
    public void execute(Player player)
    {
        if (imageBytes.length > 0)
            SplatcraftCommonHandler.COLOR_SKIN_OVERLAY_SERVER_CACHE.put(this.player, imageBytes);
        else SplatcraftCommonHandler.COLOR_SKIN_OVERLAY_SERVER_CACHE.remove(this.player);
        SplatcraftPacketHandler.sendToAll(new ReceivePlayerOverlayPacket(this.player, imageBytes));
    }
}
