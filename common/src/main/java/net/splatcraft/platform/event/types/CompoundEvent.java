package net.splatcraft.platform.event.types;

import net.splatcraft.platform.event.CompoundEventResult;

public interface CompoundEvent
{
	@FunctionalInterface
	interface Mono<P1, R>
	{
		CompoundEventResult<R> invoke(P1 parameter1);
	}
	@FunctionalInterface
	interface Bi<P1, P2, R>
	{
		CompoundEventResult<R> invoke(P1 parameter1, P2 parameter2);
	}
	@FunctionalInterface
	interface Tri<P1, P2, P3, R>
	{
		CompoundEventResult<R> invoke(P1 parameter1, P2 parameter2, P3 parameter3);
	}
	@FunctionalInterface
	interface Tetra<P1, P2, P3, P4, R>
	{
		CompoundEventResult<R> invoke(P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4);
	}
	@FunctionalInterface
	interface Penta<P1, P2, P3, P4, P5, R>
	{
		CompoundEventResult<R> invoke(P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4, P5 parameter5);
	}
	@FunctionalInterface
	interface Hexa<P1, P2, P3, P4, P5, P6, R>
	{
		CompoundEventResult<R> invoke(P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4, P5 parameter5, P6 parameter6);
	}
}
