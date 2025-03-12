package net.splatcraft.platform;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.neoforged.bus.api.Event;
import net.splatcraft.neoforge.SplatcraftNeoForge;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventHelper
{
	private static final Map<Class<? extends Event>, EventCollection<?>> EVENT_MAPS = new Object2ObjectOpenHashMap<>();
	private static final Map<Class<? extends Event>, EventCollection<?>> EVENT_LISTS = new Object2ObjectOpenHashMap<>();
	public static <T extends Event> void registerEvent(Class<T> clazz, Consumer<T> action)
	{
		SplatcraftNeoForge.modBus.addListener(clazz, action);
	}
	// todo: maybe finish this code that simplifies NeoForgePlatformHelper.serverDataPacks, or creativeTabAppends, into a single method that
	// tracks these entries to add them to a bi consumer or something so it isnt necessary to do a list for every forge event that is
	// "ok!!! you can update this list now while this event is being posted!!!!!"
	public static <E extends Event> void addToEventSpecificList(Class<E> clazz, Consumer<E> action)
	{
		boolean first = !EVENT_LISTS.containsKey(clazz);
		EventCollection<E> list = (EventCollection<E>) EVENT_LISTS.computeIfAbsent(clazz, v -> new EventCollection<E>(clazz, new ObjectArrayList<>()));
		list.eventFiringConsumer.add(action);
		
		if (first)
			registerEvent(clazz, list::process);
	}
	public static <E extends Event> void addToEventSpecificMap(Class<E> clazz, Consumer<E> action)
	{
		boolean first = !EVENT_LISTS.containsKey(clazz);
		EventCollection<E> list = (EventCollection<E>) EVENT_LISTS.computeIfAbsent(clazz, v -> new EventCollection<E>(clazz, new ObjectArrayList<>()));
		list.eventFiringConsumer.add(action);
		
		if (first)
			registerEvent(clazz, list::process);
	}
	static record EventCollection<E extends Event>(Class<E> eventClass, List<Consumer<E>> eventFiringConsumer)
	{
		public void process(E event)
		{
			for (var consumer : eventFiringConsumer)
			{
				consumer.accept(event);
			}
		}
	}
}
