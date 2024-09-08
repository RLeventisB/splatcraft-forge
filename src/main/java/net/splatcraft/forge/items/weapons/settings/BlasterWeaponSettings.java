package net.splatcraft.forge.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.util.DamageRangesRecord;
import net.splatcraft.forge.util.WeaponTooltip;

import java.util.Optional;

public class BlasterWeaponSettings extends AbstractWeaponSettings<BlasterWeaponSettings, BlasterWeaponSettings.DataRecord>
{
    public CommonRecords.ProjectileDataRecord projectileData = CommonRecords.ProjectileDataRecord.DEFAULT;
    public CommonRecords.ShotDataRecord shotData = CommonRecords.ShotDataRecord.DEFAULT;
    public DetonationRecord blasterData = DetonationRecord.DEFAULT;
    public boolean bypassesMobDamage = false;
    public static final BlasterWeaponSettings DEFAULT = new BlasterWeaponSettings("default");

    public BlasterWeaponSettings(String name)
    {
        super(name);
    }

    @Override
    public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
    {
        return projectileData.baseDamage();
    }

    @Override
    public WeaponTooltip<BlasterWeaponSettings>[] tooltipsToRegister()
    {
        return new WeaponTooltip[]
                {
                        new WeaponTooltip<BlasterWeaponSettings>("range", WeaponTooltip.Metrics.BLOCKS, settings -> calculateAproximateRange(settings.projectileData), WeaponTooltip.RANKER_ASCENDING),
                        new WeaponTooltip<BlasterWeaponSettings>("direct_damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.projectileData.baseDamage(), WeaponTooltip.RANKER_ASCENDING),
                        new WeaponTooltip<BlasterWeaponSettings>("fire_rate", WeaponTooltip.Metrics.TICKS, settings -> settings.shotData.startupTicks() + settings.shotData.endlagTicks(), WeaponTooltip.RANKER_DESCENDING)
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
        return shotData.accuracyData();
    }

    @Override
    public void deserialize(DataRecord data)
    {
        projectileData = data.projectile;
        shotData = data.shot;
        blasterData = data.blast;

        setMoveSpeed(data.mobility);
        setSecret(data.isSecret);
        setBypassesMobDamage(data.bypassesMobDamage);
    }

    public BlasterWeaponSettings setBypassesMobDamage(boolean bypassesMobDamage)
    {
        this.bypassesMobDamage = bypassesMobDamage;
        return this;
    }

    @Override
    public DataRecord serialize()
    {
        return new DataRecord(projectileData, shotData, blasterData, moveSpeed, bypassesMobDamage, isSecret);
    }

    public record DataRecord(
            CommonRecords.ProjectileDataRecord projectile,
            CommonRecords.ShotDataRecord shot,
            DetonationRecord blast,
            float mobility,
            boolean bypassesMobDamage,
            boolean isSecret
    )
    {
        public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        CommonRecords.ProjectileDataRecord.CODEC.fieldOf("projectile").forGetter(DataRecord::projectile),
                        CommonRecords.ShotDataRecord.CODEC.fieldOf("shot").forGetter(DataRecord::shot),
                        DetonationRecord.CODEC.fieldOf("blaster_data").forGetter(DataRecord::blast),
                        Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::mobility),
                        Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::bypassesMobDamage),
                        Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
                ).apply(instance, DataRecord::new)
        );
    }

    public record DetonationRecord(
            DamageRangesRecord damageRadiuses,
            float sparkDamagePenalty,
            float explosionPaint,
            boolean newAttackId
    )
    {
        public static final Codec<DetonationRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        DamageRangesRecord.CODEC.fieldOf("damage_data").forGetter(DetonationRecord::damageRadiuses),
                        Codec.FLOAT.optionalFieldOf("spark_damage_multiplier", 0.5f).forGetter(DetonationRecord::sparkDamagePenalty),
                        Codec.FLOAT.optionalFieldOf("explosion_paint_size").forGetter((DetonationRecord v) -> Optional.of(v.explosionPaint)),
                        Codec.BOOL.optionalFieldOf("new_attack_id", false).forGetter(DetonationRecord::newAttackId)
                ).apply(instance, DetonationRecord::create)
        );

        public static DetonationRecord create(DamageRangesRecord damageRadiuses,
                                              float sparkPenalty,
                                              Optional<Float> explosionPaint,
                                              boolean newAttackId)
        {
            return new DetonationRecord(damageRadiuses, sparkPenalty, explosionPaint.orElse(damageRadiuses.getMaxRegisteredDistance()), newAttackId);
        }

        public static final DetonationRecord DEFAULT = new DetonationRecord(DamageRangesRecord.DEFAULT, 0, 0, false);
    }
}
