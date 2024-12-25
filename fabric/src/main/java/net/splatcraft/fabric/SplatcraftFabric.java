package net.splatcraft.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.particles.InkExplosionParticle;
import net.splatcraft.client.particles.InkSplashParticle;
import net.splatcraft.client.particles.InkTerrainParticle;
import net.splatcraft.client.particles.SquidSoulParticle;
import net.splatcraft.data.capabilities.worldink.ChunkInkCapability;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.entities.InkSquidEntity;
import net.splatcraft.entities.SquidBumperEntity;
import net.splatcraft.registries.SplatcraftEntities;
import net.splatcraft.registries.SplatcraftParticleTypes;
import net.splatcraft.registries.SplatcraftRegistries;

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
		SplatcraftRegistries.register();
		SplatcraftRegistries.afterRegister();
		
		FabricDefaultAttributeRegistry.register(SplatcraftEntities.INK_SQUID.get(), InkSquidEntity.setCustomAttributes());
		FabricDefaultAttributeRegistry.register(SplatcraftEntities.SQUID_BUMPER.get(), SquidBumperEntity.setCustomAttributes());
		InkProjectileEntity.registerDataAccessors();
		
		ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> ChunkInkCapability.unloadChunkData(world, chunk.getPos()));
		
		ParticleFactoryRegistry.getInstance().register(SplatcraftParticleTypes.INK_SPLASH, InkSplashParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(SplatcraftParticleTypes.INK_EXPLOSION, InkExplosionParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(SplatcraftParticleTypes.SQUID_SOUL, SquidSoulParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(SplatcraftParticleTypes.INK_TERRAIN, InkTerrainParticle.Factory::new);
	}
}
