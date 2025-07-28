package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

public class UpdateInputPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateInputPacket.class);
	final float sidewaysStrafe, forwardStrafe;
	final byte jumpAndSneakState;
	public UpdateInputPacket(LivingEntity entity)
	{
		sidewaysStrafe = entity.xxa;
		forwardStrafe = entity.zza;
		jumpAndSneakState = (byte) ((entity.jumping ? 1 : 0) | (entity.isShiftKeyDown() ? 2 : 0));
	}
	public UpdateInputPacket(float sidewaysStrafe, float forwardStrafe, byte jumpAndSneakState)
	{
		this.sidewaysStrafe = sidewaysStrafe;
		this.forwardStrafe = forwardStrafe;
		this.jumpAndSneakState = jumpAndSneakState;
	}
	public UpdateInputPacket()
	{
		sidewaysStrafe = 0;
		forwardStrafe = 0;
		jumpAndSneakState = 0;
	}
	public static UpdateInputPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new UpdateInputPacket(buffer.readFloat(), buffer.readFloat(), buffer.readByte());
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeFloat(sidewaysStrafe);
		buffer.writeFloat(forwardStrafe);
		buffer.writeByte(jumpAndSneakState);
	}
	@Override
	public void execute(ServerPlayer player)
	{
		player.xxa = sidewaysStrafe;
		player.zza = sidewaysStrafe;
		player.jumping = (jumpAndSneakState & 1) == 1;
		player.setShiftKeyDown((jumpAndSneakState & 2) == 2);
	}
}
