package net.splatcraft.crafting;

import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class WeaponWorkbenchTab implements Recipe<WeaponWorkbenchRecipeInput>, Comparable<WeaponWorkbenchTab>
{
	public final boolean hidden;
	protected final ResourceLocation iconLoc;
	protected final int pos;
	protected final Optional<Component> name;
	protected final Supplier<Component> nameSupplier;
	public WeaponWorkbenchTab(ResourceLocation iconLoc, int pos, Optional<Component> name, boolean hidden)
	{
		this.iconLoc = iconLoc;
		this.pos = pos;
		this.hidden = hidden;
		this.name = name;
		// todo: find a better way to get the file name for recipe files!!! since this field (and alot more) depends on it
		// for now name is stored since this recipe is synched via a packetcodec below, and you cant serialize suppliers as far as i know
		nameSupplier = Suppliers.memoize(() -> name.orElse(Component.translatable("weaponTab." + CommonUtils.getRecipeId(this).toString())));
	}
	@Override
	public boolean matches(@NotNull WeaponWorkbenchRecipeInput inv, @NotNull Level levelIn)
	{
		return true;
	}
	@Override
	public @NotNull ItemStack assemble(@NotNull WeaponWorkbenchRecipeInput inv, @NotNull HolderLookup.Provider access)
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
		return ItemStack.EMPTY;
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
	public List<WeaponWorkbenchRecipe> getTabRecipes(Level world, Player player)
	{
		List<RecipeHolder<?>> stream = world.getRecipeManager().getRecipes().stream().filter(recipe ->
			recipe.value() instanceof WeaponWorkbenchRecipe wwRecipe &&
				equals(wwRecipe.getTab(world).value()) &&
				!wwRecipe.getAvailableRecipes(player).isEmpty()).toList();
		ArrayList<WeaponWorkbenchRecipe> recipes = Lists.newArrayList();

		stream.forEach(recipe -> recipes.add((WeaponWorkbenchRecipe) recipe.value()));

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
		return getName().toString();
	}
	public Component getName()
	{
		return nameSupplier.get();
	}
	public static class WeaponWorkbenchTabSerializer implements RecipeSerializer<WeaponWorkbenchTab>
	{
		public static final MapCodec<WeaponWorkbenchTab> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
			ResourceLocation.CODEC.fieldOf("icon").forGetter(v -> v.iconLoc),
			Codec.INT.optionalFieldOf("pos", Integer.MAX_VALUE).forGetter(v -> v.pos),
			ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(v -> v.name),
			Codec.BOOL.optionalFieldOf("hidden", false).forGetter(v -> v.hidden)
		).apply(inst, WeaponWorkbenchTab::new));
		public static final StreamCodec<RegistryFriendlyByteBuf, WeaponWorkbenchTab> PACKET_CODEC = new StreamCodec<>()
		{
			@Override
			public @NotNull WeaponWorkbenchTab decode(RegistryFriendlyByteBuf buffer)
			{
				return new WeaponWorkbenchTab(
					buffer.readResourceLocation(),
					buffer.readInt(),
					ByteBufCodecs.optional(ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC).decode(buffer),
					buffer.readBoolean());
			}
			@Override
			public void encode(RegistryFriendlyByteBuf buffer, WeaponWorkbenchTab recipe)
			{
				buffer.writeResourceLocation(recipe.iconLoc);
				buffer.writeInt(recipe.pos);
				ByteBufCodecs.optional(ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC).encode(buffer, recipe.name);
				buffer.writeBoolean(recipe.hidden);
			}
		};
		@Override
		public @NotNull MapCodec<WeaponWorkbenchTab> codec()
		{
			return CODEC;
		}
		@Override
		public @NotNull StreamCodec<RegistryFriendlyByteBuf, WeaponWorkbenchTab> streamCodec()
		{
			return PACKET_CODEC;
		}
	}
}