package net.splatcraft.network.s2c;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.world.World;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.data.PlaySession;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;

public class SendPlaySessionCreationPacket extends PlayS2CPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(SendPlaySessionCreationPacket.class);
	private final PlaySession session;
	public SendPlaySessionCreationPacket(PlaySession session)
	{
		this.session = session;
	}
	public static SendPlaySessionCreationPacket decode(RegistryByteBuf buffer)
	{
		return new SendPlaySessionCreationPacket(PlaySession.PACKET_CODEC.decode(buffer));
	}
	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
	@Override
	public void encode(RegistryByteBuf buffer)
	{
		PlaySession.PACKET_CODEC.encode(buffer, session);
	}
	@Environment(EnvType.CLIENT)
	@Override
	public void execute()
	{
		Object2ObjectOpenHashMap<String, PlaySession> map = new Object2ObjectOpenHashMap<>(SaveInfoCapability.clientSaveInfo.playSessions());
		map.put(session.stageId, session);
		SaveInfoCapability.clientSaveInfo = new SaveInfo(new SaveInfo.ImmutableObject2ObjectOpenHashMap<>(map), SaveInfoCapability.clientSaveInfo.stages(), SaveInfoCapability.clientSaveInfo.colorScores());
		ClientUtils.matchStartCameraPosProvider.reset();
		if (session.playerUuids.contains(ClientUtils.getClientPlayer().getUuid()))
		{
			SplatcraftKeyHandler.SQUID_KEYBIND.active = true;
			ClientUtils.killCamData = null;
		}
		
		session.playerUuids.forEach(uuid ->
		{
			World world = ClientUtils.getClient().world;
			if (world == null)
				return;
			
			PlayerEntity plr = world.getPlayerByUuid(uuid);
			if (plr == null)
				return;
			
			EntityInfoCapability.getOptional(plr).ifPresent(info ->
			{
				info.setIsSquid(true);
				info.setPlayingStageId(session.stageId);
			});
		});
	}
}