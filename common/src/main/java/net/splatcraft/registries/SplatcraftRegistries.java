package net.splatcraft.registries;

import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.worldgen.SplatcraftOreGen;

public class SplatcraftRegistries
{
	public static void register()
	{
		SplatcraftBlocks.REGISTRY.register();
		SplatcraftEntities.REGISTRY.register();
		SplatcraftAttributes.REGISTRY.register();
		SplatcraftItems.REGISTRY.register();
		SplatcraftItemGroups.REGISTRY.register();
		SplatcraftTileEntities.REGISTRY.register();
		SplatcraftTileEntities.CONTAINER_REGISTRY.register();
		SplatcraftOreGen.REGISTRY.register();
		SplatcraftLoot.REGISTRY.register();
		ScoreboardHandler.REGISTRY.register();
		SplatcraftCommands.ARGUMENT_REGISTRY.register();
	}
}
