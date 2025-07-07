package net.splatcraft.entities;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceKey;
import net.splatcraft.Splatcraft;
import net.splatcraft.items.weapons.settings.BlasterWeaponSettings;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.structs.RangedValueCollection;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.lang.reflect.InvocationTargetException;

public abstract class ExtraSaveData
{
	public static MappedRegistry<Class<? extends ExtraSaveData>> REGISTRY = new MappedRegistry<>(ResourceKey.createRegistryKey(Splatcraft.identifierOf("extra_save_data")), Lifecycle.stable());
	public static final EntityDataSerializer<InkProjectileEntity.ExtraDataList> SERIALIZER = new EntityDataSerializer<>()
	{
		private static final StreamCodec<? super RegistryFriendlyByteBuf, InkProjectileEntity.ExtraDataList> PACKET_CODEC = new StreamCodec<>()
		{
			@Override
			public InkProjectileEntity.@NotNull ExtraDataList decode(RegistryFriendlyByteBuf buf)
			{
				int count = buf.readInt();
				InkProjectileEntity.ExtraDataList saveDatas = new InkProjectileEntity.ExtraDataList(count);
				for (int i = 0; i < count; i++)
				{
					Class<? extends ExtraSaveData> saveData = REGISTRY.get(buf.readResourceLocation());
					try
					{
						saveDatas.add((ExtraSaveData) saveData.getDeclaredMethod("load", RegistryFriendlyByteBuf.class).invoke(null, buf));
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
			public void encode(RegistryFriendlyByteBuf buf, InkProjectileEntity.ExtraDataList saveData)
			{
				buf.writeInt(saveData.size());
				for (ExtraSaveData data : saveData)
				{
					buf.writeInt(REGISTRY.getId(data.getClass()));
					data.save(buf);
				}
			}
		};
		@Override
		public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, InkProjectileEntity.ExtraDataList> codec()
		{
			return PACKET_CODEC;
		}
		@Override
		public @NotNull EntityDataAccessor<InkProjectileEntity.ExtraDataList> createAccessor(int id)
		{
			return EntityDataSerializer.super.createAccessor(id);
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
		Registry.register(REGISTRY, Splatcraft.identifierOf("splatling_data"), SplatlingExtraData.class);
		Registry.register(REGISTRY, Splatcraft.identifierOf("blaster_explosion_data"), ExplosionExtraData.class);
		Registry.register(REGISTRY, Splatcraft.identifierOf("slosher_data"), SloshExtraData.class);
		Registry.register(REGISTRY, Splatcraft.identifierOf("dualie_data"), DualieExtraData.class);
	}
	public abstract void save(@NotNull RegistryFriendlyByteBuf buffer);
	public abstract ExtraSaveData load(@NotNull RegistryFriendlyByteBuf buffer);
	public abstract ExtraSaveData copy();
	public static final class EmptyExtraData extends ExtraSaveData
	{
		public EmptyExtraData()
		{
		
		}
		@Override
		public void save(@NotNull RegistryFriendlyByteBuf buffer)
		{
		
		}
		@Override
		public EmptyExtraData load(@NotNull RegistryFriendlyByteBuf buffer)
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
		public void save(@NotNull RegistryFriendlyByteBuf buffer)
		{
			buffer.writeFloat(charge);
		}
		@Override
		public ChargeExtraData load(@NotNull RegistryFriendlyByteBuf buffer)
		{
			return new ChargeExtraData(buffer.readFloat());
		}
		@Override
		public ChargeExtraData copy()
		{
			return new ChargeExtraData(charge);
		}
	}
	public static final class SplatlingExtraData extends ExtraSaveData
	{
		public final float dataIndex;
		public SplatlingExtraData(float dataIndex)
		{
			this.dataIndex = dataIndex;
		}
		@Override
		public void save(@NotNull RegistryFriendlyByteBuf buffer)
		{
			buffer.writeFloat(dataIndex);
		}
		@Override
		public SplatlingExtraData load(@NotNull RegistryFriendlyByteBuf buffer)
		{
			return new SplatlingExtraData(buffer.readFloat());
		}
		@Override
		public SplatlingExtraData copy()
		{
			return new SplatlingExtraData(dataIndex);
		}
	}
	public static class ExplosionExtraData extends ExtraSaveData
	{
		public final RangedValueCollection damageCalculator;
		public final RangedValueCollection sparkDamageCalculator;
		public final float explosionPaint;
		public final boolean newAttackId;
		public ExplosionExtraData(BlasterWeaponSettings.DetonationRecord detonationRecord)
		{
			this(detonationRecord.damageRadiuses(), detonationRecord.sparkDamageRadiuses(), detonationRecord.explosionPaint(), detonationRecord.newAttackId());
		}
		public ExplosionExtraData(RangedValueCollection damageCalculator, RangedValueCollection sparkDamageCalculator, float explosionPaint, boolean newAttackId)
		{
			this.damageCalculator = damageCalculator;
			this.sparkDamageCalculator = sparkDamageCalculator;
			this.explosionPaint = explosionPaint;
			this.newAttackId = newAttackId;
		}
		@Override
		public void save(@NotNull RegistryFriendlyByteBuf buffer)
		{
			RangedValueCollection.STREAM_CODEC.encode(buffer, damageCalculator);
			RangedValueCollection.STREAM_CODEC.encode(buffer, sparkDamageCalculator);
			buffer.writeFloat(explosionPaint);
			buffer.writeBoolean(newAttackId);
		}
		public RangedValueCollection getRadiuses(boolean spark, float multiplier)
		{
			return (spark ? sparkDamageCalculator : damageCalculator).cloneWithMultiplier(1, multiplier);
		}
		@Override
		public ExplosionExtraData load(@NotNull RegistryFriendlyByteBuf buffer)
		{
			return new ExplosionExtraData(RangedValueCollection.STREAM_CODEC.decode(buffer), RangedValueCollection.STREAM_CODEC.decode(buffer), buffer.readFloat(), buffer.readBoolean());
		}
		@Override
		public ExplosionExtraData copy()
		{
			return new ExplosionExtraData(
				damageCalculator.cloneWithMultiplier(1, 1),
				sparkDamageCalculator.cloneWithMultiplier(1, 1),
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
		public void save(@NotNull RegistryFriendlyByteBuf buffer)
		{
			buffer.writeBoolean(rollBullet);
		}
		@Override
		public DualieExtraData load(@NotNull RegistryFriendlyByteBuf buffer)
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
		public final Vector3f spawnPos;
		public final boolean wasAirborneOnShoot;
		public final boolean weakBullet;
		public RollerDistanceExtraData(Vector3f position, boolean[] flags)
		{
			spawnPos = position;
			wasAirborneOnShoot = flags[0];
			weakBullet = flags[1];
		}
		public RollerDistanceExtraData(Vector3f position, boolean wasAirborneOnShoot, boolean isWeak)
		{
			spawnPos = position;
			this.wasAirborneOnShoot = wasAirborneOnShoot;
			weakBullet = isWeak;
		}
		@Override
		public void save(@NotNull RegistryFriendlyByteBuf buffer)
		{
			buffer.writeVector3f(spawnPos);
			CommonUtils.writeBooleansCompact(buffer, wasAirborneOnShoot, weakBullet);
		}
		@Override
		public RollerDistanceExtraData load(@NotNull RegistryFriendlyByteBuf buffer)
		{
			return new RollerDistanceExtraData(buffer.readVector3f(), CommonUtils.readBooleansCompact(buffer, 2));
		}
		@Override
		public RollerDistanceExtraData copy()
		{
			return new RollerDistanceExtraData(spawnPos, wasAirborneOnShoot, weakBullet);
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
		public void save(@NotNull RegistryFriendlyByteBuf buffer)
		{
			buffer.writeInt(sloshDataIndex);
			buffer.writeDouble(spawnHeight);
		}
		@Override
		public SloshExtraData load(@NotNull RegistryFriendlyByteBuf buffer)
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