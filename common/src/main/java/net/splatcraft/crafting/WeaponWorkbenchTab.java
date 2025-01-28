package net.splatcraft.crafting;

import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class WeaponWorkbenchTab implements Recipe<WeaponWorkbenchRecipeInput>, Comparable<WeaponWorkbenchTab>
{
	public final boolean hidden;
	protected final Identifier iconLoc;
	protected final int pos;
	protected final Optional<Text> name;
	protected final Supplier<Text> nameSupplier;
	public WeaponWorkbenchTab(Identifier iconLoc, int pos, Optional<Text> name, boolean hidden)
	{
		this.iconLoc = iconLoc;
		this.pos = pos;
		this.hidden = hidden;
		this.name = name;
		// todo: find a better way to get the file name for recipe files!!! since this field (and alot more) depends on it
		// for now name is stored since this recipe is synched via a packetcodec below, and you cant serialize suppliers as far as i know
		nameSupplier = Suppliers.memoize(() -> name.orElse(Text.translatable("weaponTab." + CommonUtils.getRecipeId(this).toString())));
	}
	@Override
	public boolean matches(@NotNull WeaponWorkbenchRecipeInput inv, @NotNull World levelIn)
	{
		return true;
	}
	@Override
	public @NotNull ItemStack craft(@NotNull WeaponWorkbenchRecipeInput inv, @NotNull RegistryWrapper.WrapperLookup access)
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
	public List<WeaponWorkbenchRecipe> getTabRecipes(World world, PlayerEntity player)
	{
		List<RecipeEntry<?>> stream = world.getRecipeManager().values().stream().filter(recipe ->
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
	public Identifier getTabIcon()
	{
		return iconLoc;
	}
	@Override
	public String toString()
	{
		return getName().toString();
	}
	public Text getName()
	{
		return nameSupplier.get();
	}
	public static class WeaponWorkbenchTabSerializer implements RecipeSerializer<WeaponWorkbenchTab>
	{
		public static final MapCodec<WeaponWorkbenchTab> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
			Identifier.CODEC.fieldOf("icon").forGetter(v -> v.iconLoc),
			Codec.INT.optionalFieldOf("pos", Integer.MAX_VALUE).forGetter(v -> v.pos),
			TextCodecs.CODEC.optionalFieldOf("name").forGetter(v -> v.name),
			Codec.BOOL.optionalFieldOf("hidden", false).forGetter(v -> v.hidden)
		).apply(inst, WeaponWorkbenchTab::new));
		public static final PacketCodec<RegistryByteBuf, WeaponWorkbenchTab> PACKET_CODEC = new PacketCodec<>()
		{
			@Override
			public WeaponWorkbenchTab decode(RegistryByteBuf buffer)
			{
				return new WeaponWorkbenchTab(
					buffer.readIdentifier(),
					buffer.readInt(),
					PacketCodecs.optional(TextCodecs.PACKET_CODEC).decode(buffer),
					buffer.readBoolean());
			}
			@Override
			public void encode(RegistryByteBuf buffer, WeaponWorkbenchTab recipe)
			{
				buffer.writeIdentifier(recipe.iconLoc);
				buffer.writeInt(recipe.pos);
				PacketCodecs.optional(TextCodecs.PACKET_CODEC).encode(buffer, recipe.name);
				buffer.writeBoolean(recipe.hidden);
			}
		};
		@Override
		public MapCodec<WeaponWorkbenchTab> codec()
		{
			return CODEC;
		}
		@Override
		public PacketCodec<RegistryByteBuf, WeaponWorkbenchTab> packetCodec()
		{
			return PACKET_CODEC;
		}
	}
}