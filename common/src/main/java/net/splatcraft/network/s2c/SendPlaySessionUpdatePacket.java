package net.splatcraft.network.s2c;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.data.PlaySession;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class SendPlaySessionUpdatePacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendPlaySessionUpdatePacket.class);
	private static final StreamCodec<ByteBuf, List<UUID>> PLAYERS_CODEC = UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list());
	private final PlaySession session;
	public SendPlaySessionUpdatePacket(PlaySession session)
	{
		this.session = session;
	}
	public static SendPlaySessionUpdatePacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new SendPlaySessionUpdatePacket(PlaySession.STREAM_CODEC.decode(buffer));
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		PlaySession.STREAM_CODEC.encode(buffer, session);
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute()
	{
		Object2ObjectOpenHashMap<String, PlaySession> map = new Object2ObjectOpenHashMap<>(SaveInfoCapability.clientSaveInfo.playSessions());
		map.put(session.stageId, session);
		SaveInfoCapability.clientSaveInfo = new SaveInfo(SaveInfoCapability.clientSaveInfo.stages(), new SaveInfo.ImmutableObject2ObjectOpenHashMap<>(map), SaveInfoCapability.clientSaveInfo.colorScores());
	}
}