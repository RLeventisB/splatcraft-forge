package net.splatcraft.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.gui.stagepad.StageCreationScreen;
import net.splatcraft.client.gui.stagepad.StageSelectionScreen;
import net.splatcraft.client.gui.stagepad.StageSettingsScreen;
import org.jetbrains.annotations.NotNull;

public class NotifyStageCreatePacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = new Type<>(Splatcraft.identifierOf("notify_stage_create_packet"));
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
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
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
