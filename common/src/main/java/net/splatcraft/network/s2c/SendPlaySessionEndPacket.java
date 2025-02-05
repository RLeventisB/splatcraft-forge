package net.splatcraft.network.s2c;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;
import net.minecraft.world.World;
import net.splatcraft.data.PlaySession;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;

import java.util.List;
import java.util.UUID;

public class SendPlaySessionEndPacket extends PlayS2CPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(SendPlaySessionEndPacket.class);
	private static final PacketCodec<ByteBuf, List<UUID>> PLAYERS_CODEC = Uuids.PACKET_CODEC.collect(PacketCodecs.toList());
	private final String stageId;
	private final List<UUID> playerUuids;
	public SendPlaySessionEndPacket(String stageId, List<UUID> playerUuids)
	{
		this.stageId = stageId;
		this.playerUuids = playerUuids;
	}
	public static SendPlaySessionEndPacket decode(RegistryByteBuf buffer)
	{
		return new SendPlaySessionEndPacket(PacketCodecs.STRING.decode(buffer), PLAYERS_CODEC.decode(buffer));
	}
	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
	@Override
	public void encode(RegistryByteBuf buffer)
	{
		PacketCodecs.STRING.encode(buffer, stageId);
		PLAYERS_CODEC.encode(buffer, playerUuids);
	}
	@Environment(EnvType.CLIENT)
	@Override
	public void execute()
	{
		Object2ObjectOpenHashMap<String, PlaySession> map = new Object2ObjectOpenHashMap<>(SaveInfoCapability.clientSaveInfo.playSessions());
		map.remove(stageId);
		SaveInfoCapability.clientSaveInfo = new SaveInfo(new SaveInfo.ImmutableObject2ObjectOpenHashMap<>(map), SaveInfoCapability.clientSaveInfo.stages(), SaveInfoCapability.clientSaveInfo.colorScores());
		
		playerUuids.forEach(uuid ->
		{
			World world = ClientUtils.getClient().world;
			if (world == null)
				return;
			
			PlayerEntity plr = world.getPlayerByUuid(uuid);
			if (plr == null)
				return;
			
			EntityInfoCapability.getOptional(plr).ifPresent(info -> info.setPlayingStageId(null));
		});
	}
}