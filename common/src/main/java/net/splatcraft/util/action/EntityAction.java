package net.splatcraft.util.action;

import com.mojang.serialization.*;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.Splatcraft;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.data.capabilities.structs.EntityInfo;
import net.splatcraft.items.weapons.DualieItem;
import net.splatcraft.items.weapons.RollerItem;
import net.splatcraft.items.weapons.SlosherItem;
import net.splatcraft.platform.Components;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.action.specials.InkjetAction;
import net.splatcraft.util.action.specials.StingRayAction;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface EntityAction extends Cloneable
{
	Registry<Class<? extends EntityAction>> CLASS_REGISTRY = new MappedRegistry<>(ResourceKey.createRegistryKey(Splatcraft.identifierOf("entity_action_classes")), Lifecycle.stable());
	Registry<Supplier<Codec<EntityAction>>> CODEC_REGISTRY = new MappedRegistry<>(ResourceKey.createRegistryKey(Splatcraft.identifierOf("entity_action_codecs")), Lifecycle.stable());
	Codec<EntityAction> SERIALIZER_CODEC = new MapCodec<EntityAction>()
	{
		@Override
		public <T> RecordBuilder<T> encode(EntityAction input, DynamicOps<T> ops, RecordBuilder<T> builder)
		{
			ResourceLocation id = CLASS_REGISTRY.getKey(input.getClass());
			
			builder.add("id", ResourceLocation.CODEC.encodeStart(ops, id));
			builder.add("data", ops.withEncoder(CODEC_REGISTRY.get(id).get()).apply(input));
			
			return builder;
		}
		@Override
		public <T> DataResult<EntityAction> decode(DynamicOps<T> ops, MapLike<T> input)
		{
			ResourceLocation cooldownClass = ResourceLocation.CODEC.parse(ops, input.get("id")).getOrThrow();
			return CODEC_REGISTRY.get(cooldownClass).get().parse(ops, input.get("data"));
		}
		@Override
		public <T> Stream<T> keys(DynamicOps<T> ops)
		{
			return Stream.of(ops.createString("id"), ops.createString("data"));
		}
	}.codec();
	/**
	 * Retrieves an {@link EntityAction} from the specified {@link LivingEntity}.
	 *
	 * @param entity The entity to retrieve the specified action.
	 * @return The {@link EntityAction} if the {@link LivingEntity} has it, or else null.
	 */
	static EntityAction getEntityAction(LivingEntity entity)
	{
		EntityInfo entityInfo = Components.ENTITY_INFO.getOrCreate(entity);
		return entityInfo.getEntityAction();
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
		EntityInfo entityInfo = Components.ENTITY_INFO.getOrCreate(entity);
		if (!clazz.isInstance(entityInfo.getEntityAction()))
			return null;
		return (T) entityInfo.getEntityAction();
	}
	/**
	 * Tries to retrieve an {@link EntityAction} from the specified {@link LivingEntity}, the result is represented by an {@link Optional}.
	 *
	 * @param entity The entity to retrieve the specified action.
	 * @return An {@link Optional} representing an {@link EntityAction} if the {@link LivingEntity} has it and is it not null, or else {@code Optional.empty}.
	 */
	static Optional<EntityAction> getEntityActionOptional(LivingEntity entity)
	{
		return Components.ENTITY_INFO.getOptional(entity).map(EntityInfo::getEntityAction);
	}
	static <T extends EntityAction> Optional<T> getSpecificEntityActionOptional(LivingEntity entity, Class<T> clazz)
	{
		return Components.ENTITY_INFO.getOptional(entity).map(EntityInfo::getEntityAction).map(v -> clazz.isInstance(v) ? (T) v : null);
	}
	static void setEntityAction(LivingEntity entity, EntityAction action)
	{
		setEntityAction(entity, action, false, false);
	}
	static void setEntityAction(LivingEntity entity, EntityAction action, boolean forced, boolean notifyOldAction)
	{
		Components.ENTITY_INFO.updateOrCreate(entity, info -> info.setEntityAction(action, entity, forced, notifyOldAction));
	}
	static <T extends EntityAction> boolean hasSpecificEntityActionAnd(LivingEntity entity, Predicate<T> actionPredicate, Class<T> clazz)
	{
		return getSpecificEntityActionIf(entity, actionPredicate, clazz).isPresent();
	}
	static boolean hasEntityActionAnd(LivingEntity entity, Predicate<EntityAction> actionPredicate)
	{
		return getEntityActionIf(entity, actionPredicate).isPresent();
	}
	static <T extends EntityAction> Optional<T> getSpecificEntityActionIf(LivingEntity entity, Predicate<T> actionPredicate, Class<T> clazz)
	{
		if (entity != null && Components.ENTITY_INFO.has(entity))
		{
			EntityAction action = Components.ENTITY_INFO.get(entity).getEntityAction();
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
	static Optional<EntityAction> getEntityActionIf(LivingEntity entity, Predicate<EntityAction> actionPredicate)
	{
		if (entity != null && Components.ENTITY_INFO.has(entity))
		{
			EntityAction action = Components.ENTITY_INFO.get(entity).getEntityAction();
			if (action != null && actionPredicate.test(action))
			{
				return Optional.of(action);
			}
		}
		return Optional.empty();
	}
	static boolean hasEntityAction(LivingEntity entity)
	{
		if (entity == null || !Components.ENTITY_INFO.has(entity))
			return false;
		
		EntityAction cooldown = Components.ENTITY_INFO.get(entity).getEntityAction();
		return cooldown != null;
	}
	static <T extends EntityAction> boolean hasSpecificEntityAction(LivingEntity entity, Class<T> clazz)
	{
		if (entity == null || !Components.ENTITY_INFO.has(entity))
			return false;
		
		EntityAction cooldown = Components.ENTITY_INFO.get(entity).getEntityAction();
		return clazz.isInstance(cooldown);
	}
	static void registerActions()
	{
		register("default_cooldown", EntityCooldown.class, () -> EntityCooldown.CODEC);
		register("super_jump", SuperJumpCommand.SuperJump.class, () -> SuperJumpCommand.SuperJump.CODEC);
		register("slosh_action", SlosherItem.SloshAction.class, () -> SlosherItem.SloshAction.CODEC);
		register("dodge_roll_action", DualieItem.DodgeRollAction.class, () -> DualieItem.DodgeRollAction.CODEC);
		register("roller_swing_action", RollerItem.InitialSwingAction.class, () -> RollerItem.InitialSwingAction.CODEC);
		register("sting_ray_special_action", StingRayAction.class, () -> StingRayAction.CODEC);
		register("inkjet_special_action", InkjetAction.class, () -> InkjetAction.CODEC);
	}
	static <T extends EntityAction> void register(String name, Class<T> clazz, Supplier<Codec<T>> codecSupplier)
	{
		ResourceLocation id = Splatcraft.identifierOf(name);
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
	default boolean isCancellable(LivingEntity entity)
	{
		return false;
	}
	default EntitySlot getItemSlot()
	{
		return EntitySlot.EMPTY;
	}
	default ActionEndResult updateAction(LivingEntity entity)
	{
		ActionEndResult endResult = ActionEndResult.dontEnd(this);
		
		if (getTime() == getMaxTime())
			onStart(entity);
		if (isCancellable(entity) && CommonUtils.isSquid(entity))
		{
			endResult = canEnd(entity, EntityAction.EndType.CANCELLED);
			if (!endResult.tickAfter())
				return endResult;
		}
		else
		{
			tick(entity);
			entity.setSprinting(false);
		}
		if (reversedTime())
		{
			if (getTime() >= getMaxTime())
			{
				endResult = canEnd(entity, EntityAction.EndType.TIME);
				if (!endResult.tickAfter())
					return endResult;
			}
			setTime(getTime() + 1);
		}
		else
		{
			if (getTime() <= 1)
			{
				endResult = canEnd(entity, EntityAction.EndType.TIME);
				if (!endResult.tickAfter())
					return endResult;
			}
			setTime(getTime() - 1);
		}
		
		return endResult;
	}
	default ActionEndResult tick(LivingEntity entity)
	{
		return ActionEndResult.dontEnd(this);
	}
	default void onStart(LivingEntity entity)
	{
	}
	default boolean reversedTime()
	{
		return false;
	}
	default ActionEndResult canEnd(LivingEntity entity, EndType endType)
	{
		return ActionEndResult.END_ACTION;
	}
	/**
	 * Called whenever another action is about to override the current action.
	 *
	 * @param entity The entity that executes this action.
	 */
	default void beforeForcedEnd(LivingEntity entity)
	{
	
	}
	public enum EndType
	{
		TIME,
		CANCELLED
	}
}
