package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

public class UpdateInputPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateInputPacket.class);
	final float sidewaysStrafe, forwardStrafe;
	final byte jumpAndSneakState;
	public UpdateInputPacket(Player player)
	{
		sidewaysStrafe = player.xxa;
		forwardStrafe = player.zza;
		jumpAndSneakState = (byte) ((player.jumping ? 1 : 0) | (player.isShiftKeyDown() ? 2 : 0));
	}
	public UpdateInputPacket(float sidewaysStrafe, float forwardStrafe, byte jumpAndSneakState)
	{
		this.sidewaysStrafe = sidewaysStrafe;
		this.forwardStrafe = forwardStrafe;
		this.jumpAndSneakState = jumpAndSneakState;
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
	public void execute(Player player)
	{
		player.xxa = sidewaysStrafe;
		player.zza = sidewaysStrafe;
		player.jumping = (jumpAndSneakState & 1) == 1;
		player.setShiftKeyDown((jumpAndSneakState & 2) == 2);
	}
}
