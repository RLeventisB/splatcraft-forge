package net.splatcraft.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.splatcraft.client.handlers.JumpLureHudHandler;
import net.splatcraft.client.handlers.RendererHandler;

public final class SplatcraftFabricClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		Minecraft.getInstance().gui.layers.add(RendererHandler::renderGui);
		Minecraft.getInstance().gui.layers.add(JumpLureHudHandler::renderGui);
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
	}
}
