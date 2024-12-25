package net.splatcraft.registries;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.client.particles.InkTerrainParticleData;
import net.splatcraft.client.particles.SquidSoulParticleData;

public class SplatcraftParticleTypes
{
	public static final DeferredRegister<ParticleType<?>> REGISTRY = Splatcraft.deferredRegistryOf(Registries.PARTICLE_TYPE);
	public static final ParticleType<InkSplashParticleData> INK_SPLASH = new ParticleType<>(false)
	{
		@Override
		public MapCodec<InkSplashParticleData> getCodec()
		{
			return InkSplashParticleData.CODEC;
		}
		@Override
		public PacketCodec<? super RegistryByteBuf, InkSplashParticleData> getPacketCodec()
		{
			return InkSplashParticleData.PACKET_CODEC;
		}
	};
	public static final ParticleType<InkExplosionParticleData> INK_EXPLOSION = new ParticleType<>(false)
	{
		@Override
		public MapCodec<InkExplosionParticleData> getCodec()
		{
			return InkExplosionParticleData.CODEC;
		}
		@Override
		public PacketCodec<? super RegistryByteBuf, InkExplosionParticleData> getPacketCodec()
		{
			return InkExplosionParticleData.PACKET_CODEC;
		}
	};
	public static final ParticleType<SquidSoulParticleData> SQUID_SOUL = new ParticleType<>(false)
	{
		@Override
		public MapCodec<SquidSoulParticleData> getCodec()
		{
			return SquidSoulParticleData.CODEC;
		}
		@Override
		public PacketCodec<? super RegistryByteBuf, SquidSoulParticleData> getPacketCodec()
		{
			return SquidSoulParticleData.PACKET_CODEC;
		}
	};
	public static final ParticleType<InkTerrainParticleData> INK_TERRAIN = new ParticleType<>(false)
	{
		@Override
		public MapCodec<InkTerrainParticleData> getCodec()
		{
			return InkTerrainParticleData.CODEC;
		}
		@Override
		public PacketCodec<? super RegistryByteBuf, InkTerrainParticleData> getPacketCodec()
		{
			return InkTerrainParticleData.PACKET_CODEC;
		}
	};
	public static void registerParticles()
	{
		REGISTRY.register(Splatcraft.identifierOf("ink_splash"), () -> INK_SPLASH);
		REGISTRY.register(Splatcraft.identifierOf("ink_explosion"), () -> INK_EXPLOSION);
		REGISTRY.register(Splatcraft.identifierOf("squid_soul"), () -> SQUID_SOUL);
		REGISTRY.register(Splatcraft.identifierOf("ink_terrain"), () -> INK_TERRAIN);
		
		// uhhh architectury commented the code of these, why?????????????
/*
		ParticleProviderRegistry.register(INK_SPLASH, InkSplashParticle.Factory::new);
		ParticleProviderRegistry.register(INK_EXPLOSION, InkExplosionParticle.Factory::new);
		ParticleProviderRegistry.register(SQUID_SOUL, SquidSoulParticle.Factory::new);
		ParticleProviderRegistry.register(INK_TERRAIN, InkTerrainParticle.Factory::new);
*/
	}
}
