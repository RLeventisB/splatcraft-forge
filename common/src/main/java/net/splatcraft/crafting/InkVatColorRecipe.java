package net.splatcraft.crafting;

import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

public class InkVatColorRecipe implements Recipe<InkVatRecipeInput>
{
	protected static final ArrayList<InkColor> omniColors = Lists.newArrayList();
	protected final Ingredient ingredient;
	protected final ResourceLocation colorId;
	protected final Supplier<InkColor> colorSupplier;
	protected final boolean disableOmni;
	public InkVatColorRecipe(Ingredient input, ResourceLocation colorId, boolean disableOmni)
	{
		this.disableOmni = disableOmni;
		ingredient = input;
		this.colorId = colorId;
		colorSupplier = Suppliers.memoize(() ->
		{
			InkColor outputColor = InkColorRegistry.getInkColorByAlias(colorId);
			if (!disableOmni && !omniColors.contains(outputColor))
			{
				omniColors.add(outputColor);
			}
			return outputColor;
		});
	}
	public static Collection<InkColor> getOmniList()
	{
		return omniColors;
	}
	@Override
	public boolean matches(InkVatRecipeInput inv, @NotNull Level levelIn)
	{
		return ingredient.test(inv.getItem(3));
	}
	@Override
	public @NotNull ItemStack assemble(InkVatRecipeInput input, HolderLookup.@NotNull Provider lookup)
	{
		return input.getItem(0);
	}
	@Override
	public boolean canCraftInDimensions(int width, int height)
	{
		return true;
	}
	@Override
	public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registriesLookup)
	{
		return ColorUtils.withInkColor(new ItemStack(SplatcraftBlocks.inkwell.get()), colorSupplier.get());
	}
	public InkColor getOutputColor()
	{
		return colorSupplier.get();
	}
	private ResourceLocation getOutputColorId()
	{
		return colorId;
	}
	public boolean isDisableOmni()
	{
		return disableOmni;
	}
	public Ingredient getIngredient()
	{
		return ingredient;
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
		public static final MapCodec<InkVatColorRecipe> CODEC = RecordCodecBuilder.mapCodec((instance) ->
			instance.group(
				Ingredient.CODEC.optionalFieldOf("filter", Ingredient.EMPTY).forGetter(v -> v.ingredient),
				ResourceLocation.CODEC.fieldOf("color").forGetter(v -> v.colorId),
				Codec.BOOL.optionalFieldOf("not_on_omni_filter", false).forGetter(v -> v.disableOmni)
			).apply(instance, InkVatColorRecipe::new));
		public static final StreamCodec<RegistryFriendlyByteBuf, InkVatColorRecipe> PACKET_CODEC = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC, InkVatColorRecipe::getIngredient,
			ResourceLocation.STREAM_CODEC, InkVatColorRecipe::getOutputColorId,
			ByteBufCodecs.BOOL, InkVatColorRecipe::isDisableOmni,
			InkVatColorRecipe::new);
		@Override
		public @NotNull MapCodec<InkVatColorRecipe> codec()
		{
			return CODEC;
		}
		@Override
		public @NotNull StreamCodec<RegistryFriendlyByteBuf, InkVatColorRecipe> streamCodec()
		{
			return PACKET_CODEC;
		}
	}
}
