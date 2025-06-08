package net.splatcraft.platform;

import net.minecraft.core.Registry;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.splatcraft.SplatcraftNeoForge;
import net.splatcraft.crafting.SplatcraftRecipeTypes;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.registries.*;
import net.splatcraft.worldgen.SplatcraftOreGen;

import java.util.function.Supplier;

public class NeoForgeDeferredRegister<T> implements DeferredRegister<T>
{
	private final net.neoforged.neoforge.registries.DeferredRegister<T> registry;
	protected NeoForgeDeferredRegister(Registry<T> originalRegistry, String namespace)
	{
		registry = net.neoforged.neoforge.registries.DeferredRegister.create(originalRegistry, namespace);
	}
	public static void registerAllRegistries()
	{
		NeoForgePlatformHelper.ARGUMENT_REGISTRY.registerEntries();
		NeoForgePlatformHelper.DATA_SERIALIZER_REGISTRY.registerEntries();

		SplatcraftItems.REGISTRY.registerEntries();
		SplatcraftRecipeTypes.RECIPE_SERIALIZER_REGISTRY.registerEntries();
		SplatcraftTileEntities.CONTAINER_REGISTRY.registerEntries();
		SplatcraftBlocks.REGISTRY.registerEntries();
		SplatcraftRecipeTypes.RECIPE_TYPE_REGISTRY.registerEntries();
		SplatcraftItems.ARMOR_MATERIAL_REGISTRY.registerEntries();
		SplatcraftStats.STAT_REGISTRY.registerEntries();
		SplatcraftItemGroups.REGISTRY.registerEntries();
		SplatcraftSounds.REGISTRY.registerEntries();
		SplatcraftStats.CRITERION_REGISTRY.registerEntries();
		SplatcraftTileEntities.REGISTRY.registerEntries();
		ScoreboardHandler.REGISTRY.registerEntries();
		SplatcraftParticleTypes.REGISTRY.registerEntries();
		SplatcraftEntities.REGISTRY.registerEntries();
		SplatcraftOreGen.REGISTRY.registerEntries();
		SplatcraftAttributes.REGISTRY.registerEntries();
		SplatcraftLoot.REGISTRY.registerEntries();
	}
	@Override
	public <R extends T> RegistrySupplier<R> register(String path, Supplier<R> supplier)
	{
		DeferredHolder<T, R> register = registry.register(path, supplier);
		return (RegistrySupplier<R>) new RegistrySupplier<>(register);
	}

	@Override
	public void registerEntries()
	{
		registry.register(SplatcraftNeoForge.modBus);
	}
}
