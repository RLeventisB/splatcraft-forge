package net.splatcraft.forge.crafting;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WeaponWorkbenchTab implements Recipe<Container>, Comparable<WeaponWorkbenchTab>
{
	protected final ResourceLocation id;
	protected final ResourceLocation iconLoc;
	protected final int pos;
	public final boolean hidden;
	protected final Component name;
	public WeaponWorkbenchTab(ResourceLocation id, ResourceLocation iconLoc, int pos, Component name, boolean hidden)
	{
		this.id = id;
		this.iconLoc = iconLoc;
		this.pos = pos;
		this.hidden = hidden;
		this.name = name != null ? name : new TranslatableComponent("weaponTab." + getId());
	}
	@Override
	public boolean matches(@NotNull Container inv, @NotNull Level levelIn)
	{
		return true;
	}
	@Override
	public @NotNull ItemStack assemble(@NotNull Container inv)
	{
		return ItemStack.EMPTY;
	}
	@Override
	public boolean canCraftInDimensions(int width, int height)
	{
		return false;
	}
	@Override
	public @NotNull ItemStack getResultItem()
	{
		return ItemStack.EMPTY;
	}
	@Override
	public @NotNull ResourceLocation getId()
	{
		return id;
	}
	@Override
	public @NotNull RecipeSerializer<?> getSerializer()
	{
		return SplatcraftRecipeTypes.WEAPON_STATION_TAB;
	}
	@Override
	public @NotNull RecipeType<?> getType()
	{
		return SplatcraftRecipeTypes.WEAPON_STATION_TAB_TYPE;
	}
	public List<WeaponWorkbenchRecipe> getTabRecipes(Level level, Player player)
	{
		List<Recipe<?>> stream = level.getRecipeManager().getRecipes().stream().filter(recipe -> recipe instanceof WeaponWorkbenchRecipe wwRecipe && this.equals(wwRecipe.getTab(level)) && !wwRecipe.getAvailableRecipes(player).isEmpty()).collect(Collectors.toList());
		ArrayList<WeaponWorkbenchRecipe> recipes = Lists.newArrayList();
		
		stream.forEach(recipe -> recipes.add((WeaponWorkbenchRecipe) recipe));
		
		return recipes;
	}
	@Override
	public int compareTo(WeaponWorkbenchTab o)
	{
		return pos - o.pos;
	}
	public ResourceLocation getTabIcon()
	{
		return iconLoc;
	}
	@Override
	public String toString()
	{
		return getId().toString();
	}
	public Component getName()
	{
		return name;
	}
	public static class WeaponWorkbenchTabSerializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<WeaponWorkbenchTab>
	{
		public WeaponWorkbenchTabSerializer(String name)
		{
			super();
			setRegistryName(name);
		}
		@Override
		public @NotNull WeaponWorkbenchTab fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json)
		{
			Component displayComponent;
			
			if (GsonHelper.isStringValue(json, "name"))
				displayComponent = new TranslatableComponent(GsonHelper.getAsString(json, "name"));
			else
				displayComponent = json.has("name") ? Component.Serializer.fromJson(json.getAsJsonObject("name")) : null;
			return new WeaponWorkbenchTab(recipeId, new ResourceLocation(GsonHelper.getAsString(json, "icon")), GsonHelper.getAsInt(json, "pos", Integer.MAX_VALUE), displayComponent, GsonHelper.getAsBoolean(json, "hidden", false));
		}
		@Nullable
		@Override
		public WeaponWorkbenchTab fromNetwork(@NotNull ResourceLocation recipeId, FriendlyByteBuf buffer)
		{
			return new WeaponWorkbenchTab(recipeId, buffer.readResourceLocation(), buffer.readInt(), buffer.readComponent(), buffer.readBoolean());
		}
		@Override
		public void toNetwork(FriendlyByteBuf buffer, WeaponWorkbenchTab recipe)
		{
			buffer.writeResourceLocation(recipe.iconLoc);
			buffer.writeInt(recipe.pos);
			buffer.writeComponent(recipe.name);
			buffer.writeBoolean(recipe.hidden);
		}
	}
}
