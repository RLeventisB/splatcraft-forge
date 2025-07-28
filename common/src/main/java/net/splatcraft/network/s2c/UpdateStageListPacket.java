package net.splatcraft.network.s2c;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.client.gui.stagepad.AbstractStagePadScreen;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.SaveInfoCapability;
import net.splatcraft.data.capabilities.structs.SaveInfo;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

public class UpdateStageListPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateStageListPacket.class);
	private static final StreamCodec<RegistryFriendlyByteBuf, Object2ObjectMap<String, Stage>> STAGE_INFO_STREAM_CODEC =
		ByteBufCodecs.map(Object2ObjectOpenHashMap::new, ByteBufCodecs.STRING_UTF8, Stage.STREAM_CODEC);
	Object2ObjectMap<String, Stage> stages;
	public UpdateStageListPacket(Object2ObjectMap<String, Stage> stages)
	{
		this.stages = stages;
	}
	public static UpdateStageListPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new UpdateStageListPacket(STAGE_INFO_STREAM_CODEC.decode(buffer));
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		STAGE_INFO_STREAM_CODEC.encode(buffer, stages);
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute()
	{
		SaveInfoCapability.clientSaveInfo = new SaveInfo(Object2ObjectMaps.unmodifiable(stages), SaveInfoCapability.clientSaveInfo.playSessions(), SaveInfoCapability.clientSaveInfo.colorScores());
		ClientUtils.matchStartCameraPosProvider.reset();
		
		if (Minecraft.getInstance().screen instanceof AbstractStagePadScreen stagePadScreen)
			stagePadScreen.onStagesUpdate();
	}
}