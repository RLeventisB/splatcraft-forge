package net.splatcraft.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.splatcraft.crafting.SplatcraftRecipeTypes;
import net.splatcraft.worldgen.SplatcraftOreGen;

public class SplatcraftRegistries
{
    public static void register()
    {
        SplatcraftBlocks.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
        SplatcraftEntities.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
        SplatcraftAttributes.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
        SplatcraftItems.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
        SplatcraftItemGroups.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
        SplatcraftTileEntities.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
        SplatcraftTileEntities.CONTAINER_REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
        SplatcraftOreGen.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
        SplatcraftLoot.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
        SplatcraftParticleTypes.registerParticles();
        SplatcraftCommands.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());

        SplatcraftRecipeTypes.register();
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent forgeEvent)
    {
        if (forgeEvent.getRegistryKey().location().equals(ForgeRegistries.SOUND_EVENTS.getRegistryKey().location())) // uhhhhhhh
        {
            ResourceKey<? extends Registry<SoundEvent>> registryKey = (ResourceKey<? extends Registry<SoundEvent>>) forgeEvent.getRegistryKey();

            SplatcraftSounds.register((loc, v) -> forgeEvent.register(registryKey, loc, () -> v));
        }
    }
}
