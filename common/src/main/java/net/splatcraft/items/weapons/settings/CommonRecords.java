package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

import java.util.Optional;

public class CommonRecords
{
	// hello i'll put some notes about conversion between splatoon and minecraft distances here!!!!!!
	// so splatoon has an imaginary unit called Distance Unit (DU), and the distance between the lines in the shooting/test range is 50 DU
	// i took an aproximate size of the collider of the player in the game, which seems to be a cylinder with two semispheres, the entire shape has
	// 19,2308 DU of height, and 11,5385 DU of width (i could have used the model instead but the model is applied an scale in game, and while
	// i can decompile splatoon since this information isn't anywhere online, i don't really want to read 107MB of c code).
	// but here's the funny part!!! in minecraft the player is 1,8 blocks tall and 0,6 blocks wide, so in splatoon the ratio between
	// height and width is something like 1,65217, in minecraft it's 3!!!! so i converted both units to blocks (19 DU = 1,8 blocks, and
	// 11,5 DU = 0,6 blocks) and took the average of them both, which is 14,957264957264957264957264957266, also known simply as 15 DU
	// so, 15 DU = 1 block, and 1.5 IU (internal unit, which is the internal value in splatoon 3) = 1 block, since 10 DU = 1 IU
	// this is the third time but it seems that 12 DU = 1 block seems ok (i should put the raw values instead but im stupdi -> i actually did this!!!)
	// NOTE: obviously not all values will be 1:1 to this equation, there will be roundings, and in some cases adjustments,
	// like in the case that a weapon paints much less because of minecraft's cubic nature and the magic of rounding
	// also most of the data (in internal units) is taken from https://leanny.github.io/splat3/parameters.html or https://splatoonwiki.org/wiki/
	// also!!!!! most of the paint data is pretty complex (one value for close droplet paint radius, one value for far away droplet paint radius,
	// and one for any other type of droplet paint radius, and every single one of these is multiplied by 1.4, or 1.2, or 1 depending on the drop's
	// fall height, so screw all that,
	// ink_drop_coverage = the units in "All other ink droplets have a radius of x"
	//      * in case of blasters, its the units in "Ink droplets have a radius of x"
	//      * in the case of rollers, it uses the first unit's PaintRadiusShock on both attacks
	// ink_coverage_on_impact = the units in "Droplets that occur when they travel past y units of the player have a radius of x" * 1.2
	//      * in case of blasters, its the units in "The wall impact painting radius is x units"
	//      * in the case of rollers, it uses the first unit's PaintRadiusShock on both attacks
	// also³ here are some other conversions from values in splatoon (x) to minecraft (y), divisor means the current scale of DU per block, currently its 15
	// speed ->             y = x / divisor * 3
	// time ->              y = x / 3
	// damage ->            y = x / 5
	// damage over-time ->  y = x / 5 * 3
	// distance ->          y = x / divisor
	// drag ->              y = x ^ 3
	public record ProjectileDataRecord(
		ProjectileSizeRecord size,
		float lifeTicks,
		float delaySpeedMult,
		float horizontalDrag,
		float straightShotTicks,
		float gravity,
		float inkCoverageImpact,
		float inkDropCoverage,
		float distanceBetweenInkDrops,
		float baseDamage,
		float minDamage,
		float damageDecayStartTick,
		float damageDecayPerTick
	)
	{
		public static final MapCodec<ProjectileDataRecord> MAP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				ProjectileSizeRecord.CODEC.fieldOf("size").forGetter(ProjectileDataRecord::size),
				Codec.FLOAT.optionalFieldOf("lifespan", 600f).forGetter(ProjectileDataRecord::lifeTicks),
				Codec.FLOAT.optionalFieldOf("delay_speed_mult", 0.5f).forGetter(ProjectileDataRecord::delaySpeedMult),
				Codec.FLOAT.optionalFieldOf("horizontal_drag", 0.64F).forGetter(ProjectileDataRecord::horizontalDrag),
				Codec.FLOAT.optionalFieldOf("straight_shot_ticks", 0F).forGetter(ProjectileDataRecord::straightShotTicks),
				Codec.FLOAT.optionalFieldOf("gravity", 0.7F).forGetter(ProjectileDataRecord::gravity),
				Codec.FLOAT.optionalFieldOf("ink_coverage_on_impact").forGetter(r -> Optional.of(r.inkCoverageImpact)),
				Codec.FLOAT.optionalFieldOf("ink_drop_coverage").forGetter(r -> Optional.of(r.inkDropCoverage)),
				Codec.FLOAT.optionalFieldOf("distance_between_drops", 48F).forGetter(ProjectileDataRecord::distanceBetweenInkDrops),
				Codec.FLOAT.fieldOf("base_damage").forGetter(ProjectileDataRecord::baseDamage),
				Codec.FLOAT.optionalFieldOf("decayed_damage").forGetter(r -> Optional.of(r.minDamage)),
				Codec.FLOAT.optionalFieldOf("damage_decay_start_tick", 0F).forGetter(ProjectileDataRecord::damageDecayStartTick),
				Codec.FLOAT.optionalFieldOf("damage_decay_per_tick", 0F).forGetter(ProjectileDataRecord::damageDecayPerTick)
			).apply(instance, ProjectileDataRecord::create)
		);
		public static final Codec<ProjectileDataRecord> CODEC = MAP_CODEC.codec();
		public static final ProjectileDataRecord DEFAULT = new ProjectileDataRecord(ProjectileSizeRecord.DEFAULT, 600, 0.5f, 0.64F, 0, 0.7F, 0, 0, 48, 0, 0, 0, 0);
		public static ProjectileDataRecord create(ProjectileSizeRecord size, float lifeTicks, float delaySpeedMult, float horizontalDrag, float straightShotTicks, float gravity, Optional<Float> inkCoverageImpact, Optional<Float> inkDropCoverage, float distanceBetweenInkDrops, float baseDamage, Optional<Float> decayedDamage, float damageDecayStartTick, float damageDecayPerTick)
		{
			return new ProjectileDataRecord(size,
				lifeTicks,
				delaySpeedMult,
				horizontalDrag,
				straightShotTicks,
				gravity,
				inkCoverageImpact.orElse(size.hitboxRadius() * 0.85f),
				inkDropCoverage.orElse(size.hitboxRadius() * 0.75f),
				distanceBetweenInkDrops,
				baseDamage,
				decayedDamage.orElse(baseDamage),
				damageDecayStartTick,
				damageDecayPerTick);
		}
	}
	public record OptionalProjectileDataRecord(
		Optional<OptionalProjectileSizeRecord> size,
		Optional<Float> lifeTicks,
		Optional<Float> delaySpeedMult,
		Optional<Float> horizontalDrag,
		Optional<Float> straightShotTicks,
		Optional<Float> gravity,
		Optional<Float> inkCoverageImpact,
		Optional<Float> inkDropCoverage,
		Optional<Float> distanceBetweenInkDrops,
		Optional<Float> baseDamage,
		Optional<Float> minDamage,
		Optional<Float> damageDecayStartTick,
		Optional<Float> damageDecayPerTick
	)
	{
		public static final Codec<OptionalProjectileDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				OptionalProjectileSizeRecord.CODEC.optionalFieldOf("size").forGetter(OptionalProjectileDataRecord::size),
				Codec.FLOAT.optionalFieldOf("lifespan").forGetter(OptionalProjectileDataRecord::lifeTicks),
				Codec.FLOAT.optionalFieldOf("delay_speed_mult").forGetter(OptionalProjectileDataRecord::delaySpeedMult),
				Codec.FLOAT.optionalFieldOf("horizontal_drag").forGetter(OptionalProjectileDataRecord::horizontalDrag),
				Codec.FLOAT.optionalFieldOf("straight_shot_ticks").forGetter(OptionalProjectileDataRecord::straightShotTicks),
				Codec.FLOAT.optionalFieldOf("gravity").forGetter(OptionalProjectileDataRecord::gravity),
				Codec.FLOAT.optionalFieldOf("ink_coverage_on_impact").forGetter(OptionalProjectileDataRecord::inkCoverageImpact),
				Codec.FLOAT.optionalFieldOf("ink_drop_coverage").forGetter(OptionalProjectileDataRecord::inkDropCoverage),
				Codec.FLOAT.optionalFieldOf("distance_between_drops").forGetter(OptionalProjectileDataRecord::distanceBetweenInkDrops),
				Codec.FLOAT.optionalFieldOf("base_damage").forGetter(OptionalProjectileDataRecord::baseDamage),
				Codec.FLOAT.optionalFieldOf("decayed_damage").forGetter(OptionalProjectileDataRecord::minDamage),
				Codec.FLOAT.optionalFieldOf("damage_decay_start_tick").forGetter(OptionalProjectileDataRecord::damageDecayStartTick),
				Codec.FLOAT.optionalFieldOf("damage_decay_per_tick").forGetter(OptionalProjectileDataRecord::damageDecayPerTick)
			).apply(instance, OptionalProjectileDataRecord::new)
		);
		public static final OptionalProjectileDataRecord DEFAULT = new OptionalProjectileDataRecord(
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty()
		);
		public static OptionalProjectileDataRecord from(ProjectileDataRecord projectile) // this is horrible
		{
			return new OptionalProjectileDataRecord(
				Optional.of(OptionalProjectileSizeRecord.from(projectile.size)),
				Optional.of(projectile.lifeTicks),
				Optional.of(projectile.delaySpeedMult),
				Optional.of(projectile.horizontalDrag),
				Optional.of(projectile.straightShotTicks),
				Optional.of(projectile.gravity),
				Optional.of(projectile.inkCoverageImpact),
				Optional.of(projectile.inkDropCoverage),
				Optional.of(projectile.distanceBetweenInkDrops),
				Optional.of(projectile.baseDamage),
				Optional.of(projectile.minDamage),
				Optional.of(projectile.damageDecayStartTick),
				Optional.of(projectile.damageDecayPerTick)
			);
		}
		public static ProjectileDataRecord mergeWithBase(Optional<OptionalProjectileDataRecord> modified, ProjectileDataRecord base)
		{
			if (modified.isEmpty())
				return base;

			OptionalProjectileDataRecord modifiedGet = modified.get();
			return new ProjectileDataRecord(
				OptionalProjectileSizeRecord.mergeWithBase(modifiedGet.size(), base.size()),
				modifiedGet.lifeTicks().orElse(base.lifeTicks()),
				modifiedGet.delaySpeedMult().orElse(base.delaySpeedMult()),
				modifiedGet.horizontalDrag().orElse(base.horizontalDrag()),
				modifiedGet.straightShotTicks().orElse(base.straightShotTicks()),
				modifiedGet.gravity().orElse(base.gravity()),
				modifiedGet.inkCoverageImpact().orElse(base.inkCoverageImpact()),
				modifiedGet.inkDropCoverage().orElse(base.inkDropCoverage()),
				modifiedGet.distanceBetweenInkDrops().orElse(base.distanceBetweenInkDrops()),
				modifiedGet.baseDamage().orElse(base.baseDamage()),
				modifiedGet.minDamage().orElse(base.minDamage()),
				modifiedGet.damageDecayStartTick().orElse(base.damageDecayStartTick()),
				modifiedGet.damageDecayPerTick().orElse(base.damageDecayPerTick())
			);
		}
	}
	public record ProjectileSizeRecord(
		float hitboxRadius,
		float visualSize,
		float worldHitboxRadius
	)
	{
		public static final Codec<ProjectileSizeRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.optionalFieldOf("hitbox_radius", 2f).forGetter(ProjectileSizeRecord::hitboxRadius),
				Codec.FLOAT.optionalFieldOf("visual_size").forGetter(t -> Optional.of(t.visualSize())),
				Codec.FLOAT.optionalFieldOf("world_hitbox_radius").forGetter(t -> Optional.of(t.worldHitboxRadius()))
			).apply(instance, ProjectileSizeRecord::create)
		);
		public static final ProjectileSizeRecord DEFAULT = new ProjectileSizeRecord(2f, 6f, 2f);
		private static ProjectileSizeRecord create(float hitboxRadius, Optional<Float> visualSize, Optional<Float> worldHitboxRadius)
		{
			return new ProjectileSizeRecord(hitboxRadius, visualSize.orElse(hitboxRadius * 3), worldHitboxRadius.orElse(hitboxRadius));
		}
		public static ProjectileSizeRecord lerp(float delta, ProjectileSizeRecord size1, ProjectileSizeRecord size2)
		{
			return new ProjectileSizeRecord(
				Mth.lerp(delta, size1.hitboxRadius(), size2.hitboxRadius()),
				Mth.lerp(delta, size1.visualSize(), size2.visualSize()),
				Mth.lerp(delta, size1.worldHitboxRadius(), size2.worldHitboxRadius())
			);
		}
		public ProjectileSizeRecord scale(float hitboxScale, float visualScale, float worldScale)
		{
			return new ProjectileSizeRecord(
				hitboxRadius() * hitboxScale,
				visualSize() * visualScale,
				worldHitboxRadius() * worldScale
			);
		}
		public ProjectileSizeRecord divide(float denominatorHitbox, float denominatorVisual, float denominatorWorld)
		{
			return scale(1f / denominatorHitbox, 1f / denominatorVisual, 1f / denominatorWorld);
		}
	}
	public record OptionalProjectileSizeRecord(
		Optional<Float> hitboxRadius,
		Optional<Float> visualSize,
		Optional<Float> worldHitboxLength
	)
	{
		public static final Codec<OptionalProjectileSizeRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.optionalFieldOf("hitbox_radius").forGetter(OptionalProjectileSizeRecord::hitboxRadius),
				Codec.FLOAT.optionalFieldOf("visual_size").forGetter(OptionalProjectileSizeRecord::visualSize),
				Codec.FLOAT.optionalFieldOf("world_hitbox_radius").forGetter(OptionalProjectileSizeRecord::worldHitboxLength)
			).apply(instance, OptionalProjectileSizeRecord::new)
		);
		public static final OptionalProjectileSizeRecord DEFAULT = new OptionalProjectileSizeRecord(
			Optional.empty(),
			Optional.empty(),
			Optional.empty()
		);
		public static OptionalProjectileSizeRecord from(ProjectileSizeRecord size)
		{
			return new OptionalProjectileSizeRecord(
				Optional.of(size.hitboxRadius),
				Optional.of(size.visualSize),
				Optional.of(size.worldHitboxRadius)
			);
		}
		public static ProjectileSizeRecord mergeWithBase(Optional<OptionalProjectileSizeRecord> modified, ProjectileSizeRecord base)
		{
			if (modified.isEmpty())
				return base;

			OptionalProjectileSizeRecord modifiedGet = modified.get();
			return new ProjectileSizeRecord(
				modifiedGet.hitboxRadius().orElse(base.hitboxRadius()),
				modifiedGet.visualSize().orElse(base.visualSize()),
				modifiedGet.worldHitboxLength().orElse(base.worldHitboxRadius())
			);
		}
		public OptionalProjectileSizeRecord scale(float hitboxScale, float visualScale, float worldScale)
		{
			return new OptionalProjectileSizeRecord(
				hitboxRadius().map(v -> v * hitboxScale),
				visualSize().map(v -> v * visualScale),
				worldHitboxLength().map(v -> v * worldScale)
			);
		}
		public OptionalProjectileSizeRecord divide(float denominatorHitbox, float denominatorVisual, float denominatorWorld)
		{
			return scale(1f / denominatorHitbox, 1f / denominatorVisual, 1f / denominatorWorld);
		}
	}
	public record ShotDataRecord(
		float startupTicks,
		float squidStartupTicks,
		float repeatTicks,
		float endlagTicks,
		float miscEndlagTicks,
		float speed,
		int projectileCount,
		ShotDeviationDataRecord accuracyData,
		float pitchCompensation,
		float inkConsumption,
		float inkRecoveryCooldown
	)
	{
		public static final Codec<ShotDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.optionalFieldOf("startup_ticks", 0f).forGetter(ShotDataRecord::startupTicks),
				Codec.FLOAT.optionalFieldOf("startup_ticks_from_squid").forGetter(t -> Optional.of(t.squidStartupTicks)),
				Codec.FLOAT.optionalFieldOf("repeat_ticks", 1f).forGetter(ShotDataRecord::repeatTicks),
				Codec.FLOAT.optionalFieldOf("endlag_ticks", 1f).forGetter(ShotDataRecord::endlagTicks),
				Codec.FLOAT.optionalFieldOf("other_actions_endlag_ticks").forGetter(t -> Optional.of(t.miscEndlagTicks())),
				Codec.FLOAT.fieldOf("speed").forGetter(ShotDataRecord::speed),
				Codec.INT.optionalFieldOf("shot_count", 1).forGetter(ShotDataRecord::projectileCount),
				ShotDeviationDataRecord.CODEC.optionalFieldOf("accuracy_data", ShotDeviationDataRecord.PERFECT_DEFAULT).forGetter(ShotDataRecord::accuracyData),
				Codec.FLOAT.optionalFieldOf("pitch_compensation", 0f).forGetter(ShotDataRecord::pitchCompensation),
				Codec.FLOAT.fieldOf("ink_consumption").forGetter(ShotDataRecord::inkConsumption),
				Codec.FLOAT.fieldOf("ink_recovery_cooldown").forGetter(ShotDataRecord::inkRecoveryCooldown)
			).apply(instance, ShotDataRecord::create)
		);
		public static final ShotDataRecord DEFAULT = new ShotDataRecord(0, 0, 1f, 1, 1, 0, 1, ShotDeviationDataRecord.PERFECT_DEFAULT, 0, 0, 0);
		public static ShotDataRecord create(float startupTicks, Optional<Float> squidStartupTicks, float repeatTicks, float endlagTicks, Optional<Float> miscEndlagTicks, float speed, int projectileCount, ShotDeviationDataRecord accuracyData, float pitchCompensation, float inkConsumption, float inkRecoveryCooldown)
		{
			return new ShotDataRecord(startupTicks, squidStartupTicks.orElse(startupTicks), repeatTicks, endlagTicks, miscEndlagTicks.orElse(endlagTicks), speed, projectileCount, accuracyData, pitchCompensation, inkConsumption, inkRecoveryCooldown);
		}
		public float getFireRate()
		{
			return repeatTicks;
		}
	}
	public record OptionalShotDataRecord(
		Optional<Float> startupTicks,
		Optional<Float> squidStartupTicks,
		Optional<Float> repeatTicks,
		Optional<Float> endlagTicks,
		Optional<Float> miscEndlagTicks,
		Optional<Float> speed,
		Optional<Integer> projectileCount,
		Optional<OptionalShotDeviationDataRecord> accuracyData,
		Optional<Float> pitchCompensation,
		Optional<Float> inkConsumption,
		Optional<Float> inkRecoveryCooldown
	)
	{
		public static final Codec<OptionalShotDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.optionalFieldOf("startup_ticks").forGetter(OptionalShotDataRecord::startupTicks),
				Codec.FLOAT.optionalFieldOf("startup_ticks_from_squid").forGetter(OptionalShotDataRecord::startupTicks),
				Codec.FLOAT.optionalFieldOf("repeat_ticks").forGetter(OptionalShotDataRecord::repeatTicks),
				Codec.FLOAT.optionalFieldOf("endlag_ticks").forGetter(OptionalShotDataRecord::endlagTicks),
				Codec.FLOAT.optionalFieldOf("other_actions_endlag_ticks").forGetter(OptionalShotDataRecord::miscEndlagTicks),
				Codec.FLOAT.optionalFieldOf("speed").forGetter(OptionalShotDataRecord::speed),
				Codec.INT.optionalFieldOf("shot_count").forGetter(OptionalShotDataRecord::projectileCount),
				OptionalShotDeviationDataRecord.CODEC.optionalFieldOf("accuracy_data").forGetter(OptionalShotDataRecord::accuracyData),
				Codec.FLOAT.optionalFieldOf("pitch_compensation").forGetter(OptionalShotDataRecord::pitchCompensation),
				Codec.FLOAT.optionalFieldOf("ink_consumption").forGetter(OptionalShotDataRecord::inkConsumption),
				Codec.FLOAT.optionalFieldOf("ink_recovery_cooldown").forGetter(OptionalShotDataRecord::inkRecoveryCooldown)
			).apply(instance, OptionalShotDataRecord::new)
		);
		public static final OptionalShotDataRecord DEFAULT = new OptionalShotDataRecord(
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty()
		);
		public static OptionalShotDataRecord from(ShotDataRecord shot)
		{
			return new OptionalShotDataRecord(
				Optional.of(shot.startupTicks),
				Optional.of(shot.squidStartupTicks),
				Optional.of(shot.repeatTicks),
				Optional.of(shot.endlagTicks),
				Optional.of(shot.miscEndlagTicks),
				Optional.of(shot.speed),
				Optional.of(shot.projectileCount),
				Optional.of(OptionalShotDeviationDataRecord.from(shot.accuracyData)),
				Optional.of(shot.pitchCompensation),
				Optional.of(shot.inkConsumption),
				Optional.of(shot.inkRecoveryCooldown)
			);
		}
		public static ShotDataRecord mergeWithBase(Optional<OptionalShotDataRecord> modified, ShotDataRecord base)
		{
			if (modified.isEmpty())
				return base;

			OptionalShotDataRecord modifiedGet = modified.get();
			return new ShotDataRecord(
				modifiedGet.startupTicks().orElse(base.startupTicks()),
				modifiedGet.squidStartupTicks().orElse(base.squidStartupTicks()),
				modifiedGet.repeatTicks().orElse(base.repeatTicks()),
				modifiedGet.endlagTicks().orElse(base.endlagTicks()),
				modifiedGet.miscEndlagTicks().orElse(base.miscEndlagTicks()),
				modifiedGet.speed().orElse(base.speed()),
				modifiedGet.projectileCount().orElse(base.projectileCount()),
				OptionalShotDeviationDataRecord.mergeWithBase(modifiedGet.accuracyData, base.accuracyData),
				modifiedGet.pitchCompensation().orElse(base.pitchCompensation()),
				modifiedGet.inkConsumption().orElse(base.inkConsumption()),
				modifiedGet.inkRecoveryCooldown().orElse(base.inkRecoveryCooldown())
			);
		}
	}
	public record ShotDeviationDataRecord(
		float groundShotDeviation,
		float airborneShotDeviation,
		float minDeviateChance,
		float maxDeviateChance,
		float deviationChanceWhenAirborne,
		float chanceIncreasePerShot,
		float chanceDecreaseDelay,
		float chanceDecreasePerTick,
		float airborneContractDelay,
		float airborneContractTimeToDecrease
	)
	{
		public static final Codec<ShotDeviationDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.floatRange(0, Float.MAX_VALUE).fieldOf("ground_deviation_degrees").forGetter(ShotDeviationDataRecord::groundShotDeviation),
				Codec.floatRange(0, Float.MAX_VALUE).fieldOf("airborne_deviation_degrees").forGetter(ShotDeviationDataRecord::airborneShotDeviation),

				Codec.floatRange(0, 1).optionalFieldOf("chance_min", 0.01f).forGetter(ShotDeviationDataRecord::minDeviateChance),
				Codec.floatRange(0, 1).optionalFieldOf("chance_max", 0.25f).forGetter(ShotDeviationDataRecord::maxDeviateChance),
				Codec.floatRange(0, 1).optionalFieldOf("chance_set_airborne", 0.4f).forGetter(ShotDeviationDataRecord::deviationChanceWhenAirborne),
				Codec.floatRange(0, 1).optionalFieldOf("chance_increase_per_shot", 0.01f).forGetter(ShotDeviationDataRecord::chanceIncreasePerShot),

				Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("time_inactive_to_decrease", 6.0f).forGetter(ShotDeviationDataRecord::chanceDecreaseDelay),
				Codec.FLOAT.optionalFieldOf("chance_decrease_when_inactive", 0.015f).forGetter(ShotDeviationDataRecord::chanceDecreasePerTick),

				Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("delay_to_decrease_airborne_deviation", 25.0f).forGetter(ShotDeviationDataRecord::airborneContractDelay),
				Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("time_to_decrease_airborne_deviation", 70.0f).forGetter(ShotDeviationDataRecord::airborneContractTimeToDecrease)
			).apply(instance, ShotDeviationDataRecord::new)
		);
		public static final ShotDeviationDataRecord DEFAULT = new ShotDeviationDataRecord(5, 12, 0.01f, 0.25f, 0.4f, 0.01f, 6f, 0.015f, 25f, 70f);
		public static final ShotDeviationDataRecord PERFECT_DEFAULT = new ShotDeviationDataRecord(0, 0, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f);
		public float getMaximumDeviation()
		{
			return Math.max(Math.max(minDeviateChance, maxDeviateChance), deviationChanceWhenAirborne);
		}
		public static ShotDeviationDataRecord lerp(float delta, ShotDeviationDataRecord dataStart, ShotDeviationDataRecord dataEnd)
		{
			return new ShotDeviationDataRecord(
				Mth.lerp(delta, dataStart.groundShotDeviation(), dataEnd.groundShotDeviation()),
				Mth.lerp(delta, dataStart.airborneShotDeviation(), dataEnd.airborneShotDeviation()),
				Mth.lerp(delta, dataStart.minDeviateChance(), dataEnd.minDeviateChance()),
				Mth.lerp(delta, dataStart.maxDeviateChance(), dataEnd.maxDeviateChance()),
				Mth.lerp(delta, dataStart.deviationChanceWhenAirborne(), dataEnd.deviationChanceWhenAirborne()),
				Mth.lerp(delta, dataStart.chanceIncreasePerShot(), dataEnd.chanceIncreasePerShot()),
				Mth.lerp(delta, dataStart.chanceDecreaseDelay(), dataEnd.chanceDecreaseDelay()),
				Mth.lerp(delta, dataStart.chanceDecreasePerTick(), dataEnd.chanceDecreasePerTick()),
				Mth.lerp(delta, dataStart.airborneContractDelay(), dataEnd.airborneContractDelay()),
				Mth.lerp(delta, dataStart.airborneContractTimeToDecrease(), dataEnd.airborneContractTimeToDecrease())
			);
		}
	}
	public record OptionalShotDeviationDataRecord(
		Optional<Float> groundShotDeviation,
		Optional<Float> airborneShotDeviation,
		Optional<Float> minDeviateChance,
		Optional<Float> maxDeviateChance,
		Optional<Float> deviationChanceWhenAirborne,
		Optional<Float> chanceIncreasePerShot,
		Optional<Float> chanceDecreaseDelay,
		Optional<Float> chanceDecreasePerTick,
		Optional<Float> airborneContractDelay,
		Optional<Float> airborneContractTimeToDecrease
	)
	{
		public static final Codec<OptionalShotDeviationDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("ground_deviation_degrees").forGetter(OptionalShotDeviationDataRecord::groundShotDeviation),
				Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("airborne_deviation_degrees").forGetter(OptionalShotDeviationDataRecord::airborneShotDeviation),

				Codec.floatRange(0, 1).optionalFieldOf("chance_min").forGetter(OptionalShotDeviationDataRecord::minDeviateChance),
				Codec.floatRange(0, 1).optionalFieldOf("chance_max").forGetter(OptionalShotDeviationDataRecord::maxDeviateChance),
				Codec.floatRange(0, 1).optionalFieldOf("chance_set_airborne").forGetter(OptionalShotDeviationDataRecord::deviationChanceWhenAirborne),
				Codec.floatRange(0, 1).optionalFieldOf("chance_increase_per_shot").forGetter(OptionalShotDeviationDataRecord::chanceIncreasePerShot),

				Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("time_inactive_to_decrease").forGetter(OptionalShotDeviationDataRecord::chanceDecreaseDelay),
				Codec.FLOAT.optionalFieldOf("chance_decrease_when_inactive").forGetter(OptionalShotDeviationDataRecord::chanceDecreasePerTick),

				Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("delay_to_decrease_airborne_deviation").forGetter(OptionalShotDeviationDataRecord::airborneContractDelay),
				Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("time_to_decrease_airborne_deviation").forGetter(OptionalShotDeviationDataRecord::airborneContractTimeToDecrease)
			).apply(instance, OptionalShotDeviationDataRecord::new)
		);
		public static final OptionalShotDeviationDataRecord DEFAULT = new OptionalShotDeviationDataRecord(Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty()
		);
		public static OptionalShotDeviationDataRecord from(ShotDeviationDataRecord deviation)
		{
			return new OptionalShotDeviationDataRecord(
				Optional.of(deviation.groundShotDeviation),
				Optional.of(deviation.airborneShotDeviation),
				Optional.of(deviation.minDeviateChance),
				Optional.of(deviation.maxDeviateChance),
				Optional.of(deviation.deviationChanceWhenAirborne),
				Optional.of(deviation.chanceIncreasePerShot),
				Optional.of(deviation.chanceDecreaseDelay),
				Optional.of(deviation.chanceDecreasePerTick),
				Optional.of(deviation.airborneContractDelay),
				Optional.of(deviation.airborneContractTimeToDecrease)
			);
		}
		public static ShotDeviationDataRecord mergeWithBase(Optional<OptionalShotDeviationDataRecord> modified, ShotDeviationDataRecord base)
		{
			if (modified.isEmpty())
				return base;

			OptionalShotDeviationDataRecord modifiedGet = modified.get();
			return new ShotDeviationDataRecord(
				modifiedGet.groundShotDeviation().orElse(base.groundShotDeviation()),
				modifiedGet.airborneShotDeviation().orElse(base.airborneShotDeviation()),
				modifiedGet.minDeviateChance().orElse(base.minDeviateChance()),
				modifiedGet.maxDeviateChance().orElse(base.maxDeviateChance()),
				modifiedGet.deviationChanceWhenAirborne().orElse(base.deviationChanceWhenAirborne()),
				modifiedGet.chanceIncreasePerShot().orElse(base.chanceIncreasePerShot()),
				modifiedGet.chanceDecreaseDelay().orElse(base.chanceDecreaseDelay()),
				modifiedGet.chanceDecreasePerTick().orElse(base.chanceDecreasePerTick()),
				modifiedGet.airborneContractDelay().orElse(base.airborneContractDelay()),
				modifiedGet.airborneContractTimeToDecrease().orElse(base.airborneContractTimeToDecrease())
			);
		}
	}
	public record InkUsageDataRecord(
		float consumption,
		float recoveryCooldown
	)
	{
		public static final Codec<InkUsageDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.fieldOf("consumption").forGetter(InkUsageDataRecord::consumption),
				Codec.FLOAT.optionalFieldOf("recovery_cooldown", 20f).forGetter(InkUsageDataRecord::recoveryCooldown)
			).apply(instance, InkUsageDataRecord::new)
		);
		public static final InkUsageDataRecord DEFAULT = new InkUsageDataRecord(0, 20);
	}
}