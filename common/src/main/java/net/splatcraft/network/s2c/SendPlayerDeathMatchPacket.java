package net.splatcraft.network.s2c;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;

import java.util.UUID;

public class SendPlayerDeathMatchPacket extends PlayS2CPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(SendPlayerDeathMatchPacket.class);
	private final int respawnTime;
	private final UUID killerPlayer;
	public SendPlayerDeathMatchPacket(int respawnTime, Entity killer)
	{
		this(respawnTime, killer instanceof PlayerEntity ? killer.getUuid() : new UUID(0, 0));
	}
	public SendPlayerDeathMatchPacket(int respawnTime, UUID killerPlayer)
	{
		this.respawnTime = respawnTime;
		this.killerPlayer = killerPlayer;
	}
	public static SendPlayerDeathMatchPacket decode(RegistryByteBuf buffer)
	{
		return new SendPlayerDeathMatchPacket(buffer.readInt(), Uuids.PACKET_CODEC.decode(buffer));
	}
	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
	@Override
	public void encode(RegistryByteBuf buffer)
	{
		buffer.writeInt(respawnTime);
		Uuids.PACKET_CODEC.encode(buffer, killerPlayer);
	}
	@Environment(EnvType.CLIENT)
	@Override
	public void execute()
	{
		ClientPlayerEntity clientPlayer = ClientUtils.getClientPlayer();
		EntityInfoCapability.getOptional(clientPlayer).ifPresent(info ->
		{
			info.setMatchRespawnTimeLeft(respawnTime);
			info.setIsMatchRespawning(true);
		});
	}
}