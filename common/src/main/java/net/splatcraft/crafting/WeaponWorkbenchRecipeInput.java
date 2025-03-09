package net.splatcraft.crafting;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record WeaponWorkbenchRecipeInput(Inventory inventory) implements RecipeInput
{
    @Override
    public ItemStack getItem(int slot)
    {
        return inventory.getItem(slot);
    }

    @Override
    public int size()
    {
        return inventory.getContainerSize();
    }
}
