package net.splatcraft.forge.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
    // but guess what!!!! this teorically correct scale looks wrong, (range blaster gets like 15 blocks of range),
    // so i made yet another scale of 20 DU = 1 blocks bc i like it more :)
    // NOTE: obviously not all values will be 1:1 to this equation, there will be roundings, and in some cases adjustments,
    // like in the case that a weapon paints much less because of minecraft's cubic nature and the magic of rounding
    // also most of the data (in internal units) is taken from https://leanny.github.io/splat3/parameters.html or https://splatoonwiki.org/wiki/
    public record ProjectileDataRecord(
            float size,
            float visualSize,
            int lifeTicks,
            float speed,
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
        public static final Codec<ProjectileDataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.FLOAT.fieldOf("size").forGetter(ProjectileDataRecord::size),
                        Codec.FLOAT.optionalFieldOf("visual_size").forGetter(r -> Optional.of(r.visualSize)),
                        Codec.INT.optionalFieldOf("lifespan", 600).forGetter(ProjectileDataRecord::lifeTicks),
                        Codec.FLOAT.fieldOf("speed").forGetter(ProjectileDataRecord::speed),
                        Codec.FLOAT.optionalFieldOf("horizontal_drag", 0.262144F).forGetter(ProjectileDataRecord::horizontalDrag),
                        Codec.FLOAT.optionalFieldOf("straight_shot_ticks", 0F).forGetter(ProjectileDataRecord::straightShotTicks),
                        Codec.FLOAT.optionalFieldOf("gravity", 0.075F).forGetter(ProjectileDataRecord::gravity),
                        Codec.FLOAT.optionalFieldOf("ink_coverage_on_impact").forGetter(r -> Optional.of(r.inkCoverageImpact)),
                        Codec.FLOAT.optionalFieldOf("ink_drop_coverage").forGetter(r -> Optional.of(r.inkDropCoverage)),
                        Codec.FLOAT.optionalFieldOf("distance_between_drops", 4F).forGetter(ProjectileDataRecord::distanceBetweenInkDrops),
                        Codec.FLOAT.fieldOf("base_damage").forGetter(ProjectileDataRecord::baseDamage),
                        Codec.FLOAT.optionalFieldOf("decayed_damage").forGetter(r -> Optional.of(r.minDamage)),
                        Codec.FLOAT.optionalFieldOf("damage_decay_start_tick", 0F).forGetter(ProjectileDataRecord::damageDecayStartTick),
                        Codec.FLOAT.optionalFieldOf("damage_decay_per_tick", 0F).forGetter(ProjectileDataRecord::damageDecayPerTick)
                ).apply(instance, ProjectileDataRecord::create)
        );
        public static final ProjectileDataRecord DEFAULT = new ProjectileDataRecord(0, 0, 600, 0, 0.262144F, 0, 0.075F, 0, 0, 4, 0, 0, 0, 0);

        public static ProjectileDataRecord create(float size, Optional<Float> visualSize, int lifeTicks, float speed, float horizontalDrag, float straightShotTicks, float gravity, Optional<Float> inkCoverageImpact, Optional<Float> inkDropCoverage, float distanceBetweenInkDrops, float baseDamage, Optional<Float> decayedDamage, float damageDecayStartTick, float damageDecayPerTick)
        {
            return new ProjectileDataRecord(size, visualSize.orElse(size * 3), lifeTicks, speed, horizontalDrag, straightShotTicks, gravity, inkCoverageImpact.orElse(size * 0.85f), inkDropCoverage.orElse(size * 0.75f), distanceBetweenInkDrops, baseDamage, decayedDamage.orElse(baseDamage), damageDecayStartTick, damageDecayPerTick);
        }
    }

    public record OptionalProjectileDataRecord(
            Optional<Float> size,
            Optional<Float> visualSize,
            Optional<Integer> lifeTicks,
            Optional<Float> speed,
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
                        Codec.FLOAT.optionalFieldOf("size").forGetter(OptionalProjectileDataRecord::size),
                        Codec.FLOAT.optionalFieldOf("visual_size").forGetter(OptionalProjectileDataRecord::visualSize),
                        Codec.INT.optionalFieldOf("lifespan").forGetter(OptionalProjectileDataRecord::lifeTicks),
                        Codec.FLOAT.optionalFieldOf("speed").forGetter(OptionalProjectileDataRecord::speed),
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

        public static Optional<OptionalProjectileDataRecord> convert(ProjectileDataRecord projectile) // this is horrible
        {
            return Optional.of(new OptionalProjectileDataRecord(
                    Optional.of(projectile.size),
                    Optional.of(projectile.visualSize),
                    Optional.of(projectile.lifeTicks),
                    Optional.of(projectile.speed),
                    Optional.of(projectile.horizontalDrag),
                    Optional.of(projectile.straightShotTicks),
                    Optional.of(projectile.gravity),
                    Optional.of(projectile.inkCoverageImpact),
                    Optional.of(projectile.inkDropCoverage),
                    Optional.of(projectile.distanceBetweenInkDrops),
                    Optional.of(projectile.baseDamage),
                    Optional.of(projectile.minDamage),
                    Optional.of(projectile.damageDecayStartTick),
                    Optional.of(projectile.damageDecayPerTick)));
        }
    }

    public record ShotDataRecord(
            int startupTicks,
            int endlagTicks,
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
                        Codec.INT.optionalFieldOf("endlag_ticks", 1).forGetter(ShotDataRecord::endlagTicks),
                        Codec.INT.optionalFieldOf("shot_count", 1).forGetter(ShotDataRecord::projectileCount),
                        Codec.FLOAT.fieldOf("ground_inaccuracy").forGetter(ShotDataRecord::groundInaccuracy),
                        Codec.FLOAT.optionalFieldOf("airborne_inaccuracy").forGetter(r -> Optional.of(r.airborneInaccuracy)),
                        Codec.FLOAT.optionalFieldOf("pitch_compensation", 0f).forGetter(ShotDataRecord::pitchCompensation),
                        Codec.FLOAT.fieldOf("ink_consumption").forGetter(ShotDataRecord::inkConsumption),
                        Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(ShotDataRecord::inkRecoveryCooldown)
                ).apply(instance, ShotDataRecord::create)
        );
        public static final ShotDataRecord DEFAULT = new ShotDataRecord(0, 1, 1, 0, 0, 0, 0, 0);

        public static ShotDataRecord create(int startupTicks, int endlagTicks, int projectileCount, float groundInaccuracy, Optional<Float> airborneInaccuracy, float pitchCompensation, float inkConsumption, int inkRecoveryCooldown)
        {
            return new ShotDataRecord(startupTicks, endlagTicks, projectileCount, groundInaccuracy, airborneInaccuracy.orElse(groundInaccuracy), pitchCompensation, inkConsumption, inkRecoveryCooldown);
        }

        public int getFiringSpeed()
        {
            return startupTicks + endlagTicks;
        }
    }

    public record OptionalShotDataRecord(
            Optional<Integer> startupTicks,
            Optional<Integer> endlagTicks,
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