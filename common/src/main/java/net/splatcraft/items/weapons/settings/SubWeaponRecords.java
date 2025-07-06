package net.splatcraft.items.weapons.settings;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.splatcraft.items.weapons.settings.CommonRecords.InkUsageDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponSettings.SplashAroundDataRecord;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.DamageRangesRecord;
import net.splatcraft.util.NumberRange;
import net.splatcraft.util.NumberRange.FloatRange;
import org.joml.Vector2f;

import static net.splatcraft.data.SplatcraftConvertors.*;

public class SubWeaponRecords
{
	public record ThrowableExplodingSubDataRecord(
		DamageRangesRecord damageRanges,
		SplashAroundDataRecord inkSplashes,
		float inkSplashRadius,
		int fuseTime,
		float throwVelocity,
		float throwerImpulse,
		float pitchOffset
	) implements DynamicDataRecord<ThrowableExplodingSubDataRecord>
	{
		public static final MapCodec<ThrowableExplodingSubDataRecord> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				DamageRangesRecord.CODEC.fieldOf("damage_ranges").forGetter(ThrowableExplodingSubDataRecord::damageRanges),
				SplashAroundDataRecord.CODEC.fieldOf("ink_splashes").forGetter(ThrowableExplodingSubDataRecord::inkSplashes),
				Codec.FLOAT.fieldOf("ink_splash_radius").forGetter(ThrowableExplodingSubDataRecord::inkSplashRadius),
				Codec.INT.fieldOf("fuse_time").forGetter(ThrowableExplodingSubDataRecord::fuseTime),
				Codec.FLOAT.fieldOf("throw_velocity").forGetter(ThrowableExplodingSubDataRecord::throwVelocity),
				Codec.FLOAT.optionalFieldOf("thrower_impulse", 1f).forGetter(ThrowableExplodingSubDataRecord::throwerImpulse),
				Codec.FLOAT.optionalFieldOf("pitch_offset", -5f).forGetter(ThrowableExplodingSubDataRecord::pitchOffset)
			).apply(inst, ThrowableExplodingSubDataRecord::new)
		);
		public static final ThrowableExplodingSubDataRecord DEFAULT = new ThrowableExplodingSubDataRecord(
			DamageRangesRecord.DEFAULT,
			SplashAroundDataRecord.DEFAULT,
			0,
			0,
			0,
			0.8f,
			-5f
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
				throwerImpulse,
				pitchOffset
			);
		}
	}
	public record BurstBombDataRecord(
		DamageRangesRecord damageRanges,
		SplashAroundDataRecord inkSplashes,
		float inkSplashRadius,
		float directDamage,
		float throwVelocity,
		float throwerImpulse,
		float pitchOffset
	) implements DynamicDataRecord<BurstBombDataRecord>
	{
		public static final MapCodec<BurstBombDataRecord> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				DamageRangesRecord.CODEC.fieldOf("damage_ranges").forGetter(BurstBombDataRecord::damageRanges),
				SplashAroundDataRecord.CODEC.fieldOf("ink_splashes").forGetter(BurstBombDataRecord::inkSplashes),
				Codec.FLOAT.fieldOf("ink_splash_radius").forGetter(BurstBombDataRecord::inkSplashRadius),
				Codec.FLOAT.fieldOf("contact_damage").forGetter(BurstBombDataRecord::directDamage),
				Codec.FLOAT.fieldOf("throw_velocity").forGetter(BurstBombDataRecord::throwVelocity),
				Codec.FLOAT.optionalFieldOf("thrower_impulse", 1f).forGetter(BurstBombDataRecord::throwerImpulse),
				Codec.FLOAT.optionalFieldOf("pitch_offset", -5f).forGetter(BurstBombDataRecord::pitchOffset)
			).apply(inst, BurstBombDataRecord::new)
		);
		public static final BurstBombDataRecord DEFAULT = new BurstBombDataRecord(
			DamageRangesRecord.DEFAULT,
			SplashAroundDataRecord.DEFAULT,
			0,
			0,
			0,
			0.8f,
			-5f
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
				throwerImpulse,
				pitchOffset
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
		NumberRange.IntRange fuseTime,
		float contactDamage,
		float throwAngle,
		float maxCookRadiusBonus,
		boolean bounceOnEntityHit,
		int warningFrame
	) implements DynamicDataRecord<CurlingBombDataRecord>
	{
		public static final MapCodec<CurlingBombDataRecord> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				DamageRangesRecord.CODEC.fieldOf("damage_ranges").forGetter(CurlingBombDataRecord::damageRanges),
				SplashAroundDataRecord.CODEC.fieldOf("ink_splashes").forGetter(CurlingBombDataRecord::inkSplashes),
				NumberRange.FloatRange.CODEC.fieldOf("ink_explosion_range").forGetter(CurlingBombDataRecord::inkExplosionRange),
				NumberRange.FloatRange.CODEC.fieldOf("travel_speed_range").forGetter(CurlingBombDataRecord::travelSpeedRange),
				NumberRange.FloatRange.CODEC.fieldOf("trail_size_range").forGetter(CurlingBombDataRecord::trailSizeRange),
				InkUsageDataRecord.CODEC.fieldOf("max_cook_ink_usage").forGetter(CurlingBombDataRecord::maxCookInkUsage),
				NumberRange.IntRange.CODEC.fieldOf("fuse_time_range").forGetter(CurlingBombDataRecord::fuseTime),
				Codec.FLOAT.optionalFieldOf("contact_damage", 20f).forGetter(CurlingBombDataRecord::contactDamage),
				Codec.FLOAT.optionalFieldOf("throw_angle", -20f).forGetter(CurlingBombDataRecord::throwAngle),
				Codec.FLOAT.optionalFieldOf("max_cook_explosion_radius_bonus", 3f).forGetter(CurlingBombDataRecord::maxCookRadiusBonus),
				Codec.BOOL.optionalFieldOf("bounce_on_entity_hit", false).forGetter(CurlingBombDataRecord::bounceOnEntityHit),
				Codec.INT.fieldOf("warning_frame").forGetter(CurlingBombDataRecord::warningFrame)
			).apply(inst, CurlingBombDataRecord::new)
		);
		public static final CurlingBombDataRecord DEFAULT = new CurlingBombDataRecord(
			DamageRangesRecord.DEFAULT,
			SplashAroundDataRecord.DEFAULT,
			NumberRange.FloatRange.ZERO,
			NumberRange.FloatRange.ZERO,
			NumberRange.FloatRange.ZERO,
			InkUsageDataRecord.DEFAULT,
			NumberRange.IntRange.ZERO,
			20,
			-20,
			3,
			false,
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
				throwAngle,
				maxCookRadiusBonus / DistanceUnitsPerMinecraftSquare,
				bounceOnEntityHit,
				warningFrame / SplatoonFramesPerMinecraftTick
			);
		}
	}
	public record TorpedoDataRecord(
		DamageRangesRecord mainExplosionDamageRange,
		DamageRangesRecord dropletDamageRange,
		SplashAroundDataRecord dropletData,
		float mainInkSplashRadius,
		float health,
		float throwVelocity,
		float throwerImpulse,
		float pitchOffset,
		Vector2f searchRange,
		float moveSpeed,
		int searchDelay,
		int movementDelay,
		int fuseTime
	) implements DynamicDataRecord<TorpedoDataRecord>
	{
		public static final MapCodec<TorpedoDataRecord> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				DamageRangesRecord.CODEC.fieldOf("main_explosion_damage_ranges").forGetter(TorpedoDataRecord::mainExplosionDamageRange),
				DamageRangesRecord.CODEC.fieldOf("droplet_damage_ranges").forGetter(TorpedoDataRecord::dropletDamageRange),
				SplashAroundDataRecord.CODEC.fieldOf("droplet_data").forGetter(TorpedoDataRecord::dropletData),
				Codec.FLOAT.fieldOf("main_ink_splash_radius").forGetter(TorpedoDataRecord::mainInkSplashRadius),
				Codec.FLOAT.fieldOf("health").forGetter(TorpedoDataRecord::health),
				Codec.FLOAT.fieldOf("throw_velocity").forGetter(TorpedoDataRecord::throwVelocity),
				Codec.FLOAT.optionalFieldOf("thrower_impulse", 1f).forGetter(TorpedoDataRecord::throwerImpulse),
				Codec.FLOAT.optionalFieldOf("pitch_offset", 0f).forGetter(TorpedoDataRecord::pitchOffset),
				CodecUtils.Codecs.VECTOR2_MULTI_CODEC.optionalFieldOf("search_range", new Vector2f(100f, 100f)).forGetter(TorpedoDataRecord::searchRange),
				Codec.FLOAT.optionalFieldOf("move_speed", 6f).forGetter(TorpedoDataRecord::moveSpeed),
				Codec.INT.optionalFieldOf("search_delay", 60).forGetter(TorpedoDataRecord::searchDelay),
				Codec.INT.optionalFieldOf("movement_delay", 60).forGetter(TorpedoDataRecord::movementDelay),
				Codec.INT.optionalFieldOf("fuse_time", 30).forGetter(TorpedoDataRecord::movementDelay)
			).apply(inst, TorpedoDataRecord::new)
		);
		@Override
		public TorpedoDataRecord convertSelf()
		{
			return new TorpedoDataRecord(
				convert(mainExplosionDamageRange),
				convert(dropletDamageRange),
				convert(dropletData),
				mainInkSplashRadius / DistanceUnitsPerMinecraftSquare,
				health / SplatoonHealthPerMinecraftHealth,
				throwVelocity / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick,
				throwerImpulse,
				pitchOffset,
				searchRange.div(DistanceUnitsPerMinecraftSquare, new Vector2f()),
				moveSpeed / DistanceUnitsPerMinecraftSquare * SplatoonFramesPerMinecraftTick,
				searchDelay / SplatoonFramesPerMinecraftTick,
				movementDelay / SplatoonFramesPerMinecraftTick,
				fuseTime / SplatoonFramesPerMinecraftTick
			);
		}
	}
	public static class EmptyDataRecord implements DynamicDataRecord<EmptyDataRecord>
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
