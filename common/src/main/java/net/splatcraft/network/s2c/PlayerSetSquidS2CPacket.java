package net.splatcraft.network.s2c;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;

import java.util.UUID;

public class PlayerSetSquidS2CPacket extends PlayS2CPacket
{
	public static final Id<? extends CustomPayload> ID = new Id<>(Splatcraft.identifierOf("player_set_squid_s2c_packet"));
	private final boolean squid;
	UUID target;
	public PlayerSetSquidS2CPacket(UUID player, boolean squid)
	{
		this.squid = squid;
		target = player;
	}
	public static PlayerSetSquidS2CPacket decode(RegistryByteBuf buffer)
	{
		return new PlayerSetSquidS2CPacket(buffer.readUuid(), buffer.readBoolean());
	}
	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
	@Override
	public void encode(RegistryByteBuf buffer)
	{
		buffer.writeUuid(target);
		buffer.writeBoolean(squid);
	}
	@Override
	public void execute()
	{
		PlayerEntity player = MinecraftClient.getInstance().world.getPlayerByUuid(target);
		if (player == null)
		{
			return;
		}
		EntityInfo target = EntityInfoCapability.get(player);
		target.setIsSquid(squid);
		if (!squid)
			target.flagSquidCancel();
	}
}
