package net.splatcraft.registries;

import net.splatcraft.crafting.SplatcraftRecipeTypes;
import net.splatcraft.worldgen.SplatcraftOreGen;

public class SplatcraftSections
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
        SplatcraftParticleTypes.registerParticles();
        SplatcraftCommands.registerCommands();
        SplatcraftCommands.registerArguments();

        SplatcraftRecipeTypes.register();
    }
}
