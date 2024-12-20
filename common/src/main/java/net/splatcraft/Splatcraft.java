package net.splatcraft;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Mod;
import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.splatcraft.client.handlers.ClientSetupHandler;
import net.splatcraft.config.ConfigScreenProvider;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.registries.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Splatcraft
{
    public static final String MODID = "splatcraft";
    public static final String MODNAME = "Splatcraft";
    public static final Logger LOGGER = LogManager.getLogger(MODNAME);
    public static String version;
    private static Mod modInstance;

    public static void init()
    {
        // Write common init code here.
        modInstance = Platform.getMod(MODID);
        ConfigScreenProvider configProvider = new ConfigScreenProvider();
        modInstance.registerConfigurationScreen(configProvider);
        SplatcraftSections.register();
        SplatcraftConfig.initialize();

        SplatcraftPacketHandler.registerMessages();

        SplatcraftGameRules.registerGamerules();
        SplatcraftTags.register();
        SplatcraftStats.register();
        ScoreboardHandler.register();
        SplatcraftCommands.registerArguments();

//        SplatcraftOreGen.registerOres();
        SplatcraftItems.postRegister();

        LifecycleEvent.SERVER_STARTED.register(Splatcraft::onServerStart);
    }

    private static void onServerStart(MinecraftServer server)
    {
        SplatcraftGameRules.booleanRules.replaceAll((k, v) -> server.getGameRules().getBoolean(SplatcraftGameRules.getRuleFromIndex(k)));
        SplatcraftGameRules.intRules.replaceAll((k, v) -> server.getGameRules().getInt(SplatcraftGameRules.getRuleFromIndex(k)));
    }

    public static void initClient()
    {
        SplatcraftEntities.bindRenderers();
        SplatcraftTileEntities.bindTESR();

        SplatcraftItems.registerModelProperties();
        ClientSetupHandler.bindScreenContainers();

        SplatcraftSounds.register((id, sound) -> Registry.register(Registries.SOUND_EVENT, id, sound));
    }

    public static void initServer()
    {
    }

    public static <T> DeferredRegister<T> deferredRegistryOf(Registry<T> registry)
    {
        return DeferredRegister.create(MODID, (RegistryKey<Registry<T>>) registry.getKey());
    }

    public static Mod getModInstance()
    {
        return modInstance;
    }

    public static Identifier identifierOf(String path)
    {
        return Identifier.of(MODID, path);
    }
}
