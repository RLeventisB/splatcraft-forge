package net.splatcraft.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.handlers.JumpLureHudHandler;
import net.splatcraft.client.handlers.RendererHandler;
import net.splatcraft.registries.SplatcraftRegistries;

@Mod(Splatcraft.MODID)
public final class SplatcraftNeoForge
{
	public SplatcraftNeoForge(IEventBus modBus)
	{
		// Run our common setup.
		Splatcraft.init();
		modBus.addListener(this::onRegistryUnlocked);
		modBus.addListener(this::registerGuiOverlays);
	}
	public void onRegistryUnlocked(NewRegistryEvent event)
	{
		SplatcraftRegistries.register();
	}
	public void registerGuiOverlays(RegisterGuiLayersEvent event)
	{
		event.registerAbove(VanillaGuiLayers.CROSSHAIR, Splatcraft.identifierOf("overlay"), RendererHandler::renderGui);
		event.registerAbove(VanillaGuiLayers.HOTBAR, Splatcraft.identifierOf("jump_lure"), JumpLureHudHandler::renderGui);
	}
}
