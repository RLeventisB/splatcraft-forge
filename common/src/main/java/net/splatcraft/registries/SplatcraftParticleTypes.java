package net.splatcraft.registries;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.client.particles.InkTerrainParticleData;
import net.splatcraft.client.particles.SquidSoulParticleData;
import net.splatcraft.platform.DeferredRegister;

public class SplatcraftParticleTypes
{
	public static final DeferredRegister<ParticleType<?>> REGISTRY = Splatcraft.deferredRegistryOf(BuiltInRegistries.PARTICLE_TYPE);
	public static final ParticleType<InkSplashParticleData> INK_SPLASH = new ParticleType<>(false)
	{
		@Override
		public MapCodec<InkSplashParticleData> codec()
		{
			return InkSplashParticleData.CODEC;
		}
		@Override
		public StreamCodec<? super RegistryFriendlyByteBuf, InkSplashParticleData> streamCodec()
		{
			return InkSplashParticleData.PACKET_CODEC;
		}
	};
	public static final ParticleType<InkExplosionParticleData> INK_EXPLOSION = new ParticleType<>(false)
	{
		@Override
		public MapCodec<InkExplosionParticleData> codec()
		{
			return InkExplosionParticleData.CODEC;
		}
		@Override
		public StreamCodec<? super RegistryFriendlyByteBuf, InkExplosionParticleData> streamCodec()
		{
			return InkExplosionParticleData.PACKET_CODEC;
		}
	};
	public static final ParticleType<SquidSoulParticleData> SQUID_SOUL = new ParticleType<>(false)
	{
		@Override
		public MapCodec<SquidSoulParticleData> codec()
		{
			return SquidSoulParticleData.CODEC;
		}
		@Override
		public StreamCodec<? super RegistryFriendlyByteBuf, SquidSoulParticleData> streamCodec()
		{
			return SquidSoulParticleData.PACKET_CODEC;
		}
	};
	public static final ParticleType<InkTerrainParticleData> INK_TERRAIN = new ParticleType<>(false)
	{
		@Override
		public MapCodec<InkTerrainParticleData> codec()
		{
			return InkTerrainParticleData.CODEC;
		}
		@Override
		public StreamCodec<? super RegistryFriendlyByteBuf, InkTerrainParticleData> streamCodec()
		{
			return InkTerrainParticleData.PACKET_CODEC;
		}
	};
	public static void registerParticles()
	{
		REGISTRY.register("ink_splash", () -> INK_SPLASH);
		REGISTRY.register("ink_explosion", () -> INK_EXPLOSION);
		REGISTRY.register("squid_soul", () -> SQUID_SOUL);
		REGISTRY.register("ink_terrain", () -> INK_TERRAIN);
		
		// uhhh architectury commented the code of these, why?????????????
/*
		ParticleProviderRegistry.register(INK_SPLASH, InkSplashParticle.Factory::new);
		ParticleProviderRegistry.register(INK_EXPLOSION, InkExplosionParticle.Factory::new);
		ParticleProviderRegistry.register(SQUID_SOUL, SquidSoulParticle.Factory::new);
		ParticleProviderRegistry.register(INK_TERRAIN, InkTerrainParticle.Factory::new);
*/
	}
}
