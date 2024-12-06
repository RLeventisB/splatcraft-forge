package net.splatcraft.forge.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.util.AttackId.DefaultAttackId.EmptyAttackId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AttackId
{
    public static final AttackId NONE = new EmptyAttackId();
    public static List<AttackId> attackIdList = new ArrayList<>();
    private static short nextAttackId = 0;

    public static AttackId registerAttack()
    {
        AttackId attackId = new DefaultAttackId(getAttackId());
        attackIdList.add(attackId);
        return attackId;
    }

    public static short getAttackId()
    {
        return nextAttackId++;
    }

    public static AttackId readNbt(CompoundTag tag)
    {
        return tag.contains("Id") ? new DefaultAttackId(tag) : new EmptyAttackId();
    }

    public abstract short getId();

    public AttackId countProjectile()
    {
        return countProjectile(1);
    }

    public abstract AttackId countProjectile(int count);

    public abstract void projectileRemoved();

    public abstract boolean checkEntity(Entity entity);

    public abstract CompoundTag serializeNbt();

    public static final class DefaultAttackId extends AttackId
    {
        public final short id;
        public final List<UUID> hitEnemies;
        public byte projectileCount;

        private DefaultAttackId(short id)
        {
            this.id = id;
            hitEnemies = new ArrayList<>(0);
        }

        private DefaultAttackId(CompoundTag tag)
        {
            id = tag.getShort("Id");
            hitEnemies = new ArrayList<>(tag.getInt("HitCount"));
            long[] uuidArray = tag.getLongArray("HitEnemies");
            for (int i = 0; i < hitEnemies.size(); i++)
            {
                hitEnemies.add(new UUID(uuidArray[i << 1], uuidArray[(i << 1) + 1]));
            }
        }

        public short getId()
        {
            return id;
        }

        @Override
        public AttackId countProjectile(int count)
        {
            projectileCount += (byte) count;
            return this;
        }

        public byte getRemainingHits()
        {
            return projectileCount;
        }

        public void projectileRemoved()
        {
            projectileCount--;
            if (projectileCount <= 0)
            {
                if (!attackIdList.remove(this))
                {
                    Splatcraft.LOGGER.warn("Error trying removing attack id " + id);
                }
            }
        }

        public boolean checkEntity(Entity entity)
        {
            if (!hitEnemies.contains(entity.getUUID()))
            {
                hitEnemies.add(entity.getUUID());
                return true;
            }
            return false;
        }

        public CompoundTag serializeNbt()
        {
            CompoundTag ret = new CompoundTag();
            ret.putShort("Id", id);
            ret.putInt("HitCount", hitEnemies.size());
            long[] uuidArray = new long[hitEnemies.size() << 1];
            for (int i = 0; i < hitEnemies.size(); i++)
            {
                UUID uuid = hitEnemies.get(i);
                uuidArray[i << 1] = (uuid.getMostSignificantBits());
                uuidArray[(i << 1) + 1] = (uuid.getLeastSignificantBits());
            }
            ret.putLongArray("HitEnemies", uuidArray);
            return ret;
        }

        public static final class EmptyAttackId extends AttackId
        {
            @Override
            public short getId()
            {
                return 0;
            }

            @Override
            public AttackId countProjectile(int count)
            {
                return this;
            }

            @Override
            public void projectileRemoved()
            {

            }

            @Override
            public boolean checkEntity(Entity entity)
            {
                return true;
            }

            @Override
            public CompoundTag serializeNbt()
            {
                return new CompoundTag();
            }
        }
    }
}
