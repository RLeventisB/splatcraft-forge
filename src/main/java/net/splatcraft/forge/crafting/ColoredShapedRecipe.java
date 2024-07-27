package net.splatcraft.forge.crafting;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.util.ColorUtils;
import org.jetbrains.annotations.NotNull;

public class ColoredShapedRecipe extends ShapedRecipe
{
	public ColoredShapedRecipe(ResourceLocation p_i48162_1_, String p_i48162_2_, int p_i48162_3_, int p_i48162_4_, NonNullList<Ingredient> p_i48162_5_, ItemStack p_i48162_6_)
	{
		super(p_i48162_1_, p_i48162_2_, CraftingBookCategory.MISC, p_i48162_3_, p_i48162_4_, p_i48162_5_, p_i48162_6_);
	}
	@Override
	public @NotNull ItemStack assemble(CraftingContainer inventory, @NotNull RegistryAccess access)
	{
		int color = 0, j = 0, curColor = 0;
		boolean colorLock = false;
		
		for (int i = 0; i < inventory.getContainerSize(); i++)
		{
			ItemStack stack = inventory.getItem(i);
			
			if (stack.getItem() == SplatcraftItems.inkwell.get() && ColorUtils.getInkColor(stack) != -1)
			{
				color += ColorUtils.getInkColor(stack);
				j++;
				
				if (ColorUtils.isColorLocked(stack))
					colorLock = true;
				else curColor = ColorUtils.getInkColor(stack);
			}
		}
		
		if (!colorLock)
			color = curColor;
		
		return ColorUtils.setColorLocked(ColorUtils.setInkColor(super.assemble(inventory, access), j == 0 ? -1 : color / j), colorLock);
	}
	@Override
	public boolean matches(@NotNull CraftingContainer p_77569_1_, @NotNull Level p_77569_2_)
	{
		return super.matches(p_77569_1_, p_77569_2_);
	}
	@Override
	public @NotNull RecipeSerializer<?> getSerializer()
	{
		return super.getSerializer();
	}
	public static class Serializer extends ShapedRecipe.Serializer
	{
		public Serializer(String name)
		{
			super();
		}
		@Override
		public @NotNull ShapedRecipe fromJson(@NotNull ResourceLocation p_199425_1_, @NotNull JsonObject p_199425_2_)
		{
			ShapedRecipe recipe = super.fromJson(p_199425_1_, p_199425_2_);
			return new ColoredShapedRecipe(recipe.getId(), recipe.getGroup(), recipe.getRecipeWidth(), recipe.getRecipeHeight(), recipe.getIngredients(), recipe.getResultItem(RegistryAccess.EMPTY).copy());
		}
		@Override
		public ShapedRecipe fromNetwork(@NotNull ResourceLocation p_199426_1_, @NotNull FriendlyByteBuf p_199426_2_)
		{
			ShapedRecipe recipe = super.fromNetwork(p_199426_1_, p_199426_2_);
			return new ColoredShapedRecipe(recipe.getId(), recipe.getGroup(), recipe.getRecipeWidth(), recipe.getRecipeHeight(), recipe.getIngredients(), recipe.getResultItem(RegistryAccess.EMPTY).copy());
		}
	}
}
