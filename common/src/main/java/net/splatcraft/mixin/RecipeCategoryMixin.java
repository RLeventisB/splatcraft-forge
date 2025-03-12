package net.splatcraft.mixin;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.crafting.SplatcraftRecipeTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientRecipeBook.class)
public class RecipeCategoryMixin
{
	@OnlyIn(Dist.CLIENT)
	@Inject(method = "getCategory", at = @At("HEAD"), cancellable = true)
	private static void getCategory(RecipeHolder<?> recipe, CallbackInfoReturnable<RecipeBookCategories> cir)
	{
		RecipeType<?> type = recipe.value().getType();
		if (type == SplatcraftRecipeTypes.INK_VAT_COLOR_CRAFTING_TYPE || type == SplatcraftRecipeTypes.WEAPON_STATION_TAB_TYPE
			|| type == SplatcraftRecipeTypes.WEAPON_STATION_TYPE)
		{
			cir.setReturnValue(RecipeBookCategories.UNKNOWN);
		}
	}
}
