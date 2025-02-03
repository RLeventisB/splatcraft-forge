package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.items.SpecialProviderItem;
import net.splatcraft.util.CommonUtils;

public class RequestSpecialUsageDataPacket extends PlayC2SPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(RequestSpecialUsageDataPacket.class);
	final int weaponIndex, providerIndex;
	public RequestSpecialUsageDataPacket(int weaponIndex, int index)
	{
		this.weaponIndex = weaponIndex;
		providerIndex = index;
	}
	public static RequestSpecialUsageDataPacket decode(RegistryByteBuf buffer)
	{
		return new RequestSpecialUsageDataPacket(buffer.readInt(), buffer.readInt());
	}
	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
	@Override
	public void encode(RegistryByteBuf buffer)
	{
		buffer.writeInt(weaponIndex);
		buffer.writeInt(providerIndex);
	}
	@Override
	public void execute(PlayerEntity player)
	{
		PlayerInventory inventory = player.getInventory();
		ItemStack weaponStack = inventory.getStack(weaponIndex);
		ItemStack providerStack = inventory.getStack(providerIndex);
		if (providerStack.getItem() instanceof SpecialProviderItem providerItem)
		{
			providerItem.tryUsingSpecial(player.getWorld(), player, providerStack, weaponStack);
		}
	}
}
