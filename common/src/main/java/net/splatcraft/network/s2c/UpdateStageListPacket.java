package net.splatcraft.network.s2c;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.client.gui.stagepad.AbstractStagePadScreen;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.util.CommonUtils;

public class UpdateStageListPacket extends PlayS2CPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(UpdateStageListPacket.class);
	private static final PacketCodec<RegistryByteBuf, Object2ObjectOpenHashMap<String, Stage>> STAGE_INFO_PACKET_CODEC = PacketCodecs.map(Object2ObjectOpenHashMap::new, PacketCodecs.STRING, Stage.PACKET_CODEC);
	Object2ObjectOpenHashMap<String, Stage> stages;
	public UpdateStageListPacket(Object2ObjectOpenHashMap<String, Stage> stages)
	{
		this.stages = stages;
	}
	public static UpdateStageListPacket decode(RegistryByteBuf buffer)
	{
		return new UpdateStageListPacket(STAGE_INFO_PACKET_CODEC.decode(buffer));
	}
	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
	@Override
	public void encode(RegistryByteBuf buffer)
	{
		STAGE_INFO_PACKET_CODEC.encode(buffer, stages);
	}
	@Environment(EnvType.CLIENT)
	@Override
	public void execute()
	{
		SaveInfoCapability.clientSaveInfo = new SaveInfo(SaveInfoCapability.clientSaveInfo.playSessions(), new SaveInfo.ImmutableObject2ObjectOpenHashMap<>(stages), SaveInfoCapability.clientSaveInfo.colorScores());
		
		if (MinecraftClient.getInstance().currentScreen instanceof AbstractStagePadScreen stagePadScreen)
			stagePadScreen.onStagesUpdate();
	}
}