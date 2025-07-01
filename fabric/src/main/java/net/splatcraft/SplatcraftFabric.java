package net.splatcraft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.splatcraft.client.handlers.ClientSetupHandler;
import net.splatcraft.client.particles.InkExplosionParticle;
import net.splatcraft.client.particles.InkSplashParticle;
import net.splatcraft.client.particles.InkTerrainParticle;
import net.splatcraft.client.particles.SquidSoulParticle;
import net.splatcraft.registries.SplatcraftParticleTypes;

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
		
		ParticleFactoryRegistry.getInstance().register(SplatcraftParticleTypes.INK_SPLASH, InkSplashParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(SplatcraftParticleTypes.INK_EXPLOSION, InkExplosionParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(SplatcraftParticleTypes.SQUID_SOUL, SquidSoulParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(SplatcraftParticleTypes.INK_TERRAIN, InkTerrainParticle.Factory::new);
	}
}
