package net.splatcraft.dummys;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public interface ISplatcraftForgeItemDummy
{
	default boolean phOnEntityItemUpdate(ItemStack stack, ItemEntity entity)
	{
		return false;
	}
	default boolean phIsRepairable(ItemStack stack)
	{
		return stack.has(DataComponents.MAX_DAMAGE);
	}
	default int phGetMaxStackSize(ItemStack stack)
	{
		return stack.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
	}
	default boolean phShouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
	{
		return !oldStack.equals(newStack);
	}
}
