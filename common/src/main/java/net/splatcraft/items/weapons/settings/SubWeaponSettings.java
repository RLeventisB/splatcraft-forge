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
import net.splatcraft.items.weapons.settings.SubWeaponRecords.SubDataRecord;
import net.splatcraft.util.WeaponTooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.splatcraft.items.weapons.settings.CommonRecords.*;

public class SubWeaponSettings<T extends SubDataRecord<T>> extends DynamicWeaponSettings<SubWeaponSettings<T>, SubWeaponSettings.DataRecord, T>
{
	public static final SubWeaponSettings<?> DEFAULT = new SubWeaponSettings<>("default");
	public T subDataRecord;
	public DataRecord dataRecord = DataRecord.DEFAULT;
	public SubWeaponSettings(String name)
	{
		super(name);
	}
	@Override
	public Map.Entry<String, MapCodec<? extends T>>[] getDynamicCodecs()
	{
		return new Map.Entry[] {
			Map.entry("throwable_exploding", SubWeaponRecords.ThrowableExplodingSubDataRecord.CODEC),
			Map.entry("burst_bomb", SubWeaponRecords.BurstBombDataRecord.CODEC),
			Map.entry("curling_bomb", SubWeaponRecords.CurlingBombDataRecord.CODEC)
		};
	}
	@Override
	public T getDynamicDataToSerialize()
	{
		return subDataRecord;
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
		subDataRecord = SplatcraftConvertors.convert(subData);
	}
	@Override
	public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
	{
		return 0;
	}
	@Override
	public List<WeaponTooltip<SubWeaponSettings<T>>> tooltipsToRegister()
	{
		List<WeaponTooltip<SubWeaponSettings<T>>> weaponTooltips = new ArrayList<>();
		
		weaponTooltips.add(new WeaponTooltip<>("ink_consumption", WeaponTooltip.Metrics.UNITS, settings -> settings.dataRecord.inkUsage().consumption(), WeaponTooltip.RANKER_DESCENDING));
		weaponTooltips.add(new WeaponTooltip<>("ink_recovery", WeaponTooltip.Metrics.UNITS, settings -> settings.dataRecord.inkUsage().recoveryCooldown(), WeaponTooltip.RANKER_DESCENDING));
		subDataRecord.addTooltips(weaponTooltips);
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
		float mobility,
		boolean isSecret
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
	public record SplashAroundDataRecord(
		FloatRange splashVelocityRange,
		FloatRange splashPitchRange,
		int splashCount,
		float splashPaintRadius,
		float angleRandomness,
		boolean distributeEvenly
	)
	{
		public static final Codec<SplashAroundDataRecord> CODEC = RecordCodecBuilder.create(
			inst -> inst.group(
				FloatRange.CODEC.fieldOf("splash_velocity_range").forGetter(SplashAroundDataRecord::splashVelocityRange),
				FloatRange.CODEC.optionalFieldOf("splash_pitch_range", new FloatRange(19, 33)).forGetter(SplashAroundDataRecord::splashPitchRange),
				Codec.INT.fieldOf("splash_count").forGetter(SplashAroundDataRecord::splashCount),
				Codec.FLOAT.fieldOf("splash_paint_radius").forGetter(SplashAroundDataRecord::splashPaintRadius),
				Codec.FLOAT.optionalFieldOf("angle_randomness", 20f).forGetter(SplashAroundDataRecord::angleRandomness),
				Codec.BOOL.optionalFieldOf("distribute_evenly", true).forGetter(SplashAroundDataRecord::distributeEvenly)
			).apply(inst, SplashAroundDataRecord::new)
		);
		public static final SplashAroundDataRecord DEFAULT = new SplashAroundDataRecord(FloatRange.ZERO, FloatRange.ZERO, 0, 0, 20, true);
	}
}
