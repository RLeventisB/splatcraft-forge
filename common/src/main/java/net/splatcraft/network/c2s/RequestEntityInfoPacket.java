package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateEntityInfoPacket;
import net.splatcraft.util.CommonUtils;

import java.util.UUID;

public class RequestEntityInfoPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(RequestEntityInfoPacket.class);
	UUID target;
	public RequestEntityInfoPacket(Player target)
	{
		this.target = target.getUUID();
	}
	private RequestEntityInfoPacket(UUID target)
	{
		this.target = target;
	}
	public static RequestEntityInfoPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new RequestEntityInfoPacket(buffer.readUUID());
	}
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeUUID(target);
	}
	@Override
	public void execute(Player player)
	{
		ServerPlayer target = (ServerPlayer) player.level().getPlayerByUUID(this.target);
		if (target != null)
		{
			SplatcraftPacketHandler.sendToPlayer(new UpdateEntityInfoPacket(target), (ServerPlayer) player);
		}
	}
}