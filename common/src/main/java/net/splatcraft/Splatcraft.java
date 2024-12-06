package net.splatcraft;

import dev.architectury.injectables.annotations.PlatformOnly;
import dev.architectury.platform.Mod;
import dev.architectury.platform.Platform;
import net.minecraft.util.Identifier;
import net.splatcraft.config.ConfigScreenProvider;
import net.splatcraft.registries.SplatcraftInitializer;
import net.splatcraft.registries.SplatcraftRegistries;

public final class Splatcraft
{
    public static final String MODID = "splatcraft";
    private static Mod modInstance;
    private static SplatcraftInitializer initializer;

    public static void init(SplatcraftInitializer initializer)
    {
        Splatcraft.initializer = initializer;
        // Write common init code here.
        modInstance = Platform.getMod(MODID);
        ConfigScreenProvider configProvider = new ConfigScreenProvider();
        modInstance.registerConfigurationScreen(configProvider);
        SplatcraftRegistries.register();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    @PlatformOnly(PlatformOnly.FABRIC)
    public static void initClient()
    {
    }

    public static void initServer()
    {
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
