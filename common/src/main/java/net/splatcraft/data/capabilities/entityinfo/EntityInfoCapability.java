package net.splatcraft.data.capabilities.entityinfo;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.entity.LivingEntity;
import net.splatcraft.entities.InkSquidEntity;
import org.jetbrains.annotations.Contract;

import java.util.Optional;

public class EntityInfoCapability
{
	@ExpectPlatform
	@Contract
	public static EntityInfo get(LivingEntity entity)
	{
		throw new AssertionError();
	}
	@Contract
	public static Optional<EntityInfo> getOptional(LivingEntity entity)
	{
		if (hasCapability(entity))
			return Optional.of(get(entity));
		return Optional.empty();
	}
	@ExpectPlatform
	@Contract
	public static void set(LivingEntity entity, EntityInfo newData)
	{
		throw new AssertionError();
	}
	@ExpectPlatform
	@Contract
	public static boolean hasCapability(LivingEntity entity)
	{
		throw new AssertionError();
	}
	public static boolean isSquid(LivingEntity entity)
	{
		if (entity instanceof InkSquidEntity)
			return true;
		
		return hasCapability(entity) && get(entity).isSquid();
	}
}
