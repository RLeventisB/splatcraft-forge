package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdatePlayerComponentsPacket;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RequestPlayerComponentsPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(RequestPlayerComponentsPacket.class);
	UUID target;
	byte componentsFlag;
	public RequestPlayerComponentsPacket(Player target, byte componentsFlag)
	{
		this.target = target.getUUID();
		this.componentsFlag = componentsFlag;
	}
	private RequestPlayerComponentsPacket(UUID target, byte componentsFlag)
	{
		this.target = target;
		this.componentsFlag = componentsFlag;
	}
	public static RequestPlayerComponentsPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new RequestPlayerComponentsPacket(buffer.readUUID(), buffer.readByte());
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeUUID(target);
		buffer.writeByte(componentsFlag);
	}
	@Override
	public void execute(ServerPlayer player)
	{
		ServerPlayer target = (ServerPlayer) player.level().getPlayerByUUID(this.target);
		if (target != null)
		{
			SplatcraftPacketHandler.sendToPlayer(new UpdatePlayerComponentsPacket(target, componentsFlag), player);
		}
	}
}