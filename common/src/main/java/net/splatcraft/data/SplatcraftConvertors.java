package net.splatcraft.data;

import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.BlasterWeaponSettings;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.items.weapons.settings.DualieWeaponSettings;
import net.splatcraft.items.weapons.settings.SubWeaponRecords.SubDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.items.weapons.settings.SubWeaponSettings.SplashAroundDataRecord;
import net.splatcraft.util.DamageRangesRecord;

import java.util.Map;
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
			dataRecord.consumption(),
			dataRecord.recoveryCooldown() / SplatoonFramesPerMinecraftTick
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
			dataRecord.inkRecoveryCooldown() / SplatoonFramesPerMinecraftTick
		);
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
	public static DamageRangesRecord convert(DamageRangesRecord dataRecord)
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
	public static <T extends SubDataRecord<T>> SubWeaponSettings.DataRecord convert(SubWeaponSettings.DataRecord dataRecord)
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
	public static <T extends SubDataRecord<T>> T convert(T dataRecord)
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
	public static CommonRecords.FloatRange convertLength(CommonRecords.FloatRange range)
	{
		if (SkipConverting)
			return range;
		
		return new CommonRecords.FloatRange(
			range.min() / DistanceUnitsPerMinecraftSquare,
			range.max() / DistanceUnitsPerMinecraftSquare
		);
	}
	public static CommonRecords.FloatRange convertSpeed(CommonRecords.FloatRange range)
	{
		if (SkipConverting)
			return range;
		
		return new CommonRecords.FloatRange(
			range.min() / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick,
			range.max() / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick
		);
	}
	public static CommonRecords.IntRange convertTime(CommonRecords.IntRange range)
	{
		if (SkipConverting)
			return range;
		
		return new CommonRecords.IntRange(
			range.min() / SplatoonFramesPerMinecraftTick,
			range.max() / SplatoonFramesPerMinecraftTick
		);
	}
}
