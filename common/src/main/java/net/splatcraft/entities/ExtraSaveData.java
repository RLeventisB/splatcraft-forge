package net.splatcraft.entities;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.splatcraft.Splatcraft;
import net.splatcraft.items.weapons.settings.BlasterWeaponSettings;
import net.splatcraft.util.structs.RangedValueCollection;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;

public abstract class ExtraSaveData
{
	public static MappedRegistry<Class<? extends ExtraSaveData>> REGISTRY = new MappedRegistry<>(ResourceKey.createRegistryKey(Splatcraft.identifierOf("extra_save_data")), Lifecycle.stable());
	public static final EntityDataSerializer<InkProjectileEntity.ExtraDataList> SERIALIZER = new EntityDataSerializer<>()
	{
		private static final StreamCodec<RegistryFriendlyByteBuf, InkProjectileEntity.ExtraDataList> STREAM_CODEC = new StreamCodec<>()
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
			return STREAM_CODEC;
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
		Registry.register(REGISTRY, Splatcraft.identifierOf("blaster_explosion_data"), ExplosionExtraData.class);
		Registry.register(REGISTRY, Splatcraft.identifierOf("impact_sound_data"), ImpactSoundExtraData.class);
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
	public static final class ImpactSoundExtraData extends ExtraSaveData
	{
		public final SoundEvent sound;
		public ImpactSoundExtraData(SoundEvent sound)
		{
			this.sound = sound;
		}
		@Override
		public void save(@NotNull RegistryFriendlyByteBuf buffer)
		{
			SoundEvent.DIRECT_STREAM_CODEC.encode(buffer, sound);
		}
		@Override
		public ImpactSoundExtraData load(@NotNull RegistryFriendlyByteBuf buffer)
		{
			return new ImpactSoundExtraData(SoundEvent.DIRECT_STREAM_CODEC.decode(buffer));
		}
		@Override
		public ImpactSoundExtraData copy()
		{
			return new ImpactSoundExtraData(sound);
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
}