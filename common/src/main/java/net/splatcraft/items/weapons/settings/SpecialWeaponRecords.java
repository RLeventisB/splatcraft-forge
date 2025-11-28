package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.structs.RangedValueCollection;
import org.joml.Vector2f;

public class SpecialWeaponRecords
{
	public record StingRayDataRecord(
		byte startupTicks,
		byte shockwaveDelay,
		float mobilityOnUse,
		float damageCenter,
		float damageShockwave,
		float radiusCenter,
		float radiusShockwave,
		float turningValue,
		float turningValueWithShockwave,
		float revealRadius,
		float paintingRadius,
		float paintSearchRadius
	) implements DynamicDataRecord<StingRayDataRecord>
	{
		public static final MapCodec<StingRayDataRecord> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				Codec.BYTE.fieldOf("startup_ticks").forGetter(StingRayDataRecord::startupTicks),
				Codec.BYTE.fieldOf("shockwave_delay").forGetter(StingRayDataRecord::shockwaveDelay),
				Codec.FLOAT.fieldOf("mobility_on_use").forGetter(StingRayDataRecord::mobilityOnUse),
				Codec.FLOAT.fieldOf("damage_center").forGetter(StingRayDataRecord::damageCenter),
				Codec.FLOAT.fieldOf("damage_shockwave").forGetter(StingRayDataRecord::damageShockwave),
				Codec.FLOAT.fieldOf("radius_center").forGetter(StingRayDataRecord::radiusCenter),
				Codec.FLOAT.fieldOf("radius_shockwave").forGetter(StingRayDataRecord::radiusShockwave),
				Codec.FLOAT.fieldOf("turning_value").forGetter(StingRayDataRecord::turningValue),
				Codec.FLOAT.fieldOf("turning_value_with_shockwave").forGetter(StingRayDataRecord::turningValueWithShockwave),
				Codec.FLOAT.fieldOf("reveal_radius").forGetter(StingRayDataRecord::revealRadius),
				Codec.FLOAT.fieldOf("painting_radius").forGetter(StingRayDataRecord::paintingRadius),
				Codec.FLOAT.optionalFieldOf("painting_search_radius", 3000f).forGetter(StingRayDataRecord::paintSearchRadius)
			).apply(inst, StingRayDataRecord::new)
		);
		public static final ResourceLocation ID = Splatcraft.identifierOf("sting_ray");
		@Override
		public StingRayDataRecord convertSelf()
		{
			return new StingRayDataRecord(
				(byte) (startupTicks / SplatcraftConvertors.SplatoonFramesPerMinecraftTick),
				(byte) (shockwaveDelay / SplatcraftConvertors.SplatoonFramesPerMinecraftTick),
				mobilityOnUse,
				damageCenter / SplatcraftConvertors.SplatoonHealthPerMinecraftHealth * SplatcraftConvertors.SplatoonFramesPerMinecraftTick,
				damageShockwave / SplatcraftConvertors.SplatoonHealthPerMinecraftHealth * SplatcraftConvertors.SplatoonFramesPerMinecraftTick,
				radiusCenter / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare,
				radiusShockwave / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare,
				turningValue,
				turningValueWithShockwave,
				revealRadius / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare,
				paintingRadius / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare,
				paintSearchRadius / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare
			);
		}
	}
	public record InkJetDataRecord(
		RangedValueCollection thrustData,
		CommonRecords.ProjectileDataRecord projectile,
		BlasterWeaponSettings.DetonationRecord blast,
		float projectileSpeed,
		float initialStartup,
		float firingRepeatTicks,
		float impulseOnJump,
		int impulseCooldown,
		int recallTime,
		float exhaustDamage,
		float exhaustRange,
		float exhaustPaint,
		float maxMobility,
		Vector2f hitboxScale
	) implements DynamicDataRecord<InkJetDataRecord>
	{
		public static final MapCodec<InkJetDataRecord> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				RangedValueCollection.JETPACK_CODEC.fieldOf("thrust_data").forGetter(InkJetDataRecord::thrustData),
				CommonRecords.ProjectileDataRecord.CODEC.fieldOf("projectile").forGetter(InkJetDataRecord::projectile),
				BlasterWeaponSettings.DetonationRecord.NO_SPARK_CODEC.fieldOf("blast").forGetter(InkJetDataRecord::blast),
				Codec.FLOAT.fieldOf("projectile_speed").forGetter(InkJetDataRecord::projectileSpeed),
				Codec.FLOAT.fieldOf("fire_initial_startup").forGetter(InkJetDataRecord::initialStartup),
				Codec.FLOAT.fieldOf("fire_repeat_ticks").forGetter(InkJetDataRecord::firingRepeatTicks),
				Codec.FLOAT.fieldOf("impulse_on_jump").forGetter(InkJetDataRecord::impulseOnJump),
				Codec.INT.fieldOf("impulse_cooldown").forGetter(InkJetDataRecord::impulseCooldown),
				Codec.INT.fieldOf("recall_time").forGetter(InkJetDataRecord::recallTime),
				Codec.FLOAT.fieldOf("exhaust_damage").forGetter(InkJetDataRecord::exhaustDamage),
				Codec.FLOAT.fieldOf("exhaust_range").forGetter(InkJetDataRecord::exhaustRange),
				Codec.FLOAT.fieldOf("exhaust_paint").forGetter(InkJetDataRecord::exhaustPaint),
				Codec.FLOAT.fieldOf("max_mobility").forGetter(InkJetDataRecord::maxMobility),
				CodecUtils.Codecs.VECTOR2_MULTI_CODEC.optionalFieldOf("hitbox_scale", new Vector2f(1.1f)).forGetter(InkJetDataRecord::hitboxScale)
			).apply(inst, InkJetDataRecord::new)
		);
		public static final ResourceLocation ID = Splatcraft.identifierOf("inkjet");
		@Override
		public InkJetDataRecord convertSelf()
		{
			return new InkJetDataRecord(
				thrustData.cloneWithMultiplier(1f / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare, 1f),
				SplatcraftConvertors.convert(projectile),
				SplatcraftConvertors.convert(blast),
				projectileSpeed * SplatcraftConvertors.SplatoonFramesPerMinecraftTick / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare,
				initialStartup / SplatcraftConvertors.SplatoonFramesPerMinecraftTick,
				firingRepeatTicks / SplatcraftConvertors.SplatoonFramesPerMinecraftTick,
				impulseOnJump,
				impulseCooldown / SplatcraftConvertors.SplatoonFramesPerMinecraftTick,
				recallTime / SplatcraftConvertors.SplatoonFramesPerMinecraftTick,
				exhaustDamage / SplatcraftConvertors.SplatoonHealthPerMinecraftHealth,
				exhaustRange / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare,
				exhaustPaint / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare,
				maxMobility,
				hitboxScale
			);
		}
	}
	public record InkStormDataRecord(
		float throwVelocity,
		float specialCooldown,
		int riseTime,
		int formationTime,
		float cloudRiseHeight,
		float cloudRadius,
		float cloudSpeed,
		float cloudDuration,
		float damagePerSecond,
		float dropletsPerSecond,
		float dropletPaint
	) implements DynamicDataRecord<InkStormDataRecord>
	{
		public static final MapCodec<InkStormDataRecord> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				Codec.FLOAT.fieldOf("throw_velocity").forGetter(InkStormDataRecord::throwVelocity),
				Codec.FLOAT.fieldOf("special_cooldown").forGetter(InkStormDataRecord::specialCooldown),
				Codec.INT.fieldOf("rise_time").forGetter(InkStormDataRecord::riseTime),
				Codec.INT.fieldOf("formation_time").forGetter(InkStormDataRecord::formationTime),
				Codec.FLOAT.fieldOf("cloud_rise_height").forGetter(InkStormDataRecord::cloudRiseHeight),
				Codec.FLOAT.fieldOf("cloud_radius").forGetter(InkStormDataRecord::cloudRadius),
				Codec.FLOAT.fieldOf("cloud_speed").forGetter(InkStormDataRecord::cloudSpeed),
				Codec.FLOAT.fieldOf("cloud_duration").forGetter(InkStormDataRecord::cloudDuration),
				Codec.FLOAT.fieldOf("dps").forGetter(InkStormDataRecord::damagePerSecond),
				Codec.FLOAT.fieldOf("droplets_per_second").forGetter(InkStormDataRecord::dropletsPerSecond),
				Codec.FLOAT.fieldOf("droplet_paint_radius").forGetter(InkStormDataRecord::dropletPaint)
			).apply(inst, InkStormDataRecord::new)
		);
		public static final ResourceLocation ID = Splatcraft.identifierOf("ink_storm");
		@Override
		public InkStormDataRecord convertSelf()
		{
			return new InkStormDataRecord(
				throwVelocity * SplatcraftConvertors.SplatoonFramesPerMinecraftTick / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare,
				specialCooldown,
				riseTime / SplatcraftConvertors.SplatoonFramesPerMinecraftTick,
				formationTime / SplatcraftConvertors.SplatoonFramesPerMinecraftTick,
				cloudRiseHeight / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare,
				cloudRadius / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare,
				cloudSpeed * SplatcraftConvertors.SplatoonFramesPerMinecraftTick / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare,
				cloudDuration,
				damagePerSecond / SplatcraftConvertors.SplatoonHealthPerMinecraftHealth,
				dropletsPerSecond,
				dropletPaint / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare
			);
		}
	}
}
