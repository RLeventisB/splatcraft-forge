package net.splatcraft.registries;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.particle.ParticleType;
import net.splatcraft.client.particles.*;

public class SplatcraftParticleTypes
{
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
		ParticleProviderRegistry.register(INK_SPLASH, InkSplashParticle.Factory::new);
		ParticleProviderRegistry.register(INK_EXPLOSION, InkExplosionParticle.Factory::new);
		ParticleProviderRegistry.register(SQUID_SOUL, SquidSoulParticle.Factory::new);
		ParticleProviderRegistry.register(INK_TERRAIN, InkTerrainParticle.Factory::new);
	}
	@Environment(EnvType.CLIENT)
	public static void registerClientFactories()
	{
	}
}
