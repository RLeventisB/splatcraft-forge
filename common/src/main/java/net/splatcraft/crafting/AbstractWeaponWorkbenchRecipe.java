package net.splatcraft.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWeaponWorkbenchRecipe implements Recipe<RecipeInput>
{
    protected final ItemStack recipeOutput;
    protected final List<StackedIngredient> recipeItems;
    protected final Component name;

    public AbstractWeaponWorkbenchRecipe(Component name, ItemStack recipeOutput, List<StackedIngredient> recipeItems)
    {
        this.recipeOutput = recipeOutput;
        this.recipeItems = recipeItems;
        this.name = name;
    }

    @Override
    public boolean matches(RecipeInput inv, @NotNull Level world)
    {
        List<ItemStack> inputs = new ArrayList<>();
        int i = 0;

        for (int j = 0; j < inv.size(); ++j)
        {
            ItemStack itemstack = inv.getItem(j);
            if (!itemstack.isEmpty())
            {
                ++i;
                inputs.add(itemstack);
            }
        }

        return i == recipeItems.size() && CommonUtils.findMatches(inputs, recipeItems) != null;
    }

    public Component getName()
    {
        return name;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput inv, @NotNull HolderLookup.Provider access)
    {
        return recipeOutput;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height)
    {
        return false;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull HolderLookup.Provider access)
    {
        return recipeOutput;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer()
    {
        return SplatcraftRecipeTypes.WEAPON_STATION;
    }

    @Override
    public @NotNull RecipeType<?> getType()
    {
        return SplatcraftRecipeTypes.WEAPON_STATION_TYPE;
    }

    public ItemStack getOutput()
    {
        return recipeOutput;
    }

    public List<StackedIngredient> getInput()
    {
        return recipeItems;
    }
}
