package net.splatcraft.forge.registries;

import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class SplatcraftRegisties
{
	public static final void register()
	{
		SplatcraftBlocks.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
		SplatcraftEntities.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
		SplatcraftItems.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
		SplatcraftTileEntities.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
		SplatcraftTileEntities.CONTAINER_REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
	}
}