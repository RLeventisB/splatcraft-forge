package net.splatcraft.network.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.items.weapons.IChargeableWeapon;
import net.splatcraft.network.c2s.PlayC2SPacket;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.EntityStoredCharge;
import org.jetbrains.annotations.NotNull;

public class VoidedChargePacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(VoidedChargePacket.class);
	public VoidedChargePacket()
	{
	}
	public static VoidedChargePacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new VoidedChargePacket();
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
	}
	@Override
	public void execute(ServerPlayer player)
	{
		EntityStoredCharge.emptyStoredCharge(player);
		for (InteractionHand hand : InteractionHand.values())
		{
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof IChargeableWeapon chargeableWeapon)
				chargeableWeapon.setCharge(stack, 0f);
		}
	}
}
