package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.util.WeaponTooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.splatcraft.items.weapons.settings.CommonRecords.InkUsageDataRecord;
import static net.splatcraft.items.weapons.settings.CommonRecords.ShotDeviationDataRecord;

public class SpecialWeaponSettings<T extends DynamicDataRecord<T>> extends DynamicWeaponSettings<SpecialWeaponSettings<T>, SpecialWeaponSettings.DataRecord, T>
{
	public static final SpecialWeaponSettings<?> DEFAULT = new SpecialWeaponSettings<>("default");
	public T specialDataRecord;
	public DataRecord dataRecord = DataRecord.DEFAULT;
	public SpecialWeaponSettings(String name)
	{
		super(name);
	}
	@Override
	public Map.Entry<String, MapCodec<? extends T>>[] getDynamicCodecs()
	{
		return new Map.Entry[] {
			Map.entry("sting_ray", SubWeaponRecords.ThrowableExplodingSubDataRecord.CODEC)
		};
	}
	@Override
	public T getDynamicDataToSerialize()
	{
		return specialDataRecord;
	}
	@Override
	public DataRecord getDataToSerialize()
	{
		return dataRecord;
	}
	@Override
	protected void processResult(DataRecord dataRecord, T subData)
	{
		this.dataRecord = SplatcraftConvertors.convert(dataRecord);
		specialDataRecord = SplatcraftConvertors.convert(subData);
	}
	@Override
	public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
	{
		return 0;
	}
	@Override
	public List<WeaponTooltip<SpecialWeaponSettings<T>>> tooltipsToRegister()
	{
		List<WeaponTooltip<SpecialWeaponSettings<T>>> weaponTooltips = new ArrayList<>();
		
		weaponTooltips.add(new WeaponTooltip<>("ink_consumption", WeaponTooltip.Metrics.UNITS, settings -> settings.dataRecord.inkUsage().consumption(), WeaponTooltip.RANKER_DESCENDING));
		weaponTooltips.add(new WeaponTooltip<>("ink_recovery", WeaponTooltip.Metrics.UNITS, settings -> settings.dataRecord.inkUsage().recoveryCooldown(), WeaponTooltip.RANKER_DESCENDING));
		specialDataRecord.addTooltips(weaponTooltips);
		return weaponTooltips;
	}
	@Override
	public MapCodec<DataRecord> getMapCodec()
	{
		return DataRecord.CODEC;
	}
	@Override
	public ShotDeviationDataRecord getShotDeviationData(ItemStack stack, LivingEntity entity)
	{
		return ShotDeviationDataRecord.PERFECT_DEFAULT;
	}
	@Override
	public float getSpeedForRender(ClientPlayerEntity player, ItemStack mainHandItem)
	{
		return 0;
	}
	public record DataRecord(
		InkUsageDataRecord inkUsage,
		int holdTime,
		float mobility
	)
	{
		public static final InkUsageDataRecord DEFAULT_INK_USAGE = new InkUsageDataRecord(70, 70);
		public static final MapCodec<DataRecord> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				InkUsageDataRecord.CODEC.optionalFieldOf("ink_usage", DEFAULT_INK_USAGE).forGetter(DataRecord::inkUsage),
				Codec.INT.optionalFieldOf("hold_time", WeaponBaseItem.USE_DURATION).forGetter(DataRecord::holdTime),
				Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::mobility),
				Codec.BOOL.optionalFieldOf("isSecret", false).forGetter(DataRecord::isSecret)
			).apply(inst, DataRecord::new)
		);
		public static final DataRecord DEFAULT = new DataRecord(DEFAULT_INK_USAGE, WeaponBaseItem.USE_DURATION, 1f, false);
	}
}
