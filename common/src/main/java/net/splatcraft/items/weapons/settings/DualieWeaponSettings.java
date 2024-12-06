package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.items.weapons.DualieItem;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.PlayerCooldown;
import net.splatcraft.util.WeaponTooltip;

import java.util.Optional;

public class DualieWeaponSettings extends AbstractWeaponSettings<DualieWeaponSettings, DualieWeaponSettings.DataRecord>
{
    public static final DualieWeaponSettings DEFAULT = new DualieWeaponSettings("default");
    public CommonRecords.ProjectileDataRecord standardProjectileData = CommonRecords.ProjectileDataRecord.DEFAULT, turretProjectileData = CommonRecords.ProjectileDataRecord.DEFAULT;
    public CommonRecords.ShotDataRecord standardShotData = CommonRecords.ShotDataRecord.DEFAULT, turretShotData = CommonRecords.ShotDataRecord.DEFAULT;
    public RollDataRecord rollData = RollDataRecord.DEFAULT;
    public boolean bypassesMobDamage = false;

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
                new WeaponTooltip<DualieWeaponSettings>("roll_distance", WeaponTooltip.Metrics.BLOCKS, settings -> settings.rollData.rollDistance * 6, WeaponTooltip.RANKER_ASCENDING) //i used desmos to get that 6 B)
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
        return PlayerCooldown.hasPlayerCooldown(entity) && PlayerCooldown.getPlayerCooldown(entity) instanceof DualieItem.DodgeRollCooldown ? turretShotData.accuracyData() : standardShotData.accuracyData();
    }

    @Override
    public void deserialize(DataRecord data)
    {
        standardProjectileData = SplatcraftConvertors.convert(data.projectile);
        turretProjectileData = SplatcraftConvertors.convert(data.turretProjectile);
        standardShotData = SplatcraftConvertors.convert(data.shot);
        turretShotData = SplatcraftConvertors.convert(data.turretShot);
        rollData = SplatcraftConvertors.convert(data.roll);

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

    @Override
    public float getSpeedForRender(LocalPlayer player, ItemStack mainHandItem)
    {
        return standardProjectileData.speed();
    }

    public DualieWeaponSettings setBypassesMobDamage(boolean bypassesMobDamage)
    {
        this.bypassesMobDamage = bypassesMobDamage;
        return this;
    }

    public CommonRecords.ShotDataRecord getShotData(LivingEntity entity)
    {
        return CommonUtils.isRolling(entity) ? turretShotData : standardShotData;
    }

    public CommonRecords.ProjectileDataRecord getProjectileData(LivingEntity entity)
    {
        return CommonUtils.isRolling(entity) ? turretProjectileData : standardProjectileData;
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
                CommonRecords.OptionalProjectileDataRecord.CODEC.optionalFieldOf("turret_projectile").forGetter((DataRecord v) -> CommonRecords.OptionalProjectileDataRecord.from(v.turretProjectile)),
                CommonRecords.OptionalShotDataRecord.CODEC.optionalFieldOf("turret_shot").forGetter((DataRecord v) -> CommonRecords.OptionalShotDataRecord.from(v.turretShot)),
                RollDataRecord.CODEC.fieldOf("dodge_roll").forGetter(DataRecord::roll),
                Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::moveSpeed),
                Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::bypassesMobDamage),
                Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
            ).apply(instance, DataRecord::create)
        );

        public static DataRecord create(CommonRecords.ProjectileDataRecord projectile, CommonRecords.ShotDataRecord shot, Optional<CommonRecords.OptionalProjectileDataRecord> turretProjectile, Optional<CommonRecords.OptionalShotDataRecord> turretShot, RollDataRecord roll, float mobility, boolean bypassesMobDamage, boolean isSecret)
        {
            return new DataRecord(projectile, shot,
                CommonRecords.OptionalProjectileDataRecord.mergeWithBase(turretProjectile, projectile),
                CommonRecords.OptionalShotDataRecord.mergeWithBase(turretShot, shot),
                roll, mobility, bypassesMobDamage, isSecret);
        }
    }

    public record RollDataRecord(
        float count,
        float rollDistance,
        float inkConsumption,
        int inkRecoveryCooldown,
        byte rollStartup,
        byte rollDuration,
        byte rollEndlag,
        int turretDuration,
        int lastRollTurretDuration,
        boolean canMove
    )
    {
        public static final Codec<RollDataRecord> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.FLOAT.fieldOf("count").forGetter(RollDataRecord::count),
                Codec.FLOAT.fieldOf("distance_covered_by_roll").forGetter(RollDataRecord::rollDistance),
                Codec.FLOAT.fieldOf("ink_consumption").forGetter(RollDataRecord::inkConsumption),
                Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(RollDataRecord::inkRecoveryCooldown),
                Codec.BYTE.optionalFieldOf("roll_startup", (byte) 6).forGetter(RollDataRecord::rollStartup),
                Codec.BYTE.optionalFieldOf("roll_duration", (byte) 12).forGetter(RollDataRecord::rollStartup),
                Codec.BYTE.optionalFieldOf("roll_endlag", (byte) 6).forGetter(RollDataRecord::rollEndlag),
                Codec.INT.fieldOf("turret_duration").forGetter(RollDataRecord::turretDuration),
                Codec.INT.fieldOf("final_roll_turret_duration").forGetter(RollDataRecord::lastRollTurretDuration),
                Codec.BOOL.optionalFieldOf("allows_movement", false).forGetter(RollDataRecord::canMove)
            ).apply(instance, RollDataRecord::new)
        );
        public static final RollDataRecord DEFAULT = new RollDataRecord(0, 0, 0, 0, (byte) 2, (byte) 4, (byte) 2, 0, 0, false);

        public float getRollImpulse()
        {
            // x is speed, this should be the value that should be found
            // rollDistance = x * roll_duration + x / (1 - 0.4)
            // rollDistance = x * (roll_duration + 0.6)
            // rollDistance / (roll_duration + 0.6) = x
            return rollDistance / (rollDuration + 0.4f) / SplatcraftConvertors.SplatoonFramesPerMinecraftTick;
        }
    }
}
