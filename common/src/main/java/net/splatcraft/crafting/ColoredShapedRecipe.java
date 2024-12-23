package net.splatcraft.crafting;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RawShapedRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ColoredShapedRecipe extends ShapedRecipe
{
	public ColoredShapedRecipe(String group, int width, int height, DefaultedList<Ingredient> ingredients, ItemStack result)
	{
		super(group, CraftingRecipeCategory.MISC, new RawShapedRecipe(width, height, ingredients, Optional.empty()), result);
	}
	@Override
	public @NotNull ItemStack craft(CraftingRecipeInput inventory, @NotNull RegistryWrapper.WrapperLookup access)
	{
		int color = 0, j = 0, curColor = 0;
		boolean colorLock = false;
		
		for (int i = 0; i < inventory.getSize(); i++)
		{
			ItemStack stack = inventory.getStackInSlot(i);
			
			if (stack.getItem() == SplatcraftItems.inkwell.get() && ColorUtils.getInkColor(stack).isValid())
			{
				color += ColorUtils.getInkColor(stack).getColor();
				j++;
				
				if (ColorUtils.isColorLocked(stack))
					colorLock = true;
				else
					curColor = ColorUtils.getInkColor(stack).getColor();
			}
		}
		
		if (!colorLock)
			color = curColor;
		
		return ColorUtils.withColorLocked(ColorUtils.withInkColor(super.craft(inventory, access), j == 0 ? InkColor.INVALID : InkColor.constructOrReuse(color / j)), colorLock);
	}
	@Override
	public boolean matches(@NotNull CraftingRecipeInput recipe, @NotNull World world)
	{
		return super.matches(recipe, world);
	}
	@Override
	public @NotNull RecipeSerializer<?> getSerializer()
	{
		return super.getSerializer();
	}
	public static class Serializer extends ShapedRecipe.Serializer
	{
		public Serializer()
		{
			super();
		}
	}
}
