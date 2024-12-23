package net.splatcraft.neoforge;

import net.minecraft.client.network.ClientPlayerEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.handlers.JumpLureHudHandler;
import net.splatcraft.client.handlers.PlayerMovementHandler;
import net.splatcraft.client.handlers.RendererHandler;
import net.splatcraft.handlers.SquidFormHandler;
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
		NeoForge.EVENT_BUS.addListener(SplatcraftNeoForge::onGamemodeChange);
		NeoForge.EVENT_BUS.addListener(SplatcraftNeoForge::onInputUpdate);
	}
	private static void onInputUpdate(MovementInputUpdateEvent event)
	{
		PlayerMovementHandler.onInputUpdate((ClientPlayerEntity) event.getEntity(), event.getInput());
	}
	private static void onGamemodeChange(PlayerEvent.PlayerChangeGameModeEvent event)
	{
		SquidFormHandler.onGameModeSwitch(event.getEntity(), event.getNewGameMode());
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
