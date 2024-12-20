package net.splatcraft.crafting;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.util.collection.DefaultedList;

public class InkVatRecipeInput implements RecipeInput
{
    private final DefaultedList<ItemStack> inventory;

    public InkVatRecipeInput(DefaultedList<ItemStack> list)
    {
        inventory = list;
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return inventory.get(slot);
    }

    @Override
    public int getSize()
    {
        return 5;
    }
}
