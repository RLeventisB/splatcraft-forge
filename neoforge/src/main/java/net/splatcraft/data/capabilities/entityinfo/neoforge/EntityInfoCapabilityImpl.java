package net.splatcraft.data.capabilities.entityinfo.neoforge;

import net.minecraft.entity.LivingEntity;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.neoforge.SplatcraftNeoForgeDataAttachments;

public class EntityInfoCapabilityImpl
{
	public static EntityInfo get(LivingEntity entity)
	{
		return entity.getData(SplatcraftNeoForgeDataAttachments.ENTITY_INFO);
	}
	public static boolean hasCapability(LivingEntity entity)
	{
		return entity.hasData(SplatcraftNeoForgeDataAttachments.ENTITY_INFO);
	}
	public static void set(LivingEntity entity, EntityInfo newData)
	{
		entity.setData(SplatcraftNeoForgeDataAttachments.ENTITY_INFO, newData);
	}
}
