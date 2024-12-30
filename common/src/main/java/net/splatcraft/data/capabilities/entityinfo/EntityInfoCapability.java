package net.splatcraft.data.capabilities.entityinfo;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.entity.LivingEntity;
import net.splatcraft.entities.InkSquidEntity;

public class EntityInfoCapability
{
	@ExpectPlatform
	public static EntityInfo get(LivingEntity entity)
	{
		throw new AssertionError();
	}
	@ExpectPlatform
	public static void set(LivingEntity entity, EntityInfo newData)
	{
		throw new AssertionError();
	}
	@ExpectPlatform
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
