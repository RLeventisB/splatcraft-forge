package net.splatcraft;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.splatcraft.client.handlers.ClientSetupHandler;

public final class SplatcraftFabric implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		
		// Run our common setup.
		Splatcraft.init();
		
		ClientSetupHandler.bindScreenContainers((type, provider) -> MenuScreens.register(type, provider::create));
	}
}
