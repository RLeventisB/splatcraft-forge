package net.splatcraft.platform.event;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.splatcraft.platform.event.types.ConsumerEvent;

public interface LifecycleEvents
{
	public interface ClientStarted extends ConsumerEvent.Mono<Minecraft>
	{
		void register(Minecraft client);
		default void invoke(Minecraft parameter1)
		{
			register(parameter1);
		}
	}
	public interface ClientStopped extends ConsumerEvent.Mono<Minecraft>
	{
		void register(Minecraft client);
		default void invoke(Minecraft parameter1)
		{
			register(parameter1);
		}
	}
	public interface ServerStarting extends ConsumerEvent.Mono<MinecraftServer>
	{
		void register(MinecraftServer client);
		default void invoke(MinecraftServer parameter1)
		{
			register(parameter1);
		}
	}
	public interface ServerStarted extends ConsumerEvent.Mono<MinecraftServer>
	{
		void register(MinecraftServer client);
		default void invoke(MinecraftServer parameter1)
		{
			register(parameter1);
		}
	}
	public interface ServerStopped extends ConsumerEvent.Mono<MinecraftServer>
	{
		void register(MinecraftServer client);
		default void invoke(MinecraftServer parameter1)
		{
			register(parameter1);
		}
	}
}

