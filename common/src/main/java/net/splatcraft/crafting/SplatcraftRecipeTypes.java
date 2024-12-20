package net.splatcraft.crafting;

import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.registry.Registries;
import net.splatcraft.Splatcraft;

public class SplatcraftRecipeTypes
{
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPE_REGISTRY = Splatcraft.deferredRegistryOf(Registries.RECIPE_TYPE);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZER_REGISTRY = Splatcraft.deferredRegistryOf(Registries.RECIPE_SERIALIZER);
    public static final RecipeSerializer<InkVatColorRecipe> INK_VAT_COLOR_CRAFTING = new InkVatColorRecipe.InkVatColorSerializer();
    public static final RecipeSerializer<WeaponWorkbenchTab> WEAPON_STATION_TAB = new WeaponWorkbenchTab.WeaponWorkbenchTabSerializer();
    public static final RecipeSerializer<WeaponWorkbenchRecipe> WEAPON_STATION = new WeaponWorkbenchRecipe.Serializer();
    public static final RecipeSerializer<SingleUseSubRecipe> SINGLE_USE_SUB = new SpecialRecipeSerializer<>(SingleUseSubRecipe::new);
    public static final RecipeSerializer<ShapedRecipe> COLORED_SHAPED_CRAFTING = new ColoredShapedRecipe.Serializer();
    public static RecipeType<AbstractWeaponWorkbenchRecipe> WEAPON_STATION_TYPE;
    public static RecipeType<WeaponWorkbenchTab> WEAPON_STATION_TAB_TYPE;
    public static RecipeType<InkVatColorRecipe> INK_VAT_COLOR_CRAFTING_TYPE;

    public static boolean getItem(PlayerEntity player, Ingredient ingredient, int count, boolean takeItems)
    {
        for (int i = 0; i < player.getInventory().size(); ++i)
        {
            ItemStack invStack = player.getInventory().getStack(i);
            if (!takeItems)
            {
                invStack = invStack.copy();
            }

            if (ingredient.test(invStack))
            {
                if (count > invStack.getCount())
                {
                    count -= invStack.getCount();
                    invStack.setCount(0);
                }
                else
                {
                    invStack.setCount(invStack.getCount() - count);
                    return true;
                }
            }
        }
        return false;
    }

    public static void register()
    {
        INK_VAT_COLOR_CRAFTING_TYPE = createRecipeType("ink_vat_color");
        WEAPON_STATION_TAB_TYPE = createRecipeType("weapon_workbench_tab");
        WEAPON_STATION_TYPE = createRecipeType("weapon_workbench");
        RECIPE_TYPE_REGISTRY.register(Splatcraft.identifierOf("ink_vat_color"), () -> INK_VAT_COLOR_CRAFTING_TYPE);
        RECIPE_TYPE_REGISTRY.register(Splatcraft.identifierOf("weapon_workbench_tab"), () -> WEAPON_STATION_TAB_TYPE);
        RECIPE_TYPE_REGISTRY.register(Splatcraft.identifierOf("weapon_workbench"), () -> WEAPON_STATION_TYPE);

        RECIPE_SERIALIZER_REGISTRY.register(Splatcraft.identifierOf("ink_vat_color"), () -> INK_VAT_COLOR_CRAFTING);
        RECIPE_SERIALIZER_REGISTRY.register(Splatcraft.identifierOf("weapon_workbench_tab"), () -> WEAPON_STATION_TAB);
        RECIPE_SERIALIZER_REGISTRY.register(Splatcraft.identifierOf("weapon_workbench"), () -> WEAPON_STATION);
        RECIPE_SERIALIZER_REGISTRY.register(Splatcraft.identifierOf("colored_crafting_shaped"), () -> COLORED_SHAPED_CRAFTING);
        RECIPE_SERIALIZER_REGISTRY.register(Splatcraft.identifierOf("single_use_sub"), () -> SINGLE_USE_SUB);
    }

    // literally recipetype.register but without ofVanilla
    public static <T extends Recipe<?>> RecipeType<T> createRecipeType(String id)
    {
        return new RecipeType<T>()
        {
            public String toString()
            {
                return id;
            }
        };
    }
}
