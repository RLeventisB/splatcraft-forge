package net.splatcraft.data;

import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.*;
import net.splatcraft.items.weapons.settings.ChargerWeaponSettings.ChargeDataRecord;
import net.splatcraft.items.weapons.settings.SlosherWeaponSettings.SlosherShotDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponSettings.SplashAroundDataRecord;
import net.splatcraft.util.structs.NumberRange;
import net.splatcraft.util.structs.RangedValueCollection;

import java.util.List;
import java.util.Optional;

import static net.splatcraft.items.weapons.settings.ChargerWeaponSettings.ChargerProjectileDataRecord;
import static net.splatcraft.items.weapons.settings.CommonRecords.*;
import static net.splatcraft.items.weapons.settings.RollerWeaponSettings.*;
import static net.splatcraft.items.weapons.settings.SlosherWeaponSettings.SingularSloshShotData;
import static net.splatcraft.items.weapons.settings.SplatlingWeaponSettings.SplatlingShotDataRecord;

public class SplatcraftConvertors
{
	public static final int DistanceUnitsPerMinecraftSquare = 14;
	public static final int SplatoonFramesPerMinecraftTick = 3;
	public static final int SplatoonHealthPerMinecraftHealth = 5;
	public static boolean SkipConverting = false;
	public static InkUsageDataRecord convert(InkUsageDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new InkUsageDataRecord(
			dataRecord.consumption(),
			dataRecord.recoveryCooldown() / SplatoonFramesPerMinecraftTick
		);
	}
	public static ProjectileDataRecord convert(ProjectileDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new ProjectileDataRecord(
			dataRecord.size() / DistanceUnitsPerMinecraftSquare * 2, // splatoon hitboxes are circles, and often specify radiuses
			dataRecord.visualSize() / DistanceUnitsPerMinecraftSquare * 2,
			dataRecord.lifeTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.delaySpeedMult(),
			(float) Math.pow(dataRecord.horizontalDrag(), SplatoonFramesPerMinecraftTick),
			dataRecord.straightShotTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.gravity() * SplatoonFramesPerMinecraftTick / DistanceUnitsPerMinecraftSquare,
			dataRecord.inkCoverageImpact() / DistanceUnitsPerMinecraftSquare,
			dataRecord.inkDropCoverage() / DistanceUnitsPerMinecraftSquare,
			dataRecord.distanceBetweenInkDrops() / DistanceUnitsPerMinecraftSquare,
			dataRecord.baseDamage() / SplatoonHealthPerMinecraftHealth,
			dataRecord.minDamage() / SplatoonHealthPerMinecraftHealth,
			dataRecord.damageDecayStartTick() / SplatoonHealthPerMinecraftHealth / SplatoonFramesPerMinecraftTick,
			dataRecord.damageDecayPerTick() / SplatoonHealthPerMinecraftHealth / SplatoonFramesPerMinecraftTick
		);
	}
	public static OptionalProjectileDataRecord convert(OptionalProjectileDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new OptionalProjectileDataRecord(
			multiplyIfPresentFloat(dataRecord.size(), 1.0 / DistanceUnitsPerMinecraftSquare * 2),
			multiplyIfPresentFloat(dataRecord.visualSize(), 1.0 / DistanceUnitsPerMinecraftSquare * 2),
			multiplyIfPresentFloat(dataRecord.lifeTicks(), 1.0 / SplatoonFramesPerMinecraftTick),
			dataRecord.delaySpeedMult(),
			powIfPresentFloat(dataRecord.horizontalDrag(), SplatoonFramesPerMinecraftTick),
			multiplyIfPresentFloat(dataRecord.straightShotTicks(), 1.0 / SplatoonFramesPerMinecraftTick),
			multiplyIfPresentFloat(dataRecord.gravity(), (double) SplatoonFramesPerMinecraftTick / DistanceUnitsPerMinecraftSquare),
			multiplyIfPresentFloat(dataRecord.inkCoverageImpact(), 1.0 / DistanceUnitsPerMinecraftSquare),
			multiplyIfPresentFloat(dataRecord.inkDropCoverage(), 1.0 / DistanceUnitsPerMinecraftSquare),
			multiplyIfPresentFloat(dataRecord.distanceBetweenInkDrops(), 1.0 / DistanceUnitsPerMinecraftSquare),
			multiplyIfPresentFloat(dataRecord.baseDamage(), 1.0 / SplatoonHealthPerMinecraftHealth),
			multiplyIfPresentFloat(dataRecord.minDamage(), 1.0 / SplatoonHealthPerMinecraftHealth),
			multiplyIfPresentFloat(dataRecord.damageDecayStartTick(), 1.0 / SplatoonHealthPerMinecraftHealth / SplatoonFramesPerMinecraftTick),
			multiplyIfPresentFloat(dataRecord.damageDecayPerTick(), 1.0 / SplatoonHealthPerMinecraftHealth / SplatoonFramesPerMinecraftTick)
		);
	}
	public static ShotDataRecord convert(ShotDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new ShotDataRecord(
			dataRecord.startupTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.squidStartupTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.repeatTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.endlagTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.miscEndlagTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.speed() / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick,
			dataRecord.projectileCount(),
			convert(dataRecord.accuracyData()),
			dataRecord.pitchCompensation(),
			dataRecord.inkConsumption(),
			dataRecord.inkRecoveryCooldown() / SplatoonFramesPerMinecraftTick
		);
	}
	public static OptionalShotDataRecord convert(OptionalShotDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new OptionalShotDataRecord(
			dataRecord.startupTicks().map(v -> v / SplatoonFramesPerMinecraftTick),
			dataRecord.squidStartupTicks().map(v -> v / SplatoonFramesPerMinecraftTick),
			dataRecord.repeatTicks().map(v -> v / SplatoonFramesPerMinecraftTick),
			dataRecord.endlagTicks().map(v -> v / SplatoonFramesPerMinecraftTick),
			dataRecord.miscEndlagTicks().map(v -> v / SplatoonFramesPerMinecraftTick),
			dataRecord.speed().map(v -> v / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick),
			dataRecord.projectileCount(),
			dataRecord.accuracyData().map(SplatcraftConvertors::convert),
			dataRecord.pitchCompensation(),
			dataRecord.inkConsumption(),
			dataRecord.inkRecoveryCooldown().map(v -> v / SplatoonFramesPerMinecraftTick)
		);
	}
	public static ShotDeviationDataRecord convert(ShotDeviationDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new ShotDeviationDataRecord(
			dataRecord.groundShotDeviation(),
			dataRecord.airborneShotDeviation(),
			dataRecord.minDeviateChance(),
			dataRecord.maxDeviateChance(),
			dataRecord.deviationChanceWhenAirborne(),
			dataRecord.chanceIncreasePerShot(),
			dataRecord.chanceDecreaseDelay() / SplatoonFramesPerMinecraftTick,
			dataRecord.chanceDecreasePerTick() * SplatoonFramesPerMinecraftTick,
			dataRecord.airborneContractDelay() / SplatoonFramesPerMinecraftTick,
			dataRecord.airborneContractTimeToDecrease() / SplatoonFramesPerMinecraftTick);
	}
	public static DualieWeaponSettings.RollDataRecord convert(DualieWeaponSettings.RollDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		float rollTotalTime = 0;
		float startupValue = (float) dataRecord.rollStartup() / SplatoonFramesPerMinecraftTick;
		rollTotalTime += startupValue;
		byte roundedStartup = (byte) startupValue;
		
		float durationValue = (float) dataRecord.rollDuration() / SplatoonFramesPerMinecraftTick;
		rollTotalTime += durationValue;
		
		byte roundedDuration = (byte) (rollTotalTime - roundedStartup);
		
		float endlagValue = (float) dataRecord.rollEndlag() / SplatoonFramesPerMinecraftTick;
		rollTotalTime += endlagValue;
		
		byte roundedEndlag = (byte) Math.ceil(rollTotalTime - roundedStartup - roundedDuration);
		
		return new DualieWeaponSettings.RollDataRecord(
			dataRecord.count(),
			dataRecord.rollDistance() / DistanceUnitsPerMinecraftSquare,
			dataRecord.inkConsumption(),
			dataRecord.inkRecoveryCooldown() / SplatoonFramesPerMinecraftTick,
			roundedStartup,
			roundedDuration,
			roundedEndlag,
			dataRecord.turretDuration(),
			dataRecord.lastRollTurretDuration(),
			dataRecord.canMove()
		);
	}
	public static BlasterWeaponSettings.DetonationRecord convert(BlasterWeaponSettings.DetonationRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new BlasterWeaponSettings.DetonationRecord(
			convertDamage(dataRecord.damageRadiuses()),
			convertDamage(dataRecord.sparkDamageRadiuses()),
			dataRecord.explosionPaint() / DistanceUnitsPerMinecraftSquare,
			dataRecord.newAttackId()
		);
	}
	public static RangedValueCollection convertDamage(RangedValueCollection dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return dataRecord.cloneWithMultiplier(
			1f / DistanceUnitsPerMinecraftSquare,
			1f / SplatoonHealthPerMinecraftHealth
		);
	}
	public static SubWeaponSettings.DataRecord convert(SubWeaponSettings.DataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new SubWeaponSettings.DataRecord(
			convert(dataRecord.inkUsage()),
			dataRecord.holdTime() < WeaponBaseItem.USE_DURATION ? dataRecord.holdTime() / SplatoonFramesPerMinecraftTick : dataRecord.holdTime(),
			dataRecord.mobility(),
			dataRecord.isSecret()
		);
	}
	public static <T extends DynamicDataRecord<T>> T convert(T dataRecord)
	{
		if (SkipConverting || dataRecord == null)
			return dataRecord;
		
		return dataRecord.convertSelf();
	}
	public static SplashAroundDataRecord convert(SplashAroundDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new SplashAroundDataRecord(
			convertSpeed(dataRecord.splashVelocityRange()),
			dataRecord.splashPitchRange(),
			dataRecord.splashCount(),
			dataRecord.splashPaintRadius() / DistanceUnitsPerMinecraftSquare,
			dataRecord.angleRandomness(),
			dataRecord.distributeEvenly()
		);
	}
	public static SlosherShotDataRecord convert(SlosherShotDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new SlosherShotDataRecord(
			dataRecord.endlagTicks() / SplatoonFramesPerMinecraftTick,
			Math.round((float) dataRecord.miscEndlagTicks() / SplatoonFramesPerMinecraftTick),
			convert(dataRecord.sloshes()),
			dataRecord.baseSpeed() / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick,
			dataRecord.pitchCompensation(),
			dataRecord.inkConsumption(),
			dataRecord.inkRecoveryCooldown() / SplatoonFramesPerMinecraftTick,
			dataRecord.allowFlicking()
		);
	}
	private static List<SingularSloshShotData> convert(List<SingularSloshShotData> dataRecords)
	{
		if (SkipConverting)
			return dataRecords;
		
		return dataRecords.stream().map(dataRecord ->
			new SingularSloshShotData(
				dataRecord.startupTicks() / SplatoonFramesPerMinecraftTick,
				dataRecord.count(),
				dataRecord.delayBetweenProjectiles() / SplatoonFramesPerMinecraftTick,
				dataRecord.modifiedSpeed().map(v -> v / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick),
				dataRecord.speedSubstract() / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick,
				dataRecord.offsetAngle(),
				dataRecord.projectileModifications().map(SplatcraftConvertors::convert),
				dataRecord.detonationData().map(SplatcraftConvertors::convert)
			)).toList();
	}
	public static RollDataRecord convert(RollDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new RollDataRecord(
			dataRecord.inkSize() / DistanceUnitsPerMinecraftSquare,
			dataRecord.hitboxSize() / DistanceUnitsPerMinecraftSquare,
			dataRecord.inkConsumption(),
			dataRecord.inkRecoveryCooldown() / SplatoonFramesPerMinecraftTick,
			dataRecord.damage() / SplatoonHealthPerMinecraftHealth,
			dataRecord.mobility(),
			dataRecord.dashMobility(),
			dataRecord.dashConsumption(),
			dataRecord.dashTime() / SplatoonFramesPerMinecraftTick
		);
	}
	public static SwingDataRecord convert(SwingDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new SwingDataRecord(
			convert(dataRecord.projectileData()),
			convert(dataRecord.attackData()),
			dataRecord.allowJumpingOnCharge(),
			dataRecord.mobility(),
			dataRecord.attackAngle(),
			dataRecord.letalAngle(),
			dataRecord.blobCount()
		);
	}
	public static FlingDataRecord convert(FlingDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new FlingDataRecord(
			convert(dataRecord.projectileData()),
			convert(dataRecord.attackData()),
			dataRecord.startPitchCompensation(),
			dataRecord.endPitchCompensation(),
			dataRecord.forcedProjectileCount()
		);
	}
	private static RollerAttackDataRecord convert(RollerAttackDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new RollerAttackDataRecord(
			dataRecord.inkConsumption(),
			dataRecord.inkRecoveryCooldown() / SplatoonFramesPerMinecraftTick,
			dataRecord.startupTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.endlagTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.rollDelayTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.miscEndlagTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.speedRange().mapBoth(v -> v / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick)
		);
	}
	private static RollerProjectileDataRecord convert(RollerProjectileDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new RollerProjectileDataRecord(
			dataRecord.size() / DistanceUnitsPerMinecraftSquare * 2,
			dataRecord.visualSize() / DistanceUnitsPerMinecraftSquare * 2,
			dataRecord.delaySpeedMult(),
			(float) Math.pow(dataRecord.horizontalDrag(), SplatoonFramesPerMinecraftTick),
			dataRecord.straightShotTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.gravity() * SplatoonFramesPerMinecraftTick / DistanceUnitsPerMinecraftSquare,
			dataRecord.inkCoverageImpact() / DistanceUnitsPerMinecraftSquare,
			dataRecord.inkDropCoverage() / DistanceUnitsPerMinecraftSquare,
			dataRecord.distanceBetweenInkDrops() / DistanceUnitsPerMinecraftSquare,
			dataRecord.damageFalloffStartTick() / SplatoonHealthPerMinecraftHealth,
			dataRecord.damageFalloffEndTick() / SplatoonHealthPerMinecraftHealth,
			dataRecord.maxDamageFalloffPercent(),
			convertDamage(dataRecord.damageRanges()),
			dataRecord.weakDamageRanges().map(SplatcraftConvertors::convertDamage)
		);
	}
	public static ChargerProjectileDataRecord convert(ChargerProjectileDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new ChargerProjectileDataRecord(
			dataRecord.size() / DistanceUnitsPerMinecraftSquare * 2,
			dataRecord.speed().map(v -> v / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick),
			dataRecord.range().map(v -> v / DistanceUnitsPerMinecraftSquare),
			dataRecord.inkCoverageImpact().map(v -> v / DistanceUnitsPerMinecraftSquare),
			dataRecord.inkDropCoverage().map(v -> v / DistanceUnitsPerMinecraftSquare),
			dataRecord.distanceBetweenInkDrops().map(v -> v / DistanceUnitsPerMinecraftSquare),
			dataRecord.damage().map(v -> v / SplatoonHealthPerMinecraftHealth),
			dataRecord.piercesAtCharge()
		);
	}
	public static ChargerWeaponSettings.ShotDataRecord convert(ChargerWeaponSettings.ShotDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new ChargerWeaponSettings.ShotDataRecord(
			dataRecord.endlagTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.miscEndlagTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.inkConsumption(),
			dataRecord.inkRecoveryCooldown() / SplatoonFramesPerMinecraftTick,
			dataRecord.shotsCount()
		);
	}
	public static ChargeDataRecord convert(ChargeDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new ChargeDataRecord(
			dataRecord.chargeStartup() / SplatoonFramesPerMinecraftTick,
			dataRecord.chargeTime() / SplatoonFramesPerMinecraftTick,
			dataRecord.airborneChargeRate(),
			dataRecord.emptyTankChargeRate(),
			dataRecord.chargeStorageTime() / SplatoonFramesPerMinecraftTick,
			dataRecord.chargeStorageSquidLag() / SplatoonFramesPerMinecraftTick,
			dataRecord.chargeStorageShootLag() / SplatoonFramesPerMinecraftTick
		);
	}
	public static SplatlingShotDataRecord convert(SplatlingShotDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new SplatlingShotDataRecord(
			dataRecord.repeatTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.endlagTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.miscEndlagTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.projectileCount(),
			dataRecord.projectileSpeed() / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick,
			dataRecord.chargeUsePerShot(),
			convert(dataRecord.accuracyData()),
			dataRecord.pitchCompensation(),
			dataRecord.mobility()
		);
	}
	public static SplatlingWeaponSettings.ChargeDataRecord convert(SplatlingWeaponSettings.ChargeDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new SplatlingWeaponSettings.ChargeDataRecord(
			dataRecord.minChargeTime() / SplatoonFramesPerMinecraftTick,
			dataRecord.firstChargeTime() / SplatoonFramesPerMinecraftTick,
			dataRecord.secondChargeTime() / SplatoonFramesPerMinecraftTick,
			dataRecord.emptyTankFirstChargeRate(),
			dataRecord.emptyTankSecondChargeRate(),
			dataRecord.airborneFirstChargeRate(),
			dataRecord.airborneSecondChargeRate(),
			dataRecord.moveSpeed(),
			dataRecord.chargeStorageTime() / SplatoonFramesPerMinecraftTick,
			dataRecord.chargeStorageSquidLag() / SplatoonFramesPerMinecraftTick,
			dataRecord.chargeStorageShootLag() / SplatoonFramesPerMinecraftTick,
			dataRecord.canRechargeWhileFiring()
		);
	}
	public static SplatlingWeaponSettings.OptionalSplatlingShotDataRecord convert(SplatlingWeaponSettings.OptionalSplatlingShotDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new SplatlingWeaponSettings.OptionalSplatlingShotDataRecord(
			dataRecord.repeatTicks().map(v -> v / SplatoonFramesPerMinecraftTick),
			dataRecord.endlagTicks().map(v -> v / SplatoonFramesPerMinecraftTick),
			dataRecord.miscEndlagTicks().map(v -> v / SplatoonFramesPerMinecraftTick),
			dataRecord.projectileCount(),
			dataRecord.projectileSpeed().map(v -> v / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick),
			dataRecord.chargeUsePerShot(),
			dataRecord.accuracyData().map(SplatcraftConvertors::convert),
			dataRecord.pitchCompensation(),
			dataRecord.mobility()
		);
	}
	private static OptionalShotDeviationDataRecord convert(OptionalShotDeviationDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new OptionalShotDeviationDataRecord(
			dataRecord.groundShotDeviation(),
			dataRecord.airborneShotDeviation(),
			dataRecord.minDeviateChance(),
			dataRecord.maxDeviateChance(),
			dataRecord.deviationChanceWhenAirborne(),
			dataRecord.chanceIncreasePerShot(),
			dataRecord.chanceDecreaseDelay().map(v -> v / SplatoonFramesPerMinecraftTick),
			dataRecord.chanceDecreasePerTick().map(v -> v * SplatoonFramesPerMinecraftTick),
			dataRecord.airborneContractDelay().map(v -> v / SplatoonFramesPerMinecraftTick),
			dataRecord.airborneContractTimeToDecrease().map(v -> v / SplatoonFramesPerMinecraftTick)
		
		);
	}
	public static NumberRange.FloatRange convertLength(NumberRange.FloatRange range)
	{
		if (SkipConverting)
			return range;
		
		return new NumberRange.FloatRange(
			range.min() / DistanceUnitsPerMinecraftSquare,
			range.max() / DistanceUnitsPerMinecraftSquare
		);
	}
	public static NumberRange.FloatRange convertSpeed(NumberRange.FloatRange range)
	{
		if (SkipConverting)
			return range;
		
		return new NumberRange.FloatRange(
			range.min() / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick,
			range.max() / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick
		);
	}
	public static NumberRange.IntRange convertTime(NumberRange.IntRange range)
	{
		if (SkipConverting)
			return range;
		
		return new NumberRange.IntRange(
			range.min() / SplatoonFramesPerMinecraftTick,
			range.max() / SplatoonFramesPerMinecraftTick
		);
	}
	private static Optional<Float> powIfPresentFloat(Optional<Float> value, double exponent)
	{
		return value.map(aFloat -> (float) Math.pow(aFloat, exponent));
	}
	private static Optional<Float> multiplyIfPresentFloat(Optional<Float> value, double multiplier)
	{
		return value.map(aFloat -> (float) (aFloat * multiplier));
	}
}
