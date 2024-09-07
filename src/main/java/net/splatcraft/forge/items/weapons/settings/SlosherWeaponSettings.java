package net.splatcraft.forge.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.forge.entities.ExtraSaveData;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.util.WeaponTooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SlosherWeaponSettings extends AbstractWeaponSettings<SlosherWeaponSettings, SlosherWeaponSettings.DataRecord>
{
    public boolean bypassesMobDamage;
    public static final SlosherWeaponSettings DEFAULT = new SlosherWeaponSettings("default");
    public SlosherShotDataRecord shotData = SlosherShotDataRecord.DEFAULT;
    public CommonRecords.ProjectileDataRecord sampleProjectile;
    public float lowestStartup;

    public SlosherWeaponSettings(String name)
    {
        super(name);
    }

    @Override
    public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
    {
        ExtraSaveData.SloshExtraData sloshData = list.getFirstExtraData(ExtraSaveData.SloshExtraData.class);
        if (sloshData != null)
        {
            return shotData.sloshes.get(sloshData.sloshDataIndex).projectile.baseDamage();
        }
        return sampleProjectile.baseDamage();
    }

    @Override
    public WeaponTooltip<SlosherWeaponSettings>[] tooltipsToRegister()
    {
        return new WeaponTooltip[]
                {
                        new WeaponTooltip<SlosherWeaponSettings>("speed", WeaponTooltip.Metrics.BPT, settings -> settings.sampleProjectile.speed(), WeaponTooltip.RANKER_ASCENDING),
                        new WeaponTooltip<SlosherWeaponSettings>("damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.sampleProjectile.baseDamage(), WeaponTooltip.RANKER_ASCENDING),
                        new WeaponTooltip<SlosherWeaponSettings>("handling", WeaponTooltip.Metrics.TICKS, settings -> settings.shotData.endlagTicks, WeaponTooltip.RANKER_DESCENDING)
                };
    }

    @Override
    public Codec<DataRecord> getCodec()
    {
        return DataRecord.CODEC;
    }

    @Override
    public CommonRecords.ShotDeviationDataRecord getShotDeviationData(ItemStack stack, Entity entity)
    {
        return CommonRecords.ShotDeviationDataRecord.PERFECT_DEFAULT;
    }

    @Override
    public void deserialize(DataRecord data)
    {
        shotData = data.shot;

        sampleProjectile = data.sampleProjectile.orElse(SingularSloshShotData.SLOSHER_PROJECTILE_DEFAULT);

        for (var slosh : shotData.sloshes)
        {
            if (slosh.startupTicks < lowestStartup)
                lowestStartup = slosh.startupTicks;
        }
        setMoveSpeed(data.mobility);
        setSecret(data.isSecret);
        setBypassesMobDamage(data.bypassesMobDamage);
    }

    @Override
    public DataRecord serialize()
    {
        return new DataRecord(shotData, Optional.ofNullable(sampleProjectile), moveSpeed, bypassesMobDamage, isSecret);
    }

    public SlosherWeaponSettings setBypassesMobDamage(boolean bypassesMobDamage)
    {
        this.bypassesMobDamage = bypassesMobDamage;
        return this;
    }

    public record DataRecord(
            SlosherShotDataRecord shot,
            Optional<CommonRecords.ProjectileDataRecord> sampleProjectile,
            float mobility,
            boolean bypassesMobDamage,
            boolean isSecret
    )
    {
        public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        SlosherShotDataRecord.CODEC.fieldOf("shot").forGetter(DataRecord::shot),
                        SingularSloshShotData.SLOSHER_PROJECTILE_CODEC.optionalFieldOf("sample_projectile").forGetter(DataRecord::sampleProjectile),
                        Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::mobility),
                        Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::bypassesMobDamage),
                        Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
                ).apply(instance, DataRecord::create)
        );

        private static DataRecord create(SlosherShotDataRecord shot, Optional<CommonRecords.ProjectileDataRecord> sampleProjectile, float mobility, boolean bypassesMobDamage, boolean isSecret)
        {
            if (sampleProjectile.isEmpty() && !shot.sloshes.isEmpty())
                sampleProjectile = Optional.ofNullable(shot.sloshes.get(0).projectile);
            return new DataRecord(shot, sampleProjectile, mobility, bypassesMobDamage, isSecret);
        }
    }

    public record SlosherShotDataRecord(
            int endlagTicks,
            List<SingularSloshShotData> sloshes,
            float pitchCompensation,
            float inkConsumption,
            int inkRecoveryCooldown,
            boolean allowFlicking
    )
    {
        public static final Codec<SlosherShotDataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.INT.optionalFieldOf("endlag_ticks", 1).forGetter(SlosherShotDataRecord::endlagTicks),
                        SingularSloshShotData.CODEC.listOf().fieldOf("sloshes_data").forGetter(SlosherShotDataRecord::sloshes),
                        Codec.FLOAT.optionalFieldOf("pitch_compensation", 0f).forGetter(SlosherShotDataRecord::pitchCompensation),
                        Codec.FLOAT.fieldOf("ink_consumption").forGetter(SlosherShotDataRecord::inkConsumption),
                        Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(SlosherShotDataRecord::inkRecoveryCooldown),
                        Codec.BOOL.optionalFieldOf("allow_flicking", true).forGetter(SlosherShotDataRecord::allowFlicking)
                ).apply(instance, SlosherShotDataRecord::new)
        );
        public static final SlosherShotDataRecord DEFAULT = new SlosherShotDataRecord(0, new ArrayList<>(), 0, 0, 0, true);
    }

    public record SingularSloshShotData(
            float startupTicks,
            int count,
            float delayBetweenProjectiles,
            float speedSubstract,
            float offsetAngle,
            CommonRecords.ProjectileDataRecord projectile,
            Optional<BlasterWeaponSettings.DetonationRecord> detonationData
    )
    {
        public static final Codec<CommonRecords.ProjectileDataRecord> SLOSHER_PROJECTILE_CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.FLOAT.fieldOf("size").forGetter(CommonRecords.ProjectileDataRecord::size),
                        Codec.FLOAT.optionalFieldOf("visual_size").forGetter(r -> Optional.of(r.visualSize())),
                        Codec.FLOAT.fieldOf("speed").forGetter(CommonRecords.ProjectileDataRecord::speed),
                        Codec.FLOAT.optionalFieldOf("delay_speed_mult").forGetter(t -> Optional.of(t.delaySpeedMult())),
                        Codec.FLOAT.optionalFieldOf("horizontal_drag", 0.262144F).forGetter(CommonRecords.ProjectileDataRecord::horizontalDrag),
                        Codec.FLOAT.optionalFieldOf("straight_shot_ticks", 0F).forGetter(CommonRecords.ProjectileDataRecord::straightShotTicks),
                        Codec.FLOAT.optionalFieldOf("gravity", 0.175F).forGetter(CommonRecords.ProjectileDataRecord::gravity),
                        Codec.FLOAT.optionalFieldOf("ink_coverage_on_impact").forGetter(r -> Optional.of(r.inkCoverageImpact())),
                        Codec.FLOAT.optionalFieldOf("ink_drop_coverage").forGetter(r -> Optional.of(r.inkDropCoverage())),
                        Codec.FLOAT.optionalFieldOf("distance_between_drops", 4F).forGetter(CommonRecords.ProjectileDataRecord::distanceBetweenInkDrops),
                        Codec.FLOAT.fieldOf("direct_damage").forGetter(CommonRecords.ProjectileDataRecord::baseDamage),
                        Codec.FLOAT.optionalFieldOf("minimum_damage").forGetter(r -> Optional.of(r.minDamage())),
                        Codec.FLOAT.optionalFieldOf("fall_damage_start", 0F).forGetter(CommonRecords.ProjectileDataRecord::damageDecayStartTick),
                        Codec.FLOAT.optionalFieldOf("fall_damage_end", 0F).forGetter(CommonRecords.ProjectileDataRecord::damageDecayPerTick)
                ).apply(instance, SingularSloshShotData::createSlosherProjectile)
        );

        public static final Codec<SingularSloshShotData> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.FLOAT.optionalFieldOf("startup_ticks", 0f).forGetter(SingularSloshShotData::startupTicks),
                        Codec.INT.fieldOf("slosh_count").forGetter(SingularSloshShotData::count),
                        Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("delay_between_projectiles", 1f).forGetter(SingularSloshShotData::delayBetweenProjectiles),
                        Codec.FLOAT.optionalFieldOf("speed_substract_per_projectile", 0f).forGetter(SingularSloshShotData::speedSubstract),
                        Codec.FLOAT.optionalFieldOf("offset_angle", 0f).forGetter(SingularSloshShotData::offsetAngle),
                        SLOSHER_PROJECTILE_CODEC.fieldOf("slosh_projectile").forGetter(SingularSloshShotData::projectile),
                        BlasterWeaponSettings.DetonationRecord.CODEC.optionalFieldOf("detonation_data").forGetter(SingularSloshShotData::detonationData)
                ).apply(instance, SingularSloshShotData::new)
        );

        public static CommonRecords.ProjectileDataRecord createSlosherProjectile(float size, Optional<Float> visualSize, float speed, Optional<Float> delaySpeedMult, float horizontalDrag, float straightShotTicks, float gravity, Optional<Float> inkCoverageImpact, Optional<Float> inkDropCoverage, float distanceBetweenInkDrops, float directDamage, Optional<Float> minDamage, float heightDecayStart, float heightDecayEnd)
        {
            return CommonRecords.ProjectileDataRecord.create(size, visualSize, 600, speed, Optional.of(delaySpeedMult.orElse(1f)), horizontalDrag, straightShotTicks, gravity, inkCoverageImpact, inkDropCoverage, distanceBetweenInkDrops, directDamage, minDamage, heightDecayStart, heightDecayEnd);
        }

        public static final CommonRecords.ProjectileDataRecord SLOSHER_PROJECTILE_DEFAULT = new CommonRecords.ProjectileDataRecord(0, 0, 600, 0, 0.5f, 0.262144F, 0, 0.175F, 0, 0, 4, 0, 0, 0, 0);

        public static final SingularSloshShotData DEFAULT = new SingularSloshShotData(0, 1, 1f, 0, 0, SLOSHER_PROJECTILE_DEFAULT, Optional.empty());
    }
}
