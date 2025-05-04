package net.splatcraft.platform;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.splatcraft.SplatcraftNeoForge;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class EventHelper
{
	private static final Map<EventRecord<?, ?>, List<?>> EVENT_CONSUMERS = new Object2ObjectOpenHashMap<>();
	public static <T extends Event> void registerEvent(Class<T> clazz, Consumer<T> action)
	{
		if (IModBusEvent.class.isAssignableFrom(clazz))
			SplatcraftNeoForge.modBus.addListener(clazz, action);
		else
			NeoForge.EVENT_BUS.addListener(clazz, action);
	}
	public static <T extends Event> T postEvent(T event)
	{
		if (event instanceof IModBusEvent)
			return SplatcraftNeoForge.modBus.post(event);
		else
			return NeoForge.EVENT_BUS.post(event);
	}
	// todo: maybe finish this code that simplifies NeoForgePlatformHelper.serverDataPacks, or creativeTabAppends, into a single method that
	// tracks these entries to add them to a bi consumer or something so it isnt necessary to do a list for every forge event that is
	// "ok!!! you can update this list now while this event is being posted!!!!!"
	public static <E extends Event, V> void addToEventSpecificList(Class<E> eventClass, V value, BiConsumer<E, V> action)
	{
		EventRecord<E, V> record = new EventRecord<>(eventClass, action);
		boolean first = !EVENT_CONSUMERS.containsKey(record);
		List<V> list = (List<V>) EVENT_CONSUMERS.computeIfAbsent(record, v -> new ObjectArrayList<>());
		list.add(value);

		if (first)
			registerEvent(eventClass, v -> record.process(v, list));
	}
	public static <E extends Event, K, V> void addToEventSpecificMap(Class<E> eventClass, K key, V value, TriConsumer<E, K, V> action)
	{
		addToEventSpecificList(eventClass, Pair.of(key, value), (e, pair) -> action.accept(e, pair.key(), pair.value()));
	}
	record EventRecord<E extends Event, V>(Class<E> evtClass, BiConsumer<E, V> eventFiringConsumer)
	{
		public void process(E event, List<V> values)
		{
			for (var value : values)
			{
				eventFiringConsumer.accept(event, value);
			}
		}
	}
	record EventConsumer<E extends Event, V>(BiConsumer<E, V> eventFiringConsumer, List<V> values)
	{
		public void process(E event)
		{
			for (var value : values)
			{
				eventFiringConsumer.accept(event, value);
			}
		}
	}
}
