package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.items.weapons.IChargeableWeapon;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.PlayerCharge;

public class ReleaseChargePacket extends PlayC2SPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(ReleaseChargePacket.class);
	private final float charge;
	private final ItemStack stack;
	private final boolean resetCharge;
	public ReleaseChargePacket(float charge, ItemStack stack)
	{
		this(charge, stack, true);
	}
	public ReleaseChargePacket(float charge, ItemStack stack, boolean resetCharge)
	{
		this.charge = charge;
		this.stack = stack;
		this.resetCharge = resetCharge;
	}
	public static ReleaseChargePacket decode(RegistryByteBuf buffer)
	{
		return new ReleaseChargePacket(buffer.readFloat(), ItemStack.PACKET_CODEC.decode(buffer), buffer.readBoolean());
	}
	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
	@Override
	public void execute(PlayerEntity player)
	{
		if (!PlayerCharge.hasCharge(player))
		{
			throw new IllegalStateException(
				String.format("%s attempted to release a charge (%.2f; %s), but the server does not recall them having a charge",
					player.getGameProfile(), charge, stack.getItem()));
		}
		
		if (stack.getItem() instanceof IChargeableWeapon weapon)
		{
			weapon.onReleaseCharge(player.getWorld(), player, stack, charge);
		}
		
		if (resetCharge)
			PlayerCharge.updateServerMap(player, false);
	}
	@Override
	public void encode(RegistryByteBuf buffer)
	{
		buffer.writeFloat(charge);
		ItemStack.PACKET_CODEC.encode(buffer, stack);
		buffer.writeBoolean(resetCharge);
	}
}