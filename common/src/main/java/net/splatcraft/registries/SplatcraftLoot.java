package net.splatcraft.registries;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.splatcraft.Splatcraft;
import net.splatcraft.loot.BlueprintLootFunction;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.RegistrySupplier;

public class SplatcraftLoot
{
	public static final DeferredRegister<LootItemFunctionType<?>> REGISTRY = Splatcraft.deferredRegistryOf(BuiltInRegistries.LOOT_FUNCTION_TYPE);
	public static final RegistrySupplier<LootItemFunctionType<?>> BLUEPRINT = REGISTRY.register("blueprint_pool", () -> new LootItemFunctionType<>(BlueprintLootFunction.CODEC));
	public static final ResourceKey<LootTable> STORAGE_SUNKEN_CRATE, STORAGE_EGG_CRATE;
	static
	{
		STORAGE_SUNKEN_CRATE = createKey("storage/sunken_crate");
		STORAGE_EGG_CRATE = createKey("storage/egg_crate");
	}
	private static ResourceKey<LootTable> createKey(String path)
	{
//        return LootTables.registerLootTable(RegistryKey.of(RegistryKeys.LOOT_TABLE, Splatcraft.identifierOf(path)));
		return ResourceKey.create(Registries.LOOT_TABLE, Splatcraft.identifierOf(path));
	}
//    public static final RegistrySupplier<LootFunctionType<? extends LootFunction>> FISHING = REGISTRY.register("fishing", () -> new LootFunctionType<>(FishingLootModifier.CODEC));
//    public static final RegistrySupplier<LootFunctionType<? extends LootFunction>> CHEST_LOOT = REGISTRY.register("chest_loot", () -> new LootFunctionType<>(ChestLootModifier.CODEC));
	
	//	@SubscribeEvent
//	public static void registerGLM(NewRegistryEvent event)
//	{
//		IForgeRegistry<Codec<? extends IGlobalLootModifier>> registry = Registries.GLOBAL_LOOT_MODIFIER_SERIALIZERS.get();
//
//		registry.register(Splatcraft.identifierOf("fishing"), new FishingLootModifier.Serializer());
//		registry.register(new ChestLootModifier.Serializer().setRegistryName(Splatcraft.identifierOf("chest_loot")));
//	}
}
