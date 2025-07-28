package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.items.SpecialProviderItem;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

public class RequestSpecialUsageDataPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(RequestSpecialUsageDataPacket.class);
	final int weaponIndex, providerIndex;
	public RequestSpecialUsageDataPacket(int weaponIndex, int index)
	{
		this.weaponIndex = weaponIndex;
		providerIndex = index;
	}
	public static RequestSpecialUsageDataPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new RequestSpecialUsageDataPacket(buffer.readInt(), buffer.readInt());
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeInt(weaponIndex);
		buffer.writeInt(providerIndex);
	}
	@Override
	public void execute(ServerPlayer player)
	{
		Inventory inventory = player.getInventory();
		ItemStack weaponStack = inventory.getItem(weaponIndex);
		ItemStack providerStack = inventory.getItem(providerIndex);
		if (providerStack.getItem() instanceof SpecialProviderItem providerItem)
		{
			providerItem.tryUsingSpecial(player.level(), player, providerStack, weaponStack);
		}
	}
}
