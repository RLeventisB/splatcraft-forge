package net.splatcraft.data.capabilities.entityinfo;

import net.minecraft.world.entity.LivingEntity;
import net.splatcraft.entities.InkSquidEntity;
import net.splatcraft.platform.Services;
import org.jetbrains.annotations.Contract;

import java.util.Optional;

public class EntityInfoCapability
{
	@Contract
	public static EntityInfo get(LivingEntity entity)
	{
		return Services.PLATFORM.getEntityInfo(entity);
	}
	@Contract
	public static Optional<EntityInfo> getOptional(LivingEntity entity)
	{
		if (hasCapability(entity))
			return Optional.of(get(entity));
		return Optional.empty();
	}
	@Contract
	public static void set(LivingEntity entity, EntityInfo newData)
	{
		Services.PLATFORM.setEntityInfo(entity, newData);
	}
	@Contract
	public static boolean hasCapability(LivingEntity entity)
	{
		return Services.PLATFORM.hasEntityInfo(entity);
	}
	public static boolean isSquid(LivingEntity entity)
	{
		if (entity instanceof InkSquidEntity)
			return true;
		
		return hasCapability(entity) && get(entity).isSquid();
	}
}
