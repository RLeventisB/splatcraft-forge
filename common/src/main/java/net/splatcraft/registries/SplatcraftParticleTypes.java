package net.splatcraft.registries;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.particles.*;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.Services;
import org.jetbrains.annotations.NotNull;

public class SplatcraftParticleTypes
{
	public static final DeferredRegister<ParticleType<?>> REGISTRY = Splatcraft.deferredRegistryOf(BuiltInRegistries.PARTICLE_TYPE);
	public static final ParticleType<InkCloudParticleData> INK_CLOUD = new ParticleType<>(false)
	{
		@Override
		public @NotNull MapCodec<InkCloudParticleData> codec()
		{
			return InkCloudParticleData.CODEC;
		}
		@Override
		public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, InkCloudParticleData> streamCodec()
		{
			return InkCloudParticleData.STREAM_CODEC;
		}
	};
	public static final ParticleType<InkSplashParticleData> INK_SPLASH = new ParticleType<>(false)
	{
		@Override
		public @NotNull MapCodec<InkSplashParticleData> codec()
		{
			return InkSplashParticleData.CODEC;
		}
		@Override
		public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, InkSplashParticleData> streamCodec()
		{
			return InkSplashParticleData.STREAM_CODEC;
		}
	};
	public static final ParticleType<InkExplosionParticleData> INK_EXPLOSION = new ParticleType<>(false)
	{
		@Override
		public @NotNull MapCodec<InkExplosionParticleData> codec()
		{
			return InkExplosionParticleData.CODEC;
		}
		@Override
		public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, InkExplosionParticleData> streamCodec()
		{
			return InkExplosionParticleData.STREAM_CODEC;
		}
	};
	public static final ParticleType<SquidSoulParticleData> SQUID_SOUL = new ParticleType<>(false)
	{
		@Override
		public @NotNull MapCodec<SquidSoulParticleData> codec()
		{
			return SquidSoulParticleData.CODEC;
		}
		@Override
		public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, SquidSoulParticleData> streamCodec()
		{
			return SquidSoulParticleData.STREAM_CODEC;
		}
	};
	public static final ParticleType<InkTerrainParticleData> INK_TERRAIN = new ParticleType<>(false)
	{
		@Override
		public @NotNull MapCodec<InkTerrainParticleData> codec()
		{
			return InkTerrainParticleData.CODEC;
		}
		@Override
		public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, InkTerrainParticleData> streamCodec()
		{
			return InkTerrainParticleData.STREAM_CODEC;
		}
	};
	public static final ParticleType<InkHitParticleData> INK_HIT = new ParticleType<>(false)
	{
		@Override
		public @NotNull MapCodec<InkHitParticleData> codec()
		{
			return InkHitParticleData.CODEC;
		}
		@Override
		public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, InkHitParticleData> streamCodec()
		{
			return InkHitParticleData.STREAM_CODEC;
		}
	};
	public static void registerParticles()
	{
		REGISTRY.register("ink_cloud", () -> INK_CLOUD);
		REGISTRY.register("ink_splash", () -> INK_SPLASH);
		REGISTRY.register("ink_explosion", () -> INK_EXPLOSION);
		REGISTRY.register("squid_soul", () -> SQUID_SOUL);
		REGISTRY.register("ink_terrain", () -> INK_TERRAIN);
		REGISTRY.register("ink_hit", () -> INK_HIT);
		
		Services.PLATFORM.registerParticleFactories(INK_CLOUD, InkCloudParticle.Factory::new);
		Services.PLATFORM.registerParticleFactories(INK_SPLASH, InkSplashParticle.Factory::new);
		Services.PLATFORM.registerParticleFactories(INK_EXPLOSION, InkExplosionParticle.Factory::new);
		Services.PLATFORM.registerParticleFactories(SQUID_SOUL, SquidSoulParticle.Factory::new);
		Services.PLATFORM.registerParticleFactories(INK_TERRAIN, InkTerrainParticle.Factory::new);
		Services.PLATFORM.registerParticleFactories(INK_HIT, InkHitParticle.Factory::new);
	}
}
