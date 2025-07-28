package net.splatcraft.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendStageWarpDataToPadPacket;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

public class RequestWarpDataPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(RequestWarpDataPacket.class);
	public static RequestWarpDataPacket decode(FriendlyByteBuf buf)
	{
		return new RequestWarpDataPacket();
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
	@Override
	public void execute(ServerPlayer player)
	{
		SplatcraftPacketHandler.sendToPlayer(SendStageWarpDataToPadPacket.compile(player), player);
	}
}
