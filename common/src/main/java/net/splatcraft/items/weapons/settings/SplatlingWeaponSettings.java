package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.util.WeaponTooltip;

import java.util.Optional;

public class SplatlingWeaponSettings extends AbstractWeaponSettings<SplatlingWeaponSettings, SplatlingWeaponSettings.DataRecord>
{
    public static final SplatlingWeaponSettings DEFAULT = new SplatlingWeaponSettings("default");
    public ShotDataRecord firstChargeLevelShot = ShotDataRecord.DEFAULT;
    public CommonRecords.ProjectileDataRecord firstChargeLevelProjectile = CommonRecords.ProjectileDataRecord.DEFAULT;
    public ShotDataRecord secondChargeLevelShot = ShotDataRecord.DEFAULT;
    public CommonRecords.ProjectileDataRecord secondChargeLevelProjectile = CommonRecords.ProjectileDataRecord.DEFAULT;
    public ChargeDataRecord chargeData = ChargeDataRecord.DEFAULT;
    public boolean bypassesMobDamage = false;
    public float inkConsumption;
    public int inkRecoveryCooldown;

    public SplatlingWeaponSettings(String name)
    {
        super(name);
    }

    @Override
    public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
    {
        ExtraSaveData.ChargeExtraData data = list.getFirstExtraData(ExtraSaveData.ChargeExtraData.class);
        if (data == null) // oh no
            return 0;
        if (data.charge >= 1)
            return projectile.calculateDamageDecay(secondChargeLevelProjectile.baseDamage(), secondChargeLevelProjectile.damageDecayStartTick(), secondChargeLevelProjectile.damageDecayPerTick(), secondChargeLevelProjectile.minDamage());
        return projectile.calculateDamageDecay(firstChargeLevelProjectile.baseDamage(), firstChargeLevelProjectile.damageDecayStartTick(), firstChargeLevelProjectile.damageDecayPerTick(), firstChargeLevelProjectile.minDamage());
    }

    @Override
    public WeaponTooltip<SplatlingWeaponSettings>[] tooltipsToRegister()
    {
        return new WeaponTooltip[]
            {
                new WeaponTooltip<SplatlingWeaponSettings>("range", WeaponTooltip.Metrics.BLOCKS, settings ->
                    Math.max(calculateAproximateRange(settings.firstChargeLevelProjectile),
                        calculateAproximateRange(settings.secondChargeLevelProjectile)), WeaponTooltip.RANKER_ASCENDING),
                new WeaponTooltip<SplatlingWeaponSettings>("charge_speed", WeaponTooltip.Metrics.SECONDS, settings -> (settings.chargeData.firstChargeTime + settings.chargeData.secondChargeTime) / 20f, WeaponTooltip.RANKER_DESCENDING),
                new WeaponTooltip<SplatlingWeaponSettings>("mobility", WeaponTooltip.Metrics.MULTIPLIER, settings -> settings.moveSpeed, WeaponTooltip.RANKER_ASCENDING)
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
        return stack.get(SplatcraftComponents.CHARGE) > 1 ? secondChargeLevelShot.accuracyData : firstChargeLevelShot.accuracyData;
    }

    @Override
    public void deserialize(DataRecord data)
    {
        firstChargeLevelProjectile = data.projectile;
        firstChargeLevelShot = data.shot;
        secondChargeLevelProjectile = data.secondChargeLevelProjectile;
        secondChargeLevelShot = data.secondChargeLevelShot;

        setMoveSpeed(data.moveSpeed);
        setSecret(data.isSecret);
        setBypassesMobDamage(data.bypassesMobDamage);

        setInkConsumption(data.inkConsumption);
        setInkRecoveryCooldown(data.inkRecoveryCooldown);
    }

    @Override
    public DataRecord serialize()
    {
        return new DataRecord(firstChargeLevelProjectile, firstChargeLevelShot, secondChargeLevelProjectile, secondChargeLevelShot, chargeData, inkConsumption, inkRecoveryCooldown, moveSpeed, (bypassesMobDamage), (isSecret));
    }

    @Override
    public float getSpeedForRender(ClientPlayerEntity player, ItemStack mainHandItem)
    {
        return firstChargeLevelProjectile.speed();
    }

    public SplatlingWeaponSettings setBypassesMobDamage(boolean bypassesMobDamage)
    {
        this.bypassesMobDamage = bypassesMobDamage;
        return this;
    }

    public SplatlingWeaponSettings setInkConsumption(float inkConsumption)
    {
        this.inkConsumption = inkConsumption;
        return this;
    }

    public SplatlingWeaponSettings setInkRecoveryCooldown(int inkRecoveryCooldown)
    {
        this.inkRecoveryCooldown = inkRecoveryCooldown;
        return this;
    }

    public int getDualieOffhandFiringOffset(boolean secondChargeLevel) // ok this would be funny to implement
    {
        return firstChargeLevelShot.firingSpeed / 2;
    }

    public record DataRecord(
        CommonRecords.ProjectileDataRecord projectile,
        ShotDataRecord shot,
        CommonRecords.ProjectileDataRecord secondChargeLevelProjectile,
        ShotDataRecord secondChargeLevelShot,
        ChargeDataRecord charge,
        float inkConsumption,
        int inkRecoveryCooldown,
        float moveSpeed,
        boolean bypassesMobDamage,
        boolean isSecret
    )
    {
        public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                CommonRecords.ProjectileDataRecord.CODEC.fieldOf("projectile").forGetter(DataRecord::projectile),
                ShotDataRecord.CODEC.fieldOf("shot").forGetter(DataRecord::shot),
                CommonRecords.ProjectileDataRecord.CODEC.optionalFieldOf("second_charge_projectile").forGetter(v -> Optional.of(v.secondChargeLevelProjectile)),
                ShotDataRecord.CODEC.optionalFieldOf("second_charge_shot").forGetter(v -> Optional.of(v.secondChargeLevelShot)),
                ChargeDataRecord.CODEC.fieldOf("charge").forGetter(DataRecord::charge),
                Codec.FLOAT.fieldOf("max_ink_consumption").forGetter(DataRecord::inkConsumption),
                Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(DataRecord::inkRecoveryCooldown),
                Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::moveSpeed),
                Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::bypassesMobDamage),
                Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
            ).apply(instance, DataRecord::create)
        );

        public static DataRecord create(CommonRecords.ProjectileDataRecord projectile, ShotDataRecord shot, Optional<CommonRecords.ProjectileDataRecord> secondChargeLevelProjectile, Optional<ShotDataRecord> secondChargeLevelShot, ChargeDataRecord charge, float inkConsumption, int inkRecoveryCooldown, float moveSpeed, boolean bypassesMobDamage, boolean isSecret)
        {
            ChargeDataRecord parsedCharge = new ChargeDataRecord(charge.firstChargeTime, charge.secondChargeTime, charge.emptyTankFirstChargeTime, charge.emptyTankSecondChargeTime, charge.firingDuration, Optional.of(charge.moveSpeed.orElse(moveSpeed)), charge.chargeStorageTime, charge.canRechargeWhileFiring);
            return new DataRecord(projectile, shot, secondChargeLevelProjectile.orElse(projectile), secondChargeLevelShot.orElse(shot), parsedCharge, inkConsumption, inkRecoveryCooldown, moveSpeed, bypassesMobDamage, isSecret);
        }
    }

    public record ChargeDataRecord(
        int firstChargeTime,
        int secondChargeTime,
        int emptyTankFirstChargeTime,
        int emptyTankSecondChargeTime,
        int firingDuration,
        Optional<Float> moveSpeed,
        int chargeStorageTime,
        boolean canRechargeWhileFiring
    )
    {
        public static final Codec<ChargeDataRecord> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.INT.fieldOf("first_charge_time_ticks").forGetter(ChargeDataRecord::firstChargeTime),
                Codec.INT.fieldOf("second_charge_time_ticks").forGetter(ChargeDataRecord::secondChargeTime),
                Codec.INT.optionalFieldOf("empty_tank_first_charge_time_ticks").forGetter(v -> Optional.of(v.emptyTankFirstChargeTime)),
                Codec.INT.optionalFieldOf("empty_tank_second_charge_time_ticks").forGetter(v -> Optional.of(v.emptyTankSecondChargeTime)),
                Codec.INT.fieldOf("total_firing_duration").forGetter(ChargeDataRecord::firingDuration),
                Codec.FLOAT.optionalFieldOf("mobility_while_charging").forGetter(ChargeDataRecord::moveSpeed),
                Codec.INT.optionalFieldOf("charge_storage_ticks", 0).forGetter(ChargeDataRecord::chargeStorageTime),
                Codec.BOOL.optionalFieldOf("can_recharge_while_firing", false).forGetter(ChargeDataRecord::canRechargeWhileFiring)
            ).apply(instance, ChargeDataRecord::create)
        );
        public static final ChargeDataRecord DEFAULT = new ChargeDataRecord(0, 0, 0, 0, 0, Optional.empty(), 0, false);

        public static ChargeDataRecord create(int firstChargeTime, int secondChargeTime, Optional<Integer> emptyTankFirstChargeTime, Optional<Integer> emptyTankSecondChargeTime, int firingDuration, Optional<Float> moveSpeed, int chargeStorageTime, boolean canRechargeWhileFiring)
        {
            return new ChargeDataRecord(firstChargeTime, secondChargeTime, emptyTankFirstChargeTime.orElse(firstChargeTime * 6), emptyTankSecondChargeTime.orElse(secondChargeTime * 6), firingDuration, moveSpeed, chargeStorageTime, canRechargeWhileFiring);
        }
    }

    public record ShotDataRecord(
        int startupTicks,
        int firingSpeed,
        int projectileCount,
        CommonRecords.ShotDeviationDataRecord accuracyData,
        float pitchCompensation
    )
    {
        public static final Codec<ShotDataRecord> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.INT.optionalFieldOf("startup_ticks", 0).forGetter(ShotDataRecord::startupTicks),
                Codec.INT.fieldOf("firing_speed").forGetter(ShotDataRecord::firingSpeed),
                Codec.INT.optionalFieldOf("shot_count", 1).forGetter(ShotDataRecord::projectileCount),
                CommonRecords.ShotDeviationDataRecord.CODEC.optionalFieldOf("accuracy_data", CommonRecords.ShotDeviationDataRecord.PERFECT_DEFAULT).forGetter(ShotDataRecord::accuracyData),
                Codec.FLOAT.optionalFieldOf("pitch_compensation", 0f).forGetter(ShotDataRecord::pitchCompensation)
            ).apply(instance, ShotDataRecord::create)
        );
        public static final ShotDataRecord DEFAULT = new ShotDataRecord(0, 0, 1, CommonRecords.ShotDeviationDataRecord.DEFAULT, 0);

        public static ShotDataRecord create(int startupTicks, int firingSpeed, int projectileCount, CommonRecords.ShotDeviationDataRecord accuracyData, float pitchCompensation)
        {
            return new ShotDataRecord(startupTicks, firingSpeed, projectileCount, accuracyData, pitchCompensation);
        }
    }
}
