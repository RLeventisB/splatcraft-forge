package net.splatcraft.items.weapons.settings;

import com.mojang.datafixers.util.Function5;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.util.WeaponTooltip;

public class SubWeaponSettings extends AbstractWeaponSettings<SubWeaponSettings, SubWeaponSettings.BaseSubRecord>
{
	public static final SubWeaponSettings DEFAULT = new SubWeaponSettings("default");
	public BaseSubRecord subDataRecord = BaseSubRecord.DEFAULT;
	public SubWeaponSettings(String name)
	{
		super(name);
	}
	public static <T> Codec<BaseSubRecord<T>> createSubCodec(Codec<T> subDataCodec, Function5<T, CommonRecords.InkUsageDataRecord, Integer, Float, Boolean, BaseSubRecord<T>> constructor)
	{
		return RecordCodecBuilder.create(
			instance -> instance.group(
				subDataCodec.fieldOf("sub_data").forGetter(BaseSubRecord::subData),
				CommonRecords.InkUsageDataRecord.CODEC.optionalFieldOf("ink_usage", BaseSubRecord.DEFAULT_INK_USAGE).forGetter(BaseSubRecord::inkUsage),
				Codec.INT.optionalFieldOf("hold_time", WeaponBaseItem.USE_DURATION).forGetter(BaseSubRecord::holdTime),
				Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(BaseSubRecord::mobility),
				Codec.BOOL.optionalFieldOf("isSecret", false).forGetter(BaseSubRecord::isSecret)
			).apply(instance, constructor)
		);
	}
	@Override
	public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
	{
		return subDataRecord.directDamage;
	}
	@Override
	public WeaponTooltip<SubWeaponSettings>[] tooltipsToRegister()
	{
		return new WeaponTooltip[]
			{
				new WeaponTooltip<SubWeaponSettings>("direct_damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.subDataRecord.directDamage, WeaponTooltip.RANKER_ASCENDING),
				new WeaponTooltip<SubWeaponSettings>("splash_damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.subDataRecord.damageRanges.getMaxRegisteredDamage(), WeaponTooltip.RANKER_ASCENDING),
				new WeaponTooltip<SubWeaponSettings>("ink_consumption", WeaponTooltip.Metrics.UNITS, settings -> settings.subDataRecord.inkConsumption, WeaponTooltip.RANKER_DESCENDING)
			};
	}
	@Override
	public Codec<BaseSubRecord> getCodec()
	{
		return BaseSubRecord.CODEC;
	}
	@Override
	public CommonRecords.ShotDeviationDataRecord getShotDeviationData(ItemStack stack, LivingEntity entity)
	{
		return CommonRecords.ShotDeviationDataRecord.PERFECT_DEFAULT;
	}
	@Override
	public void deserialize(BaseSubRecord data)
	{
		subDataRecord = SplatcraftConvertors.convert(data);
	}
	@Override
	public BaseSubRecord serialize()
	{
		return subDataRecord;
	}
	@Override
	public float getSpeedForRender(ClientPlayerEntity player, ItemStack mainHandItem)
	{
		return 0;
	}
	public record BaseSubRecord<T>(
		T subData,
		CommonRecords.InkUsageDataRecord inkUsage,
		int holdTime,
		float mobility,
		boolean isSecret
	)
	{
		public static final CommonRecords.InkUsageDataRecord DEFAULT_INK_USAGE = new CommonRecords.InkUsageDataRecord(70, 70);
		public static final BaseSubRecord DEFAULT = new BaseSubRecord(null, DEFAULT_INK_USAGE, WeaponBaseItem.USE_DURATION, 1f, false);
	}
	public record CurlingDataRecord(
		int cookTime,
		float contactDamage
	)
	{
		public static final Codec<CurlingDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.INT.optionalFieldOf("cook_time", 180).forGetter(CurlingDataRecord::cookTime),
				Codec.FLOAT.optionalFieldOf("contact_damage", 5f).forGetter(CurlingDataRecord::contactDamage)
			).apply(instance, CurlingDataRecord::new)
		);
		public static final CurlingDataRecord DEFAULT = new CurlingDataRecord(180, 5);
	}
}
