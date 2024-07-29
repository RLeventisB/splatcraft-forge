package net.splatcraft.forge.entities;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;
import org.jetbrains.annotations.NotNull;

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
					saveDatas.add((saveData.newInstance()).load(buf));
				}
				catch (InstantiationException | IllegalAccessException e)
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
		public float charge;
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
		public float explosionRadius;
		public float maxIndirectDamage;
		public float sparkDamagePenalty;
		public float explosionPaint;
		public ExplosionExtraData(float explosionRadius, float maxIndirectDamage, float sparkDamagePenalty, float explosionPaint)
		{
			this.explosionRadius = explosionRadius;
			this.maxIndirectDamage = maxIndirectDamage;
			this.sparkDamagePenalty = sparkDamagePenalty;
			this.explosionPaint = explosionPaint;
		}
		@Override
		public void save(@NotNull FriendlyByteBuf buffer)
		{
			buffer.writeFloat(explosionRadius);
			buffer.writeFloat(maxIndirectDamage);
			buffer.writeFloat(sparkDamagePenalty);
			buffer.writeFloat(explosionPaint);
		}
		@Override
		public ExplosionExtraData load(@NotNull FriendlyByteBuf buffer)
		{
			return new ExplosionExtraData(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
		}
		@Override
		public ExplosionExtraData copy()
		{
			return new ExplosionExtraData(explosionRadius, maxIndirectDamage, sparkDamagePenalty, explosionPaint);
		}
	}
	public static final class DualieExtraData extends ExtraSaveData
	{
		public boolean rollBullet;
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
	public static class SloshExtraData extends ExtraSaveData
	{
		public int sloshDataIndex;
		public SloshExtraData(int sloshDataIndex)
		{
			this.sloshDataIndex = sloshDataIndex;
		}
		@Override
		public void save(@NotNull FriendlyByteBuf buffer)
		{
			buffer.writeInt(sloshDataIndex);
		}
		@Override
		public SloshExtraData load(@NotNull FriendlyByteBuf buffer)
		{
			return new SloshExtraData(buffer.readInt());
		}
		@Override
		public SloshExtraData copy()
		{
			return new SloshExtraData(sloshDataIndex);
		}
	}
}