package net.splatcraft.platform.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.splatcraft.platform.event.types.ConsumerEvent;
import net.splatcraft.platform.event.types.SimpleEvent;

public interface PlayerEvents
{
	@FunctionalInterface
	public interface PlayerClone extends ConsumerEvent.Tri<ServerPlayer, ServerPlayer, Boolean>
	{
		void register(ServerPlayer oldPlayer, ServerPlayer newPlayer, Boolean beatenEnderDragon);
		default void invoke(ServerPlayer parameter1, ServerPlayer parameter2, Boolean parameter3)
		{
			register(parameter1, parameter2, parameter3);
		}
	}
	@FunctionalInterface
	public interface PlayerDeath extends ConsumerEvent.Mono<Player>
	{
		void register(Player player);
		default void invoke(Player parameter1)
		{
			register(parameter1);
		}
	}
	@FunctionalInterface
	public interface AttackEntity extends SimpleEvent.Penta<Player, Level, Entity, InteractionHand, EntityHitResult>
	{
		EventResult register(Player player, Level level, Entity target, InteractionHand hand, EntityHitResult hitResult);
		default EventResult invoke(Player parameter1, Level parameter2, Entity parameter3, InteractionHand parameter4, EntityHitResult parameter5)
		{
			return register(parameter1, parameter2, parameter3, parameter4, parameter5);
		}
	}
	@FunctionalInterface
	public interface Quit extends ConsumerEvent.Mono<ServerPlayer>
	{
		void invoke(ServerPlayer player);
	}
	public interface LogIn extends ConsumerEvent.Mono<ServerPlayer>
	{
		void invoke(ServerPlayer player);
	}
}

