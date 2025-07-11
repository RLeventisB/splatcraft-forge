package net.splatcraft.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.function.Predicate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class StackedIngredient implements Predicate<ItemStack>
{
	public static final Codec<StackedIngredient> CODEC = RecordCodecBuilder.create(inst ->
		inst.group(
			RecipeIngredient.CODEC.fieldOf("id").forGetter(v -> v.ingredient),
			Codec.INT.fieldOf("count").forGetter(v -> v.count)
		).apply(inst, StackedIngredient::new)
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, StackedIngredient> STREAM_CODEC = StreamCodec.of(
		(buffer, ingredient) ->
		{
			RecipeIngredient.STREAM_CODEC.encode(buffer, ingredient.ingredient);
			ByteBufCodecs.INT.encode(buffer, ingredient.count);
		},
		(buffer) ->
			new StackedIngredient(RecipeIngredient.STREAM_CODEC.decode(buffer), ByteBufCodecs.INT.decode(buffer))
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, List<StackedIngredient>> LIST_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs.list());
	protected final RecipeIngredient ingredient;
	protected final int count;
	protected StackedIngredient(RecipeIngredient ingredient, int count)
	{
		this.ingredient = ingredient;
		this.count = count;
	}
	public RecipeIngredient getIngredient()
	{
		return ingredient;
	}
	public int getCount()
	{
		return count;
	}
	@Override
	public boolean test(ItemStack itemStack)
	{
		return getIngredient().test(itemStack);
	}
	public record RecipeIngredient(ResourceLocation itemId)
	{
		public static final StreamCodec<RegistryFriendlyByteBuf, RecipeIngredient> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, RecipeIngredient::itemId,
			RecipeIngredient::new
		);
		public static final Codec<RecipeIngredient> CODEC = ResourceLocation.CODEC.xmap(RecipeIngredient::new, RecipeIngredient::itemId);
		public boolean test(ItemStack stack)
		{
			return BuiltInRegistries.ITEM.getKey(stack.getItem().asItem()).equals(itemId);
		}
		public boolean test(ItemStack stack, int count)
		{
			return test(stack) && stack.getCount() >= count;
		}
		public Item getItem()
		{
			return BuiltInRegistries.ITEM.get(itemId);
		}
		public ItemStack getStack()
		{
			return new ItemStack(getItem());
		}
	}
}
