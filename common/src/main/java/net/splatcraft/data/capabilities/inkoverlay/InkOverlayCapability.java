package net.splatcraft.data.capabilities.inkoverlay;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.concurrent.ConcurrentHashMap;

public class InkOverlayCapability
{
	// todo: handle entity spawning and despawning pls
	private static final ConcurrentHashMap<LivingEntity, InkOverlayInfo> map = new ConcurrentHashMap<>();
	public static InkOverlayInfo get(LivingEntity entity) throws NullPointerException
	{
		return map.computeIfAbsent(entity, t -> new InkOverlayInfo());
	}
	public static boolean hasCapability(LivingEntity entity)
	{
		return map.containsKey(entity);
	}
	public static void serialize(LivingEntity entity, NbtCompound data)
	{
		get(entity).writeNBT(data);
	}
	public static void deserialize(LivingEntity entity, NbtCompound data)
	{
		get(entity).readNBT(data);
	}
	public static void remove(LivingEntity livingEntity)
	{
		map.remove(livingEntity);
	}
}
