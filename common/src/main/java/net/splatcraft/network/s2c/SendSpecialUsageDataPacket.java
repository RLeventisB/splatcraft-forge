package net.splatcraft.network.s2c;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.splatcraft.handlers.SpecialHandler;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;

import java.util.UUID;

public class SendSpecialUsageDataPacket extends PlayS2CPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(SendSpecialUsageDataPacket.class);
	final Identifier specialId;
	final UUID target;
	public SendSpecialUsageDataPacket(Identifier specialId, UUID targetUuid)
	{
		this.specialId = specialId;
		target = targetUuid;
	}
	public static SendSpecialUsageDataPacket decode(RegistryByteBuf buffer)
	{
		return new SendSpecialUsageDataPacket(Identifier.PACKET_CODEC.decode(buffer), buffer.readUuid());
	}
	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
	@Override
	public void encode(RegistryByteBuf buffer)
	{
		Identifier.PACKET_CODEC.encode(buffer, specialId);
		buffer.writeUuid(target);
	}
	@Environment(EnvType.CLIENT)
	@Override
	public void execute()
	{
		PlayerEntity player = ClientUtils.getClient().world.getPlayerByUuid(target);
		if (player != null)
		{
			SpecialHandler.startUsingSpecial(player, specialId);
		}
	}
}
