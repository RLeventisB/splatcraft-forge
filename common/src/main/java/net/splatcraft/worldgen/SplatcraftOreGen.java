package net.splatcraft.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.CountConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.splatcraft.Splatcraft;
import net.splatcraft.worldgen.features.CrateFeature;
import net.splatcraft.worldgen.features.SardiniumDepositFeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SplatcraftOreGen
{
	public static final DeferredRegister<Feature<?>> REGISTRY = Splatcraft.deferredRegistryOf(BuiltInRegistries.FEATURE);
	public static final RegistrySupplier<Feature<CountConfiguration>> crate_feature = REGISTRY.register("crate", () -> new CrateFeature(CountConfiguration.CODEC));
	public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> sardinium_deposit_feature = REGISTRY.register("sardinium_deposit", () -> new SardiniumDepositFeature(NoneFeatureConfiguration.CODEC));
	private static final ArrayList<Holder<PlacedFeature>> overworldGen = new ArrayList<>();
	private static final ArrayList<Holder<PlacedFeature>> beachGen = new ArrayList<>();
	private static final ArrayList<Holder<PlacedFeature>> oceanGen = new ArrayList<>();
	//	public static void registerOres()
//	{
//		Holder<ConfiguredFeature<CountConfig, ?>> crate_small = FeatureUtils.register(Splatcraft.MODID + ":crate_small", crate_feature.get(), new CountConfig(8));
//		Holder<ConfiguredFeature<CountConfig, ?>> crate_large = ConfiguredFeatures.register(REGISTRY, RegistryKey.of(RegistryKeys.FEATURE, Splatcraft.identifierOf("crate_large")), crate_feature.get(), new CountConfig(12));
//
//		Holder<ConfiguredFeature<DefaultFeatureConfig, ?>> sardinium_deposit = FeatureUtils.register(Splatcraft.MODID + ":sardinium_deposit", sardinium_deposit_feature.get(), new DefaultFeatureConfig());
//
//		oceanGen.add(PlacedFeatures.register(Splatcraft.MODID + ":crate_small", crate_small, RarityFilter.onAverageOnceEvery(16), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome()));
//		oceanGen.add(PlacedFeatures.register(Splatcraft.MODID + ":crate_large", crate_large, RarityFilter.onAverageOnceEvery(28), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome()));
//		oceanGen.add(PlacedFeatures.register(Splatcraft.MODID + ":sardinium_deposit", sardinium_deposit, RarityFilter.onAverageOnceEvery(32), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome()));
//	}
//	@SubscribeEvent
//	public static void onBiomeLoad(BiomeLoadingEvent event)
//	{
//		BiomeGenerationSettingsBuilder generation = event.getGeneration();
//
//		switch (event.getCategory())
//		{
//			case OCEAN -> generation.getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES).addAll(oceanGen);
//			case BEACH -> generation.getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES).addAll(beachGen);
//		}
//	}
	private static List<PlacementModifier> orePlacement(PlacementModifier p_195347_, PlacementModifier p_195348_)
	{
		return Arrays.asList(p_195347_, InSquarePlacement.spread(), p_195348_, BiomeFilter.biome());
	}
	private static List<PlacementModifier> commonOrePlacement(int veinsPerChunk, PlacementModifier modifier)
	{
		return orePlacement(CountPlacement.of(veinsPerChunk), modifier);
	}
	private static List<PlacementModifier> rareOrePlacement(int chunksPerVein, PlacementModifier modifier)
	{
		return orePlacement(RarityFilter.onAverageOnceEvery(chunksPerVein), modifier);
	}
}
