package net.splatcraft.network.s2c;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;

public class SendPlayerRespawnMatchPacket extends PlayS2CPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(SendPlayerRespawnMatchPacket.class);
	public SendPlayerRespawnMatchPacket()
	{
	}
	public static SendPlayerRespawnMatchPacket decode(RegistryByteBuf buffer)
	{
		return new SendPlayerRespawnMatchPacket();
	}
	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
	@Override
	public void encode(RegistryByteBuf buffer)
	{
	}
	@Environment(EnvType.CLIENT)
	@Override
	public void execute()
	{
		ClientPlayerEntity clientPlayer = ClientUtils.getClientPlayer();
		EntityInfoCapability.getOptional(clientPlayer).ifPresent(info ->
		{
			info.setMatchRespawnTimeLeft(0);
			info.setIsMatchRespawning(false);
		});
	}
}