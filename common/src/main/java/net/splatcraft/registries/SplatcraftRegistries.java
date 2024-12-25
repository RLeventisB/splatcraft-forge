package net.splatcraft.registries;

import net.splatcraft.client.handlers.ClientSetupHandler;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.worldgen.SplatcraftOreGen;

public class SplatcraftRegistries
{
	public static void register()
	{
		SplatcraftSounds.register();
		SplatcraftBlocks.REGISTRY.register();
		SplatcraftEntities.REGISTRY.register();
		SplatcraftAttributes.REGISTRY.register();
		SplatcraftItems.REGISTRY.register();
		SplatcraftItems.ARMOR_MATERIAL_REGISTRY.register();
		SplatcraftItemGroups.REGISTRY.register();
		SplatcraftTileEntities.REGISTRY.register();
		SplatcraftTileEntities.CONTAINER_REGISTRY.register();
		SplatcraftOreGen.REGISTRY.register();
		SplatcraftLoot.REGISTRY.register();
		ScoreboardHandler.REGISTRY.register();
		SplatcraftCommands.ARGUMENT_REGISTRY.register();
		SplatcraftStats.CRITERION_REGISTRY.register();
		SplatcraftStats.STAT_REGISTRY.register();
		SplatcraftParticleTypes.REGISTRY.register();
	}
	public static void afterRegister()
	{
		SplatcraftTileEntities.bindTESR();
		SplatcraftItems.registerModelProperties();
		ClientSetupHandler.bindScreenContainers();
	}
}
