package net.splatcraft.platform;

import net.neoforged.bus.api.Event;
import net.splatcraft.neoforge.SplatcraftNeoForge;

import java.util.function.Consumer;

public class EventHelper
{
	public static <T extends Event> void registerEvent(Consumer<T> action, Class<T> clazz)
	{
		SplatcraftNeoForge.modBus.addListener(clazz, action);
	}
}
