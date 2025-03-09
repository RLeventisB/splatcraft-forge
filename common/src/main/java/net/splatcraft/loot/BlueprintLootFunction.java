package net.splatcraft.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.splatcraft.items.BlueprintItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record BlueprintLootFunction(List<ResourceLocation> advancementIds, String weaponType) implements LootItemFunction
{
	public static final MapCodec<BlueprintLootFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
		instance.group(
			ResourceLocation.CODEC.listOf().optionalFieldOf("advancements", List.of()).forGetter(BlueprintLootFunction::advancementIds),
			Codec.STRING.optionalFieldOf("weapon_pool", "").forGetter(BlueprintLootFunction::weaponType)
		).apply(instance, BlueprintLootFunction::new)
	);
	@Override
	public @NotNull LootItemFunctionType<? extends BlueprintLootFunction> getType()
	{
		return new LootItemFunctionType<>(CODEC);
	}
	@Override
	public ItemStack apply(ItemStack stack, LootContext lootContext)
	{
		BlueprintItem.setPoolFromWeaponType(stack, weaponType);
		
		return BlueprintItem.addToAdvancementPool(stack, advancementIds.stream());
	}
}
