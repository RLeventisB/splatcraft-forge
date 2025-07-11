package net.splatcraft.network.s2c;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.data.PlaySession;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.platform.Components;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class SendPlaySessionEndPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendPlaySessionEndPacket.class);
	private static final StreamCodec<ByteBuf, List<UUID>> PLAYERS_CODEC = UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list());
	private final String stageId;
	private final List<UUID> playerUuids;
	public SendPlaySessionEndPacket(String stageId, List<UUID> playerUuids)
	{
		this.stageId = stageId;
		this.playerUuids = playerUuids;
	}
	public static SendPlaySessionEndPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new SendPlaySessionEndPacket(ByteBufCodecs.STRING_UTF8.decode(buffer), PLAYERS_CODEC.decode(buffer));
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		ByteBufCodecs.STRING_UTF8.encode(buffer, stageId);
		PLAYERS_CODEC.encode(buffer, playerUuids);
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute()
	{
		Object2ObjectOpenHashMap<String, PlaySession> map = new Object2ObjectOpenHashMap<>(SaveInfoCapability.clientSaveInfo.playSessions());
		map.remove(stageId);
		SaveInfoCapability.clientSaveInfo = new SaveInfo(new SaveInfo.ImmutableObject2ObjectOpenHashMap<>(map), SaveInfoCapability.clientSaveInfo.stages(), SaveInfoCapability.clientSaveInfo.colorScores());
		
		playerUuids.forEach(uuid ->
		{
			Level world = ClientUtils.getClient().level;
			if (world == null)
				return;
			
			Player plr = world.getPlayerByUUID(uuid);
			if (plr == null)
				return;
			
			EntityInfo info = Components.ENTITY_INFO.getOrCreate(plr);
			
			info.setPlayingStageId(null);
		});
	}
}