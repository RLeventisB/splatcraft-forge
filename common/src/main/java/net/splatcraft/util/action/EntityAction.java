package net.splatcraft.util.action;

import com.mojang.serialization.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.splatcraft.Splatcraft;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.DualieItem;
import net.splatcraft.items.weapons.RollerItem;
import net.splatcraft.items.weapons.SlosherItem;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface EntityAction
{
	Registry<Class<? extends EntityAction>> CLASS_REGISTRY = new SimpleRegistry<>(RegistryKey.ofRegistry(Splatcraft.identifierOf("player_cooldown_classes")), Lifecycle.stable());
	Registry<Supplier<Codec<EntityAction>>> CODEC_REGISTRY = new SimpleRegistry<>(RegistryKey.ofRegistry(Splatcraft.identifierOf("player_cooldown_codecs")), Lifecycle.stable());
	MapCodec<EntityAction> SERIALIZER_CODEC = new MapCodec<>()
	{
		@Override
		public <T> RecordBuilder<T> encode(EntityAction input, DynamicOps<T> ops, RecordBuilder<T> builder)
		{
			Identifier id = CLASS_REGISTRY.getId(input.getClass());
			
			builder.add("id", Identifier.CODEC.encodeStart(ops, id));
			builder.add("data", ops.withEncoder(CODEC_REGISTRY.get(id).get()).apply(input));
			
			return builder;
		}
		@Override
		public <T> DataResult<EntityAction> decode(DynamicOps<T> ops, MapLike<T> input)
		{
			Identifier cooldownClass = Identifier.CODEC.parse(ops, input.get("id")).getOrThrow();
			return CODEC_REGISTRY.get(cooldownClass).get().parse(ops, input.get("data"));
		}
		@Override
		public <T> Stream<T> keys(DynamicOps<T> ops)
		{
			return Stream.of(ops.createString("id"), ops.createString("data"));
		}
	};
	/**
	 * Retrieves an {@link EntityAction} from the specified {@link LivingEntity}.
	 *
	 * @param entity The entity to retrieve the specified action.
	 * @return The {@link EntityAction} if the {@link LivingEntity} has it, or else null.
	 */
	static EntityAction getEntityAction(LivingEntity entity)
	{
		EntityInfo playerInfo = EntityInfoCapability.get(entity);
		if (playerInfo == null)
			return null;
		return playerInfo.getEntityAction();
	}
	/**
	 * Retrieves an {@link EntityAction} from the specified {@link LivingEntity}, if the {@link EntityAction} implements or extends a specified {@link Class}.
	 *
	 * @param entity The entity to retrieve the specified action.
	 * @param clazz  The class of the action.
	 * @return The {@link EntityAction} if the {@link LivingEntity} has it, and is an instance of the specified {@link Class}, or else null.
	 */
	static <T extends EntityAction> T getSpecificEntityAction(LivingEntity entity, Class<T> clazz)
	{
		EntityInfo playerInfo = EntityInfoCapability.get(entity);
		if (playerInfo == null || !clazz.isInstance(playerInfo.getEntityAction()))
			return null;
		return (T) playerInfo.getEntityAction();
	}
	/**
	 * Tries to retrieve an {@link EntityAction} from the specified {@link LivingEntity}, the result is represented by an {@link Optional}.
	 *
	 * @param entity The entity to retrieve the specified action.
	 * @return An {@link Optional} representing an {@link EntityAction} if the {@link LivingEntity} has it and is it not null, or else {@code Optional.empty}.
	 */
	static Optional<EntityAction> getEntityActionOptional(LivingEntity entity)
	{
		return EntityInfoCapability.getOptional(entity).map(EntityInfo::getEntityAction);
	}
	static <T extends EntityAction> Optional<T> geSpecificEntityActionOptional(LivingEntity entity, Class<T> clazz)
	{
		return EntityInfoCapability.getOptional(entity).map(EntityInfo::getEntityAction).map(v -> clazz.isInstance(v) ? (T) v : null);
	}
	static void setEntityAction(LivingEntity entity, EntityAction action)
	{
		EntityInfoCapability.get(entity).setEntityAction(action);
	}
	static EntityAction setActionTime(LivingEntity entity, int time)
	{
		EntityAction action = EntityInfoCapability.get(entity).getEntityAction();
		if (action == null)
		{
			return null;
		}
		else
		{
			action.setTime(time);
		}
		
		return action;
	}
	static boolean hasActionAnd(LivingEntity entity, Predicate<EntityAction> actionPredicate)
	{
		return getActionIf(entity, actionPredicate).isPresent();
	}
	static <T extends EntityAction> Optional<T> getSpecificActionIf(LivingEntity entity, Predicate<T> actionPredicate, Class<T> clazz)
	{
		if (entity != null && EntityInfoCapability.hasCapability(entity))
		{
			EntityAction action = EntityInfoCapability.get(entity).getEntityAction();
			if (clazz != null && clazz.isInstance(action))
			{
				T castedAction = (T) action;
				if (actionPredicate.test(castedAction))
				{
					return Optional.of(castedAction);
				}
			}
		}
		return Optional.empty();
	}
	static Optional<EntityAction> getActionIf(LivingEntity entity, Predicate<EntityAction> actionPredicate)
	{
		if (entity != null && EntityInfoCapability.hasCapability(entity))
		{
			EntityAction action = EntityInfoCapability.get(entity).getEntityAction();
			if (action != null && actionPredicate.test(action))
			{
				return Optional.of(action);
			}
		}
		return Optional.empty();
	}
	static boolean hasEntityAction(LivingEntity entity)
	{
		if (entity == null || !EntityInfoCapability.hasCapability(entity))
			return false;
		EntityAction cooldown = EntityInfoCapability.get(entity).getEntityAction();
		return cooldown != null;
	}
	static <T extends EntityAction> boolean hasSpecificEntityAction(LivingEntity entity, Class<T> clazz)
	{
		if (entity == null || !EntityInfoCapability.hasCapability(entity))
			return false;
		EntityAction cooldown = EntityInfoCapability.get(entity).getEntityAction();
		return clazz.isInstance(cooldown);
	}
	static void registerActions()
	{
		register("default_cooldown", EntityCooldown.class, () -> EntityCooldown.CODEC);
		register("super_jump", SuperJumpCommand.SuperJump.class, () -> SuperJumpCommand.SuperJump.CODEC);
		register("slosh_action", SlosherItem.SloshAction.class, () -> SlosherItem.SloshAction.CODEC);
		register("dodge_roll_action", DualieItem.DodgeRollAction.class, () -> DualieItem.DodgeRollAction.CODEC);
		register("roller_swing_action", RollerItem.InitialSwingAction.class, () -> RollerItem.InitialSwingAction.CODEC);
	}
	static <T extends EntityAction> void register(String name, Class<T> clazz, Supplier<Codec<T>> codecSupplier)
	{
		Identifier id = Splatcraft.identifierOf(name);
		Registry.register(CLASS_REGISTRY, id, clazz);
		Registry.register(CODEC_REGISTRY, id, (Supplier<Codec<EntityAction>>) (Object) codecSupplier); // >:(
	}
	default boolean canMove()
	{
		return true;
	}
	default boolean forceCrouch()
	{
		return false;
	}
	default boolean preventWeaponUse()
	{
		return false;
	}
	default boolean preventStopUsing()
	{
		return false;
	}
	float getTime();
	EntityAction setTime(float time);
	float getMaxTime();
	EntityAction setMaxTime(float maxTime);
	default ItemStack getStoredStack()
	{
		return ItemStack.EMPTY;
	}
	default boolean isCancellable()
	{
		return false;
	}
	default int getSlotIndex()
	{
		return -1;
	}
	default Hand getHand()
	{
		return Hand.MAIN_HAND;
	}
	default void tick(LivingEntity entity)
	{
	}
	default void onStart(LivingEntity entity)
	{
	}
	default boolean canEnd(LivingEntity entity)
	{
		return true;
	}
}
