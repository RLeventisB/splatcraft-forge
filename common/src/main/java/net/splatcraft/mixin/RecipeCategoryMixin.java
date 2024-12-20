package net.splatcraft.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.recipebook.RecipeBookGroup;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.splatcraft.crafting.SplatcraftRecipeTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientRecipeBook.class)
public class RecipeCategoryMixin
{
    @Environment(EnvType.CLIENT)
    @Inject(method = "getGroupForRecipe", at = @At("HEAD"), cancellable = true)
    private static void getCategory(RecipeEntry<?> recipe, CallbackInfoReturnable<RecipeBookGroup> cir)
    {
        RecipeType<?> type = recipe.value().getType();
        if (type == SplatcraftRecipeTypes.INK_VAT_COLOR_CRAFTING_TYPE || type == SplatcraftRecipeTypes.WEAPON_STATION_TAB_TYPE
            || type == SplatcraftRecipeTypes.WEAPON_STATION_TYPE)
        {
            cir.setReturnValue(RecipeBookGroup.UNKNOWN);
        }
    }
}
