package net.splatcraft.forge.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.splatcraft.forge.items.BlueprintItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BlueprintLootFunction implements LootItemFunction
{
	final List<String> advancementIds;
	final String weaponType;
	//	public static final Supplier<Codec<ChestLootModifier>> CODEC = Suppliers.memoize(() -> RecordCodecBuilder.create(inst -> codecStart(inst).and(
//		inst.group(
//			Codec.STRING.listOf().fieldOf("advancementIds").forGetter(v -> v.countMin),
//			Codec.STRING.fieldOf("weaponType").forGetter(v -> v.weaponType),
//		)).apply(inst, ChestLootModifier::new)
//	));
	public BlueprintLootFunction(String weaponType, List<String> advancements)
	{
		this.weaponType = weaponType;
		advancementIds = advancements;
	}
	@Override
	public @NotNull LootItemFunctionType getType()
	{
		return null;
	}
	@Override
	public ItemStack apply(ItemStack stack, LootContext lootContext)
	{
		BlueprintItem.setPoolFromWeaponType(stack, weaponType);
		
		return BlueprintItem.addToAdvancementPool(stack, this.advancementIds.stream());
	}
	public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<BlueprintLootFunction>
	{
		@Override
		public void serialize(JsonObject json, BlueprintLootFunction lootFunction, @NotNull JsonSerializationContext context)
		{
			JsonArray array = new JsonArray();
			
			lootFunction.advancementIds.forEach(array::add);
			
			json.add("advancements", array);
			if (!lootFunction.weaponType.isEmpty())
				json.addProperty("weapon_pool", lootFunction.weaponType);
		}
		@Override
		public @NotNull BlueprintLootFunction deserialize(@NotNull JsonObject json, @NotNull JsonDeserializationContext context)
		{
			List<String> advancements = new ArrayList<>();
			
			JsonArray array = GsonHelper.getAsJsonArray(json, "advancements", new JsonArray());
			array.forEach(element -> advancements.add(element.getAsString()));
			
			return new BlueprintLootFunction(GsonHelper.getAsString(json, "weapon_pool", ""), advancements);
		}
	}
}
