package net.splatcraft.forge.registries;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.client.particles.*;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SplatcraftParticleTypes
{
    public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Splatcraft.MODID);

    public static final RegistryObject<ParticleType<InkSplashParticleData>> INK_SPLASH = REGISTRY.register("ink_splash", () -> new ParticleType<>(false, InkSplashParticleData.DESERIALIZER)
    {
        @Override
        public @NotNull Codec<InkSplashParticleData> codec()
        {
            return InkSplashParticleData.CODEC;
        }
    });
    public static final RegistryObject<ParticleType<InkExplosionParticleData>> INK_EXPLOSION = REGISTRY.register("ink_explosion", () -> new ParticleType<>(false, InkExplosionParticleData.DESERIALIZER)
    {
        @Override
        public @NotNull Codec<InkExplosionParticleData> codec()
        {
            return InkExplosionParticleData.CODEC;
        }
    });
    public static final RegistryObject<ParticleType<SquidSoulParticleData>> SQUID_SOUL = REGISTRY.register("squid_soul", () -> new ParticleType<>(false, SquidSoulParticleData.DESERIALIZER)
    {
        @Override
        public @NotNull Codec<SquidSoulParticleData> codec()
        {
            return SquidSoulParticleData.CODEC;
        }
    });
    public static final RegistryObject<ParticleType<InkTerrainParticleData>> INK_TERRAIN = REGISTRY.register("ink_terrain", () -> new ParticleType<InkTerrainParticleData>(false, InkTerrainParticleData.DESERIALIZER)
    {
        @Override
        public @NotNull Codec<InkTerrainParticleData> codec()
        {
            return InkTerrainParticleData.CODEC;
        }
    });

    @SubscribeEvent
    public static void registerFactories(RegisterParticleProvidersEvent event)
    {
        event.registerSpriteSet(INK_SPLASH.get(), InkSplashParticle.Factory::new);
        event.registerSpriteSet(INK_EXPLOSION.get(), InkExplosionParticle.Factory::new);
        event.registerSpriteSet(SQUID_SOUL.get(), SquidSoulParticle.Factory::new);
        event.registerSpriteSet(INK_TERRAIN.get(), InkTerrainParticle.Factory::new);
    }
}
