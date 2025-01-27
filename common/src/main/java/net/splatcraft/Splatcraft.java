package net.splatcraft;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Mod;
import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.DeferredRegister;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.splatcraft.client.handlers.JumpLureHudHandler;
import net.splatcraft.client.handlers.PlayerMovementHandler;
import net.splatcraft.client.handlers.RendererHandler;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.config.ConfigScreenProvider;
import net.splatcraft.crafting.SplatcraftRecipeTypes;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.handlers.*;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.registries.*;
import net.splatcraft.util.action.EntityAction;
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
		SplatcraftConfig.initialize();
		
		DataHandler.addReloadListeners();
		SplatcraftCommands.registerCommands();
		SplatcraftTags.register();
		ScoreboardHandler.register();
		SplatcraftCommands.registerArguments();
		SplatcraftGameRules.registerGamerules();
		SplatcraftPacketHandler.registerMessages();
		SplatcraftParticleTypes.registerParticles();
		SplatcraftRecipeTypes.register();
		SplatcraftEntities.registerDataTrackers();
		SplatcraftEntities.registerAttributes();
		WeaponHandler.registerEvents();
		ChunkInkHandler.registerEvents();
		ShootingHandler.registerEvents();
		SplatcraftCommonHandler.registerEvents();
		SquidFormHandler.registerEvents();
		PlayerMovementHandler.registerEvents();
		EntityAction.registerActions();
//		SplatcraftOreGen.registerOres();
		SplatcraftItemGroups.addSplatcraftItemsToVanillaGroups();
		
		if (Platform.getEnv().equals(EnvType.CLIENT))
			initClient();
		
		LifecycleEvent.SERVER_STARTED.register(Splatcraft::onServerStart);
	}
	public static void onServerStart(MinecraftServer server)
	{
		SplatcraftGameRules.booleanRules.replaceAll((k, v) -> server.getGameRules().getBoolean(SplatcraftGameRules.getRuleFromIndex(k)));
		SplatcraftGameRules.intRules.replaceAll((k, v) -> server.getGameRules().getInt(SplatcraftGameRules.getRuleFromIndex(k)));
		
		SplatcraftItems.postRegister();
	}
	@Environment(EnvType.CLIENT)
	public static void initClient()
	{
		JumpLureHudHandler.registerEvents();
		RendererHandler.registerEvents();
		SplatcraftEntities.bindRenderers();
		SplatcraftEntities.defineModelLayers();
		
		ClientLifecycleEvent.CLIENT_SETUP.register(Splatcraft::initClientAfter);
	}
	@Environment(EnvType.CLIENT)
	public static void initClientAfter(MinecraftClient client)
	{
		ConfigScreenProvider configProvider = new ConfigScreenProvider();
		modInstance.registerConfigurationScreen(configProvider);
		
		SplatcraftTileEntities.bindTESR();
		SplatcraftKeyHandler.registerBindingsAndEvents();
		SplatcraftItems.registerModelProperties();
	}
	public static <T> DeferredRegister<T> deferredRegistryOf(Registry<T> registry)
	{
		return DeferredRegister.create(MODID, (RegistryKey<Registry<T>>) registry.getKey());
	}
	public static <T> DeferredRegister<T> deferredRegistryOf(RegistryKey<Registry<T>> registry)
	{
		return DeferredRegister.create(MODID, registry);
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
