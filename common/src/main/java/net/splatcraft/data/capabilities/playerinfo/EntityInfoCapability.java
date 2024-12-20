package net.splatcraft.data.capabilities.playerinfo;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.splatcraft.entities.InkSquidEntity;

import java.util.concurrent.ConcurrentHashMap;

public class EntityInfoCapability
{
    private static final ConcurrentHashMap<LivingEntity, EntityInfo> map = new ConcurrentHashMap<>();

    public static EntityInfo get(LivingEntity entity)
    {
        return map.computeIfAbsent(entity, t -> new EntityInfo());
    }

    public static boolean hasCapability(LivingEntity entity)
    {
        return map.containsKey(entity);
    }

    public static boolean isSquid(LivingEntity entity)
    {
        if (entity instanceof InkSquidEntity)
            return true;

        return hasCapability(entity) && get(entity).isSquid();
    }

    public static void serialize(LivingEntity entity, NbtCompound data)
    {
        get(entity).writeNBT(data, entity.getRegistryManager());
    }

    public static void deserialize(LivingEntity entity, NbtCompound data)
    {
        get(entity).readNBT(data, entity.getRegistryManager());
    }

    public static void remove(LivingEntity livingEntity)
    {
        map.remove(livingEntity);
    }
}
