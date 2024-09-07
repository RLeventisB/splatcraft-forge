package net.splatcraft.forge.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.forge.entities.ExtraSaveData;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.util.WeaponTooltip;

import java.util.Optional;

public class DualieWeaponSettings extends AbstractWeaponSettings<DualieWeaponSettings, DualieWeaponSettings.DataRecord>
{
    public CommonRecords.ProjectileDataRecord standardProjectileData = CommonRecords.ProjectileDataRecord.DEFAULT, turretProjectileData = CommonRecords.ProjectileDataRecord.DEFAULT;
    public CommonRecords.ShotDataRecord standardShotData = CommonRecords.ShotDataRecord.DEFAULT, turretShotData = CommonRecords.ShotDataRecord.DEFAULT;
    public RollDataRecord rollData = RollDataRecord.DEFAULT;
    public boolean bypassesMobDamage = false;
    public static final DualieWeaponSettings DEFAULT = new DualieWeaponSettings("default");

    public DualieWeaponSettings(String name)
    {
        super(name);
    }

    @Override
    public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
    {
        ExtraSaveData.DualieExtraData dualieData = list.getFirstExtraData(ExtraSaveData.DualieExtraData.class);
        if (dualieData != null && dualieData.rollBullet)
        {
            return projectile.calculateDamageDecay(turretProjectileData.baseDamage(), turretProjectileData.damageDecayStartTick(), turretProjectileData.damageDecayPerTick(), turretProjectileData.minDamage());
        }

        return projectile.calculateDamageDecay(standardProjectileData.baseDamage(), standardProjectileData.damageDecayStartTick(), standardProjectileData.damageDecayPerTick(), standardProjectileData.minDamage());
    }

    @Override
    public WeaponTooltip<DualieWeaponSettings>[] tooltipsToRegister()
    {
        return new WeaponTooltip[]
                {
                        new WeaponTooltip<DualieWeaponSettings>("range", WeaponTooltip.Metrics.BLOCKS, settings -> calculateAproximateRange(settings.standardProjectileData), WeaponTooltip.RANKER_ASCENDING),
                        new WeaponTooltip<DualieWeaponSettings>("damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.standardProjectileData.baseDamage(), WeaponTooltip.RANKER_ASCENDING),
                        new WeaponTooltip<DualieWeaponSettings>("roll_distance", WeaponTooltip.Metrics.BLOCKS, settings -> settings.rollData.speed * 6, WeaponTooltip.RANKER_ASCENDING) //i used desmos to get that 6 B)
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
        return standardShotData.accuracyData();
    }

    @Override
    public void deserialize(DataRecord data)
    {
        standardProjectileData = data.projectile;
        turretProjectileData = data.turretProjectile;
        standardShotData = data.shot;
        turretShotData = data.turretShot;
        rollData = data.roll;

        setMoveSpeed(data.moveSpeed);
        setSecret(data.isSecret);
        setBypassesMobDamage(data.bypassesMobDamage);
    }

    @Override
    public DataRecord serialize()
    {
        return new DataRecord(
                standardProjectileData,
                standardShotData,
                turretProjectileData,
                turretShotData,
                rollData,
                moveSpeed,
                bypassesMobDamage,
                isSecret);
    }

    public DualieWeaponSettings setBypassesMobDamage(boolean bypassesMobDamage)
    {
        this.bypassesMobDamage = bypassesMobDamage;
        return this;
    }

    public record DataRecord(
            CommonRecords.ProjectileDataRecord projectile,
            CommonRecords.ShotDataRecord shot,
            CommonRecords.ProjectileDataRecord turretProjectile,
            CommonRecords.ShotDataRecord turretShot,
            RollDataRecord roll,
            float moveSpeed,
            boolean bypassesMobDamage,
            boolean isSecret
    )
    {
        public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        CommonRecords.ProjectileDataRecord.CODEC.fieldOf("projectile").forGetter(DataRecord::projectile),
                        CommonRecords.ShotDataRecord.CODEC.fieldOf("shot").forGetter(DataRecord::shot),
                        CommonRecords.OptionalProjectileDataRecord.CODEC.optionalFieldOf("turret_projectile").forGetter((DataRecord v) -> CommonRecords.OptionalProjectileDataRecord.convert(v.turretProjectile)),
                        CommonRecords.OptionalShotDataRecord.CODEC.optionalFieldOf("turret_shot").forGetter((DataRecord v) -> CommonRecords.OptionalShotDataRecord.convert(v.turretShot)),
                        RollDataRecord.CODEC.fieldOf("dodge_roll").forGetter(DataRecord::roll),
                        Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::moveSpeed),
                        Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::bypassesMobDamage),
                        Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
                ).apply(instance, DataRecord::create)
        );

        public static DataRecord create(CommonRecords.ProjectileDataRecord projectile, CommonRecords.ShotDataRecord shot, Optional<CommonRecords.OptionalProjectileDataRecord> turretProjectile, Optional<CommonRecords.OptionalShotDataRecord> turretShot, RollDataRecord roll, float mobility, boolean bypassesMobDamage, boolean isSecret)
        {
            CommonRecords.ProjectileDataRecord parsedTurretProjectile;
            CommonRecords.ShotDataRecord parsedTurretShot;
            if (turretProjectile.isPresent())
            {
                CommonRecords.OptionalProjectileDataRecord localTurretProjectile = turretProjectile.get(); // ok so i just moved the boilerplate code to another place, great!!!!
                parsedTurretProjectile = new CommonRecords.ProjectileDataRecord(
                        localTurretProjectile.size().orElse(projectile.size()),
                        localTurretProjectile.visualSize().orElse(projectile.visualSize()),
                        localTurretProjectile.lifeTicks().orElse(projectile.lifeTicks()),
                        localTurretProjectile.speed().orElse(projectile.speed()),
                        localTurretProjectile.delayStartSpeed().orElse(projectile.delaySpeedMult()),
                        localTurretProjectile.horizontalDrag().orElse(projectile.horizontalDrag()),
                        localTurretProjectile.straightShotTicks().orElse(projectile.straightShotTicks()),
                        localTurretProjectile.gravity().orElse(projectile.gravity()),
                        localTurretProjectile.inkCoverageImpact().orElse(projectile.inkCoverageImpact()),
                        localTurretProjectile.inkDropCoverage().orElse(projectile.inkDropCoverage()),
                        localTurretProjectile.distanceBetweenInkDrops().orElse(projectile.distanceBetweenInkDrops()),
                        localTurretProjectile.baseDamage().orElse(projectile.baseDamage()),
                        localTurretProjectile.minDamage().orElse(projectile.minDamage()),
                        localTurretProjectile.damageDecayStartTick().orElse(projectile.damageDecayStartTick()),
                        localTurretProjectile.damageDecayPerTick().orElse(projectile.damageDecayPerTick())
                );
            }
            else
            {
                parsedTurretProjectile = projectile;
            }
            if (turretShot.isPresent())
            {
                CommonRecords.OptionalShotDataRecord localTurretShot = turretShot.get(); // ok so i just moved the boilerplate code to another place, great!!!!
                parsedTurretShot = new CommonRecords.ShotDataRecord(
                        localTurretShot.startupTicks().orElse(shot.startupTicks()),
                        localTurretShot.endlagTicks().orElse(shot.endlagTicks()),
                        localTurretShot.projectileCount().orElse(shot.projectileCount()),
                        localTurretShot.accuracyData().orElse(shot.accuracyData()),
                        localTurretShot.pitchCompensation().orElse(shot.pitchCompensation()),
                        localTurretShot.inkConsumption().orElse(shot.inkConsumption()),
                        localTurretShot.inkRecoveryCooldown().orElse(shot.inkRecoveryCooldown())
                );
            }
            else
            {
                parsedTurretShot = shot;
            }
            return new DataRecord(projectile, shot, parsedTurretProjectile, parsedTurretShot, roll, mobility, bypassesMobDamage, isSecret);
        }
    }

    public record RollDataRecord(
            float count,
            float speed,
            float inkConsumption,
            int inkRecoveryCooldown,
            byte rollStartup,
            byte rollEndlag,
            int turretDuration,
            int lastRollTurretDuration,
            boolean canMove
    )
    {
        public static final Codec<RollDataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.FLOAT.fieldOf("count").forGetter(RollDataRecord::count),
                        Codec.FLOAT.fieldOf("movement_impulse").forGetter(RollDataRecord::speed),
                        Codec.FLOAT.fieldOf("ink_consumption").forGetter(RollDataRecord::inkConsumption),
                        Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(RollDataRecord::inkRecoveryCooldown),
                        Codec.BYTE.optionalFieldOf("roll_startup", (byte) 2).forGetter(RollDataRecord::rollStartup),
                        Codec.BYTE.optionalFieldOf("roll_endlag", (byte) 2).forGetter(RollDataRecord::rollEndlag),
                        Codec.INT.fieldOf("turret_duration").forGetter(RollDataRecord::turretDuration),
                        Codec.INT.fieldOf("final_roll_turret_duration").forGetter(RollDataRecord::lastRollTurretDuration),
                        Codec.BOOL.optionalFieldOf("allows_movement", false).forGetter(RollDataRecord::canMove)
                ).apply(instance, RollDataRecord::new)
        );
        public static final RollDataRecord DEFAULT = new RollDataRecord(0, 0, 0, 0, (byte) 2, (byte) 2, 0, 0, false);
    }
}
