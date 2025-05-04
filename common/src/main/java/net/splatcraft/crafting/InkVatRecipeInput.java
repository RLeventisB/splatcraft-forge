package net.splatcraft.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

public class InkVatRecipeInput implements RecipeInput
{
	private final NonNullList<ItemStack> inventory;

	public InkVatRecipeInput(NonNullList<ItemStack> list)
	{
		inventory = list;
	}

	@Override
	public @NotNull ItemStack getItem(int slot)
	{
		return inventory.get(slot);
	}

	@Override
	public int size()
	{
		return 5;
	}
}
