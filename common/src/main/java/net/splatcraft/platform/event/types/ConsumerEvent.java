package net.splatcraft.platform.event.types;

public interface ConsumerEvent
{
	@FunctionalInterface
	interface Mono<P1>
	{
		void invoke(P1 parameter1);
	}
	@FunctionalInterface
	interface Bi<P1, P2>
	{
		void invoke(P1 parameter1, P2 parameter2);
	}
	@FunctionalInterface
	interface Tri<P1, P2, P3>
	{
		void invoke(P1 parameter1, P2 parameter2, P3 parameter3);
	}
	@FunctionalInterface
	interface Tetra<P1, P2, P3, P4>
	{
		void invoke(P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4);
	}
	@FunctionalInterface
	interface Penta<P1, P2, P3, P4, P5>
	{
		void invoke(P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4, P5 parameter5);
	}
	@FunctionalInterface
	interface Hexa<P1, P2, P3, P4, P5, P6>
	{
		void invoke(P1 parameter1, P2 parameter2, P3 parameter3, P4 parameter4, P5 parameter5, P6 parameter6);
	}
}
