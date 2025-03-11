package net.splatcraft.platform.event.types;

import net.splatcraft.platform.event.EventResult;

public interface SimpleEvent
{
	@FunctionalInterface
	public interface Mono<P1>
	{
		EventResult invoke(P1 parameter1);
	}
	@FunctionalInterface
	public interface Bi<P1, P2>
	{
		EventResult invoke(P1 parameter1, P2 parameter2);
	}
	@FunctionalInterface
	public interface Tri<P1, P2, P3>
	{
		EventResult invoke(P1 parameter1, P2 parameter2, P3 parameter3);
	}
	@FunctionalInterface
	public interface Tetra<P1, P2, P3, P4>
	{
		EventResult invoke(P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4);
	}
	@FunctionalInterface
	public interface Penta<P1, P2, P3, P4, P5>
	{
		EventResult invoke(P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4, P5 parameter5);
	}
	@FunctionalInterface
	public interface Hexa<P1, P2, P3, P4, P5, P6>
	{
		EventResult invoke(P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4, P5 parameter5, P6 parameter6);
	}
}
