package net.splatcraft.entities;

import com.mojang.serialization.Lifecycle;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.splatcraft.Splatcraft;
import net.splatcraft.items.weapons.settings.BlasterWeaponSettings;
import net.splatcraft.util.DamageRangesRecord;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.lang.reflect.InvocationTargetException;
import java.util.TreeMap;

public abstract class ExtraSaveData
{
    public static SimpleRegistry<Class<? extends ExtraSaveData>> REGISTRY = new SimpleRegistry<>(RegistryKey.ofRegistry(Splatcraft.identifierOf("extra_save_data")), Lifecycle.stable());
    public static final TrackedDataHandler<InkProjectileEntity.ExtraDataList> SERIALIZER = new TrackedDataHandler<InkProjectileEntity.ExtraDataList>()
    {
        private static final PacketCodec<? super RegistryByteBuf, InkProjectileEntity.ExtraDataList> PACKET_CODEC = new PacketCodec<RegistryByteBuf, InkProjectileEntity.ExtraDataList>()
        {
            @Override
            public InkProjectileEntity.ExtraDataList decode(RegistryByteBuf buf)
            {
                int count = buf.readInt();
                InkProjectileEntity.ExtraDataList saveDatas = new InkProjectileEntity.ExtraDataList(count);
                for (int i = 0; i < count; i++)
                {
                    Class<? extends ExtraSaveData> saveData = REGISTRY.get(buf.readIdentifier());
                    try
                    {
                        saveDatas.add((ExtraSaveData) saveData.getDeclaredMethod("load", RegistryByteBuf.class).invoke(null, buf));
                    }
                    catch (IllegalAccessException | NoSuchMethodException |
                           InvocationTargetException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                return saveDatas;
            }

            @Override
            public void encode(RegistryByteBuf buf, InkProjectileEntity.ExtraDataList saveData)
            {
                buf.writeInt(saveData.size());
                for (ExtraSaveData data : saveData)
                {
                    buf.writeInt(REGISTRY.getRawId(data.getClass()));
                    data.save(buf);
                }
            }
        };

        @Override
        public PacketCodec<? super RegistryByteBuf, InkProjectileEntity.ExtraDataList> codec()
        {
            return PACKET_CODEC;
        }

        @Override
        public TrackedData<InkProjectileEntity.ExtraDataList> create(int id)
        {
            return TrackedDataHandler.super.create(id);
        }

        @Override
        public @NotNull InkProjectileEntity.ExtraDataList copy(@NotNull InkProjectileEntity.ExtraDataList saveData)
        {
            return new InkProjectileEntity.ExtraDataList(saveData.stream().map(ExtraSaveData::copy).toList());
        }
    };

    static
    {
        Registry.register(REGISTRY, Splatcraft.identifierOf("charge_data"), ChargeExtraData.class);
        Registry.register(REGISTRY, Splatcraft.identifierOf("blaster_explosion_data"), ExplosionExtraData.class);
        Registry.register(REGISTRY, Splatcraft.identifierOf("slosher_data"), SloshExtraData.class);
        Registry.register(REGISTRY, Splatcraft.identifierOf("dualie_data"), DualieExtraData.class);
    }

    public abstract void save(@NotNull RegistryByteBuf buffer);

    public abstract ExtraSaveData load(@NotNull RegistryByteBuf buffer);

    public abstract ExtraSaveData copy();

    public static final class EmptyExtraData extends ExtraSaveData
    {
        public EmptyExtraData()
        {

        }

        @Override
        public void save(@NotNull RegistryByteBuf buffer)
        {

        }

        @Override
        public EmptyExtraData load(@NotNull RegistryByteBuf buffer)
        {
            return new EmptyExtraData();
        }

        @Override
        public EmptyExtraData copy()
        {
            return new EmptyExtraData();
        }
    }

    public static final class ChargeExtraData extends ExtraSaveData
    {
        public final float charge;

        public ChargeExtraData(float charge)
        {
            this.charge = charge;
        }

        @Override
        public void save(@NotNull RegistryByteBuf buffer)
        {
            buffer.writeFloat(charge);
        }

        @Override
        public ChargeExtraData load(@NotNull RegistryByteBuf buffer)
        {
            return new ChargeExtraData(buffer.readFloat());
        }

        @Override
        public ChargeExtraData copy()
        {
            return new ChargeExtraData(charge);
        }
    }

    public static class ExplosionExtraData extends ExtraSaveData
    {
        public final DamageRangesRecord damageCalculator;
        public final DamageRangesRecord sparkDamageCalculator;
        public final float explosionPaint;
        public final boolean newAttackId;

        public ExplosionExtraData(BlasterWeaponSettings.DetonationRecord detonationRecord)
        {
            this(detonationRecord.damageRadiuses(), detonationRecord.sparkDamageRadiuses(), detonationRecord.explosionPaint(), detonationRecord.newAttackId());
        }

        public ExplosionExtraData(DamageRangesRecord damageCalculator, DamageRangesRecord sparkDamageCalculator, float explosionPaint, boolean newAttackId)
        {
            this.damageCalculator = damageCalculator;
            this.sparkDamageCalculator = sparkDamageCalculator;
            this.explosionPaint = explosionPaint;
            this.newAttackId = newAttackId;
        }

        @Override
        public void save(@NotNull RegistryByteBuf buffer)
        {
            damageCalculator.writeToBuffer(buffer);
            sparkDamageCalculator.writeToBuffer(buffer);
            buffer.writeFloat(explosionPaint);
            buffer.writeBoolean(newAttackId);
        }

        public DamageRangesRecord getRadiuses(boolean spark, float multiplier)
        {
            return (spark ? sparkDamageCalculator : damageCalculator).cloneWithMultiplier(1, multiplier);
        }

        @Override
        public ExplosionExtraData load(@NotNull RegistryByteBuf buffer)
        {
            return new ExplosionExtraData(DamageRangesRecord.fromBuffer(buffer), DamageRangesRecord.fromBuffer(buffer), buffer.readFloat(), buffer.readBoolean());
        }

        @Override
        public ExplosionExtraData copy()
        {
            return new ExplosionExtraData(
                new DamageRangesRecord(new TreeMap<>(damageCalculator.damageValues()), damageCalculator.lerpBetween()),
                new DamageRangesRecord(new TreeMap<>(sparkDamageCalculator.damageValues()), sparkDamageCalculator.lerpBetween()),
                explosionPaint, newAttackId);
        }
    }

    public static final class DualieExtraData extends ExtraSaveData
    {
        public final boolean rollBullet;

        public DualieExtraData(boolean rollBullet)
        {
            this.rollBullet = rollBullet;
        }

        @Override
        public void save(@NotNull RegistryByteBuf buffer)
        {
            buffer.writeBoolean(rollBullet);
        }

        @Override
        public DualieExtraData load(@NotNull RegistryByteBuf buffer)
        {
            return new DualieExtraData(buffer.readBoolean());
        }

        @Override
        public DualieExtraData copy()
        {
            return new DualieExtraData(rollBullet);
        }
    }

    public static final class RollerDistanceExtraData extends ExtraSaveData
    {
        public Vector3f spawnPos;

        public RollerDistanceExtraData(Vector3f position)
        {
            spawnPos = position;
        }

        @Override
        public void save(@NotNull RegistryByteBuf buffer)
        {
            buffer.writeVector3f(spawnPos);
        }

        @Override
        public RollerDistanceExtraData load(@NotNull RegistryByteBuf buffer)
        {
            return new RollerDistanceExtraData(buffer.readVector3f());
        }

        @Override
        public RollerDistanceExtraData copy()
        {
            return new RollerDistanceExtraData(spawnPos);
        }
    }

    public static class SloshExtraData extends ExtraSaveData
    {
        public final int sloshDataIndex;
        public final double spawnHeight;

        public SloshExtraData(int sloshDataIndex, double spawnHeight)
        {
            this.sloshDataIndex = sloshDataIndex;
            this.spawnHeight = spawnHeight;
        }

        @Override
        public void save(@NotNull RegistryByteBuf buffer)
        {
            buffer.writeInt(sloshDataIndex);
            buffer.writeDouble(spawnHeight);
        }

        @Override
        public SloshExtraData load(@NotNull RegistryByteBuf buffer)
        {
            return new SloshExtraData(buffer.readInt(), buffer.readDouble());
        }

        @Override
        public SloshExtraData copy()
        {
            return new SloshExtraData(sloshDataIndex, spawnHeight);
        }
    }
}