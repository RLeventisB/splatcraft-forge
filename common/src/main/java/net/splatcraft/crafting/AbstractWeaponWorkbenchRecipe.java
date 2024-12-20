package net.splatcraft.crafting;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWeaponWorkbenchRecipe implements Recipe<SingleStackRecipeInput>
{
    protected final ItemStack recipeOutput;
    protected final List<StackedIngredient> recipeItems;
    protected final Text name;

    public AbstractWeaponWorkbenchRecipe(Text name, ItemStack recipeOutput, List<StackedIngredient> recipeItems)
    {
        this.recipeOutput = recipeOutput;
        this.recipeItems = recipeItems;
        this.name = name;
    }

    @Override
    public boolean matches(SingleStackRecipeInput inv, @NotNull World world)
    {
        List<ItemStack> inputs = new ArrayList<>();
        int i = 0;

        for (int j = 0; j < inv.getSize(); ++j)
        {
            ItemStack itemstack = inv.getStackInSlot(j);
            if (!itemstack.isEmpty())
            {
                ++i;
                inputs.add(itemstack);
            }
        }

        return i == recipeItems.size() && CommonUtils.findMatches(inputs, recipeItems) != null;
    }

    public Text getName()
    {
        return name;
    }

    @Override
    public @NotNull ItemStack craft(@NotNull SingleStackRecipeInput inv, @NotNull RegistryWrapper.WrapperLookup access)
    {
        return recipeOutput;
    }

    @Override
    public boolean fits(int width, int height)
    {
        return false;
    }

    @Override
    public @NotNull ItemStack getResult(@NotNull RegistryWrapper.WrapperLookup access)
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
