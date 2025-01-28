package net.splatcraft.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record WeaponWorkbenchRecipe(Identifier tab, List<WeaponWorkbenchSubtypeRecipe> subRecipes,
                                    int pos) implements Recipe<SingleStackRecipeInput>, Comparable<WeaponWorkbenchRecipe>
{
	@Override
	public boolean matches(@NotNull SingleStackRecipeInput inv, @NotNull World levelIn)
	{
		return true;
	}
	@Override
	public @NotNull ItemStack craft(@NotNull SingleStackRecipeInput inv, @NotNull RegistryWrapper.WrapperLookup access)
	{
		return ItemStack.EMPTY;
	}
	@Override
	public boolean fits(int width, int height)
	{
		return false;
	}
	@Override
	public @NotNull ItemStack getResult(@NotNull RegistryWrapper.WrapperLookup access)
	{
		return subRecipes.isEmpty() ? ItemStack.EMPTY : subRecipes.getFirst().getOutput().copy();
	}
	public Identifier getId(RecipeManager manager)
	{
		List<RecipeEntry<WeaponWorkbenchRecipe>> recipeEntries = new ArrayList<>(manager.listAllOfType(SplatcraftRecipeTypes.WEAPON_STATION_TYPE));
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
	public RecipeEntry<?> getTab(World world)
	{
		return world.getRecipeManager().get(tab).orElse(null);
	}
	public WeaponWorkbenchSubtypeRecipe getRecipeFromIndex(PlayerEntity player, int subTypePos)
	{
		return getAvailableRecipes(player).get(subTypePos);
	}
	public int getAvailableRecipesTotal(PlayerEntity player)
	{
		return getAvailableRecipes(player).size();
	}
	public List<WeaponWorkbenchSubtypeRecipe> getAvailableRecipes(PlayerEntity player)
	{
		return subRecipes.stream().filter(weaponWorkbenchSubtypeRecipe -> weaponWorkbenchSubtypeRecipe.isAvailable(player)).toList();
	}
	public static class Serializer implements RecipeSerializer<WeaponWorkbenchRecipe>
	{
		public static final PacketCodec<RegistryByteBuf, WeaponWorkbenchRecipe> PACKET_CODEC = PacketCodec.of((recipe, buffer) ->
		{
			buffer.writeIdentifier(recipe.tab);
			WeaponWorkbenchSubtypeRecipe.LIST_PACKET_CODEC.encode(buffer, recipe.subRecipes);
			buffer.writeInt(recipe.pos);
		}, (buffer) -> new WeaponWorkbenchRecipe(buffer.readIdentifier(), WeaponWorkbenchSubtypeRecipe.LIST_PACKET_CODEC.decode(buffer), buffer.readInt()));
		public static final MapCodec<WeaponWorkbenchRecipe> CODEC = RecordCodecBuilder.mapCodec(inst ->
			inst.group(
				Identifier.CODEC.fieldOf("tab").forGetter(WeaponWorkbenchRecipe::tab),
				WeaponWorkbenchSubtypeRecipe.CODEC.listOf().fieldOf("recipes").forGetter(WeaponWorkbenchRecipe::subRecipes),
				Codec.INT.fieldOf("pos").forGetter(WeaponWorkbenchRecipe::pos)
			).apply(inst, WeaponWorkbenchRecipe::new)
		);
		@Override
		public MapCodec<WeaponWorkbenchRecipe> codec()
		{
			return CODEC;
		}
		@Override
		public PacketCodec<RegistryByteBuf, WeaponWorkbenchRecipe> packetCodec()
		{
			return PACKET_CODEC;
		}
	}
}
