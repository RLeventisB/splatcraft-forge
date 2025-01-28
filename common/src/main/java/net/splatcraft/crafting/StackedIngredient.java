package net.splatcraft.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.Predicate;

public class StackedIngredient implements Predicate<ItemStack>
{
    public static final Codec<StackedIngredient> CODEC = RecordCodecBuilder.create(inst ->
        inst.group(
            RecipeIngredient.CODEC.fieldOf("id").forGetter(v -> v.ingredient),
            Codec.INT.fieldOf("count").forGetter(v -> v.count)
        ).apply(inst, StackedIngredient::new)
    );
    public static final PacketCodec<RegistryByteBuf, StackedIngredient> PACKET_CODEC = PacketCodec.ofStatic(
        (buffer, ingredient) ->
        {
            RecipeIngredient.PACKET_CODEC.encode(buffer, ingredient.ingredient);
            PacketCodecs.INTEGER.encode(buffer, ingredient.count);
        },
        (buffer) ->
            new StackedIngredient(RecipeIngredient.PACKET_CODEC.decode(buffer), PacketCodecs.INTEGER.decode(buffer))
    );
    public static final PacketCodec<RegistryByteBuf, List<StackedIngredient>> LIST_PACKET_CODEC = PACKET_CODEC.collect(PacketCodecs.toList());
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
    public record RecipeIngredient(Identifier itemId)
    {
        public static final PacketCodec<RegistryByteBuf, RecipeIngredient> PACKET_CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC, RecipeIngredient::itemId,
            RecipeIngredient::new
        );
        public static final Codec<RecipeIngredient> CODEC = Identifier.CODEC.xmap(RecipeIngredient::new, RecipeIngredient::itemId);
        public boolean test(ItemStack stack)
        {
            return Registries.ITEM.getId(stack.getItem().asItem()).equals(itemId);
        }
        public boolean test(ItemStack stack, int count)
        {
            return test(stack) && stack.getCount() >= count;
        }
        public Item getItem()
        {
            return Registries.ITEM.get(itemId);
        }
        public ItemStack getStack()
        {
            return new ItemStack(getItem());
        }
    }
}
