package net.splatcraft.forge.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.forge.entities.ExtraSaveData;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.util.DamageRangesRecord;
import net.splatcraft.forge.util.WeaponTooltip;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class RollerWeaponSettings extends AbstractWeaponSettings<RollerWeaponSettings, RollerWeaponSettings.DataRecord>
{
    public static final RollerWeaponSettings DEFAULT = new RollerWeaponSettings("default");
    public String name;
    public boolean isBrush;
    public RollDataRecord rollData = RollDataRecord.DEFAULT;
    public boolean bypassesMobDamage = false;
    public SwingDataRecord swingData = SwingDataRecord.DEFAULT;
    public FlingDataRecord flingData = FlingDataRecord.DEFAULT;

    public RollerWeaponSettings(String name)
    {
        super(name);
    }

    public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
    {
        ExtraSaveData.RollerDistanceExtraData data = list.getFirstExtraData(ExtraSaveData.RollerDistanceExtraData.class);
        float distance = data == null ? 0 : data.spawnPos.distance(projectile.position().toVector3f());

        RollerProjectileDataRecord projectileData = projectile.throwerAirborne ? flingData.projectileData : swingData.projectileData;
        float timeDamagePercent = projectile.calculateDamageDecay(1, projectileData.damageFalloffStartTick, projectileData.damageFalloffEndTick, projectileData.maxDamageFalloffPercent);
        return projectileData.damageRanges.getDamage(distance) * timeDamagePercent;
    }

    @Override
    public WeaponTooltip<RollerWeaponSettings>[] tooltipsToRegister()
    {
        return new WeaponTooltip[]
            {
                new WeaponTooltip<RollerWeaponSettings>("speed", WeaponTooltip.Metrics.BPT, settings -> settings.swingData.attackData.maxSpeed, WeaponTooltip.RANKER_ASCENDING),
                new WeaponTooltip<RollerWeaponSettings>("mobility", WeaponTooltip.Metrics.MULTIPLIER, settings -> settings.rollData.dashMobility(), WeaponTooltip.RANKER_ASCENDING),
                new WeaponTooltip<RollerWeaponSettings>("direct_damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.rollData.damage, WeaponTooltip.RANKER_ASCENDING)
            };
    }

    @Override
    public Codec<DataRecord> getCodec()
    {
        return DataRecord.CODEC;
    }

    @Override
    public CommonRecords.ShotDeviationDataRecord getShotDeviationData(ItemStack stack, LivingEntity entity)
    {
        return CommonRecords.ShotDeviationDataRecord.PERFECT_DEFAULT;
    }

    @Override
    public void deserialize(DataRecord data)
    {
        isBrush = data.isBrush;

        bypassesMobDamage = data.fullDamageToMobs;
        isSecret = data.isSecret;

        rollData = data.roll;
        swingData = data.swing;
        flingData = data.fling;
    }

    @Override
    public DataRecord serialize()
    {
        return new DataRecord(isBrush, rollData, swingData, flingData, bypassesMobDamage, isSecret);
    }

    @Override
    public float getSpeedForRender(LocalPlayer player, ItemStack mainHandItem)
    {
        return swingData.attackData().maxSpeed;
    }

    public RollerWeaponSettings setName(String name)
    {
        this.name = name;
        return this;
    }

    public RollerWeaponSettings setBrush(boolean brush)
    {
        this.isBrush = brush;
        return this;
    }

    public record DataRecord(
        boolean isBrush,
        RollDataRecord roll,
        SwingDataRecord swing,
        FlingDataRecord fling,
        boolean fullDamageToMobs,
        boolean isSecret
    )
    {
        public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.BOOL.optionalFieldOf("is_brush", false).forGetter(DataRecord::isBrush),
                RollDataRecord.CODEC.fieldOf("roll").forGetter(DataRecord::roll),
                SwingDataRecord.CODEC.fieldOf("swing").forGetter(DataRecord::swing),
                FlingDataRecord.CODEC.fieldOf("fling").forGetter(DataRecord::fling),
                Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::fullDamageToMobs),
                Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
            ).apply(instance, DataRecord::new)
        );
    }

    public record RollerProjectileDataRecord(
        float size,
        float visualSize,
        float delaySpeedMult,
        float horizontalDrag,
        float straightShotTicks,
        float gravity,
        float inkCoverageImpact,
        float inkDropCoverage,
        float distanceBetweenInkDrops,
        float damageFalloffStartTick,
        float damageFalloffEndTick,
        float maxDamageFalloffPercent,
        DamageRangesRecord damageRanges,
        Optional<DamageRangesRecord> weakDamageRanges
    )
    {
        public static final Codec<RollerProjectileDataRecord> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.FLOAT.fieldOf("size").forGetter(RollerProjectileDataRecord::size),
                Codec.FLOAT.optionalFieldOf("visual_size").forGetter(r -> Optional.of(r.visualSize)),
                Codec.FLOAT.optionalFieldOf("delay_speed_mult", 1f).forGetter(RollerProjectileDataRecord::delaySpeedMult),
                Codec.FLOAT.optionalFieldOf("horizontal_drag", 0.262144F).forGetter(RollerProjectileDataRecord::horizontalDrag),
                Codec.FLOAT.optionalFieldOf("straight_shot_ticks", 0F).forGetter(RollerProjectileDataRecord::straightShotTicks),
                Codec.FLOAT.optionalFieldOf("gravity", 0.175F).forGetter(RollerProjectileDataRecord::gravity),
                Codec.FLOAT.optionalFieldOf("ink_coverage_on_impact").forGetter(r -> Optional.of(r.inkCoverageImpact)),
                Codec.FLOAT.optionalFieldOf("ink_drop_coverage").forGetter(r -> Optional.of(r.inkDropCoverage)),
                Codec.FLOAT.optionalFieldOf("distance_between_drops", 4F).forGetter(RollerProjectileDataRecord::distanceBetweenInkDrops),
                Codec.FLOAT.optionalFieldOf("damage_falloff_start_tick", 8.333333f).forGetter(RollerProjectileDataRecord::damageFalloffStartTick),
                Codec.FLOAT.optionalFieldOf("damage_falloff_end_tick", 15f).forGetter(RollerProjectileDataRecord::damageFalloffEndTick),
                Codec.FLOAT.optionalFieldOf("damage_falloff_percentage", 0.5f).forGetter(RollerProjectileDataRecord::maxDamageFalloffPercent),
                DamageRangesRecord.CODEC.fieldOf("damage_ranges").forGetter(RollerProjectileDataRecord::damageRanges),
                DamageRangesRecord.CODEC.optionalFieldOf("weak_damage_ranges").forGetter(RollerProjectileDataRecord::weakDamageRanges)

            ).apply(instance, RollerProjectileDataRecord::create)
        );
        public static final RollerProjectileDataRecord DEFAULT = new RollerProjectileDataRecord(1, 1, 1f, 0.729f, 0f, 0.16f, 1f, 0.5f, 1f, 8.3333f, 15f, 0.5f, DamageRangesRecord.DEFAULT, Optional.empty());

        public static RollerProjectileDataRecord create(float size,
                                                        Optional<Float> visualSize,
                                                        float delaySpeedMult,
                                                        float horizontalDrag,
                                                        float straightShotTicks,
                                                        float gravity,
                                                        Optional<Float> inkCoverageImpact,
                                                        Optional<Float> inkDropCoverage,
                                                        float distanceBetweenInkDrops,
                                                        float damageFalloffStartTick,
                                                        float damageFalloffEndTick,
                                                        float maxDamageFalloffPercent,
                                                        DamageRangesRecord damageRanges,
                                                        Optional<DamageRangesRecord> weakDamageRanges)
        {
            return new RollerProjectileDataRecord(size,
                visualSize.orElse(size * 3),
                delaySpeedMult,
                horizontalDrag,
                straightShotTicks,
                gravity,
                inkCoverageImpact.orElse(size * 0.85f),
                inkDropCoverage.orElse(size * 0.75f),
                distanceBetweenInkDrops,
                damageFalloffStartTick,
                damageFalloffEndTick,
                maxDamageFalloffPercent,
                damageRanges,
                weakDamageRanges);
        }
    }

    public record RollDataRecord(
        int inkSize,
        int hitboxSize,
        float inkConsumption,
        int inkRecoveryCooldown,
        float damage,
        float mobility,
        float dashMobility,
        float dashConsumption,
        float dashTime
    )
    {
        public static final Codec<RollDataRecord> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.INT.fieldOf("ink_size").forGetter(RollDataRecord::inkSize),
                Codec.INT.optionalFieldOf("hitbox_size").forGetter(v -> Optional.of(v.hitboxSize())),
                Codec.FLOAT.fieldOf("ink_consumption").forGetter(RollDataRecord::inkConsumption),
                Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(RollDataRecord::inkRecoveryCooldown),
                Codec.FLOAT.fieldOf("damage").forGetter(RollDataRecord::damage),
                Codec.FLOAT.fieldOf("mobility").forGetter(RollDataRecord::mobility),
                Codec.FLOAT.optionalFieldOf("dash_mobility").forGetter(v -> Optional.of(v.dashMobility())),
                Codec.FLOAT.optionalFieldOf("dash_consumption").forGetter(v -> Optional.of(v.dashConsumption())),
                Codec.FLOAT.optionalFieldOf("dash_time", 1f).forGetter(RollDataRecord::dashTime)
            ).apply(instance, RollDataRecord::create)
        );
        public static final RollDataRecord DEFAULT = new RollDataRecord(3, 3, 1, 10, 20, 1, 2, 2, 10);

        private static @NotNull RollDataRecord create(Integer inkSize, Optional<Integer> hitboxSize, Float inkConsumption, Integer inkRecoveryCooldown, Float damage, Float mobility, Optional<Float> dashMobility, Optional<Float> dashConsumption, float dashTime)
        {
            return new RollDataRecord(inkSize, hitboxSize.orElse(inkSize), inkConsumption, inkRecoveryCooldown, damage, mobility, dashMobility.orElse(mobility), dashConsumption.orElse(inkConsumption), dashTime);
        }
    }

    public record SwingDataRecord(
        RollerProjectileDataRecord projectileData,
        RollerAttackDataRecord attackData,
        boolean allowJumpingOnCharge,
        float mobility,
        float attackAngle,
        float letalAngle,
        float offAnglePenalty
    )
    {
        public static final Codec<SwingDataRecord> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                RollerProjectileDataRecord.CODEC.fieldOf("projectile").forGetter(SwingDataRecord::projectileData),
                RollerAttackDataRecord.CODEC.fieldOf("attack_data").forGetter(SwingDataRecord::attackData),
                Codec.BOOL.optionalFieldOf("allow_jumping_on_charge", false).forGetter(SwingDataRecord::allowJumpingOnCharge),
                Codec.FLOAT.fieldOf("mobility").forGetter(SwingDataRecord::mobility),
                Codec.FLOAT.fieldOf("swing_angle").forGetter(SwingDataRecord::attackAngle),
                Codec.FLOAT.optionalFieldOf("letal_angle", 16f).forGetter(SwingDataRecord::letalAngle),
                Codec.FLOAT.optionalFieldOf("offangle_penalty", 0.5f).forGetter(SwingDataRecord::offAnglePenalty)
            ).apply(instance, SwingDataRecord::new)
        );
        public static final SwingDataRecord DEFAULT = new SwingDataRecord(RollerProjectileDataRecord.DEFAULT, RollerAttackDataRecord.DEFAULT, false, 0.5f, 18f, 16f, 0.5f);
    }

    public record FlingDataRecord(
        RollerProjectileDataRecord projectileData,
        RollerAttackDataRecord attackData,
        float startPitchCompensation,
        float endPitchCompensation
    )
    {
        public static final Codec<FlingDataRecord> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                RollerProjectileDataRecord.CODEC.fieldOf("projectile").forGetter(FlingDataRecord::projectileData),
                RollerAttackDataRecord.CODEC.fieldOf("attack_data").forGetter(FlingDataRecord::attackData),
                Codec.FLOAT.optionalFieldOf("start_pitch_compensation", -7.5f).forGetter(FlingDataRecord::startPitchCompensation),
                Codec.FLOAT.optionalFieldOf("end_pitch_compensation", 0f).forGetter(FlingDataRecord::endPitchCompensation)
            ).apply(instance, FlingDataRecord::new)
        );
        public static final FlingDataRecord DEFAULT = new FlingDataRecord(RollerProjectileDataRecord.DEFAULT, RollerAttackDataRecord.DEFAULT, -7.5f, 0f);

        public int calculateProjectileCount()
        {
            return (int) ((attackData.maxSpeed() - attackData.minSpeed()) / (projectileData.size / 3.3));
        }
    }

    public record RollerAttackDataRecord(
        float inkConsumption,
        int inkRecoveryCooldown,
        int startupTime,
        float minSpeed,
        float maxSpeed
    )
    {
        public static final Codec<RollerAttackDataRecord> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.FLOAT.fieldOf("ink_consumption").forGetter(RollerAttackDataRecord::inkConsumption),
                Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(RollerAttackDataRecord::inkRecoveryCooldown),
                Codec.INT.fieldOf("startup_time").forGetter(RollerAttackDataRecord::startupTime),
                Codec.FLOAT.fieldOf("min_speed").forGetter(RollerAttackDataRecord::minSpeed),
                Codec.FLOAT.fieldOf("max_speed").forGetter(RollerAttackDataRecord::maxSpeed)
            ).apply(instance, RollerAttackDataRecord::new)
        );
        public static final RollerAttackDataRecord DEFAULT = new RollerAttackDataRecord(10f, 20, 10, 1, 4);
    }
}
