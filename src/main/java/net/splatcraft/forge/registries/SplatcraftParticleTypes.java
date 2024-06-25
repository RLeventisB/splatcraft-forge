package net.splatcraft.forge.registries;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.splatcraft.forge.client.particles.*;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SplatcraftParticleTypes
{

    public static final ParticleType<InkSplashParticleData> INK_SPLASH = new ParticleType<>(false, InkSplashParticleData.DESERIALIZER) {
        @Override
        public @NotNull Codec<InkSplashParticleData> codec() {
            return InkSplashParticleData.CODEC;
        }
    };
    public static final ParticleType<InkExplosionParticleData> INK_EXPLOSION = new ParticleType<InkExplosionParticleData>(false, InkExplosionParticleData.DESERIALIZER)
    {
        @Override
        public @NotNull Codec<InkExplosionParticleData> codec()
        {
            return InkExplosionParticleData.CODEC;
        }
    };
    public static final ParticleType<SquidSoulParticleData> SQUID_SOUL = new ParticleType<SquidSoulParticleData>(false, SquidSoulParticleData.DESERIALIZER)
    {
        @Override
        public @NotNull Codec<SquidSoulParticleData> codec()
        {
            return SquidSoulParticleData.CODEC;
        }
    };
    public static final ParticleType<InkTerrainParticleData> INK_TERRAIN = new ParticleType<InkTerrainParticleData>(false, InkTerrainParticleData.DESERIALIZER)
    {
        @Override
        public @NotNull Codec<InkTerrainParticleData> codec()
        {
            return InkTerrainParticleData.CODEC;
        }
    };

    @SubscribeEvent
    public static void registerFactories(ParticleFactoryRegisterEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        mc.particleEngine.register(INK_SPLASH, InkSplashParticle.Factory::new);
        mc.particleEngine.register(INK_EXPLOSION, InkExplosionParticle.Factory::new);
        mc.particleEngine.register(SQUID_SOUL, SquidSoulParticle.Factory::new);
        mc.particleEngine.register(INK_TERRAIN, InkTerrainParticle.Factory::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegistryEvent.Register<ParticleType<?>> event)
    {
        IForgeRegistry<ParticleType<?>> registry = event.getRegistry();

        registry.register(INK_SPLASH.setRegistryName("ink_splash"));
        registry.register(INK_EXPLOSION.setRegistryName("ink_explosion"));
        registry.register(SQUID_SOUL.setRegistryName("squid_soul"));
        registry.register(INK_TERRAIN.setRegistryName("ink_terrain"));
    }
}
