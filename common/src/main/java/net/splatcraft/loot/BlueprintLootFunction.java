package net.splatcraft.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.util.Identifier;
import net.splatcraft.items.BlueprintItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record BlueprintLootFunction(List<Identifier> advancementIds, String weaponType) implements LootFunction
{

    public static final MapCodec<BlueprintLootFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Identifier.CODEC.listOf().fieldOf("advancementIds").forGetter(BlueprintLootFunction::advancementIds),
            Codec.STRING.fieldOf("weaponType").forGetter(BlueprintLootFunction::weaponType)
        ).apply(instance, BlueprintLootFunction::new)
    );

    @Override
    public @NotNull LootFunctionType<? extends BlueprintLootFunction> getType()
    {
        return new LootFunctionType<>(CODEC);
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext lootContext)
    {
        BlueprintItem.setPoolFromWeaponType(stack, weaponType);

        return BlueprintItem.addToAdvancementPool(stack, advancementIds.stream());
    }
}
