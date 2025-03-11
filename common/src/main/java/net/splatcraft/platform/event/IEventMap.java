package net.splatcraft.platform.event;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.splatcraft.platform.event.types.CompoundEvent;
import net.splatcraft.platform.event.types.ConsumerEvent;
import net.splatcraft.platform.event.types.SimpleEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public interface IEventMap
{
	EventRegistry events = new EventRegistry();
	default <EVT> void registerListener(Class<EVT> eventClass, EVT evt)
	{
		EventList<EVT> list = events.get(eventClass);
		list.register(evt);
	}
	default <EVT extends SimpleEvent.Mono<P1>, P1> EventResult invokeEvent(Class<EVT> eventClass, P1 parameter1)
	{
		return events.get(eventClass).invokeSimple(v -> v.invoke(parameter1));
	}
	default <EVT extends ConsumerEvent.Mono<P1>, P1> void invokeConsumerEvent(Class<EVT> eventClass, P1 parameter1)
	{
		events.get(eventClass).invokeConsumer(v -> v.invoke(parameter1));
	}
	default <EVT extends CompoundEvent.Mono<P1, R>, P1, R> CompoundEventResult<R> invokeCompoundEvent(Class<EVT> eventClass, P1 parameter1)
	{
		return events.get(eventClass).invokeCompound(v -> v.invoke(parameter1));
	}
	default <EVT extends SimpleEvent.Bi<P1, P2>, P1, P2> EventResult invokeEvent(Class<EVT> eventClass, P1 parameter1, P2 parameter2)
	{
		return events.get(eventClass).invokeSimple(v -> v.invoke(parameter1, parameter2));
	}
	default <EVT extends ConsumerEvent.Bi<P1, P2>, P1, P2> void invokeConsumerEvent(Class<EVT> eventClass, P1 parameter1, P2 parameter2)
	{
		events.get(eventClass).invokeConsumer(v -> v.invoke(parameter1, parameter2));
	}
	default <EVT extends CompoundEvent.Bi<P1, P2, R>, P1, P2, R> CompoundEventResult<R> invokeCompoundEvent(Class<EVT> eventClass, P1 parameter1, P2 parameter2)
	{
		return events.get(eventClass).invokeCompound(v -> v.invoke(parameter1, parameter2));
	}
	default <EVT extends SimpleEvent.Tri<P1, P2, P3>, P1, P2, P3> EventResult invokeEvent(Class<EVT> eventClass, P1 parameter1, P2 parameter2, P3 parameter3)
	{
		return events.get(eventClass).invokeSimple(v -> v.invoke(parameter1, parameter2, parameter3));
	}
	default <EVT extends ConsumerEvent.Tri<P1, P2, P3>, P1, P2, P3> void invokeConsumerEvent(Class<EVT> eventClass, P1 parameter1, P2 parameter2, P3 parameter3)
	{
		events.get(eventClass).invokeConsumer(v -> v.invoke(parameter1, parameter2, parameter3));
	}
	default <EVT extends CompoundEvent.Tri<P1, P2, P3, R>, P1, P2, P3, R> CompoundEventResult<R> invokeCompoundEvent(Class<EVT> eventClass, P1 parameter1, P2 parameter2, P3 parameter3)
	{
		return events.get(eventClass).invokeCompound(v -> v.invoke(parameter1, parameter2, parameter3));
	}
	default <EVT extends SimpleEvent.Tetra<P1, P2, P3, P4>, P1, P2, P3, P4> EventResult invokeEvent(Class<EVT> eventClass, P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4)
	{
		return events.get(eventClass).invokeSimple(v -> v.invoke(parameter1, parameter2, parameter3, parameter4));
	}
	default <EVT extends ConsumerEvent.Tetra<P1, P2, P3, P4>, P1, P2, P3, P4> void invokeConsumerEvent(Class<EVT> eventClass, P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4)
	{
		events.get(eventClass).invokeConsumer(v -> v.invoke(parameter1, parameter2, parameter3, parameter4));
	}
	default <EVT extends CompoundEvent.Tetra<P1, P2, P3, P4, R>, P1, P2, P3, P4, R> CompoundEventResult<R> invokeCompoundEvent(Class<EVT> eventClass, P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4)
	{
		return events.get(eventClass).invokeCompound(v -> v.invoke(parameter1, parameter2, parameter3, parameter4));
	}
	default <EVT extends SimpleEvent.Penta<P1, P2, P3, P4, P5>, P1, P2, P3, P4, P5> EventResult invokeEvent(Class<EVT> eventClass, P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4, P5 parameter5)
	{
		return events.get(eventClass).invokeSimple(v -> v.invoke(parameter1, parameter2, parameter3, parameter4, parameter5));
	}
	default <EVT extends ConsumerEvent.Penta<P1, P2, P3, P4, P5>, P1, P2, P3, P4, P5> void invokeConsumerEvent(Class<EVT> eventClass, P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4, P5 parameter5)
	{
		events.get(eventClass).invokeConsumer(v -> v.invoke(parameter1, parameter2, parameter3, parameter4, parameter5));
	}
	default <EVT extends CompoundEvent.Penta<P1, P2, P3, P4, P5, R>, P1, P2, P3, P4, P5, R> CompoundEventResult<R> invokeCompoundEvent(Class<EVT> eventClass, P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4, P5 parameter5)
	{
		return events.get(eventClass).invokeCompound(v -> v.invoke(parameter1, parameter2, parameter3, parameter4, parameter5));
	}
	default <EVT extends SimpleEvent.Hexa<P1, P2, P3, P4, P5, P6>, P1, P2, P3, P4, P5, P6> EventResult invokeEvent(Class<EVT> eventClass, P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4, P5 parameter5, P6 parameter6)
	{
		return events.get(eventClass).invokeSimple(v -> v.invoke(parameter1, parameter2, parameter3, parameter4, parameter5, parameter6));
	}
	default <EVT extends ConsumerEvent.Hexa<P1, P2, P3, P4, P5, P6>, P1, P2, P3, P4, P5, P6> void invokeConsumerEvent(Class<EVT> eventClass, P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4, P5 parameter5, P6 parameter6)
	{
		events.get(eventClass).invokeConsumer(v -> v.invoke(parameter1, parameter2, parameter3, parameter4, parameter5, parameter6));
	}
	default <EVT extends CompoundEvent.Hexa<P1, P2, P3, P4, P5, P6, R>, P1, P2, P3, P4, P5, P6, R> CompoundEventResult<R> invokeCompoundEvent(Class<EVT> eventClass, P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4, P5 parameter5, P6 parameter6)
	{
		return events.get(eventClass).invokeCompound(v -> v.invoke(parameter1, parameter2, parameter3, parameter4, parameter5, parameter6));
	}
	class EventRegistry
	{
		public Map<Class<?>, EventList<?>> events;
		public <EVT> EventList<EVT> get(Class<EVT> eventClass)
		{
			return (EventList<EVT>) events.computeIfAbsent(eventClass, v -> new EventList<EVT>());
		}
	}
	class EventList<T>
	{
		public List<T> registeredEvents = new ObjectArrayList<>();
		public void register(T evt)
		{
			registeredEvents.add(evt);
		}
		public void invokeConsumer(Consumer<T> consumer)
		{
			for (var evt : registeredEvents)
			{
				consumer.accept(evt);
			}
		}
		public <R> CompoundEventResult<R> invokeCompound(Function<T, CompoundEventResult<R>> consumer)
		{
			for (var evt : registeredEvents)
			{
				CompoundEventResult<R> result = consumer.apply(evt);
				if (!result.result().interrupts)
					continue;
				
				return result;
			}
			return CompoundEventResult.pass();
		}
		public EventResult invokeSimple(Function<T, EventResult> consumer)
		{
			for (var evt : registeredEvents)
			{
				EventResult result = consumer.apply(evt);
				if (!result.interrupts)
					continue;
				
				return result;
			}
			return EventResult.PASS;
		}
		public void freezeRegisteredEvents()
		{
			registeredEvents = ImmutableList.copyOf(registeredEvents);
		}
	}
}
