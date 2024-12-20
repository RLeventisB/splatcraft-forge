package net.splatcraft.registries;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.splatcraft.Splatcraft;
import net.splatcraft.loot.BlueprintLootFunction;

public class SplatcraftLoot
{
    public static final DeferredRegister<LootFunctionType<?>> REGISTRY = Splatcraft.deferredRegistryOf(Registries.LOOT_FUNCTION_TYPE);
    public static final RegistrySupplier<LootFunctionType<?>> BLUEPRINT = REGISTRY.register("blueprint_pool", () -> new LootFunctionType<>(BlueprintLootFunction.CODEC));
    public static final RegistryKey<LootTable> STORAGE_SUNKEN_CRATE, STORAGE_EGG_CRATE;

    static
    {
        STORAGE_SUNKEN_CRATE = createKey("storage/sunken_crate");
        STORAGE_EGG_CRATE = createKey("storage/egg_crate");
    }

    private static RegistryKey<LootTable> createKey(String path)
    {
//        return LootTables.registerLootTable(RegistryKey.of(RegistryKeys.LOOT_TABLE, Splatcraft.identifierOf(path)));
        return RegistryKey.of(RegistryKeys.LOOT_TABLE, Splatcraft.identifierOf(path));
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
