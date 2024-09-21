package net.splatcraft.forge.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
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
    public CommonRecords.ProjectileDataRecord baseProjectile = SingularSloshShotData.SLOSHER_PROJECTILE_DEFAULT;
    public float lowestStartup;
    public CommonRecords.ProjectileDataRecord[] mergedProjectileData = new CommonRecords.ProjectileDataRecord[0];

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
            CommonRecords.ProjectileDataRecord projectileData = getProjectileDataAtIndex(sloshData.sloshDataIndex);
            if (projectileData.minDamage() == projectileData.baseDamage())
                return projectileData.baseDamage();

            double relativeY = projectile.getY() - sloshData.spawnHeight;
            return getDamage(projectileData, (float) relativeY);
        }
        return baseProjectile.baseDamage();
    }

    private static float getDamage(CommonRecords.ProjectileDataRecord projectileData, float relativeY)
    {
        float minDamageHeight = projectileData.damageDecayPerTick();
        float damageDecayStartHeight = projectileData.damageDecayStartTick();

        float damage = projectileData.baseDamage();
        if (relativeY < -minDamageHeight)
            damage = projectileData.minDamage();
        else if (relativeY < -damageDecayStartHeight)
            damage = Mth.lerp(Mth.inverseLerp(-relativeY, damageDecayStartHeight, minDamageHeight), projectileData.baseDamage(), projectileData.minDamage());
        return damage;
    }

    @Override
    public WeaponTooltip<SlosherWeaponSettings>[] tooltipsToRegister()
    {
        return new WeaponTooltip[]
                {
                        new WeaponTooltip<SlosherWeaponSettings>("speed", WeaponTooltip.Metrics.BPT, settings -> settings.baseProjectile.speed(), WeaponTooltip.RANKER_ASCENDING),
                        new WeaponTooltip<SlosherWeaponSettings>("damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.baseProjectile.baseDamage(), WeaponTooltip.RANKER_ASCENDING),
                        new WeaponTooltip<SlosherWeaponSettings>("handling", WeaponTooltip.Metrics.TICKS, settings -> settings.shotData.endlagTicks, WeaponTooltip.RANKER_DESCENDING)
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
        shotData = data.shot;
        baseProjectile = data.baseProjectile;
        mergedProjectileData = new CommonRecords.ProjectileDataRecord[shotData.sloshes.size()];
        for (int i = 0; i < shotData.sloshes.size(); i++)
        {
            mergedProjectileData[i] = CommonRecords.OptionalProjectileDataRecord.mergeWithBase(shotData.sloshes.get(i).projectileModifications, baseProjectile);
        }

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
        return new DataRecord(shotData, baseProjectile, moveSpeed, bypassesMobDamage, isSecret);
    }

    public SlosherWeaponSettings setBypassesMobDamage(boolean bypassesMobDamage)
    {
        this.bypassesMobDamage = bypassesMobDamage;
        return this;
    }

    public CommonRecords.ProjectileDataRecord getProjectileDataAtIndex(int sloshDataIndex)
    {
        return mergedProjectileData[sloshDataIndex];
    }

    public record DataRecord(
            SlosherShotDataRecord shot,
            CommonRecords.ProjectileDataRecord baseProjectile,
            float mobility,
            boolean bypassesMobDamage,
            boolean isSecret
    )
    {
        public static final Codec<CommonRecords.ProjectileDataRecord> BASE_SLOSHER_PROJECTILE_CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.FLOAT.fieldOf("size").forGetter(CommonRecords.ProjectileDataRecord::size),
                        Codec.FLOAT.optionalFieldOf("visual_size").forGetter(r -> Optional.of(r.visualSize())),
                        Codec.FLOAT.fieldOf("speed").forGetter(CommonRecords.ProjectileDataRecord::speed),
                        Codec.FLOAT.optionalFieldOf("delay_speed_mult").forGetter(t -> Optional.of(t.delaySpeedMult())),
                        Codec.FLOAT.optionalFieldOf("horizontal_drag", 0.681472f).forGetter(CommonRecords.ProjectileDataRecord::horizontalDrag),
                        Codec.FLOAT.optionalFieldOf("straight_shot_ticks", 0F).forGetter(CommonRecords.ProjectileDataRecord::straightShotTicks),
                        Codec.FLOAT.optionalFieldOf("gravity", 0.125f).forGetter(CommonRecords.ProjectileDataRecord::gravity),
                        Codec.FLOAT.optionalFieldOf("ink_coverage_on_impact").forGetter(r -> Optional.of(r.inkCoverageImpact())),
                        Codec.FLOAT.optionalFieldOf("ink_drop_coverage", 0f).forGetter(CommonRecords.ProjectileDataRecord::inkDropCoverage),
                        Codec.FLOAT.optionalFieldOf("distance_between_drops", 4f).forGetter(CommonRecords.ProjectileDataRecord::distanceBetweenInkDrops),
                        Codec.FLOAT.fieldOf("direct_damage").forGetter(CommonRecords.ProjectileDataRecord::baseDamage),
                        Codec.FLOAT.optionalFieldOf("minimum_damage").forGetter(r -> Optional.of(r.minDamage())),
                        Codec.FLOAT.optionalFieldOf("fall_damage_start", 1f).forGetter(CommonRecords.ProjectileDataRecord::damageDecayStartTick),
                        Codec.FLOAT.optionalFieldOf("fall_damage_end", 5f).forGetter(CommonRecords.ProjectileDataRecord::damageDecayPerTick)
                ).apply(instance, DataRecord::createSlosherProjectile)
        );

        public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        SlosherShotDataRecord.CODEC.fieldOf("shot").forGetter(DataRecord::shot),
                        BASE_SLOSHER_PROJECTILE_CODEC.fieldOf("base_projectile").forGetter(DataRecord::baseProjectile),
                        Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::mobility),
                        Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::bypassesMobDamage),
                        Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
                ).apply(instance, DataRecord::create)
        );

        public static CommonRecords.ProjectileDataRecord createSlosherProjectile(float size, Optional<Float> visualSize, float speed, Optional<Float> delaySpeedMult, float horizontalDrag, float straightShotTicks, float gravity, Optional<Float> inkCoverageImpact, float inkDropCoverage, float distanceBetweenInkDrops, float directDamage, Optional<Float> minDamage, float heightDecayStart, float heightDecayEnd)
        {
            return CommonRecords.ProjectileDataRecord.create(size, visualSize, 600, speed, delaySpeedMult.orElse(1f), horizontalDrag, straightShotTicks, gravity, inkCoverageImpact, Optional.of(inkDropCoverage), distanceBetweenInkDrops, directDamage, minDamage, heightDecayStart, heightDecayEnd);
        }

        private static DataRecord create(SlosherShotDataRecord shot, CommonRecords.ProjectileDataRecord baseProjectile, float mobility, boolean bypassesMobDamage, boolean isSecret)
        {
            return new DataRecord(shot, baseProjectile, mobility, bypassesMobDamage, isSecret);
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
            Optional<CommonRecords.OptionalProjectileDataRecord> projectileModifications,
            Optional<BlasterWeaponSettings.DetonationRecord> detonationData
    )
    {
        // this only renames some variables lol
        public static final Codec<CommonRecords.OptionalProjectileDataRecord> PROJECTILE_MODIFICATIONS_CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.FLOAT.optionalFieldOf("size").forGetter(CommonRecords.OptionalProjectileDataRecord::size),
                        Codec.FLOAT.optionalFieldOf("visual_size").forGetter(CommonRecords.OptionalProjectileDataRecord::visualSize),
                        Codec.FLOAT.optionalFieldOf("lifespan").forGetter(CommonRecords.OptionalProjectileDataRecord::lifeTicks),
                        Codec.FLOAT.optionalFieldOf("speed").forGetter(CommonRecords.OptionalProjectileDataRecord::speed),
                        Codec.FLOAT.optionalFieldOf("delay_speed_mult").forGetter(CommonRecords.OptionalProjectileDataRecord::delaySpeedMult),
                        Codec.FLOAT.optionalFieldOf("horizontal_drag").forGetter(CommonRecords.OptionalProjectileDataRecord::horizontalDrag),
                        Codec.FLOAT.optionalFieldOf("straight_shot_ticks").forGetter(CommonRecords.OptionalProjectileDataRecord::straightShotTicks),
                        Codec.FLOAT.optionalFieldOf("gravity").forGetter(CommonRecords.OptionalProjectileDataRecord::gravity),
                        Codec.FLOAT.optionalFieldOf("ink_coverage_on_impact").forGetter(CommonRecords.OptionalProjectileDataRecord::inkCoverageImpact),
                        Codec.FLOAT.optionalFieldOf("ink_drop_coverage").forGetter(CommonRecords.OptionalProjectileDataRecord::inkDropCoverage),
                        Codec.FLOAT.optionalFieldOf("distance_between_drops").forGetter(CommonRecords.OptionalProjectileDataRecord::distanceBetweenInkDrops),
                        Codec.FLOAT.optionalFieldOf("direct_damage").forGetter(CommonRecords.OptionalProjectileDataRecord::baseDamage),
                        Codec.FLOAT.optionalFieldOf("minimum_damage").forGetter(CommonRecords.OptionalProjectileDataRecord::minDamage),
                        Codec.FLOAT.optionalFieldOf("fall_damage_start").forGetter(CommonRecords.OptionalProjectileDataRecord::damageDecayStartTick),
                        Codec.FLOAT.optionalFieldOf("fall_damage_end").forGetter(CommonRecords.OptionalProjectileDataRecord::damageDecayPerTick)
                ).apply(instance, CommonRecords.OptionalProjectileDataRecord::new)
        );

        public static final Codec<SingularSloshShotData> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.FLOAT.optionalFieldOf("startup_ticks", 0f).forGetter(SingularSloshShotData::startupTicks),
                        Codec.INT.fieldOf("slosh_count").forGetter(SingularSloshShotData::count),
                        Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("delay_between_projectiles", 1f).forGetter(SingularSloshShotData::delayBetweenProjectiles),
                        Codec.FLOAT.optionalFieldOf("speed_substract_per_projectile", 0f).forGetter(SingularSloshShotData::speedSubstract),
                        Codec.FLOAT.optionalFieldOf("offset_angle", 0f).forGetter(SingularSloshShotData::offsetAngle),
                        PROJECTILE_MODIFICATIONS_CODEC.optionalFieldOf("slosh_projectile_modifications").forGetter(SingularSloshShotData::projectileModifications),
                        BlasterWeaponSettings.DetonationRecord.CODEC.optionalFieldOf("detonation_data").forGetter(SingularSloshShotData::detonationData)
                ).apply(instance, SingularSloshShotData::new)
        );

        public static final CommonRecords.ProjectileDataRecord SLOSHER_PROJECTILE_DEFAULT = new CommonRecords.ProjectileDataRecord(0, 0, 600, 0, 1f, 0.729f, 0, 0.225f, 0, 0, 4, 0, 0, 1f, 5f);

        public static final SingularSloshShotData DEFAULT = new SingularSloshShotData(0, 1, 1f, 0, 0, Optional.empty(), Optional.empty());
    }
}
