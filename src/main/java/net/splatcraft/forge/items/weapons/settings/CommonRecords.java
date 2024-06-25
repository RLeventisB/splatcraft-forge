package net.splatcraft.forge.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public class CommonRecords
{
	public record ProjectileDataRecord(
		float size,
		int lifeTicks,
		float speed,
		float horizontalDrag,
		int straightShotTicks,
		float gravity,
		float inkCoverageImpact,
		float inkTrailCoverage,
		int inkTrailCooldown,
		float baseDamage,
		float minDamage,
		int damageDecayStartTick,
		float damageDecayPerTick
	)
	{
		public static final Codec<ProjectileDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.fieldOf("size").forGetter(ProjectileDataRecord::size),
				Codec.INT.optionalFieldOf("lifespan", 600).forGetter(ProjectileDataRecord::lifeTicks),
				Codec.FLOAT.fieldOf("speed").forGetter(ProjectileDataRecord::speed),
				Codec.FLOAT.optionalFieldOf("horizontal_drag", 0.8F).forGetter(ProjectileDataRecord::horizontalDrag),
				Codec.INT.optionalFieldOf("straight_shot_ticks", 0).forGetter(ProjectileDataRecord::straightShotTicks),
				Codec.FLOAT.optionalFieldOf("gravity", 0.075f).forGetter(ProjectileDataRecord::gravity),
				Codec.FLOAT.optionalFieldOf("ink_coverage_on_impact").forGetter(r -> Optional.of(r.inkCoverageImpact)),
				Codec.FLOAT.optionalFieldOf("ink_trail_coverage").forGetter(r -> Optional.of(r.inkTrailCoverage)),
				Codec.INT.optionalFieldOf("ink_trail_tick_interval", 4).forGetter(ProjectileDataRecord::inkTrailCooldown),
				Codec.FLOAT.fieldOf("base_damage").forGetter(ProjectileDataRecord::baseDamage),
				Codec.FLOAT.optionalFieldOf("decayed_damage").forGetter(r -> Optional.of(r.minDamage)),
				Codec.INT.optionalFieldOf("damage_decay_start_tick", 0).forGetter(ProjectileDataRecord::damageDecayStartTick),
				Codec.FLOAT.optionalFieldOf("damage_decay_per_tick", 0f).forGetter(ProjectileDataRecord::damageDecayPerTick)
			).apply(instance, ProjectileDataRecord::create)
		);
		public static final ProjectileDataRecord DEFAULT = new ProjectileDataRecord(0, 600, 0, 0.8F, 0, 0.075F, 0, 0, 4, 0, 0, 0, 0);
		private static ProjectileDataRecord create(float size, int lifeTicks, float speed, float horizontalDrag, int straightShotTicks, float gravity, Optional<Float> inkCoverageImpact, Optional<Float> inkTrailCoverage, int inkTrailCooldown, float baseDamage, Optional<Float> decayedDamage, int damageDecayStartTick, float damageDecayPerTick)
		{
			return new ProjectileDataRecord(size, lifeTicks, speed, horizontalDrag, straightShotTicks, gravity, inkCoverageImpact.orElse(size * 0.85f), inkTrailCoverage.orElse(size * 0.75f), inkTrailCooldown, baseDamage, decayedDamage.orElse(baseDamage), damageDecayStartTick, damageDecayPerTick);
		}
	}
	public record OptionalProjectileDataRecord(
		Optional<Float> size,
		Optional<Integer> lifeTicks,
		Optional<Float> speed,
		Optional<Float> horizontalDrag,
		Optional<Integer> straightShotTicks,
		Optional<Float> gravity,
		Optional<Float> inkCoverageImpact,
		Optional<Float> inkTrailCoverage,
		Optional<Integer> inkTrailCooldown,
		Optional<Float> baseDamage,
		Optional<Float> minDamage,
		Optional<Integer> damageDecayStartTick,
		Optional<Float> damageDecayPerTick
	)
	{
		public static final Codec<OptionalProjectileDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.optionalFieldOf("size").forGetter(OptionalProjectileDataRecord::size),
				Codec.INT.optionalFieldOf("lifespan").forGetter(OptionalProjectileDataRecord::lifeTicks),
				Codec.FLOAT.optionalFieldOf("speed").forGetter(OptionalProjectileDataRecord::speed),
				Codec.FLOAT.optionalFieldOf("horizontal_drag").forGetter(OptionalProjectileDataRecord::horizontalDrag),
				Codec.INT.optionalFieldOf("straight_shot_ticks").forGetter(OptionalProjectileDataRecord::straightShotTicks),
				Codec.FLOAT.optionalFieldOf("gravity").forGetter(OptionalProjectileDataRecord::gravity),
				Codec.FLOAT.optionalFieldOf("ink_coverage_on_impact").forGetter(OptionalProjectileDataRecord::inkCoverageImpact),
				Codec.FLOAT.optionalFieldOf("ink_trail_coverage").forGetter(OptionalProjectileDataRecord::inkTrailCoverage),
				Codec.INT.optionalFieldOf("ink_trail_tick_interval").forGetter(OptionalProjectileDataRecord::inkTrailCooldown),
				Codec.FLOAT.optionalFieldOf("base_damage").forGetter(OptionalProjectileDataRecord::baseDamage),
				Codec.FLOAT.optionalFieldOf("decayed_damage").forGetter(OptionalProjectileDataRecord::minDamage),
				Codec.INT.optionalFieldOf("damage_decay_start_tick").forGetter(OptionalProjectileDataRecord::damageDecayStartTick),
				Codec.FLOAT.optionalFieldOf("damage_decay_per_tick").forGetter(OptionalProjectileDataRecord::damageDecayPerTick)
			).apply(instance, OptionalProjectileDataRecord::new)
		);
		public static Optional<OptionalProjectileDataRecord> convert(ProjectileDataRecord projectile) // this is horrible
		{
			return Optional.of(new OptionalProjectileDataRecord(
				Optional.of(projectile.size),
				Optional.of(projectile.lifeTicks),
				Optional.of(projectile.speed),
				Optional.of(projectile.horizontalDrag),
				Optional.of(projectile.straightShotTicks),
				Optional.of(projectile.gravity),
				Optional.of(projectile.inkCoverageImpact),
				Optional.of(projectile.inkTrailCoverage),
				Optional.of(projectile.inkTrailCooldown),
				Optional.of(projectile.baseDamage),
				Optional.of(projectile.minDamage),
				Optional.of(projectile.damageDecayStartTick),
				Optional.of(projectile.damageDecayPerTick)));
		}
	}
	public record ShotDataRecord(
		int startupTicks,
		int endlagTicks,
		int firingSpeed,
		int projectileCount,
		float groundInaccuracy,
		float airborneInaccuracy,
		float pitchCompensation,
		float inkConsumption,
		int inkRecoveryCooldown
	)
	{
		public static final Codec<ShotDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.INT.optionalFieldOf("startup_ticks", 0).forGetter(ShotDataRecord::startupTicks),
				Codec.INT.optionalFieldOf("endlag_ticks", 0).forGetter(ShotDataRecord::endlagTicks),
				Codec.INT.fieldOf("firing_speed").forGetter(ShotDataRecord::firingSpeed),
				Codec.INT.optionalFieldOf("shot_count", 1).forGetter(ShotDataRecord::projectileCount),
				Codec.FLOAT.fieldOf("ground_inaccuracy").forGetter(ShotDataRecord::groundInaccuracy),
				Codec.FLOAT.optionalFieldOf("airborne_inaccuracy").forGetter(r -> Optional.of(r.airborneInaccuracy)),
				Codec.FLOAT.optionalFieldOf("pitch_compensation", 0f).forGetter(ShotDataRecord::pitchCompensation),
				Codec.FLOAT.fieldOf("ink_consumption").forGetter(ShotDataRecord::inkConsumption),
				Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(ShotDataRecord::inkRecoveryCooldown)
			).apply(instance, ShotDataRecord::create)
		);
		public static final ShotDataRecord DEFAULT = new ShotDataRecord(0, 0, 0, 1, 0, 0, 0, 0, 0);
		public static ShotDataRecord create(int startupTicks, int endlagTicks, int firingSpeed, int projectileCount, float groundInaccuracy, Optional<Float> airborneInaccuracy, float pitchCompensation, float inkConsumption, int inkRecoveryCooldown)
		{
			return new ShotDataRecord(startupTicks, endlagTicks, firingSpeed, projectileCount, groundInaccuracy, airborneInaccuracy.orElse(groundInaccuracy), pitchCompensation, inkConsumption, inkRecoveryCooldown);
		}
	}
	public record OptionalShotDataRecord(
		Optional<Integer> startupTicks,
		Optional<Integer> endlagTicks,
		Optional<Integer> firingSpeed,
		Optional<Integer> projectileCount,
		Optional<Float> groundInaccuracy,
		Optional<Float> airborneInaccuracy,
		Optional<Float> pitchCompensation,
		Optional<Float> inkConsumption,
		Optional<Integer> inkRecoveryCooldown
	)
	{
		public static final Codec<OptionalShotDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.INT.optionalFieldOf("startup_ticks").forGetter(OptionalShotDataRecord::startupTicks),
				Codec.INT.optionalFieldOf("endlag_ticks").forGetter(OptionalShotDataRecord::endlagTicks),
				Codec.INT.optionalFieldOf("firing_speed").forGetter(OptionalShotDataRecord::firingSpeed),
				Codec.INT.optionalFieldOf("shot_count").forGetter(OptionalShotDataRecord::projectileCount),
				Codec.FLOAT.optionalFieldOf("ground_inaccuracy").forGetter(OptionalShotDataRecord::groundInaccuracy),
				Codec.FLOAT.optionalFieldOf("airborne_inaccuracy").forGetter(OptionalShotDataRecord::airborneInaccuracy),
				Codec.FLOAT.optionalFieldOf("pitch_compensation").forGetter(OptionalShotDataRecord::pitchCompensation),
				Codec.FLOAT.optionalFieldOf("ink_consumption").forGetter(OptionalShotDataRecord::inkConsumption),
				Codec.INT.optionalFieldOf("ink_recovery_cooldown").forGetter(OptionalShotDataRecord::inkRecoveryCooldown)
			).apply(instance, OptionalShotDataRecord::new)
		);
		public static Optional<OptionalShotDataRecord> convert(ShotDataRecord shot)
		{
			return Optional.of(new OptionalShotDataRecord(
				Optional.of(shot.startupTicks),
				Optional.of(shot.endlagTicks),
				Optional.of(shot.firingSpeed),
				Optional.of(shot.projectileCount),
				Optional.of(shot.groundInaccuracy),
				Optional.of(shot.airborneInaccuracy),
				Optional.of(shot.pitchCompensation),
				Optional.of(shot.inkConsumption),
				Optional.of(shot.inkRecoveryCooldown)
			));
		}
	}
}