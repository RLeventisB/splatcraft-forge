package net.splatcraft.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record WeaponWorkbenchRecipe(ResourceLocation tab, List<WeaponWorkbenchSubtypeRecipe> subRecipes,
                                    int pos) implements Recipe<SingleRecipeInput>, Comparable<WeaponWorkbenchRecipe>
{
	@Override
	public boolean matches(@NotNull SingleRecipeInput inv, @NotNull Level levelIn)
	{
		return true;
	}
	@Override
	public @NotNull ItemStack assemble(@NotNull SingleRecipeInput inv, @NotNull HolderLookup.Provider access)
	{
		return ItemStack.EMPTY;
	}
	@Override
	public boolean canCraftInDimensions(int width, int height)
	{
		return false;
	}
	@Override
	public @NotNull ItemStack getResultItem(@NotNull HolderLookup.Provider access)
	{
		return subRecipes.isEmpty() ? ItemStack.EMPTY : subRecipes.getFirst().getOutput().copy();
	}
	public ResourceLocation getId(RecipeManager manager)
	{
		List<RecipeHolder<WeaponWorkbenchRecipe>> recipeEntries = new ArrayList<>(manager.getAllRecipesFor(SplatcraftRecipeTypes.WEAPON_STATION_TYPE));
		recipeEntries.removeIf(v -> v.value() != this);
		return recipeEntries.getFirst().id();
	}
	@Override
	public @NotNull RecipeSerializer<?> getSerializer()
	{
		return SplatcraftRecipeTypes.WEAPON_STATION;
	}
	@Override
	public @NotNull RecipeType<?> getType()
	{
		return SplatcraftRecipeTypes.WEAPON_STATION_TYPE;
	}
	@Override
	public int compareTo(WeaponWorkbenchRecipe o)
	{
		return pos - o.pos;
	}
	public RecipeHolder<?> getTab(Level world)
	{
		return world.getRecipeManager().byKey(tab).orElse(null);
	}
	public WeaponWorkbenchSubtypeRecipe getRecipeFromIndex(Player player, int subTypePos)
	{
		return getAvailableRecipes(player).get(subTypePos);
	}
	public int getAvailableRecipesTotal(Player player)
	{
		return getAvailableRecipes(player).size();
	}
	public List<WeaponWorkbenchSubtypeRecipe> getAvailableRecipes(Player player)
	{
		return subRecipes.stream().filter(weaponWorkbenchSubtypeRecipe -> weaponWorkbenchSubtypeRecipe.isAvailable(player)).toList();
	}
	public static class Serializer implements RecipeSerializer<WeaponWorkbenchRecipe>
	{
		public static final StreamCodec<RegistryFriendlyByteBuf, WeaponWorkbenchRecipe> PACKET_CODEC = StreamCodec.ofMember((recipe, buffer) ->
		{
			buffer.writeResourceLocation(recipe.tab);
			WeaponWorkbenchSubtypeRecipe.LIST_PACKET_CODEC.encode(buffer, recipe.subRecipes);
			buffer.writeInt(recipe.pos);
		}, (buffer) -> new WeaponWorkbenchRecipe(buffer.readResourceLocation(), WeaponWorkbenchSubtypeRecipe.LIST_PACKET_CODEC.decode(buffer), buffer.readInt()));
		public static final MapCodec<WeaponWorkbenchRecipe> CODEC = RecordCodecBuilder.mapCodec(inst ->
			inst.group(
				ResourceLocation.CODEC.fieldOf("tab").forGetter(WeaponWorkbenchRecipe::tab),
				WeaponWorkbenchSubtypeRecipe.CODEC.listOf().fieldOf("recipes").forGetter(WeaponWorkbenchRecipe::subRecipes),
				Codec.INT.fieldOf("pos").forGetter(WeaponWorkbenchRecipe::pos)
			).apply(inst, WeaponWorkbenchRecipe::new)
		);
		@Override
		public @NotNull MapCodec<WeaponWorkbenchRecipe> codec()
		{
			return CODEC;
		}
		@Override
		public @NotNull StreamCodec<RegistryFriendlyByteBuf, WeaponWorkbenchRecipe> streamCodec()
		{
			return PACKET_CODEC;
		}
	}
}
