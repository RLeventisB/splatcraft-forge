package net.splatcraft.platform.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.platform.event.types.ConsumerEvent;

public interface TickEvents
{
	@FunctionalInterface
	public interface ClientBefore extends ConsumerEvent.Mono<Minecraft>
	{
		void register(Minecraft client);
		default void invoke(Minecraft parameter1)
		{
			register(parameter1);
		}
	}
	@FunctionalInterface
	public interface ClientAfter extends ConsumerEvent.Mono<Minecraft>
	{
		void register(Minecraft client);
		default void invoke(Minecraft parameter1)
		{
			register(parameter1);
		}
	}
	@FunctionalInterface
	public interface PlayerBefore extends ConsumerEvent.Mono<Player>
	{
		void register(Player player);
		default void invoke(Player parameter1)
		{
			register(parameter1);
		}
	}
	@FunctionalInterface
	public interface PlayerAfter extends ConsumerEvent.Mono<Player>
	{
		void register(Player player);
		default void invoke(Player parameter1)
		{
			register(parameter1);
		}
	}
	@FunctionalInterface
	public interface ServerBefore extends ConsumerEvent.Mono<MinecraftServer>
	{
		void register(MinecraftServer server);
		default void invoke(MinecraftServer parameter1)
		{
			register(parameter1);
		}
	}
	@FunctionalInterface
	public interface ServerAfter extends ConsumerEvent.Mono<MinecraftServer>
	{
		void register(MinecraftServer server);
		default void invoke(MinecraftServer parameter1)
		{
			register(parameter1);
		}
	}
	@FunctionalInterface
	public interface ServerLevelBefore extends ConsumerEvent.Mono<ServerLevel>
	{
		void register(ServerLevel level);
		default void invoke(ServerLevel parameter1)
		{
			register(parameter1);
		}
	}
	@FunctionalInterface
	public interface ServerLevelAfter extends ConsumerEvent.Mono<ServerLevel>
	{
		void register(ServerLevel level);
		default void invoke(ServerLevel parameter1)
		{
			register(parameter1);
		}
	}
	@FunctionalInterface
	public interface ClientLevelBefore extends ConsumerEvent.Mono<ClientLevel>
	{
		void invoke(ClientLevel level);
	}
	@FunctionalInterface
	public interface ClientLevelAfter extends ConsumerEvent.Mono<ClientLevel>
	{
		void invoke(ClientLevel level);
	}
}

