package net.splatcraft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

public class SendSquidDisablePacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendSquidDisablePacket.class);
	public SendSquidDisablePacket()
	{
	
	}
	public static SendSquidDisablePacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new SendSquidDisablePacket();
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute()
	{
		SplatcraftKeyHandler.SQUID_KEYBIND.active = false;
	}
}
