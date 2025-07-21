package net.splatcraft;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.splatcraft.client.handlers.JumpLureHudHandler;
import net.splatcraft.client.handlers.PlayerMovementHandler;
import net.splatcraft.client.handlers.RendererHandler;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.crafting.SplatcraftRecipeTypes;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.handlers.*;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.Services;
import net.splatcraft.platform.event.LifecycleEvents;
import net.splatcraft.registries.*;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.structs.DamageCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Splatcraft
{
	public static final String MODID = "splatcraft";
	public static final String MODNAME = "Splatcraft";
	public static final Logger LOGGER = LogManager.getLogger(MODNAME);
	public static String version;
	public static void initClient()
	{
		JumpLureHudHandler.registerEvents();
		RendererHandler.registerEvents();
		SplatcraftEntities.bindRenderers();
		SplatcraftTileEntities.bindTESR();
		SplatcraftEntities.defineModelLayers();
		SplatcraftKeyHandler.registerBindingsAndEvents();
		
		Services.PLATFORM.registerListener(LifecycleEvents.ClientStarted.class, Splatcraft::initClientAfter);
	}
	public static void init()
	{
		SplatcraftConfig.initialize();
		DamageCalculator.initialize();
		
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
		SplatcraftCommonHandler.registerEvents();
		WeaponHandler.registerEvents();
		SaveInfo.registerEvents();
		ChunkInkHandler.registerEvents();
		SquidFormHandler.registerEvents();
		SpecialHandler.registerSpecials();
		PlayerMovementHandler.registerEvents();
		EntityAction.registerActions();
//		SplatcraftOreGen.registerOres();
		SplatcraftItemGroups.addSplatcraftItemsToVanillaGroups();
		
		Services.PLATFORM.registerListener(LifecycleEvents.ServerStarted.class, Splatcraft::onServerStart);
	}
	public static void onServerStart(MinecraftServer server)
	{
		SplatcraftGameRules.booleanRules.replaceAll((k, v) -> server.getGameRules().getBoolean(SplatcraftGameRules.getRuleFromIndex(k)));
		SplatcraftGameRules.intRules.replaceAll((k, v) -> server.getGameRules().getInt(SplatcraftGameRules.getRuleFromIndex(k)));
	}
	public static <T> DeferredRegister<T> deferredRegistryOf(Registry<T> registry)
	{
		return Services.PLATFORM.createRegistry(registry);
	}
	public static ResourceLocation identifierOf(String path)
	{
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}
	public static void initClientAfter(Minecraft client)
	{
		SplatcraftItems.registerModelProperties();
		SplatcraftItems.postRegister();
	}
}
