package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateColorScoresPacket;
import net.splatcraft.util.CommonUtils;

import java.util.ArrayList;

public class RequestColorScoresPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(RequestColorScoresPacket.class);
	public RequestColorScoresPacket()
	{
	
	}
	public static RequestColorScoresPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new RequestColorScoresPacket();
	}
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void execute(Player player)
	{
		SplatcraftPacketHandler.sendToPlayer(new UpdateColorScoresPacket(true, true, new ArrayList<>(ScoreboardHandler.getCriteriaKeySet())), (ServerPlayer) player);
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
	
	}
}
