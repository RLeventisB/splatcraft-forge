package net.splatcraft.items.weapons.settings;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.entities.InkProjectileEntity.ExtraDataList;
import net.splatcraft.items.weapons.settings.CommonRecords.ProjectileDataRecord;
import net.splatcraft.items.weapons.settings.CommonRecords.ShotDeviationDataRecord;
import net.splatcraft.items.weapons.settings.SplatlingWeaponSettings.DataRecord;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.util.WeaponTooltip;
import net.splatcraft.util.WeaponTooltip.Metrics;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static net.splatcraft.entities.ExtraSaveData.SplatlingExtraData;
import static net.splatcraft.items.weapons.settings.CommonRecords.OptionalProjectileDataRecord;
import static net.splatcraft.items.weapons.settings.CommonRecords.OptionalShotDeviationDataRecord;

public class SplatlingWeaponSettings<T extends DynamicDataRecord<T>> extends DynamicWeaponSettings<SplatlingWeaponSettings<T>, DataRecord, T, SplatlingWeaponSettings.ShotDataSelectorType>
{
	public static final Class<? extends AbstractWeaponSettings<?, ?>> CLASS = getClassCasted();
	private static Class<? extends AbstractWeaponSettings<?, ?>> getClassCasted()
	{
		try
		{
			return (Class<? extends AbstractWeaponSettings<?, ?>>) Class.forName("net.splatcraft.items.weapons.settings.SplatlingWeaponSettings");
		}
		catch (ClassNotFoundException e)
		{
			return null;
		}
	}
	public static final SplatlingWeaponSettings DEFAULT = new SplatlingWeaponSettings("default");
	public ProjectileDataRecord[] projectileDatas = new ProjectileDataRecord[] {ProjectileDataRecord.DEFAULT};
	public SplatlingShotDataRecord[] shotDatas = new SplatlingShotDataRecord[] {SplatlingShotDataRecord.DEFAULT};
	public Optional<OptionalProjectileDataRecord> secondLevelProjectileMods = Optional.empty(), fullLevelProjectileMods = Optional.empty();
	public Optional<OptionalSplatlingShotDataRecord> secondLevelShotMods = Optional.empty(), fullLevelShotMods = Optional.empty();
	public ChargeDataRecord chargeData = ChargeDataRecord.DEFAULT;
	public boolean bypassesMobDamage = false;
	public float inkConsumption;
	public float inkRecoveryCooldown;
	public T shotSelectionData;
	public SplatlingWeaponSettings(String name)
	{
		super(name);
	}
	@Override
	public Map.Entry<ShotDataSelectorType, MapCodec<? extends T>>[] getDynamicCodecs()
	{
		return new Map.Entry[] {
			Map.entry(ShotDataSelectorType.TIME_USED, TimeUsedSelectionData.CODEC)
		};
	}
	@Override
	protected MapCodec<DataRecord> getMapCodec()
	{
		return DataRecord.MAP_CODEC;
	}
	@Override
	public String getDynamicCodecKeyName()
	{
		return "shot_data_selection_type";
	}
	@Override
	public MapCodec<ShotDataSelectorType> getFieldOfDynamicKey()
	{
		return getDynamicCodecKeyCodec().optionalFieldOf(getDynamicCodecKeyName(), ShotDataSelectorType.STATIC);
	}
	@Override
	public Codec<ShotDataSelectorType> getDynamicCodecKeyCodec()
	{
		return ShotDataSelectorType.CODEC;
	}
	@Override
	public T getDynamicDataToSerialize()
	{
		return shotSelectionData;
	}
	private static float calculateSplatlingAproxRange(ProjectileDataRecord projSettings, float speed)
	{
		return calculateAproximateRange(projSettings.straightShotTicks(), projSettings.horizontalDrag(), speed, projSettings.delaySpeedMult(), projSettings.lifeTicks());
	}
	@Override
	public float calculateDamage(InkProjectileEntity projectile, ExtraDataList list)
	{
		SplatlingExtraData data = list.getFirstExtraData(SplatlingExtraData.class);
		if (data == null) // oh no
			return 0;
		ProjectileDataRecord projectileData = interpolateData(data.dataIndex).getFirst();
		return projectile.calculateDamageDecay(projectileData.baseDamage(), projectileData.damageDecayStartTick(), projectileData.damageDecayPerTick(), projectileData.minDamage());
	}
	@Override
	public List<WeaponTooltip<SplatlingWeaponSettings<T>>> tooltipsToRegister()
	{
		return List.of(
			new WeaponTooltip<>("range", Metrics.BLOCKS, settings ->
				IntStream.range(0, 3).mapToObj(i -> calculateSplatlingAproxRange(settings.projectileDatas[i], settings.shotDatas[i].projectileSpeed)).max(Float::compareTo).get(), WeaponTooltip.RANKER_ASCENDING),
			new WeaponTooltip<>("charge_speed", Metrics.SECONDS, settings -> (settings.chargeData.firstChargeTime + settings.chargeData.secondChargeTime) / 20f, WeaponTooltip.RANKER_DESCENDING),
			new WeaponTooltip<>("mobility", Metrics.MULTIPLIER, settings -> settings.getMoveSpeed(true, 0), WeaponTooltip.RANKER_ASCENDING)
		);
	}
	@Override
	public ShotDeviationDataRecord getShotDeviationData(ItemStack stack, LivingEntity entity)
	{
		return getShotData(
			stack.get(SplatcraftComponents.CHARGE_DATA).charge(),
			stack.get(SplatcraftComponents.SPLATLING_FIRING_DATA).shotTypeData()
		).getSecond().accuracyData();
	}
	@Override
	protected void processResult(DataRecord data, T subData)
	{
		secondLevelShotMods = data.secondChargeShot.map(SplatcraftConvertors::convert);
		fullLevelShotMods = data.fullChargeShot.map(SplatcraftConvertors::convert);
		secondLevelProjectileMods = data.secondChargeProjectile.map(SplatcraftConvertors::convert);
		fullLevelProjectileMods = data.fullChargeProjectile.map(SplatcraftConvertors::convert);
		
		ProjectileDataRecord secondChargeProjectile = OptionalProjectileDataRecord.mergeWithBase(data.secondChargeProjectile, data.baseProjectile);
		SplatlingShotDataRecord secondChargeShot = OptionalSplatlingShotDataRecord.mergeWithBase(data.secondChargeShot, data.baseShot);
		
		projectileDatas = new ProjectileDataRecord[] {
			SplatcraftConvertors.convert(data.baseProjectile),
			SplatcraftConvertors.convert(secondChargeProjectile),
			SplatcraftConvertors.convert(OptionalProjectileDataRecord.mergeWithBase(data.fullChargeProjectile, secondChargeProjectile))
		};
		shotDatas = new SplatlingShotDataRecord[] {
			SplatcraftConvertors.convert(data.baseShot),
			SplatcraftConvertors.convert(secondChargeShot),
			SplatcraftConvertors.convert(OptionalSplatlingShotDataRecord.mergeWithBase(data.fullChargeShot, secondChargeShot))
		};
		
		setSecret(data.isSecret);
		setBypassesMobDamage(data.bypassesMobDamage);
		shotSelectionData = SplatcraftConvertors.convert(subData);
		chargeData = SplatcraftConvertors.convert(data.charge);
		setInkConsumption(data.inkConsumption);
		setInkRecoveryCooldown(SplatcraftConvertors.SkipConverting ? data.inkRecoveryCooldown : (data.inkRecoveryCooldown / SplatcraftConvertors.SplatoonFramesPerMinecraftTick));
	}
	@Override
	public DataRecord getDataToSerialize()
	{
		return new DataRecord(projectileDatas[0],
			shotDatas[0],
			secondLevelProjectileMods,
			secondLevelShotMods,
			fullLevelProjectileMods,
			fullLevelShotMods,
			chargeData,
			inkConsumption,
			inkRecoveryCooldown,
			bypassesMobDamage,
			isSecret);
	}
	@Override
	public float getSpeedForRender(Player player, ItemStack stack)
	{
		return getShotData(
			stack.get(SplatcraftComponents.CHARGE_DATA).charge(),
			stack.get(SplatcraftComponents.SPLATLING_FIRING_DATA).shotTypeData()
		).getSecond().projectileSpeed();
	}
	public SplatlingWeaponSettings<T> setBypassesMobDamage(boolean bypassesMobDamage)
	{
		this.bypassesMobDamage = bypassesMobDamage;
		return this;
	}
	public SplatlingWeaponSettings<T> setInkConsumption(float inkConsumption)
	{
		this.inkConsumption = inkConsumption;
		return this;
	}
	public SplatlingWeaponSettings<T> setInkRecoveryCooldown(float inkRecoveryCooldown)
	{
		this.inkRecoveryCooldown = inkRecoveryCooldown;
		return this;
	}
	public Pair<ProjectileDataRecord, SplatlingShotDataRecord> getShotData(float charge, short data)
	{
		return interpolateData(getShotTypeIndex(charge, data));
	}
	public float getShotTypeIndex(float charge, short data)
	{
		return switch (getDynamicDataKey())
		{
			case STATIC -> Mth.clamp(data, 0, 2);
			case CURRENT_CHARGE -> getShotIndexFromCharge(charge);
			case TIME_USED ->
			{
				TimeUsedSelectionData timeUsedData = (TimeUsedSelectionData) shotSelectionData;
				if (data >= timeUsedData.shotTimeForSecondLevel + timeUsedData.shotTransitionTime)
					yield 1;
				if (data >= timeUsedData.shotTimeForSecondLevel)
					yield ((float) data - timeUsedData.shotTimeForSecondLevel) / timeUsedData.shotTransitionTime;
				yield 0;
			}
			case SHOTS_FIRED -> 0.0F;
		};
	}
	public Pair<ProjectileDataRecord, SplatlingShotDataRecord> interpolateData(float progress)
	{
		progress = Math.min(2, progress);
		int intProgress = (int) progress;
		if (progress == intProgress)
			return Pair.of(projectileDatas[intProgress], shotDatas[intProgress]);
		progress -= intProgress;
		return Pair.of(
			lerpProjectileData(progress, projectileDatas[intProgress], projectileDatas[intProgress + 1]),
			lerpShotData(progress, shotDatas[intProgress], shotDatas[intProgress + 1])
		);
	}
	public static byte getShotIndexFromCharge(float charge)
	{
		if (charge >= 2)
			return 2;
		else if (charge >= 1)
			return 1;
		else
			return 0;
	}
	/*
		public float getDualieOffhandFiringOffset(boolean secondChargeLevel) // ok this would be funny to implement
		{
			return firstChargeLevelShot.repeatTicks / 2;
		}
	*/
	public float getMoveSpeed(boolean charging, float progress)
	{
		progress = Math.min(2, progress);
		float shotMoveSpeed;
		int intProgress = (int) progress;
		if (progress == intProgress)
			shotMoveSpeed = shotDatas[intProgress].mobility;
		else
		{
			progress -= intProgress;
			shotMoveSpeed = lerpShotData(progress, shotDatas[intProgress], shotDatas[intProgress + 1]).mobility;
		}
		
		return charging ? chargeData.moveSpeed.orElse(moveSpeed) : shotMoveSpeed;
	}
	public ProjectileDataRecord lerpProjectileData(float progress, ProjectileDataRecord dataStart, ProjectileDataRecord dataEnd)
	{
		return new ProjectileDataRecord(
			Mth.lerp(progress, dataStart.size(), dataEnd.size()),
			Mth.lerp(progress, dataStart.visualSize(), dataEnd.visualSize()),
			Mth.lerp(progress, dataStart.lifeTicks(), dataEnd.lifeTicks()),
			Mth.lerp(progress, dataStart.delaySpeedMult(), dataEnd.delaySpeedMult()),
			Mth.lerp(progress, dataStart.horizontalDrag(), dataEnd.horizontalDrag()),
			Mth.lerp(progress, dataStart.straightShotTicks(), dataEnd.straightShotTicks()),
			Mth.lerp(progress, dataStart.gravity(), dataEnd.gravity()),
			Mth.lerp(progress, dataStart.inkCoverageImpact(), dataEnd.inkCoverageImpact()),
			Mth.lerp(progress, dataStart.inkDropCoverage(), dataEnd.inkDropCoverage()),
			Mth.lerp(progress, dataStart.distanceBetweenInkDrops(), dataEnd.distanceBetweenInkDrops()),
			Mth.lerp(progress, dataStart.baseDamage(), dataEnd.baseDamage()),
			Mth.lerp(progress, dataStart.minDamage(), dataEnd.minDamage()),
			Mth.lerp(progress, dataStart.damageDecayStartTick(), dataEnd.damageDecayStartTick()),
			Mth.lerp(progress, dataStart.damageDecayPerTick(), dataEnd.damageDecayPerTick())
		);
	}
	public SplatlingShotDataRecord lerpShotData(float progress, SplatlingShotDataRecord dataStart, SplatlingShotDataRecord dataEnd)
	{
		return new SplatlingShotDataRecord(
			Mth.lerp(progress, dataStart.repeatTicks(), dataEnd.repeatTicks()),
			Mth.lerp(progress, dataStart.endlagTicks(), dataEnd.endlagTicks()),
			Mth.lerpInt(progress, dataStart.miscEndlagTicks(), dataEnd.miscEndlagTicks()),
			Mth.lerpInt(progress, dataStart.projectileCount(), dataEnd.projectileCount()),
			Mth.lerp(progress, dataStart.projectileSpeed(), dataEnd.projectileSpeed()),
			Mth.lerp(progress, dataStart.chargeUsePerShot(), dataEnd.chargeUsePerShot()),
			ShotDeviationDataRecord.lerp(progress, dataStart.accuracyData(), dataEnd.accuracyData()),
			Mth.lerp(progress, dataStart.pitchCompensation(), dataEnd.pitchCompensation()),
			Mth.lerp(progress, dataStart.mobility(), dataEnd.mobility())
		);
	}
	public record DataRecord(
		ProjectileDataRecord baseProjectile,
		SplatlingShotDataRecord baseShot,
		Optional<OptionalProjectileDataRecord> secondChargeProjectile,
		Optional<OptionalSplatlingShotDataRecord> secondChargeShot,
		Optional<OptionalProjectileDataRecord> fullChargeProjectile,
		Optional<OptionalSplatlingShotDataRecord> fullChargeShot,
		ChargeDataRecord charge,
		float inkConsumption,
		float inkRecoveryCooldown,
		boolean bypassesMobDamage,
		boolean isSecret
	)
	{
		public static final MapCodec<DataRecord> MAP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				ProjectileDataRecord.CODEC.fieldOf("projectile").forGetter(DataRecord::baseProjectile),
				SplatlingShotDataRecord.CODEC.fieldOf("shot").forGetter(DataRecord::baseShot),
				OptionalProjectileDataRecord.CODEC.optionalFieldOf("second_charge_projectile").forGetter(DataRecord::secondChargeProjectile),
				OptionalSplatlingShotDataRecord.CODEC.optionalFieldOf("second_charge_shot").forGetter(DataRecord::secondChargeShot),
				OptionalProjectileDataRecord.CODEC.optionalFieldOf("full_charge_projectile").forGetter(DataRecord::fullChargeProjectile),
				OptionalSplatlingShotDataRecord.CODEC.optionalFieldOf("full_charge_shot").forGetter(DataRecord::fullChargeShot),
				ChargeDataRecord.CODEC.fieldOf("charge").forGetter(DataRecord::charge),
				Codec.FLOAT.fieldOf("max_ink_consumption").forGetter(DataRecord::inkConsumption),
				Codec.FLOAT.fieldOf("ink_recovery_cooldown").forGetter(DataRecord::inkRecoveryCooldown),
				Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::bypassesMobDamage),
				Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
			).apply(instance, DataRecord::new)
		);
		public static final Codec<DataRecord> CODEC = MAP_CODEC.codec();
	}
	public enum ShotDataSelectorType implements StringRepresentable
	{
		STATIC, // maintain the data type used for the duration of the barrage, default behaviour
		CURRENT_CHARGE, // update the data according to the current charge, this means if a splatling goes from a second level to the first one, the shot uses the first level data,
		TIME_USED, // ballpoint-like type, when firing for an specified amount, use the second charge data
		SHOTS_FIRED; // actually ballpoint-like type, after a set amount of shots, use the second charge data
		public static final Codec<ShotDataSelectorType> CODEC = StringRepresentable.fromEnum(ShotDataSelectorType::values);
		@Override
		public @NotNull String getSerializedName()
		{
			return name();
		}
	}
	public record ChargeDataRecord(
		float minChargeTime,
		float firstChargeTime,
		float secondChargeTime,
		float emptyTankFirstChargeRate,
		float emptyTankSecondChargeRate,
		float airborneFirstChargeRate,
		float airborneSecondChargeRate,
		Optional<Float> moveSpeed,
		int chargeStorageTime,
		int chargeStorageSquidLag,
		int chargeStorageShootLag,
		boolean canRechargeWhileFiring
	)
	{
		public static final Codec<ChargeDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("min_charge_time_ticks", 2f).forGetter(ChargeDataRecord::minChargeTime),
				ExtraCodecs.POSITIVE_FLOAT.fieldOf("first_charge_time_ticks").forGetter(ChargeDataRecord::firstChargeTime),
				ExtraCodecs.POSITIVE_FLOAT.fieldOf("second_charge_time_ticks").forGetter(ChargeDataRecord::secondChargeTime),
				Codec.floatRange(0, 1).optionalFieldOf("empty_tank_first_charge_rate", 1 / 3f).forGetter(ChargeDataRecord::emptyTankFirstChargeRate),
				Codec.floatRange(0, 1).optionalFieldOf("empty_tank_second_charge_rate", 1 / 3f).forGetter(ChargeDataRecord::emptyTankSecondChargeRate),
				Codec.floatRange(0, 1).optionalFieldOf("airborne_first_charge_rate", 1 / 3f).forGetter(ChargeDataRecord::airborneFirstChargeRate),
				Codec.floatRange(0, 1).optionalFieldOf("airborne_second_charge_rate", 1 / 3f).forGetter(ChargeDataRecord::airborneSecondChargeRate),
				Codec.FLOAT.optionalFieldOf("mobility_while_charging").forGetter(ChargeDataRecord::moveSpeed),
				Codec.INT.optionalFieldOf("charge_storage_ticks", 0).forGetter(ChargeDataRecord::chargeStorageTime),
				ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("charge_storage_squid_lag", 3).forGetter(ChargeDataRecord::chargeStorageSquidLag),
				ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("charge_storage_shooting_lag", 10).forGetter(ChargeDataRecord::chargeStorageShootLag),
				Codec.BOOL.optionalFieldOf("can_recharge_while_firing", false).forGetter(ChargeDataRecord::canRechargeWhileFiring)
			).apply(instance, ChargeDataRecord::new)
		);
		public static final ChargeDataRecord DEFAULT = new ChargeDataRecord(
			1,
			10,
			20,
			1 / 3f,
			1 / 3f,
			1 / 3f,
			1 / 3f,
			Optional.empty(),
			0,
			3,
			10,
			false);
		public float getChargeStep(float currentCharge)
		{
			float secondChargeStep = 1f / secondChargeTime;
			if (currentCharge < 1f)
			{
				float firstChargeStep = 1f / firstChargeTime;
				if (currentCharge + firstChargeStep >= 1)
					return 1f - currentCharge + secondChargeStep;
				else
					return firstChargeStep;
			}
			else if (currentCharge + secondChargeStep >= 2)
				return 2f - currentCharge;
			return secondChargeStep;
		}
	}
	public record SplatlingShotDataRecord(
		float repeatTicks,
		float endlagTicks,
		int miscEndlagTicks,
		int projectileCount,
		float projectileSpeed,
		float chargeUsePerShot,
		ShotDeviationDataRecord accuracyData,
		float pitchCompensation,
		float mobility
	)
	{
		public static final MapCodec<SplatlingShotDataRecord> MAP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				Codec.FLOAT.fieldOf("repeat_ticks").forGetter(SplatlingShotDataRecord::repeatTicks),
				Codec.FLOAT.fieldOf("endlag_ticks").forGetter(SplatlingShotDataRecord::endlagTicks),
				Codec.INT.optionalFieldOf("other_actions_endlag_ticks", 10).forGetter(SplatlingShotDataRecord::miscEndlagTicks),
				Codec.INT.optionalFieldOf("projectile_count_per_shot", 1).forGetter(SplatlingShotDataRecord::projectileCount),
				Codec.FLOAT.optionalFieldOf("projectile_speed", 0f).forGetter(SplatlingShotDataRecord::projectileSpeed),
				Codec.FLOAT.fieldOf("charge_usage_per_shot").forGetter(SplatlingShotDataRecord::chargeUsePerShot),
				ShotDeviationDataRecord.CODEC.optionalFieldOf("accuracy_data", ShotDeviationDataRecord.PERFECT_DEFAULT).forGetter(SplatlingShotDataRecord::accuracyData),
				Codec.FLOAT.optionalFieldOf("pitch_compensation", 0f).forGetter(SplatlingShotDataRecord::pitchCompensation),
				Codec.FLOAT.optionalFieldOf("mobility", 0.5f).forGetter(SplatlingShotDataRecord::mobility)
			).apply(instance, SplatlingShotDataRecord::new)
		);
		public static final Codec<SplatlingShotDataRecord> CODEC = MAP_CODEC.codec();
		public static final SplatlingShotDataRecord DEFAULT = new SplatlingShotDataRecord(0, 1, 0, 1, 1, 0.2f, ShotDeviationDataRecord.DEFAULT, 0, 0.5f);
	}
	public record OptionalSplatlingShotDataRecord(
		Optional<Float> repeatTicks,
		Optional<Float> endlagTicks,
		Optional<Integer> miscEndlagTicks,
		Optional<Integer> projectileCount,
		Optional<Float> projectileSpeed,
		Optional<Float> chargeUsePerShot,
		Optional<OptionalShotDeviationDataRecord> accuracyData,
		Optional<Float> pitchCompensation,
		Optional<Float> mobility
	)
	{
		public static final MapCodec<OptionalSplatlingShotDataRecord> MAP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				Codec.FLOAT.optionalFieldOf("repeat_ticks").forGetter(OptionalSplatlingShotDataRecord::repeatTicks),
				Codec.FLOAT.optionalFieldOf("endlag_ticks").forGetter(OptionalSplatlingShotDataRecord::endlagTicks),
				Codec.INT.optionalFieldOf("other_actions_endlag_ticks").forGetter(OptionalSplatlingShotDataRecord::miscEndlagTicks),
				Codec.INT.optionalFieldOf("projectile_count_per_shot").forGetter(OptionalSplatlingShotDataRecord::projectileCount),
				Codec.FLOAT.optionalFieldOf("projectile_speed").forGetter(OptionalSplatlingShotDataRecord::projectileSpeed),
				Codec.FLOAT.optionalFieldOf("charge_usage_per_shot").forGetter(OptionalSplatlingShotDataRecord::chargeUsePerShot),
				OptionalShotDeviationDataRecord.CODEC.optionalFieldOf("accuracy_data").forGetter(OptionalSplatlingShotDataRecord::accuracyData),
				Codec.FLOAT.optionalFieldOf("pitch_compensation").forGetter(OptionalSplatlingShotDataRecord::pitchCompensation),
				Codec.FLOAT.optionalFieldOf("mobility").forGetter(OptionalSplatlingShotDataRecord::mobility)
			).apply(instance, OptionalSplatlingShotDataRecord::new)
		);
		public static final Codec<OptionalSplatlingShotDataRecord> CODEC = MAP_CODEC.codec();
		public static final OptionalSplatlingShotDataRecord DEFAULT = new OptionalSplatlingShotDataRecord(Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty());
		public static OptionalSplatlingShotDataRecord from(SplatlingShotDataRecord shot)
		{
			return new OptionalSplatlingShotDataRecord(
				Optional.of(shot.repeatTicks),
				Optional.of(shot.endlagTicks),
				Optional.of(shot.miscEndlagTicks),
				Optional.of(shot.projectileCount),
				Optional.of(shot.projectileSpeed),
				Optional.of(shot.chargeUsePerShot),
				Optional.of(OptionalShotDeviationDataRecord.from(shot.accuracyData)),
				Optional.of(shot.pitchCompensation),
				Optional.of(shot.mobility)
			);
		}
		public static SplatlingShotDataRecord mergeWithBase(Optional<OptionalSplatlingShotDataRecord> modified, SplatlingShotDataRecord base)
		{
			if (modified.isEmpty())
				return base;
			
			OptionalSplatlingShotDataRecord modifiedGet = modified.get();
			return new SplatlingShotDataRecord(
				modifiedGet.repeatTicks.orElse(base.repeatTicks),
				modifiedGet.endlagTicks.orElse(base.endlagTicks),
				modifiedGet.miscEndlagTicks.orElse(base.miscEndlagTicks),
				modifiedGet.projectileCount.orElse(base.projectileCount),
				modifiedGet.projectileSpeed.orElse(base.projectileSpeed),
				modifiedGet.chargeUsePerShot.orElse(base.chargeUsePerShot),
				OptionalShotDeviationDataRecord.mergeWithBase(modifiedGet.accuracyData, base.accuracyData),
				modifiedGet.pitchCompensation.orElse(base.pitchCompensation),
				modifiedGet.mobility.orElse(base.mobility)
			);
		}
	}
	public record TimeUsedSelectionData(
		short shotTimeForSecondLevel,
		short shotTransitionTime
	) implements DynamicDataRecord<TimeUsedSelectionData>
	{
		public static final MapCodec<TimeUsedSelectionData> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				Codec.SHORT.optionalFieldOf("shot_time_for_second_level", (short) 0).forGetter(TimeUsedSelectionData::shotTimeForSecondLevel),
				Codec.SHORT.optionalFieldOf("shot_transition_time", (short) 0).forGetter(TimeUsedSelectionData::shotTransitionTime)
			).apply(inst, TimeUsedSelectionData::new)
		);
		@Override
		public TimeUsedSelectionData convertSelf()
		{
			return new TimeUsedSelectionData(
				(short) (shotTimeForSecondLevel / SplatcraftConvertors.SplatoonFramesPerMinecraftTick),
				(short) (shotTransitionTime / SplatcraftConvertors.SplatoonFramesPerMinecraftTick)
			);
		}
	}
}
