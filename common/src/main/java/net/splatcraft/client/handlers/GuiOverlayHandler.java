package net.splatcraft.client.handlers;

import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class GuiOverlayHandler
{
    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event)
    {
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "splatcraft_overlay", RendererHandler::renderGuiInternal);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "splatcraft_jump_lure", JumpLureHudHandler::renderGui);
    }
}
