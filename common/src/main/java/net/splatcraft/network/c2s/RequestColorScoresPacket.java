package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateColorScoresPacket;
import net.splatcraft.util.CommonUtils;

import java.util.ArrayList;

public class RequestColorScoresPacket extends PlayC2SPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(RequestColorScoresPacket.class);
	public RequestColorScoresPacket()
	{
	
	}
	public static RequestColorScoresPacket decode(RegistryByteBuf buffer)
	{
		return new RequestColorScoresPacket();
	}
	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
	@Override
	public void execute(PlayerEntity player)
	{
		SplatcraftPacketHandler.sendToPlayer(new UpdateColorScoresPacket(true, true, new ArrayList<>(ScoreboardHandler.getCriteriaKeySet())), (ServerPlayerEntity) player);
	}
	@Override
	public void encode(RegistryByteBuf buffer)
	{
	
	}
}
