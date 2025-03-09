package net.splatcraft.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.handlers.ClientSetupHandler;

@Mod(value = Splatcraft.MODID, dist = Dist.CLIENT)
public class SplatcraftNeoForgeClient
{
	public SplatcraftNeoForgeClient(IEventBus modBus)
	{
		Splatcraft.initClient();
		modBus.addListener(SplatcraftNeoForgeClient::beforeRegisterScreens);
	}
	private static void beforeRegisterScreens(RegisterMenuScreensEvent event)
	{
		ClientSetupHandler.bindScreenContainers((menuType, screenConstructor) -> event.register(menuType, screenConstructor::create));
	}
}
