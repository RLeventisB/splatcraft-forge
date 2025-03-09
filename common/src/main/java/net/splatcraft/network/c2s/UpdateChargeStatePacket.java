package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.PlayerCharge;

public class UpdateChargeStatePacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateChargeStatePacket.class);
	private final boolean hasCharge;
	public UpdateChargeStatePacket(boolean hasCharge)
	{
		this.hasCharge = hasCharge;
	}
	public static UpdateChargeStatePacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new UpdateChargeStatePacket(buffer.readBoolean());
	}
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeBoolean(hasCharge);
	}
	@Override
	public void execute(Player player)
	{
		PlayerCharge.updateServerMap(player, hasCharge);
	}
}
