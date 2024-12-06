package net.splatcraft.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.splatcraft.Splatcraft;
import net.splatcraft.entities.InkSquidEntity;
import net.splatcraft.registries.SplatcraftEntities;

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
		FabricDefaultAttributeRegistry.register(SplatcraftEntities.INK_SQUID, InkSquidEntity.setCustomAttributes());
	}
}
