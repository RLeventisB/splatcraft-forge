package net.splatcraft.worldgen;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.CountConfig;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.*;
import net.splatcraft.Splatcraft;
import net.splatcraft.worldgen.features.CrateFeature;
import net.splatcraft.worldgen.features.SardiniumDepositFeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SplatcraftOreGen
{
    public static final DeferredRegister<Feature<?>> REGISTRY = Splatcraft.deferredRegistryOf(Registries.FEATURE);
    public static final RegistrySupplier<Feature<CountConfig>> crate_feature = REGISTRY.register("crate", () -> new CrateFeature(CountConfig.CODEC));
    public static final RegistrySupplier<Feature<DefaultFeatureConfig>> sardinium_deposit_feature = REGISTRY.register("sardinium_deposit", () -> new SardiniumDepositFeature(DefaultFeatureConfig.CODEC));
    private static final ArrayList<RegistryEntry<PlacedFeature>> overworldGen = new ArrayList<>();
    private static final ArrayList<RegistryEntry<PlacedFeature>> beachGen = new ArrayList<>();
    private static final ArrayList<RegistryEntry<PlacedFeature>> oceanGen = new ArrayList<>();

    /*public static void registerOres()
    {
        Holder<ConfiguredFeature<CountConfig, ?>> crate_small = FeatureUtils.register(Splatcraft.MODID + ":crate_small", crate_feature.get(), new CountConfig(8));
        Holder<ConfiguredFeature<CountConfig, ?>> crate_large = FeatureUtils.register(Splatcraft.MODID + ":crate_large", crate_feature.get(), new CountConfig(12));

        Holder<ConfiguredFeature<DefaultFeatureConfig, ?>> sardinium_deposit = FeatureUtils.register(Splatcraft.MODID + ":sardinium_deposit", sardinium_deposit_feature.get(), new DefaultFeatureConfig());

        oceanGen.add(PlacementUtils.register(Splatcraft.MODID + ":crate_small", crate_small, RarityFilter.onAverageOnceEvery(16), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome()));
        oceanGen.add(PlacementUtils.register(Splatcraft.MODID + ":crate_large", crate_large, RarityFilter.onAverageOnceEvery(28), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome()));
        oceanGen.add(PlacementUtils.register(Splatcraft.MODID + ":sardinium_deposit", sardinium_deposit, RarityFilter.onAverageOnceEvery(32), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome()));
    }

    @SubscribeEvent
    public static void onBiomeLoad(BiomeLoadingEvent event)
    {
        BiomeGenerationSettingsBuilder generation = event.getGeneration();

        switch (event.getCategory())
        {
            case OCEAN -> generation.getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES).addAll(oceanGen);
            case BEACH -> generation.getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES).addAll(beachGen);
        }
    }*/
    private static List<PlacementModifier> orePlacement(PlacementModifier p_195347_, PlacementModifier p_195348_)
    {
        return Arrays.asList(p_195347_, SquarePlacementModifier.of(), p_195348_, BiomePlacementModifier.of());
    }

    private static List<PlacementModifier> commonOrePlacement(int veinsPerChunk, PlacementModifier modifier)
    {
        return orePlacement(CountPlacementModifier.of(veinsPerChunk), modifier);
    }

    private static List<PlacementModifier> rareOrePlacement(int chunksPerVein, PlacementModifier modifier)
    {
        return orePlacement(RarityFilterPlacementModifier.of(chunksPerVein), modifier);
    }
}
