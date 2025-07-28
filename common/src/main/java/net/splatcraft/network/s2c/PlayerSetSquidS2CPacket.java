package net.splatcraft.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.Splatcraft;
import net.splatcraft.handlers.SquidFormHandler;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerSetSquidS2CPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = new Type<>(Splatcraft.identifierOf("player_set_squid_s2c_packet"));
	private final boolean squid;
	UUID target;
	public PlayerSetSquidS2CPacket(UUID player, boolean squid)
	{
		this.squid = squid;
		target = player;
	}
	public static PlayerSetSquidS2CPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new PlayerSetSquidS2CPacket(buffer.readUUID(), buffer.readBoolean());
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
		buffer.writeBoolean(squid);
	}
	@Override
	public void execute()
	{
		Player player = Minecraft.getInstance().level.getPlayerByUUID(target);
		if (player == null)
		{
			return;
		}
		
		SquidFormHandler.setSquid(player, squid);
	}
}
