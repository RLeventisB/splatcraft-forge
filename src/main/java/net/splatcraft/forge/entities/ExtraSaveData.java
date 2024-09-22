package net.splatcraft.forge.entities;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;
import net.splatcraft.forge.items.weapons.settings.BlasterWeaponSettings;
import net.splatcraft.forge.util.DamageRangesRecord;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.lang.reflect.InvocationTargetException;
import java.util.TreeMap;
import java.util.function.Supplier;

import static net.splatcraft.forge.Splatcraft.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public abstract class ExtraSaveData
{
    public static Supplier<IForgeRegistry<Class<? extends ExtraSaveData>>> REGISTRY;

    @SubscribeEvent
    public static void registerRegistry(final NewRegistryEvent event)
    {
        RegistryBuilder<Class<? extends ExtraSaveData>> registryBuilder = new RegistryBuilder<>();
        registryBuilder.setName(new ResourceLocation(MODID, "extra_save_data"));
        REGISTRY = event.create(registryBuilder, (registry) ->
        {
            registry.register(new ResourceLocation(MODID, "charge_data"), ChargeExtraData.class);
            registry.register(new ResourceLocation(MODID, "blaster_explosion_data"), ExplosionExtraData.class);
            registry.register(new ResourceLocation(MODID, "slosher_data"), SloshExtraData.class);
            registry.register(new ResourceLocation(MODID, "dualie_data"), DualieExtraData.class);
        });
    }

    public static final EntityDataSerializer<InkProjectileEntity.ExtraDataList> SERIALIZER = new EntityDataSerializer<>()
    {
        @Override
        public void write(@NotNull FriendlyByteBuf buf, @NotNull InkProjectileEntity.ExtraDataList saveData)
        {
            buf.writeInt(saveData.size());
            for (ExtraSaveData data : saveData)
            {
                buf.writeResourceLocation(REGISTRY.get().getKey(data.getClass()));
                data.save(buf);
            }
        }

        @Override
        public @NotNull InkProjectileEntity.ExtraDataList read(@NotNull FriendlyByteBuf buf)
        {
            int count = buf.readInt();
            InkProjectileEntity.ExtraDataList saveDatas = new InkProjectileEntity.ExtraDataList(count);
            for (int i = 0; i < count; i++)
            {
                Class<? extends ExtraSaveData> saveData = REGISTRY.get().getValue(buf.readResourceLocation());
                try
                {
                    saveDatas.add((ExtraSaveData) saveData.getDeclaredMethod("load", FriendlyByteBuf.class).invoke(null, buf));
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
        public @NotNull InkProjectileEntity.ExtraDataList copy(@NotNull InkProjectileEntity.ExtraDataList saveData)
        {
            return new InkProjectileEntity.ExtraDataList(saveData.stream().map(ExtraSaveData::copy).toList());
        }
    };

    public abstract void save(@NotNull FriendlyByteBuf buffer);

    public abstract ExtraSaveData load(@NotNull FriendlyByteBuf buffer);

    public abstract ExtraSaveData copy();

    public static final class EmptyExtraData extends ExtraSaveData
    {
        public EmptyExtraData()
        {

        }

        @Override
        public void save(@NotNull FriendlyByteBuf buffer)
        {

        }

        @Override
        public EmptyExtraData load(@NotNull FriendlyByteBuf buffer)
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
        public void save(@NotNull FriendlyByteBuf buffer)
        {
            buffer.writeFloat(charge);
        }

        @Override
        public ChargeExtraData load(@NotNull FriendlyByteBuf buffer)
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
        public final float sparkDamagePenalty;
        public final float explosionPaint;
        public final boolean newAttackId;

        public ExplosionExtraData(BlasterWeaponSettings.DetonationRecord detonationRecord)
        {
            this(detonationRecord.damageRadiuses(), detonationRecord.sparkDamagePenalty(), detonationRecord.explosionPaint(), detonationRecord.newAttackId());
        }

        public ExplosionExtraData(DamageRangesRecord damageCalculator, float sparkDamagePenalty, float explosionPaint, boolean newAttackId)
        {
            this.damageCalculator = damageCalculator;
            this.sparkDamagePenalty = sparkDamagePenalty;
            this.explosionPaint = explosionPaint;
            this.newAttackId = newAttackId;
        }

        @Override
        public void save(@NotNull FriendlyByteBuf buffer)
        {
            damageCalculator.writeToBuffer(buffer);
            buffer.writeFloat(sparkDamagePenalty);
            buffer.writeFloat(explosionPaint);
        }

        @Override
        public ExplosionExtraData load(@NotNull FriendlyByteBuf buffer)
        {
            return new ExplosionExtraData(DamageRangesRecord.fromBuffer(buffer), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean());
        }

        @Override
        public ExplosionExtraData copy()
        {
            return new ExplosionExtraData(new DamageRangesRecord((TreeMap<Float, Float>) damageCalculator.damageValues().clone(), damageCalculator.lerpBetween()), sparkDamagePenalty, explosionPaint, newAttackId);
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
        public void save(@NotNull FriendlyByteBuf buffer)
        {
            buffer.writeBoolean(rollBullet);
        }

        @Override
        public DualieExtraData load(@NotNull FriendlyByteBuf buffer)
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
            this.spawnPos = position;
        }

        @Override
        public void save(@NotNull FriendlyByteBuf buffer)
        {
            buffer.writeVector3f(spawnPos);
        }

        @Override
        public RollerDistanceExtraData load(@NotNull FriendlyByteBuf buffer)
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
        public void save(@NotNull FriendlyByteBuf buffer)
        {
            buffer.writeInt(sloshDataIndex);
            buffer.writeDouble(spawnHeight);
        }

        @Override
        public SloshExtraData load(@NotNull FriendlyByteBuf buffer)
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