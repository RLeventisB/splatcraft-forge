package net.splatcraft.network.s2c;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.splatcraft.client.gui.stagepad.AbstractStagePadScreen;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.util.CommonUtils;

public class UpdateStageListPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateStageListPacket.class);
	private static final StreamCodec<RegistryFriendlyByteBuf, Object2ObjectOpenHashMap<String, Stage>> STAGE_INFO_PACKET_CODEC = ByteBufCodecs.map(Object2ObjectOpenHashMap::new, ByteBufCodecs.STRING_UTF8, Stage.PACKET_CODEC);
	Object2ObjectOpenHashMap<String, Stage> stages;
	public UpdateStageListPacket(Object2ObjectOpenHashMap<String, Stage> stages)
	{
		this.stages = stages;
	}
	public static UpdateStageListPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new UpdateStageListPacket(STAGE_INFO_PACKET_CODEC.decode(buffer));
	}
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		STAGE_INFO_PACKET_CODEC.encode(buffer, stages);
	}
	@Environment(EnvType.CLIENT)
	@Override
	public void execute()
	{
		SaveInfoCapability.clientSaveInfo = new SaveInfo(SaveInfoCapability.clientSaveInfo.playSessions(), new SaveInfo.ImmutableObject2ObjectOpenHashMap<>(stages), SaveInfoCapability.clientSaveInfo.colorScores());
		
		if (Minecraft.getInstance().screen instanceof AbstractStagePadScreen stagePadScreen)
			stagePadScreen.onStagesUpdate();
	}
}