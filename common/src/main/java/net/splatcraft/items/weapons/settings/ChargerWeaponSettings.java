package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.util.WeaponTooltip;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ChargerWeaponSettings extends AbstractWeaponSettings<ChargerWeaponSettings, ChargerWeaponSettings.DataRecord>
{
	public static final ChargerWeaponSettings DEFAULT = new ChargerWeaponSettings("default");
	public ChargerProjectileDataRecord projectileData = ChargerProjectileDataRecord.DEFAULT;
	public ShotDataRecord shotData = ShotDataRecord.DEFAULT;
	public ChargeDataRecord chargeData = ChargeDataRecord.DEFAULT;
	public boolean bypassesMobDamage = false;
	public ChargerWeaponSettings(String name)
	{
		super(name);
	}
	@Override
	public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
	{
		ExtraSaveData.ChargeExtraData chargeData = list.getFirstExtraData(ExtraSaveData.ChargeExtraData.class);
		if (chargeData != null)
		{
			return projectileData.damage.getValue(chargeData.charge);
		}
		return projectileData.damage.minValue;
	}
	@Override
	public List<WeaponTooltip<ChargerWeaponSettings>> tooltipsToRegister()
	{
		return List.of(
			new WeaponTooltip<>("range", WeaponTooltip.Metrics.BLOCKS, settings -> settings.projectileData.range.fullValue, WeaponTooltip.RANKER_ASCENDING),
			new WeaponTooltip<>("charge_speed", WeaponTooltip.Metrics.SECONDS, settings -> settings.chargeData.chargeTime() / 20f, WeaponTooltip.RANKER_DESCENDING),
			new WeaponTooltip<>("mobility", WeaponTooltip.Metrics.MULTIPLIER, settings -> settings.moveSpeed, WeaponTooltip.RANKER_ASCENDING)
		);
	}
	@Override
	public Codec<DataRecord> getCodec()
	{
		return DataRecord.CODEC;
	}
	@Override
	public CommonRecords.ShotDeviationDataRecord getShotDeviationData(ItemStack stack, LivingEntity entity)
	{
		return CommonRecords.ShotDeviationDataRecord.PERFECT_DEFAULT;
	}
	@Override
	public void processData(DataRecord data)
	{
		projectileData = SplatcraftConvertors.convert(data.projectile);
		shotData = SplatcraftConvertors.convert(data.shot);
		chargeData = SplatcraftConvertors.convert(data.charge);
		
		setMoveSpeed(data.mobility);
		setSecret(data.isSecret);
		setBypassesMobDamage(data.fullDamageToMobs);
	}
	@Override
	public DataRecord getDataToSerialize()
	{
		return new DataRecord(projectileData,
			shotData,
			chargeData,
			moveSpeed,
			bypassesMobDamage, isSecret);
	}
	@Override
	public float getSpeedForRender(Player player, ItemStack mainHandItem)
	{
		return 0;
	}
	public ChargerWeaponSettings setBypassesMobDamage(boolean bypassesMobDamage)
	{
		this.bypassesMobDamage = bypassesMobDamage;
		return this;
	}
	public record DataRecord(
		ChargerProjectileDataRecord projectile,
		ShotDataRecord shot,
		ChargeDataRecord charge,
		float mobility,
		boolean fullDamageToMobs,
		boolean isSecret
	)
	{
		public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				ChargerProjectileDataRecord.CODEC.fieldOf("projectile").forGetter(DataRecord::projectile),
				ShotDataRecord.CODEC.fieldOf("shot").forGetter(DataRecord::shot),
				ChargeDataRecord.CODEC.fieldOf("charge").forGetter(DataRecord::charge),
				Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::mobility),
				Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::fullDamageToMobs),
				Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
			).apply(instance, DataRecord::new)
		);
	}
	public record ChargerProjectileDataRecord(
		float size,
		ChargeValueRecord speed,
		ChargeValueRecord range,
		ChargeValueRecord inkCoverageImpact,
		ChargeValueRecord inkDropCoverage,
		ChargeValueRecord distanceBetweenInkDrops,
		ChargeValueRecord damage,
		float piercesAtCharge
	)
	{
		public static final Codec<ChargerProjectileDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.fieldOf("size").forGetter(ChargerProjectileDataRecord::size),
				ChargeValueRecord.CODEC.fieldOf("speed").forGetter(ChargerProjectileDataRecord::speed),
				ChargeValueRecord.CODEC.fieldOf("range").forGetter(ChargerProjectileDataRecord::range),
				ChargeValueRecord.CODEC.optionalFieldOf("ink_drop_coverage").forGetter(v -> Optional.of(v.inkDropCoverage)),
				ChargeValueRecord.CODEC.optionalFieldOf("ink_coverage_on_impact").forGetter(v -> Optional.of(v.inkCoverageImpact)),
				ChargeValueRecord.CODEC.optionalFieldOf("distance_between_drops", ChargeValueRecord.create(4)).forGetter(ChargerProjectileDataRecord::distanceBetweenInkDrops),
				ChargeValueRecord.CODEC.fieldOf("damage").forGetter(ChargerProjectileDataRecord::damage),
				Codec.FLOAT.optionalFieldOf("pierces_at_charge", 1f).forGetter(ChargerProjectileDataRecord::piercesAtCharge)
			).apply(instance, ChargerProjectileDataRecord::create)
		);
		public static final ChargerProjectileDataRecord DEFAULT = new ChargerProjectileDataRecord(0, ChargeValueRecord.DEFAULT, ChargeValueRecord.DEFAULT, ChargeValueRecord.DEFAULT, ChargeValueRecord.DEFAULT, ChargeValueRecord.DEFAULT, ChargeValueRecord.DEFAULT, 2f);
		public static ChargerProjectileDataRecord create(float size,
		                                                 ChargeValueRecord speed,
		                                                 ChargeValueRecord range,
		                                                 Optional<ChargeValueRecord> inkCoverageImpact,
		                                                 Optional<ChargeValueRecord> inkDropCoverage,
		                                                 ChargeValueRecord distanceBetweenInkDrops,
		                                                 ChargeValueRecord damage,
		                                                 float piercesAtCharge
		)
		{
			return new ChargerProjectileDataRecord(
				size,
				speed,
				range,
				inkCoverageImpact.orElse(ChargeValueRecord.create(size * 0.85f)),
				inkDropCoverage.orElse(ChargeValueRecord.create(size * 1.1f)),
				distanceBetweenInkDrops,
				damage,
				piercesAtCharge
			);
		}
	}
	public record ChargeDataRecord(
		int minChargeTime,
		int chargeTime,
		float airborneChargeRate,
		float emptyTankChargeRate,
		int chargeStorageTime
	)
	{
		public static final Codec<ChargeDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("min_charge_time_ticks", 8).forGetter(ChargeDataRecord::minChargeTime),
				Codec.intRange(1, Integer.MAX_VALUE).fieldOf("charge_time_ticks").forGetter(ChargeDataRecord::chargeTime),
				Codec.floatRange(0, 1).optionalFieldOf("airborne_charge_rate", 1f / 3).forGetter(ChargeDataRecord::airborneChargeRate),
				Codec.floatRange(0, 1).optionalFieldOf("empty_tank_charge_rate", 1f / 3).forGetter(ChargeDataRecord::emptyTankChargeRate),
				Codec.INT.optionalFieldOf("charge_storage_ticks", 25).forGetter(ChargeDataRecord::chargeStorageTime)
			).apply(instance, ChargeDataRecord::new)
		);
		public static final ChargeDataRecord DEFAULT = new ChargeDataRecord(8, 30, 1f / 3, 1f / 3, 25);
		public float getChargePercentPerTick()
		{
			return 1f / chargeTime;
		}
	}
	public record ShotDataRecord(
		int endlagTicks,
		ChargeValueRecord inkConsumption,
		float inkRecoveryCooldown,
		int shotsCount
	
	)
	{
		public static final Codec<ShotDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.INT.fieldOf("endlag_ticks").forGetter(ShotDataRecord::endlagTicks),
				ChargeValueRecord.CODEC.fieldOf("ink_consumption").forGetter(ShotDataRecord::inkConsumption),
				Codec.FLOAT.fieldOf("ink_recovery_cooldown").forGetter(ShotDataRecord::inkRecoveryCooldown),
				Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("shots_after_charge", 1).forGetter(ShotDataRecord::shotsCount)
			).apply(instance, ShotDataRecord::new)
		);
		public static final ShotDataRecord DEFAULT = new ShotDataRecord(10, ChargeValueRecord.DEFAULT, 25, 1);
	}
	public record ChargeValueRecord(float minValue, float maxValue, float fullValue)
	{
		public static final Codec<ChargeValueRecord> OBJECT_PAIR_CODEC = RecordCodecBuilder.create(
			inst -> inst.group(
				Codec.FLOAT.fieldOf("min").forGetter(ChargeValueRecord::minValue),
				Codec.FLOAT.fieldOf("max").forGetter(ChargeValueRecord::maxValue),
				Codec.FLOAT.optionalFieldOf("full").forGetter(v -> Optional.of(v.fullValue))
			).apply(inst, (Float minValue, Float maxValue, Optional<Float> fullValue)
				-> new ChargeValueRecord(minValue, maxValue, fullValue.orElse(maxValue)))
		);
		public static final Codec<ChargeValueRecord> LIST_PAIR_CODEC =
			Codec.list(Codec.FLOAT, 1, 3).comapFlatMap(ChargeValueRecord::fromList, v -> List.of(v.minValue, v.maxValue, v.fullValue));
		public static final Codec<ChargeValueRecord> CODEC = Codec.withAlternative(LIST_PAIR_CODEC, OBJECT_PAIR_CODEC);
		public static final ChargeValueRecord DEFAULT = new ChargeValueRecord(1, 1, 1);
		public static DataResult<ChargeValueRecord> fromList(List<Float> values)
		{
			if (values.isEmpty())
				return DataResult.error(() -> "Not enough values was providen by the list.");
			
			if (values.size() == 1)
				return DataResult.success(new ChargeValueRecord(values.get(0), values.get(0), values.get(0)));
			
			if (values.size() == 2)
				return DataResult.success(new ChargeValueRecord(values.get(0), values.get(1), values.get(1)));
			
			return DataResult.success(new ChargeValueRecord(values.get(0), values.get(1), values.get(2)));
		}
		public static ChargeValueRecord create(float... values)
		{
			return switch (values.length)
			{
				case 0 -> DEFAULT;
				case 1 -> new ChargeValueRecord(values[0], values[0], values[0]);
				case 2 -> new ChargeValueRecord(values[0], values[1], values[1]);
				default -> new ChargeValueRecord(values[0], values[1], values[2]);
			};
		}
		public float getValue(float charge)
		{
			return charge >= 1 ? fullValue : Mth.lerp(charge, minValue, maxValue);
		}
		public ChargeValueRecord map(Function<Float, Float> function)
		{
			return new ChargeValueRecord(function.apply(minValue), function.apply(maxValue), function.apply(fullValue));
		}
	}
}
