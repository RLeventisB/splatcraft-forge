package net.splatcraft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

public class SendSquidLagPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendSquidLagPacket.class);
	byte lag;
	public SendSquidLagPacket(byte lag)
	{
		this.lag = lag;
	}
	public static SendSquidLagPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new SendSquidLagPacket(buffer.readByte());
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeByte(lag);
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute()
	{
		SplatcraftKeyHandler.setSquidDelayInternal(lag);
	}
}
