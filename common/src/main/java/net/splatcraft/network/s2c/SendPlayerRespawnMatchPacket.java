package net.splatcraft.network.s2c;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;

public class SendPlayerRespawnMatchPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendPlayerRespawnMatchPacket.class);
	public SendPlayerRespawnMatchPacket()
	{
	}
	public static SendPlayerRespawnMatchPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new SendPlayerRespawnMatchPacket();
	}
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
	}
	@Environment(EnvType.CLIENT)
	@Override
	public void execute()
	{
		LocalPlayer clientPlayer = ClientUtils.getClientPlayer();
		EntityInfoCapability.getOptional(clientPlayer).ifPresent(info ->
		{
			info.setMatchRespawnTimeLeft(0);
			info.setIsMatchRespawning(false);
		});
	}
}