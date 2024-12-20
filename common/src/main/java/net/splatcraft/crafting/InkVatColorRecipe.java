package net.splatcraft.crafting;

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
import net.minecraft.world.World;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class InkVatColorRecipe implements Recipe<InkVatRecipeInput>
{
    protected static final ArrayList<InkColor> omniColors = Lists.newArrayList();
    protected final Ingredient ingredient;
    protected final InkColor color;
    protected final boolean disableOmni;

    public InkVatColorRecipe(Ingredient input, InkColor outputColor, boolean disableOmni)
    {
        this.disableOmni = disableOmni;
        ingredient = input;
        color = outputColor;

        if (!disableOmni && !omniColors.contains(color))
        {
            omniColors.add(color);
        }
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
        return ColorUtils.setInkColor(new ItemStack(SplatcraftBlocks.inkwell.get()), color);
    }

    public InkColor getOutputColor()
    {
        return color;
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
                Ingredient.ALLOW_EMPTY_CODEC.fieldOf("filter").forGetter(v -> v.ingredient),
                InkColor.CODEC.fieldOf("color").forGetter(v -> v.color),
                Codec.BOOL.fieldOf("not_on_omni_filter").forGetter(v -> v.disableOmni)
            ).apply(instance, InkVatColorRecipe::new));
        public static final PacketCodec<RegistryByteBuf, InkVatColorRecipe> PACKET_CODEC = PacketCodec.tuple(
            Ingredient.PACKET_CODEC, InkVatColorRecipe::getIngredient,
            InkColor.PACKET_CODEC, InkVatColorRecipe::getOutputColor,
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
