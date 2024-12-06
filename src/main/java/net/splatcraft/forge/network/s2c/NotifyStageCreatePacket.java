package net.splatcraft.forge.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.splatcraft.forge.client.gui.stagepad.StageCreationScreen;
import net.splatcraft.forge.client.gui.stagepad.StageSelectionScreen;
import net.splatcraft.forge.client.gui.stagepad.StageSettingsScreen;

public class NotifyStageCreatePacket extends PlayS2CPacket
{
    final String stageId;

    public NotifyStageCreatePacket(String stageId)
    {
        this.stageId = stageId;
    }

    public static NotifyStageCreatePacket decode(FriendlyByteBuf buf)
    {
        return new NotifyStageCreatePacket(buf.readUtf());
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeUtf(stageId);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void execute()
    {
        if (Minecraft.getInstance().screen instanceof StageCreationScreen screen && stageId.equals(screen.getStageId()))
            Minecraft.getInstance().setScreen(new StageSettingsScreen(screen.getTitle(), stageId, StageSelectionScreen.instance));
    }
}
