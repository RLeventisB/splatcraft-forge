package net.splatcraft.data;

import net.splatcraft.items.weapons.settings.BlasterWeaponSettings;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.items.weapons.settings.DualieWeaponSettings;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.util.DamageRangesRecord;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class SplatcraftConvertors
{
	public static final int DistanceUnitsPerMinecraftSquare = 14;
	public static final int SplatoonFramesPerMinecraftTick = 3;
	public static final int SplatoonHealthPerMinecraftHealth = 5;
	public static boolean SkipConverting = false;
	public static CommonRecords.InkUsageDataRecord convert(CommonRecords.InkUsageDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new CommonRecords.InkUsageDataRecord(
			dataRecord.inkConsumption(),
			dataRecord.inkRecoveryCooldown() / SplatoonFramesPerMinecraftTick
		);
	}
	public static CommonRecords.ProjectileDataRecord convert(CommonRecords.ProjectileDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new CommonRecords.ProjectileDataRecord(
			dataRecord.size() / DistanceUnitsPerMinecraftSquare * 2, // splatoon hitboxes are circles, and often specify radiuses
			dataRecord.visualSize() / DistanceUnitsPerMinecraftSquare * 2,
			dataRecord.lifeTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.speed() / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick,
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
			dataRecord.damageDecayPerTick() / SplatoonHealthPerMinecraftHealth / SplatoonFramesPerMinecraftTick);
	}
	public static CommonRecords.ShotDataRecord convert(CommonRecords.ShotDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new CommonRecords.ShotDataRecord(
			dataRecord.startupTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.squidStartupTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.endlagTicks() / SplatoonFramesPerMinecraftTick,
			dataRecord.projectileCount(),
			convert(dataRecord.accuracyData()),
			dataRecord.pitchCompensation(),
			dataRecord.inkConsumption(),
			dataRecord.inkRecoveryCooldown() / SplatoonFramesPerMinecraftTick);
	}
	public static CommonRecords.OptionalProjectileDataRecord convert(CommonRecords.OptionalProjectileDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new CommonRecords.OptionalProjectileDataRecord(
			multiplyIfPresentFloat(dataRecord.size(), 1.0 / DistanceUnitsPerMinecraftSquare * 2),
			multiplyIfPresentFloat(dataRecord.visualSize(), 1.0 / DistanceUnitsPerMinecraftSquare * 2),
			multiplyIfPresentFloat(dataRecord.lifeTicks(), 1.0 / SplatoonFramesPerMinecraftTick),
			multiplyIfPresentFloat(dataRecord.speed(), 1.0 / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick),
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
			multiplyIfPresentFloat(dataRecord.damageDecayPerTick(), 1.0 / SplatoonHealthPerMinecraftHealth / SplatoonFramesPerMinecraftTick));
	}
	public static CommonRecords.OptionalShotDataRecord convert(CommonRecords.OptionalShotDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new CommonRecords.OptionalShotDataRecord(
			multiplyIfPresentFloat(dataRecord.startupTicks(), 1.0 / SplatoonFramesPerMinecraftTick),
			multiplyIfPresentFloat(dataRecord.squidStartupTicks(), 1.0 / SplatoonFramesPerMinecraftTick),
			multiplyIfPresentFloat(dataRecord.endlagTicks(), 1.0 / SplatoonFramesPerMinecraftTick),
			dataRecord.projectileCount(),
			dataRecord.accuracyData().map(v -> convert(dataRecord.accuracyData().get())),
			dataRecord.pitchCompensation(),
			dataRecord.inkConsumption(),
			multiplyIfPresentFloat(dataRecord.inkRecoveryCooldown(), 1.0 / SplatoonFramesPerMinecraftTick));
	}
	public static CommonRecords.ShotDeviationDataRecord convert(CommonRecords.ShotDeviationDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new CommonRecords.ShotDeviationDataRecord(
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
			convert(dataRecord.damageRadiuses()),
			convert(dataRecord.sparkDamageRadiuses()),
			dataRecord.explosionPaint() / DistanceUnitsPerMinecraftSquare,
			dataRecord.newAttackId()
		);
	}
	private static DamageRangesRecord convert(DamageRangesRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		TreeMap<Float, Float> values = new TreeMap<>();
		for (Map.Entry<Float, Float> entry : dataRecord.damageValues().entrySet())
		{
			values.put(entry.getKey() / DistanceUnitsPerMinecraftSquare, entry.getValue() / SplatoonHealthPerMinecraftHealth);
		}
		return new DamageRangesRecord(
			new TreeMap<>(values),
			dataRecord.lerpBetween()
		);
	}
	public static SubWeaponSettings.BaseSubRecord convert(SubWeaponSettings.BaseSubRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new SubWeaponSettings.BaseSubRecord(
			dataRecord.directDamage() / SplatoonHealthPerMinecraftHealth,
			convert(dataRecord.damageRanges()),
			dataRecord.inkSplashRadius() / DistanceUnitsPerMinecraftSquare,
			dataRecord.fuseTime() / SplatoonFramesPerMinecraftTick,
			dataRecord.inkConsumption(),
			dataRecord.inkRecoveryCooldown() / SplatoonFramesPerMinecraftTick,
			dataRecord.throwVelocity() / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick,
			dataRecord.throwAngle(),
			dataRecord.holdTime() / SplatoonFramesPerMinecraftTick,
			dataRecord.mobility(),
			convert(dataRecord.curlingData()),
			dataRecord.isSecret()
		);
	}
	private static SubWeaponSettings.CurlingDataRecord convert(SubWeaponSettings.CurlingDataRecord dataRecord)
	{
		if (SkipConverting)
			return dataRecord;
		
		return new SubWeaponSettings.CurlingDataRecord(
			dataRecord.cookTime() / SplatoonFramesPerMinecraftTick,
			dataRecord.contactDamage() / SplatoonHealthPerMinecraftHealth
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
	private static Optional<Double> multiplyIfPresentDouble(Optional<Double> value, double multiplier)
	{
		return value.map(aFloat -> (aFloat * multiplier));
	}
	private static Optional<Integer> multiplyIfPresentInt(Optional<Integer> value, double multiplier)
	{
		return value.map(aFloat -> (int) (aFloat * multiplier));
	}
}
