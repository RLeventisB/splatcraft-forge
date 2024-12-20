package net.splatcraft.client.handlers.neoforge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.handlers.JumpLureHudHandler;
import net.splatcraft.client.handlers.RendererHandler;

@EventBusSubscriber
public class GuiOverlayHandler
{
    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiLayersEvent event)
    {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, Splatcraft.identifierOf("overlay"), RendererHandler::renderGui);
        event.registerAbove(VanillaGuiLayers.HOTBAR, Splatcraft.identifierOf("jump_lure"), JumpLureHudHandler::renderGui);
    }
}
