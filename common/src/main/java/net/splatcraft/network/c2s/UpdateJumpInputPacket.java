package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

public class UpdateJumpInputPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateJumpInputPacket.class);
	final boolean jumping;
	public UpdateJumpInputPacket(LivingEntity entity)
	{
		jumping = entity.jumping;
	}
	public UpdateJumpInputPacket(boolean jumping)
	{
		this.jumping = jumping;
	}
	public static UpdateJumpInputPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new UpdateJumpInputPacket(buffer.readBoolean());
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeBoolean(jumping);
	}
	@Override
	public void execute(ServerPlayer player)
	{
		player.jumping = jumping;
	}
}
