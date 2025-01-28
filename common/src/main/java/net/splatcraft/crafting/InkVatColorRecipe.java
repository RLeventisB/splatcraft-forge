package net.splatcraft.crafting;

import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
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
	protected final Identifier colorId;
	protected final Supplier<InkColor> colorSupplier;
	protected final boolean disableOmni;
	public InkVatColorRecipe(Ingredient input, Identifier colorId, boolean disableOmni)
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
	public boolean matches(InkVatRecipeInput inv, @NotNull World levelIn)
	{
		return ingredient.test(inv.getStackInSlot(3));
	}
	@Override
	public ItemStack craft(InkVatRecipeInput input, RegistryWrapper.WrapperLookup lookup)
	{
		return input.getStackInSlot(0);
	}
	@Override
	public boolean fits(int width, int height)
	{
		return true;
	}
	@Override
	public ItemStack getResult(RegistryWrapper.WrapperLookup registriesLookup)
	{
		return ColorUtils.withInkColor(new ItemStack(SplatcraftBlocks.inkwell.get()), colorSupplier.get());
	}
	public InkColor getOutputColor()
	{
		return colorSupplier.get();
	}
	private Identifier getOutputColorId()
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
	public @NotNull ItemStack createIcon()
	{
		return new ItemStack(SplatcraftBlocks.inkVat.get());
	}
	public static class InkVatColorSerializer implements RecipeSerializer<InkVatColorRecipe>
	{
		public static final MapCodec<InkVatColorRecipe> CODEC = RecordCodecBuilder.mapCodec((instance) ->
			instance.group(
				Ingredient.ALLOW_EMPTY_CODEC.optionalFieldOf("filter", Ingredient.EMPTY).forGetter(v -> v.ingredient),
				Identifier.CODEC.fieldOf("color").forGetter(v -> v.colorId),
				Codec.BOOL.optionalFieldOf("not_on_omni_filter", false).forGetter(v -> v.disableOmni)
			).apply(instance, InkVatColorRecipe::new));
		public static final PacketCodec<RegistryByteBuf, InkVatColorRecipe> PACKET_CODEC = PacketCodec.tuple(
			Ingredient.PACKET_CODEC, InkVatColorRecipe::getIngredient,
			Identifier.PACKET_CODEC, InkVatColorRecipe::getOutputColorId,
			PacketCodecs.BOOL, InkVatColorRecipe::isDisableOmni,
			InkVatColorRecipe::new);
		@Override
		public MapCodec<InkVatColorRecipe> codec()
		{
			return CODEC;
		}
		@Override
		public PacketCodec<RegistryByteBuf, InkVatColorRecipe> packetCodec()
		{
			return PACKET_CODEC;
		}
	}
}
