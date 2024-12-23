package net.splatcraft.dummys;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;

public interface ISplatcraftForgeItemDummy
{
	default boolean phOnEntityItemUpdate(ItemStack stack, ItemEntity entity)
	{
		return false;
	}
	default boolean phIsRepairable(ItemStack stack)
	{
		return stack.contains(DataComponentTypes.MAX_DAMAGE);
	}
	default int phGetMaxStackSize(ItemStack stack)
	{
		return stack.getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 1);
	}
	default boolean phShouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
	{
		return !oldStack.equals(newStack);
	}
}
