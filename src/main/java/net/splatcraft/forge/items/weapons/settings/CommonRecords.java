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
    // this is the third time but it seems that 12 DU = 1 block seems ok (i should put the raw values instead but im stupdi)
    // NOTE: obviously not all values will be 1:1 to this equation, there will be roundings, and in some cases adjustments,
    // like in the case that a weapon paints much less because of minecraft's cubic nature and the magic of rounding
    // also most of the data (in internal units) is taken from https://leanny.github.io/splat3/parameters.html or https://splatoonwiki.org/wiki/
    // also!!!!! most of the paint data is pretty complex (one value for close droplet paint radius, one value for far away droplet paint radius,
    // and one for any other type of droplet paint radius, and every single one of these is multiplied by 1.4, or 1.2, or 1 depending on the drop's
    // fall height, so screw all that,
    // ink_drop_coverage = the units in "All other ink droplets have a radius of x"
    //      * in case of blasters, its the units in "Ink droplets have a radius of x" * 1.2
    //      * in the case of rollers, it uses the first unit's PaintRadiusShock on both attacks
    // ink_coverage_on_impact = the units in "Droplets that occur when they travel past y units of the player have a radius of x" * 1.2
    //      * in the case of rollers, it uses the first unit's PaintRadiusShock on both attacks
    // also³ here are some other conversions from values in splatoon (x) to minecraft (y), divisor means the current scale of DU per block, currently its 15
    // speed ->             y = x / divisor * 3
    // time ->              y = x / 3
    // damage ->            y = x / 5
    // damage over-time ->  y = x / 5 * 3
    // distance ->          y = x / divisor
    // drag ->              y = x ^ 3
    public record ProjectileDataRecord(
            float size,
            float visualSize,
            float lifeTicks,
            float speed,
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
        public static final Codec<ProjectileDataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.FLOAT.fieldOf("size").forGetter(ProjectileDataRecord::size),
                        Codec.FLOAT.optionalFieldOf("visual_size").forGetter(r -> Optional.of(r.visualSize)),
                        Codec.FLOAT.optionalFieldOf("lifespan", 600f).forGetter(ProjectileDataRecord::lifeTicks),
                        Codec.FLOAT.fieldOf("speed").forGetter(ProjectileDataRecord::speed),
                        Codec.FLOAT.optionalFieldOf("delay_speed_mult", 0.5f).forGetter(ProjectileDataRecord::delaySpeedMult),
                        Codec.FLOAT.optionalFieldOf("horizontal_drag", 0.262144F).forGetter(ProjectileDataRecord::horizontalDrag),
                        Codec.FLOAT.optionalFieldOf("straight_shot_ticks", 0F).forGetter(ProjectileDataRecord::straightShotTicks),
                        Codec.FLOAT.optionalFieldOf("gravity", 0.175F).forGetter(ProjectileDataRecord::gravity),
                        Codec.FLOAT.optionalFieldOf("ink_coverage_on_impact").forGetter(r -> Optional.of(r.inkCoverageImpact)),
                        Codec.FLOAT.optionalFieldOf("ink_drop_coverage").forGetter(r -> Optional.of(r.inkDropCoverage)),
                        Codec.FLOAT.optionalFieldOf("distance_between_drops", 4F).forGetter(ProjectileDataRecord::distanceBetweenInkDrops),
                        Codec.FLOAT.fieldOf("base_damage").forGetter(ProjectileDataRecord::baseDamage),
                        Codec.FLOAT.optionalFieldOf("decayed_damage").forGetter(r -> Optional.of(r.minDamage)),
                        Codec.FLOAT.optionalFieldOf("damage_decay_start_tick", 0F).forGetter(ProjectileDataRecord::damageDecayStartTick),
                        Codec.FLOAT.optionalFieldOf("damage_decay_per_tick", 0F).forGetter(ProjectileDataRecord::damageDecayPerTick)
                ).apply(instance, ProjectileDataRecord::create)
        );
        public static final ProjectileDataRecord DEFAULT = new ProjectileDataRecord(0, 0, 600, 0, 0.5f, 0.262144F, 0, 0.175F, 0, 0, 4, 0, 0, 0, 0);

        public static ProjectileDataRecord create(float size, Optional<Float> visualSize, float lifeTicks, float speed, float delaySpeedMult, float horizontalDrag, float straightShotTicks, float gravity, Optional<Float> inkCoverageImpact, Optional<Float> inkDropCoverage, float distanceBetweenInkDrops, float baseDamage, Optional<Float> decayedDamage, float damageDecayStartTick, float damageDecayPerTick)
        {
            return new ProjectileDataRecord(size,
                    visualSize.orElse(size * 3),
                    lifeTicks,
                    speed,
                    delaySpeedMult,
                    horizontalDrag,
                    straightShotTicks,
                    gravity,
                    inkCoverageImpact.orElse(size * 0.85f),
                    inkDropCoverage.orElse(size * 0.75f),
                    distanceBetweenInkDrops,
                    baseDamage,
                    decayedDamage.orElse(baseDamage),
                    damageDecayStartTick,
                    damageDecayPerTick);
        }
    }

    public record ProjectileSizeRecord(
            float hitboxSize,
            float visualScale,
            float worldHitboxSize
    )
    {
        public static final Codec<ProjectileSizeRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.FLOAT.optionalFieldOf("hitbox_size", 0.1f).forGetter(ProjectileSizeRecord::hitboxSize),
                        Codec.FLOAT.optionalFieldOf("visual_size").forGetter(t -> Optional.of(t.visualScale())),
                        Codec.FLOAT.optionalFieldOf("world_hitbox_size").forGetter(t -> Optional.of(t.worldHitboxSize()))
                ).apply(instance, ProjectileSizeRecord::create)
        );

        private static ProjectileSizeRecord create(float hitboxSize, Optional<Float> visualSize, Optional<Float> worldHitboxSize)
        {
            return new ProjectileSizeRecord(hitboxSize, visualSize.orElse(hitboxSize * 3), worldHitboxSize.orElse(hitboxSize));
        }
    }

    public record OptionalProjectileDataRecord(
            Optional<Float> size,
            Optional<Float> visualSize,
            Optional<Float> lifeTicks,
            Optional<Float> speed,
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
                        Codec.FLOAT.optionalFieldOf("size").forGetter(OptionalProjectileDataRecord::size),
                        Codec.FLOAT.optionalFieldOf("visual_size").forGetter(OptionalProjectileDataRecord::visualSize),
                        Codec.FLOAT.optionalFieldOf("lifespan").forGetter(OptionalProjectileDataRecord::lifeTicks),
                        Codec.FLOAT.optionalFieldOf("speed").forGetter(OptionalProjectileDataRecord::speed),
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
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );

        public static Optional<OptionalProjectileDataRecord> from(ProjectileDataRecord projectile) // this is horrible
        {
            return Optional.of(new OptionalProjectileDataRecord(
                    Optional.of(projectile.size),
                    Optional.of(projectile.visualSize),
                    Optional.of(projectile.lifeTicks),
                    Optional.of(projectile.speed),
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
            ));
        }

        public static ProjectileDataRecord mergeWithBase(Optional<OptionalProjectileDataRecord> modified, ProjectileDataRecord base)
        {
            if (modified.isEmpty())
                return base;

            OptionalProjectileDataRecord modifiedGet = modified.get();
            return new ProjectileDataRecord(
                    modifiedGet.size().orElse(base.size()),
                    modifiedGet.visualSize().orElse(base.visualSize()),
                    modifiedGet.lifeTicks().orElse(base.lifeTicks()),
                    modifiedGet.speed().orElse(base.speed()),
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

    public record ShotDataRecord(
            int startupTicks,
            int endlagTicks,
            int projectileCount,
            ShotDeviationDataRecord accuracyData,
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
                        ShotDeviationDataRecord.CODEC.optionalFieldOf("accuracy_data", ShotDeviationDataRecord.PERFECT_DEFAULT).forGetter(ShotDataRecord::accuracyData),
                        Codec.FLOAT.optionalFieldOf("pitch_compensation", 0f).forGetter(ShotDataRecord::pitchCompensation),
                        Codec.FLOAT.fieldOf("ink_consumption").forGetter(ShotDataRecord::inkConsumption),
                        Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(ShotDataRecord::inkRecoveryCooldown)
                ).apply(instance, ShotDataRecord::create)
        );
        public static final ShotDataRecord DEFAULT = new ShotDataRecord(0, 1, 1, ShotDeviationDataRecord.PERFECT_DEFAULT, 0, 0, 0);

        public static ShotDataRecord create(int startupTicks, int endlagTicks, int projectileCount, ShotDeviationDataRecord accuracyData, float pitchCompensation, float inkConsumption, int inkRecoveryCooldown)
        {
            return new ShotDataRecord(startupTicks, endlagTicks, projectileCount, accuracyData, pitchCompensation, inkConsumption, inkRecoveryCooldown);
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
            Optional<ShotDeviationDataRecord> accuracyData,
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
                        ShotDeviationDataRecord.CODEC.optionalFieldOf("accuracy_data").forGetter(OptionalShotDataRecord::accuracyData),
                        Codec.FLOAT.optionalFieldOf("pitch_compensation").forGetter(OptionalShotDataRecord::pitchCompensation),
                        Codec.FLOAT.optionalFieldOf("ink_consumption").forGetter(OptionalShotDataRecord::inkConsumption),
                        Codec.INT.optionalFieldOf("ink_recovery_cooldown").forGetter(OptionalShotDataRecord::inkRecoveryCooldown)
                ).apply(instance, OptionalShotDataRecord::new)
        );

        public static final OptionalShotDataRecord DEFAULT = new OptionalShotDataRecord(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );

        public static Optional<OptionalShotDataRecord> from(ShotDataRecord shot)
        {
            return Optional.of(new OptionalShotDataRecord(
                    Optional.of(shot.startupTicks),
                    Optional.of(shot.endlagTicks),
                    Optional.of(shot.projectileCount),
                    Optional.of(shot.accuracyData),
                    Optional.of(shot.pitchCompensation),
                    Optional.of(shot.inkConsumption),
                    Optional.of(shot.inkRecoveryCooldown)
            ));
        }

        public static ShotDataRecord mergeWithBase(Optional<OptionalShotDataRecord> modified, ShotDataRecord base)
        {
            if (modified.isEmpty())
                return base;

            OptionalShotDataRecord modifiedGet = modified.get();
            return new ShotDataRecord(
                    modifiedGet.startupTicks().orElse(base.startupTicks()),
                    modifiedGet.endlagTicks().orElse(base.endlagTicks()),
                    modifiedGet.projectileCount().orElse(base.projectileCount()),
                    modifiedGet.accuracyData().orElse(base.accuracyData()),
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

                        Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("time_inactive_to_decrease", 2f).forGetter(ShotDeviationDataRecord::chanceDecreaseDelay),
                        Codec.FLOAT.optionalFieldOf("chance_decrease_when_inactive", 0.045f).forGetter(ShotDeviationDataRecord::chanceDecreasePerTick),

                        Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("delay_to_decrease_airborne_deviation", 8.333333f).forGetter(ShotDeviationDataRecord::airborneContractDelay),
                        Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("time_to_decrease_airborne_deviation", 23.333334f).forGetter(ShotDeviationDataRecord::airborneContractTimeToDecrease)
                ).apply(instance, ShotDeviationDataRecord::new)
        );

        public float getMaximumDeviation()
        {
            return Math.max(Math.max(minDeviateChance, maxDeviateChance), deviationChanceWhenAirborne);
        }

        public static final ShotDeviationDataRecord DEFAULT = new ShotDeviationDataRecord(5, 12, 0.01f, 0.25f, 0.4f, 0.01f, 2f, 0.045f, 8.333333f, 23.333334f);
        public static final ShotDeviationDataRecord PERFECT_DEFAULT = new ShotDeviationDataRecord(0, 0, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
    }
}