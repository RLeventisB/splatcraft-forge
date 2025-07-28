package net.splatcraft.data.capabilities.structs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.splatcraft.util.action.EntityAction;

import java.util.Objects;
import java.util.Optional;

public record EntityInfo(
	Optional<EntityAction> entityAction
)
{
	public static final MapCodec<EntityInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
		EntityAction.SERIALIZER_CODEC.lenientOptionalFieldOf("entity_action").forGetter(EntityInfo::entityAction)
	).apply(inst, EntityInfo::new));
	public static final Codec<EntityInfo> CODEC = MAP_CODEC.codec();
	public EntityInfo()
	{
		this(Optional.empty());
	}
	public EntityAction getEntityAction()
	{
		return entityAction.orElse(null);
	}
	public EntityInfo setEntityAction(EntityAction action, LivingEntity entity, boolean forced, boolean notifyOldAction)
	{
		if (notifyOldAction)
			entityAction.ifPresent(value -> value.beforeForcedEnd(entity));
		return forced ? setEntityAction(Optional.ofNullable(action)) : withEntityAction(Optional.ofNullable(action));
	}
	private EntityInfo withEntityAction(Optional<EntityAction> action)
	{
		if (Objects.equals(this.entityAction, action))
			return this;
		return new EntityInfo(action);
	}
	private EntityInfo setEntityAction(Optional<EntityAction> action)
	{
		return new EntityInfo(action);
	}
	public boolean hasActiveAction()
	{
		return entityAction.isPresent() && entityAction.get().getTime() > 0;
	}
}
