package net.splatcraft.items.weapons.settings;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.splatcraft.items.weapons.settings.CommonRecords.FloatRange;
import net.splatcraft.items.weapons.settings.CommonRecords.InkUsageDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponSettings.SplashAroundDataRecord;
import net.splatcraft.util.DamageRangesRecord;
import net.splatcraft.util.WeaponTooltip;

import java.util.List;

import static net.splatcraft.data.SplatcraftConvertors.*;

public class SubWeaponRecords
{
	public interface SubDataRecord<Self>
	{
		Self convertSelf();
		default <T extends SubDataRecord<T>> void addTooltips(List<WeaponTooltip<SubWeaponSettings<T>>> weaponTooltips)
		{
		
		}
	}
	public record ThrowableExplodingSubDataRecord(
		DamageRangesRecord damageRanges,
		SplashAroundDataRecord inkSplashes,
		float inkSplashRadius,
		int fuseTime,
		float throwVelocity,
		float throwAngle
	) implements SubDataRecord<ThrowableExplodingSubDataRecord>
	{
		public static final MapCodec<ThrowableExplodingSubDataRecord> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				DamageRangesRecord.CODEC.fieldOf("damage_ranges").forGetter(ThrowableExplodingSubDataRecord::damageRanges),
				SplashAroundDataRecord.CODEC.fieldOf("ink_splashes").forGetter(ThrowableExplodingSubDataRecord::inkSplashes),
				Codec.FLOAT.fieldOf("ink_splash_radius").forGetter(ThrowableExplodingSubDataRecord::inkSplashRadius),
				Codec.INT.fieldOf("fuse_time").forGetter(ThrowableExplodingSubDataRecord::fuseTime),
				Codec.FLOAT.fieldOf("throw_velocity").forGetter(ThrowableExplodingSubDataRecord::throwVelocity),
				Codec.FLOAT.fieldOf("throw_angle").forGetter(ThrowableExplodingSubDataRecord::throwAngle)
			).apply(inst, ThrowableExplodingSubDataRecord::new)
		);
		public static final ThrowableExplodingSubDataRecord DEFAULT = new ThrowableExplodingSubDataRecord(
			DamageRangesRecord.DEFAULT,
			SplashAroundDataRecord.DEFAULT,
			0,
			0,
			0,
			0
		);
		@Override
		public ThrowableExplodingSubDataRecord convertSelf()
		{
			return new ThrowableExplodingSubDataRecord(
				convert(damageRanges),
				convert(inkSplashes),
				inkSplashRadius / DistanceUnitsPerMinecraftSquare,
				fuseTime,
				throwVelocity / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick,
				throwAngle
			);
		}
	}
	public record BurstBombDataRecord(
		DamageRangesRecord damageRanges,
		SplashAroundDataRecord inkSplashes,
		float inkSplashRadius,
		float directDamage,
		float throwVelocity,
		float throwAngle
	) implements SubDataRecord<BurstBombDataRecord>
	{
		public static final MapCodec<BurstBombDataRecord> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				DamageRangesRecord.CODEC.fieldOf("damage_ranges").forGetter(BurstBombDataRecord::damageRanges),
				SplashAroundDataRecord.CODEC.fieldOf("ink_splashes").forGetter(BurstBombDataRecord::inkSplashes),
				Codec.FLOAT.fieldOf("ink_splash_radius").forGetter(BurstBombDataRecord::inkSplashRadius),
				Codec.FLOAT.fieldOf("contact_damage").forGetter(BurstBombDataRecord::directDamage),
				Codec.FLOAT.fieldOf("throw_velocity").forGetter(BurstBombDataRecord::throwVelocity),
				Codec.FLOAT.fieldOf("throw_angle").forGetter(BurstBombDataRecord::throwAngle)
			).apply(inst, BurstBombDataRecord::new)
		);
		public static final BurstBombDataRecord DEFAULT = new BurstBombDataRecord(
			DamageRangesRecord.DEFAULT,
			SplashAroundDataRecord.DEFAULT,
			0,
			0,
			0,
			0
		);
		@Override
		public BurstBombDataRecord convertSelf()
		{
			return new BurstBombDataRecord(
				convert(damageRanges),
				convert(inkSplashes),
				inkSplashRadius / DistanceUnitsPerMinecraftSquare,
				directDamage / SplatoonHealthPerMinecraftHealth,
				throwVelocity / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick,
				throwAngle
			);
		}
	}
	public record CurlingBombDataRecord(
		DamageRangesRecord damageRanges,
		SplashAroundDataRecord inkSplashes,
		FloatRange inkExplosionRange,
		FloatRange travelSpeedRange,
		FloatRange trailSizeRange,
		InkUsageDataRecord maxCookInkUsage,
		CommonRecords.IntRange fuseTime,
		float contactDamage,
		float maxCookRadiusBonus,
		int warningFrame
	) implements SubDataRecord<CurlingBombDataRecord>
	{
		public static final MapCodec<CurlingBombDataRecord> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				DamageRangesRecord.CODEC.fieldOf("damage_ranges").forGetter(CurlingBombDataRecord::damageRanges),
				SplashAroundDataRecord.CODEC.fieldOf("ink_splashes").forGetter(CurlingBombDataRecord::inkSplashes),
				FloatRange.CODEC.fieldOf("ink_explosion_range").forGetter(CurlingBombDataRecord::inkExplosionRange),
				FloatRange.CODEC.fieldOf("travel_speed_range").forGetter(CurlingBombDataRecord::travelSpeedRange),
				FloatRange.CODEC.fieldOf("trail_size_range").forGetter(CurlingBombDataRecord::trailSizeRange),
				InkUsageDataRecord.CODEC.fieldOf("max_cook_ink_usage").forGetter(CurlingBombDataRecord::maxCookInkUsage),
				CommonRecords.IntRange.CODEC.fieldOf("fuse_time_range").forGetter(CurlingBombDataRecord::fuseTime),
				Codec.FLOAT.optionalFieldOf("contact_damage", 20f).forGetter(CurlingBombDataRecord::contactDamage),
				Codec.FLOAT.optionalFieldOf("max_cook_explosion_radius_bonus", 3f).forGetter(CurlingBombDataRecord::maxCookRadiusBonus),
				Codec.INT.fieldOf("warning_frame").forGetter(CurlingBombDataRecord::warningFrame)
			).apply(inst, CurlingBombDataRecord::new)
		);
		public static final CurlingBombDataRecord DEFAULT = new CurlingBombDataRecord(
			DamageRangesRecord.DEFAULT,
			SplashAroundDataRecord.DEFAULT,
			FloatRange.ZERO,
			FloatRange.ZERO,
			FloatRange.ZERO,
			InkUsageDataRecord.DEFAULT,
			CommonRecords.IntRange.ZERO,
			20,
			3,
			60
		);
		@Override
		public CurlingBombDataRecord convertSelf()
		{
			return new CurlingBombDataRecord(
				convert(damageRanges),
				convert(inkSplashes),
				convertLength(inkExplosionRange),
				convertSpeed(travelSpeedRange),
				convertLength(trailSizeRange),
				convert(maxCookInkUsage),
				convertTime(fuseTime),
				contactDamage / SplatoonHealthPerMinecraftHealth,
				maxCookRadiusBonus / DistanceUnitsPerMinecraftSquare,
				warningFrame / SplatoonFramesPerMinecraftTick
			);
		}
	}
	public static class EmptyDataRecord implements SubDataRecord<EmptyDataRecord>
	{
		public static final EmptyDataRecord DEFAULT = new EmptyDataRecord();
		public static final Codec<EmptyDataRecord> CODEC = new Codec<>()
		{
			@Override
			public <T> DataResult<Pair<EmptyDataRecord, T>> decode(DynamicOps<T> ops, T input)
			{
				return DataResult.success(Pair.of(new EmptyDataRecord(), input));
			}
			@Override
			public <T> DataResult<T> encode(EmptyDataRecord input, DynamicOps<T> ops, T prefix)
			{
				return DataResult.success((T) new Object());
			}
		};
		@Override
		public EmptyDataRecord convertSelf()
		{
			return this;
		}
	}
}
