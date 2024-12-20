package net.splatcraft.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.handlers.JumpLureHudHandler;
import net.splatcraft.client.handlers.RendererHandler;

public final class SplatcraftFabricClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        Splatcraft.initClient();

        MinecraftClient.getInstance().inGameHud.layeredDrawer.addLayer(RendererHandler::renderGui);
        MinecraftClient.getInstance().inGameHud.layeredDrawer.addLayer(JumpLureHudHandler::renderGui);
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
    }
}
