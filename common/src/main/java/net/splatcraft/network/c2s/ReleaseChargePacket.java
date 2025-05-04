package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.items.weapons.IChargeableWeapon;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.PlayerCharge;
import org.jetbrains.annotations.NotNull;

public class ReleaseChargePacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(ReleaseChargePacket.class);
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
	public static ReleaseChargePacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new ReleaseChargePacket(buffer.readFloat(), ItemStack.STREAM_CODEC.decode(buffer), buffer.readBoolean());
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void execute(Player player)
	{
		if (!PlayerCharge.hasCharge(player))
		{
			throw new IllegalStateException(
				String.format("%s attempted to release a charge (%.2f; %s), but the server does not recall them having a charge",
					player.getGameProfile(), charge, stack.getItem()));
		}

		if (stack.getItem() instanceof IChargeableWeapon weapon)
		{
			weapon.onReleaseCharge(player.level(), player, stack, charge);
		}

		if (resetCharge)
			PlayerCharge.updateServerMap(player, false);
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeFloat(charge);
		ItemStack.STREAM_CODEC.encode(buffer, stack);
		buffer.writeBoolean(resetCharge);
	}
}