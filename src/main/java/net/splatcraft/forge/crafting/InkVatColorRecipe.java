package net.splatcraft.forge.crafting;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.splatcraft.forge.registries.SplatcraftBlocks;
import net.splatcraft.forge.util.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

import static net.splatcraft.forge.Splatcraft.LOGGER;

public class InkVatColorRecipe implements Recipe<Container>
{
	protected static final ArrayList<Integer> omniColors = Lists.newArrayList();
	protected final Ingredient ingredient;
	protected final int color;
	protected final boolean disableOmni;
	protected final ResourceLocation id;
	public InkVatColorRecipe(ResourceLocation id, Ingredient input, int outputColor, boolean disableOmni)
	{
		this.id = id;
		this.disableOmni = disableOmni;
		ingredient = input;
		color = outputColor;
		
		if (!disableOmni && !omniColors.contains(color))
		{
			omniColors.add(color);
		}
	}
	public static Collection<Integer> getOmniList()
	{
		return omniColors;
	}
	@Override
	public boolean matches(Container inv, @NotNull Level levelIn)
	{
		return ingredient.test(inv.getItem(3));
	}
	@Override
	public @NotNull ItemStack assemble(Container inv, @NotNull RegistryAccess access)
	{
		return inv.getItem(0);
	}
	@Override
	public boolean canCraftInDimensions(int width, int height)
	{
		return true;
	}
	@Override
	public @NotNull ItemStack getResultItem(@NotNull RegistryAccess access)
	{
		return ColorUtils.setInkColor(new ItemStack(SplatcraftBlocks.inkwell.get()), color);
	}
	public int getOutputColor()
	{
		return color;
	}
	@Override
	public @NotNull ResourceLocation getId()
	{
		return id;
	}
	@Override
	public @NotNull RecipeSerializer<?> getSerializer()
	{
		return SplatcraftRecipeTypes.INK_VAT_COLOR_CRAFTING;
	}
	@Override
	public @NotNull RecipeType<?> getType()
	{
		return SplatcraftRecipeTypes.INK_VAT_COLOR_CRAFTING_TYPE;
	}
	@Override
	public @NotNull ItemStack getToastSymbol()
	{
		return new ItemStack(SplatcraftBlocks.inkVat.get());
	}
	public static class InkVatColorSerializer implements RecipeSerializer<InkVatColorRecipe>
	{
		@Override
		public @NotNull InkVatColorRecipe fromJson(@NotNull ResourceLocation recipeId, JsonObject json)
		{
			Ingredient ingredient = json.has("filter") ? Ingredient.fromJson(json.get("filter")) : Ingredient.EMPTY;
			boolean disableOmni = json.has("not_on_omni_filter") && GsonHelper.getAsBoolean(json, "not_on_omni_filter");
			int color;
			
			try
			{
				color = GsonHelper.getAsInt(json, "color");
			}
			catch (JsonSyntaxException e)
			{
				color = -1;
			}
			
			if (color < 0 || color > 0xFFFFFF)
			{
				String colorStr = GsonHelper.getAsString(json, "color");
				try
				{
					color = Integer.parseInt(colorStr, 16);
				}
				catch (NumberFormatException nfe)
				{
					try
					{
						color = SplatcraftInkColors.REGISTRY.get().getValue(new ResourceLocation(colorStr)).getColor();
					}
					catch (NullPointerException npe)
					{
						LOGGER.error("Parsing error loading recipe {}", recipeId, npe);
						return null;
					}
				}
			}
			
			return new InkVatColorRecipe(recipeId, ingredient, color, disableOmni);
		}
		@Nullable
		@Override
		public InkVatColorRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer)
		{
			return new InkVatColorRecipe(recipeId, Ingredient.fromNetwork(buffer), buffer.readInt(), buffer.readBoolean());
		}
		@Override
		public void toNetwork(@NotNull FriendlyByteBuf buffer, InkVatColorRecipe recipe)
		{
			recipe.ingredient.toNetwork(buffer);
			buffer.writeInt(recipe.color);
			buffer.writeBoolean(recipe.disableOmni);
		}
	}
}
