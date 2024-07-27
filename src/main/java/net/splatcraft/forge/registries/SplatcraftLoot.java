package net.splatcraft.forge.registries;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.loot.BlueprintLootFunction;
import net.splatcraft.forge.loot.ChestLootModifier;
import net.splatcraft.forge.loot.FishingLootModifier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = Splatcraft.MODID)
public class SplatcraftLoot
{
	public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> REGISTRY = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Splatcraft.MODID);
	static
	{
		REGISTRY.register("fishing", FishingLootModifier.CODEC);
		REGISTRY.register("chest_loot", ChestLootModifier.CODEC);
	}
	public static final DeferredRegister<LootItemFunctionType> REGISTRY2 = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, Splatcraft.MODID);
	public static final RegistryObject<LootItemFunctionType> BLUEPRINT = REGISTRY2.register("blueprint_pool", () -> new LootItemFunctionType(new BlueprintLootFunction.Serializer()));
//	@SubscribeEvent
//	public static void registerGLM(NewRegistryEvent event)
//	{
//		IForgeRegistry<Codec<? extends IGlobalLootModifier>> registry = ForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS.get();
//
//		registry.register(new ResourceLocation(Splatcraft.MODID, "fishing"), new FishingLootModifier.Serializer());
//		registry.register(new ChestLootModifier.Serializer().setRegistryName(new ResourceLocation(Splatcraft.MODID, "chest_loot")));
//	}
}
