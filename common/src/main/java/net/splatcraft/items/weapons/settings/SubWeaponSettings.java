package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.NumberRange;
import net.splatcraft.util.WeaponTooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.splatcraft.items.weapons.settings.CommonRecords.InkUsageDataRecord;
import static net.splatcraft.items.weapons.settings.CommonRecords.ShotDeviationDataRecord;

public class SubWeaponSettings<T extends DynamicDataRecord<T>> extends DynamicWeaponSettings<SubWeaponSettings<T>, SubWeaponSettings.DataRecord, T, ResourceLocation>
{
	public static final SubWeaponSettings<?> DEFAULT = new SubWeaponSettings<>("default");
	public T subDataRecord;
	public DataRecord dataRecord = DataRecord.DEFAULT;
	public SubWeaponSettings(String name)
	{
		super(name);
	}
	@Override
	public Map.Entry<ResourceLocation, MapCodec<? extends T>>[] getDynamicCodecs()
	{
		return new Map.Entry[]{
			Map.entry(Splatcraft.identifierOf("throwable_exploding"), SubWeaponRecords.ThrowableExplodingSubDataRecord.CODEC),
			Map.entry(Splatcraft.identifierOf("burst_bomb"), SubWeaponRecords.BurstBombDataRecord.CODEC),
			Map.entry(Splatcraft.identifierOf("curling_bomb"), SubWeaponRecords.CurlingBombDataRecord.CODEC)
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

		setSecret(dataRecord.isSecret);
		setMoveSpeed(dataRecord.mobility);
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
	public Codec<ResourceLocation> getDynamicCodecKeyCodec()
	{
		return CodecUtils.Codecs.SPLATCRAFT_IDENTIFIER_CODEC;
	}
	@Override
	public ShotDeviationDataRecord getShotDeviationData(ItemStack stack, LivingEntity entity)
	{
		return ShotDeviationDataRecord.PERFECT_DEFAULT;
	}
	@Override
	public float getSpeedForRender(Player player, ItemStack mainHandItem)
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
		NumberRange.FloatRange splashVelocityRange,
		NumberRange.FloatRange splashPitchRange,
		int splashCount,
		float splashPaintRadius,
		float angleRandomness,
		boolean distributeEvenly
	)
	{
		public static final Codec<SplashAroundDataRecord> CODEC = RecordCodecBuilder.create(
			inst -> inst.group(
				NumberRange.FloatRange.CODEC.fieldOf("splash_velocity_range").forGetter(SplashAroundDataRecord::splashVelocityRange),
				NumberRange.FloatRange.CODEC.optionalFieldOf("splash_pitch_range", new NumberRange.FloatRange(19f, 33f)).forGetter(SplashAroundDataRecord::splashPitchRange),
				Codec.INT.fieldOf("splash_count").forGetter(SplashAroundDataRecord::splashCount),
				Codec.FLOAT.fieldOf("splash_paint_radius").forGetter(SplashAroundDataRecord::splashPaintRadius),
				Codec.FLOAT.optionalFieldOf("angle_randomness", 20f).forGetter(SplashAroundDataRecord::angleRandomness),
				Codec.BOOL.optionalFieldOf("distribute_evenly", true).forGetter(SplashAroundDataRecord::distributeEvenly)
			).apply(inst, SplashAroundDataRecord::new)
		);
		public static final SplashAroundDataRecord DEFAULT = new SplashAroundDataRecord(NumberRange.FloatRange.ZERO, NumberRange.FloatRange.ZERO, 0, 0, 20, true);
	}
}
