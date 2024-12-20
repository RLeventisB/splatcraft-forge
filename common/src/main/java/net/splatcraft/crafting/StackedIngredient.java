package net.splatcraft.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class StackedIngredient implements Predicate<ItemStack>
{
    public static final StackedIngredient EMPTY = new StackedIngredient(Ingredient.EMPTY, 0);
    public static final Codec<StackedIngredient> CODEC = RecordCodecBuilder.create(inst ->
        inst.group(
            Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("ingredient").forGetter(v -> v.ingredient),
            Codec.INT.fieldOf("count").forGetter(v -> v.count)
        ).apply(inst, StackedIngredient::new)
    );
    public static final PacketCodec<RegistryByteBuf, StackedIngredient> PACKET_CODEC = PacketCodec.ofStatic(
        (buffer, ingredient) ->
        {
            Ingredient.PACKET_CODEC.encode(buffer, ingredient.ingredient);
            PacketCodecs.INTEGER.encode(buffer, ingredient.count);
        },
        (buffer) ->
            new StackedIngredient(Ingredient.PACKET_CODEC.decode(buffer), PacketCodecs.INTEGER.decode(buffer))
    );
    public static final PacketCodec<RegistryByteBuf, List<StackedIngredient>> LIST_PACKET_CODEC = PACKET_CODEC.collect(PacketCodecs.toList());
    protected final Ingredient ingredient;
    protected final int count;

    protected StackedIngredient(Ingredient ingredient, int count)
    {
        this.ingredient = ingredient;
        this.count = count;
    }

    public static StackedIngredient deserialize(@Nullable JsonElement json)
    {
        if (json != null && !json.isJsonNull() && json.isJsonObject())
        {
            return new StackedIngredient(Ingredient.ALLOW_EMPTY_CODEC.decode(JsonOps.INSTANCE, json).getOrThrow().getFirst(), JsonHelper.getInt(json.getAsJsonObject(), "count"));
        }
        else
        {
            throw new JsonSyntaxException("Item cannot be null");
        }
    }

    public Ingredient getIngredient()
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
}
