package net.splatcraft.network.s2c;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.handlers.SpecialHandler;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;

import java.util.UUID;

public class SendSpecialUsageDataPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendSpecialUsageDataPacket.class);
	final ResourceLocation specialId;
	final UUID target;
	final EntitySlot providerSlot;
	public SendSpecialUsageDataPacket(ResourceLocation specialId, UUID targetUuid, EntitySlot slot)
	{
		this.specialId = specialId;
		target = targetUuid;
		providerSlot = slot;
	}
	public static SendSpecialUsageDataPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new SendSpecialUsageDataPacket(ResourceLocation.STREAM_CODEC.decode(buffer), buffer.readUUID(), EntitySlot.SERIALIZER_PACKET_CODEC.decode(buffer));
	}
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		ResourceLocation.STREAM_CODEC.encode(buffer, specialId);
		buffer.writeUUID(target);
		EntitySlot.SERIALIZER_PACKET_CODEC.encode(buffer, providerSlot);
	}
	@Environment(EnvType.CLIENT)
	@Override
	public void execute()
	{
		Player player = ClientUtils.getClient().level.getPlayerByUUID(target);
		if (player != null)
		{
			SpecialHandler.startUsingSpecial(player, specialId, providerSlot);
		}
	}
}
