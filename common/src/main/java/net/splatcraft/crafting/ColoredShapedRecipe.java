package net.splatcraft.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ColoredShapedRecipe extends ShapedRecipe
{
	public ColoredShapedRecipe(String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result)
	{
		super(group, CraftingBookCategory.MISC, new ShapedRecipePattern(width, height, ingredients, Optional.empty()), result);
	}
	@Override
	public @NotNull ItemStack assemble(CraftingInput inventory, @NotNull HolderLookup.Provider access)
	{
		int color = 0, j = 0, curColor = 0;
		boolean colorLock = false;
		
		for (int i = 0; i < inventory.size(); i++)
		{
			ItemStack stack = inventory.getItem(i);
			
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
		
		return ColorUtils.withColorLocked(ColorUtils.withInkColor(super.assemble(inventory, access), j == 0 ? InkColor.INVALID : InkColor.constructOrReuse(color / j)), colorLock);
	}
	@Override
	public boolean matches(@NotNull CraftingInput recipe, @NotNull Level world)
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
