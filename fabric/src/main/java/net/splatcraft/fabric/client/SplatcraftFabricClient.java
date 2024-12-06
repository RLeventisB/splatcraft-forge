package net.splatcraft.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.splatcraft.Splatcraft;

public final class SplatcraftFabricClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		Splatcraft.initClient();
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
	}
}
