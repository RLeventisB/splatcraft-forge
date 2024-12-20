package net.splatcraft.crafting;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.RecipeInput;

public record WeaponWorkbenchRecipeInput(PlayerInventory inventory) implements RecipeInput
{
    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return inventory.getStack(slot);
    }

    @Override
    public int getSize()
    {
        return inventory.size();
    }
}
