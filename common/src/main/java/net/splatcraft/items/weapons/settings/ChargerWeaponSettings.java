package net.splatcraft.forge.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.forge.entities.ExtraSaveData;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.util.WeaponTooltip;

import java.util.Optional;

public class ChargerWeaponSettings extends AbstractWeaponSettings<ChargerWeaponSettings, ChargerWeaponSettings.DataRecord>
{
    public static final ChargerWeaponSettings DEFAULT = new ChargerWeaponSettings("default");
    public ChargerProjectileDataRecord projectileData = ChargerProjectileDataRecord.DEFAULT;
    public ShotDataRecord shotData = ShotDataRecord.DEFAULT;
    public ChargeDataRecord chargeData = ChargeDataRecord.DEFAULT;
    public boolean bypassesMobDamage = false;

    public ChargerWeaponSettings(String name)
    {
        super(name);
    }

    @Override
    public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
    {
        ExtraSaveData.ChargeExtraData chargeData = list.getFirstExtraData(ExtraSaveData.ChargeExtraData.class);
        if (chargeData != null)
        {
            return chargeData.charge >= 1.0f ? projectileData.fullyChargedDamage : projectileData.minChargeDamage + (projectileData.maxChargeDamage - projectileData.minChargeDamage) * chargeData.charge;
        }
        return projectileData.minChargeDamage;
    }

    @Override
    public WeaponTooltip<ChargerWeaponSettings>[] tooltipsToRegister()
    {
        return new WeaponTooltip[]
            {
                new WeaponTooltip<ChargerWeaponSettings>("range", WeaponTooltip.Metrics.BLOCKS, settings -> settings.projectileData.fullyChargedRange, WeaponTooltip.RANKER_ASCENDING),
                new WeaponTooltip<ChargerWeaponSettings>("charge_speed", WeaponTooltip.Metrics.SECONDS, settings -> settings.chargeData.chargeTime() / 20f, WeaponTooltip.RANKER_DESCENDING),
                new WeaponTooltip<ChargerWeaponSettings>("mobility", WeaponTooltip.Metrics.MULTIPLIER, settings -> settings.moveSpeed, WeaponTooltip.RANKER_ASCENDING)
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
        projectileData = data.projectile;
        shotData = data.shot;
        chargeData = data.charge;

        setMoveSpeed(data.mobility);
        setSecret(data.isSecret);
        setBypassesMobDamage(data.fullDamageToMobs);
    }

    @Override
    public DataRecord serialize()
    {
        return new DataRecord(projectileData,
            shotData,
            chargeData,
            moveSpeed,
            bypassesMobDamage, isSecret);
    }

    @Override
    public float getSpeedForRender(LocalPlayer player, ItemStack mainHandItem)
    {
        return projectileData.speed();
    }

    public ChargerWeaponSettings setBypassesMobDamage(boolean bypassesMobDamage)
    {
        this.bypassesMobDamage = bypassesMobDamage;
        return this;
    }

    public record DataRecord(
        ChargerProjectileDataRecord projectile,
        ShotDataRecord shot,
        ChargeDataRecord charge,
        float mobility,
        boolean fullDamageToMobs,
        boolean isSecret
    )
    {
        public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                ChargerProjectileDataRecord.CODEC.fieldOf("projectile").forGetter(DataRecord::projectile),
                ShotDataRecord.CODEC.fieldOf("shot").forGetter(DataRecord::shot),
                ChargeDataRecord.CODEC.fieldOf("charge").forGetter(DataRecord::charge),
                Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::mobility),
                Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::fullDamageToMobs),
                Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
            ).apply(instance, DataRecord::new)
        );
    }

    public record ChargerProjectileDataRecord(
        float size,
        float minChargeRange,
        float maxChargeRange,
        float fullyChargedRange,
        float speed,
        float inkCoverageImpact,
        float inkDropCoverage,
        float distanceBetweenInkDrops,
        float minChargeDamage,
        float maxChargeDamage,
        float fullyChargedDamage,
        float piercesAtCharge
    )
    {
        public static final Codec<ChargerProjectileDataRecord> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.FLOAT.fieldOf("size").forGetter(ChargerProjectileDataRecord::size),
                Codec.FLOAT.fieldOf("min_charge_range").forGetter(ChargerProjectileDataRecord::minChargeRange),
                Codec.FLOAT.fieldOf("max_charge_range").forGetter(ChargerProjectileDataRecord::maxChargeRange),
                Codec.FLOAT.optionalFieldOf("fully_charge_range").forGetter(v -> Optional.of(v.maxChargeRange())),
                Codec.FLOAT.fieldOf("speed").forGetter(ChargerProjectileDataRecord::speed),
                Codec.FLOAT.optionalFieldOf("ink_drop_coverage").forGetter(v -> Optional.of(v.inkDropCoverage)),
                Codec.FLOAT.optionalFieldOf("ink_coverage_on_impact").forGetter(v -> Optional.of(v.inkCoverageImpact)),
                Codec.FLOAT.optionalFieldOf("distance_between_drops", 4f).forGetter(ChargerProjectileDataRecord::distanceBetweenInkDrops),
                Codec.FLOAT.fieldOf("min_partial_charge_damage").forGetter(ChargerProjectileDataRecord::minChargeDamage),
                Codec.FLOAT.fieldOf("max_partial_charge_damage").forGetter(ChargerProjectileDataRecord::maxChargeDamage),
                Codec.FLOAT.optionalFieldOf("fully_charged_damage").forGetter(v -> Optional.of(v.fullyChargedDamage)),
                Codec.FLOAT.optionalFieldOf("pierces_at_charge", 2f).forGetter(ChargerProjectileDataRecord::piercesAtCharge)
            ).apply(instance, ChargerProjectileDataRecord::create)
        );
        public static final ChargerProjectileDataRecord DEFAULT = new ChargerProjectileDataRecord(0, 10, 20, 20, 1, 1, 1, 1, 10, 15, 20, 2f);

        public static ChargerProjectileDataRecord create(float size,
                                                         float minRange,
                                                         float maxRange,
                                                         Optional<Float> fullyChargedRange,
                                                         float speed,
                                                         Optional<Float> inkCoverageImpact,
                                                         Optional<Float> inkDropCoverage,
                                                         float distanceBetweenInkDrops,
                                                         float minChargeDamage,
                                                         float baseChargeDamage,
                                                         Optional<Float> fullyChargedDamage,
                                                         float piercesAtCharge
        )
        {
            return new ChargerProjectileDataRecord(
                size,
                minRange,
                maxRange,
                fullyChargedRange.orElse(maxRange),
                speed,
                inkCoverageImpact.orElse(size * 0.85f),
                inkDropCoverage.orElse(size * 1.1f),
                distanceBetweenInkDrops,
                minChargeDamage,
                baseChargeDamage,
                fullyChargedDamage.orElse(baseChargeDamage),
                piercesAtCharge
            );
        }
    }

    public record ChargeDataRecord(
        int chargeTime,
        float airborneChargeRate,
        float emptyTankChargeRate,
        int chargeStorageTime
    )
    {
        public static final Codec<ChargeDataRecord> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("charge_time_ticks").forGetter(ChargeDataRecord::chargeTime),
                Codec.floatRange(0, 1).optionalFieldOf("airborne_charge_rate", 0.33f).forGetter(ChargeDataRecord::airborneChargeRate),
                Codec.floatRange(0, 1).optionalFieldOf("empty_tank_charge_rate", 0.33f).forGetter(ChargeDataRecord::emptyTankChargeRate),
                Codec.INT.optionalFieldOf("charge_storage_ticks", 25).forGetter(ChargeDataRecord::chargeStorageTime)
            ).apply(instance, ChargeDataRecord::create)
        );
        public static final ChargeDataRecord DEFAULT = new ChargeDataRecord(10, 1f / 3, 1f / 3, 25);

        private static ChargeDataRecord create(int chargeTime,
                                               float airborneChargeTime,
                                               float emptyTankChargeTime,
                                               int chargeStorageTime)
        {
            return new ChargeDataRecord(chargeTime, airborneChargeTime, emptyTankChargeTime, chargeStorageTime);
        }

        public float getChargePercentPerTick()
        {
            return 1f / chargeTime;
        }
    }

    public record ShotDataRecord(
        int endlagTicks,
        float minInkConsumption,
        float maxInkConsumption,
        int inkRecoveryCooldown,
        int shotsCount

    )
    {
        public static final Codec<ShotDataRecord> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.INT.fieldOf("endlag_ticks").forGetter(ShotDataRecord::endlagTicks),
                Codec.FLOAT.fieldOf("min_charge_ink_consumption").forGetter(ShotDataRecord::minInkConsumption),
                Codec.FLOAT.fieldOf("full_charge_ink_consumption").forGetter(ShotDataRecord::maxInkConsumption),
                Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(ShotDataRecord::inkRecoveryCooldown),
                Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("shots_after_charge", 1).forGetter(ShotDataRecord::shotsCount)
            ).apply(instance, ShotDataRecord::new)
        );
        public static final ShotDataRecord DEFAULT = new ShotDataRecord(10, 2f, 14f, 25, 1);
    }
}
