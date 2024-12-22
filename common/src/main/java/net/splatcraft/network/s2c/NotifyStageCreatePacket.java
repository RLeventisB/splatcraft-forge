package net.splatcraft.network.s2c;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.gui.stagepad.StageCreationScreen;
import net.splatcraft.client.gui.stagepad.StageSelectionScreen;
import net.splatcraft.client.gui.stagepad.StageSettingsScreen;

public class NotifyStageCreatePacket extends PlayS2CPacket
{
    public static final Id<? extends CustomPayload> ID = new Id<>(Splatcraft.identifierOf("notify_stage_create_packet"));
    final String stageId;

    public NotifyStageCreatePacket(String stageId)
    {
        this.stageId = stageId;
    }

    public static NotifyStageCreatePacket decode(PacketByteBuf buf)
    {
        return new NotifyStageCreatePacket(buf.readString());
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeString(stageId);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void execute()
    {
        if (MinecraftClient.getInstance().currentScreen instanceof StageCreationScreen screen && stageId.equals(screen.getStageId()))
            MinecraftClient.getInstance().setScreen(new StageSettingsScreen(screen.getTitle(), stageId, StageSelectionScreen.instance));
    }
}
