package net.splatcraft.forge.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.forge.data.SplatcraftConvertors;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.items.weapons.WeaponBaseItem;
import net.splatcraft.forge.util.DamageRangesRecord;
import net.splatcraft.forge.util.WeaponTooltip;

public class SubWeaponSettings extends AbstractWeaponSettings<SubWeaponSettings, SubWeaponSettings.DataRecord>
{
    public DataRecord subDataRecord = DataRecord.DEFAULT;
    public static final SubWeaponSettings DEFAULT = new SubWeaponSettings("default");

    public SubWeaponSettings(String name)
    {
        super(name);
    }

    @Override
    public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
    {
        return subDataRecord.directDamage;
    }

    @Override
    public WeaponTooltip<SubWeaponSettings>[] tooltipsToRegister()
    {
        return new WeaponTooltip[]
                {
                        new WeaponTooltip<SubWeaponSettings>("direct_damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.subDataRecord.directDamage, WeaponTooltip.RANKER_ASCENDING),
                        new WeaponTooltip<SubWeaponSettings>("splash_damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.subDataRecord.damageRanges.getMaxRegisteredDamage(), WeaponTooltip.RANKER_ASCENDING),
                        new WeaponTooltip<SubWeaponSettings>("ink_consumption", WeaponTooltip.Metrics.UNITS, settings -> settings.subDataRecord.inkConsumption, WeaponTooltip.RANKER_DESCENDING)
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
        subDataRecord = SplatcraftConvertors.convert(data);
    }

    @Override
    public DataRecord serialize()
    {
        return subDataRecord;
    }

    public record DataRecord(
            float directDamage,
            DamageRangesRecord damageRanges,
            float inkSplashRadius,
            int fuseTime,
            float inkConsumption,
            float inkRecoveryCooldown,
            float throwVelocity,
            float throwAngle,
            int holdTime,
            float mobility,
            CurlingDataRecord curlingData,
            boolean isSecret
    )
    {
        public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.FLOAT.fieldOf("direct_damage").forGetter(DataRecord::directDamage),
                        DamageRangesRecord.CODEC.fieldOf("damage_values").forGetter(DataRecord::damageRanges),
                        Codec.FLOAT.optionalFieldOf("ink_splash_radius", 30f).forGetter(DataRecord::inkSplashRadius),
                        Codec.INT.optionalFieldOf("fuse_time", 30).forGetter(DataRecord::fuseTime),
                        Codec.FLOAT.fieldOf("ink_consumption").forGetter(DataRecord::inkConsumption),
                        Codec.FLOAT.fieldOf("ink_recovery_cooldown").forGetter(DataRecord::inkRecoveryCooldown),
                        Codec.FLOAT.optionalFieldOf("throw_velocity", 0.75f).forGetter(DataRecord::throwVelocity),
                        Codec.FLOAT.optionalFieldOf("throw_angle", -14f).forGetter(DataRecord::throwAngle),
                        Codec.INT.optionalFieldOf("hold_time", WeaponBaseItem.USE_DURATION).forGetter(DataRecord::holdTime),
                        Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::mobility),
                        CurlingDataRecord.CODEC.optionalFieldOf("curling", CurlingDataRecord.DEFAULT).forGetter(DataRecord::curlingData),
                        Codec.BOOL.optionalFieldOf("isSecret", false).forGetter(DataRecord::isSecret)
                ).apply(instance, DataRecord::new)
        );
        public static final DataRecord DEFAULT = new DataRecord(20, DamageRangesRecord.DEFAULT, 3f, 30, 70, 70, 0.75f, -20f, WeaponBaseItem.USE_DURATION, 1f, CurlingDataRecord.DEFAULT, false);
    }

    public record CurlingDataRecord(
            int cookTime,
            float contactDamage
    )
    {
        public static final Codec<CurlingDataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.INT.optionalFieldOf("cook_time", 180).forGetter(CurlingDataRecord::cookTime),
                        Codec.FLOAT.optionalFieldOf("contact_damage", 5f).forGetter(CurlingDataRecord::contactDamage)
                ).apply(instance, CurlingDataRecord::new)
        );
        public static final CurlingDataRecord DEFAULT = new CurlingDataRecord(180, 5);
    }
}
