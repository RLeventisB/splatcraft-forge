package net.splatcraft.forge.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.items.weapons.WeaponBaseItem;
import net.splatcraft.forge.util.WeaponTooltip;

import java.util.Optional;

public class SubWeaponSettings extends AbstractWeaponSettings<SubWeaponSettings, SubWeaponSettings.DataRecord>
{
    public float directDamage;
    public float indirectDamage;
    public float propDamage;
    public float explosionSize;
    public float inkConsumption;
    public int inkRecoveryCooldown;
    public int fuseTime;
    public float throwVelocity = 0.75f;
    public float throwAngle = -30;
    public int holdTime = WeaponBaseItem.USE_DURATION;
    //Curling
    public float contactDamage;
    public int cookTime;
    public static final SubWeaponSettings DEFAULT = new SubWeaponSettings("default");

    public SubWeaponSettings(String name)
    {
        super(name);
    }

    @Override
    public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
    {
        return directDamage;
    }

    @Override
    public WeaponTooltip<SubWeaponSettings>[] tooltipsToRegister()
    {
        return new WeaponTooltip[]
                {
                        new WeaponTooltip<SubWeaponSettings>("direct_damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.directDamage, WeaponTooltip.RANKER_ASCENDING),
                        new WeaponTooltip<SubWeaponSettings>("splash_damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.indirectDamage, WeaponTooltip.RANKER_ASCENDING),
                        new WeaponTooltip<SubWeaponSettings>("ink_consumption", WeaponTooltip.Metrics.UNITS, settings -> settings.inkConsumption, WeaponTooltip.RANKER_DESCENDING)
                };
    }

    @Override
    public Codec<DataRecord> getCodec()
    {
        return DataRecord.CODEC;
    }

    @Override
    public void deserialize(DataRecord data)
    {
        data.mobility.ifPresent((mobility) -> moveSpeed = mobility);
        data.isSecret.ifPresent(this::setSecret);

        setDirectDamage(data.directDamage);
        setIndirectDamage(data.indirectDamage);
        setPropDamage(data.propDamage);

        setExplosionSize(data.explosionSize);
        data.fuseTime.ifPresent(this::setFuseTime);

        setInkConsumption(data.inkConsumption);
        setInkRecoveryCooldown(data.inkRecoveryCooldown);

        data.throwVelocity.ifPresent(this::setThrowVelocity);
        data.throwAngle.ifPresent(this::setThrowAngle);
        data.holdTime.ifPresent(this::setHoldTime);

        data.curlingData.ifPresent(curlingData ->
        {
            setCookTime(curlingData.cookTime);
            setContactDamage(curlingData.contactDamage);
        });
    }

    @Override
    public DataRecord serialize()
    {
        return new DataRecord(directDamage, indirectDamage, propDamage, explosionSize, Optional.of(fuseTime), inkConsumption, inkRecoveryCooldown, Optional.of(throwVelocity), Optional.of(throwAngle), Optional.of(holdTime),
                Optional.of(moveSpeed), Optional.of(new CurlingDataRecord(cookTime, contactDamage)),
                Optional.of(isSecret));
    }

    public SubWeaponSettings setContactDamage(float contactDamage)
    {
        this.contactDamage = contactDamage;
        return this;
    }

    public SubWeaponSettings setCookTime(int cookTime)
    {
        this.cookTime = cookTime;
        return this;
    }

    public SubWeaponSettings setDirectDamage(float directDamage)
    {
        this.directDamage = directDamage;
        return this;
    }

    public SubWeaponSettings setExplosionSize(float explosionSize)
    {
        this.explosionSize = explosionSize;
        return this;
    }

    public SubWeaponSettings setFuseTime(int fuseTime)
    {
        this.fuseTime = fuseTime;
        return this;
    }

    public SubWeaponSettings setHoldTime(int holdTime)
    {
        this.holdTime = holdTime;
        return this;
    }

    public SubWeaponSettings setIndirectDamage(float indirectDamage)
    {
        this.indirectDamage = indirectDamage;
        return this;
    }

    public SubWeaponSettings setInkConsumption(float inkConsumption)
    {
        this.inkConsumption = inkConsumption;
        return this;
    }

    public SubWeaponSettings setInkRecoveryCooldown(int inkRecoveryCooldown)
    {
        this.inkRecoveryCooldown = inkRecoveryCooldown;
        return this;
    }

    public SubWeaponSettings setPropDamage(float propDamage)
    {
        this.propDamage = propDamage;
        return this;
    }

    public SubWeaponSettings setThrowAngle(float throwAngle)
    {
        this.throwAngle = throwAngle;
        return this;
    }

    public SubWeaponSettings setThrowVelocity(float throwVelocity)
    {
        this.throwVelocity = throwVelocity;
        return this;
    }

    public record DataRecord(
            float directDamage,
            float indirectDamage,
            float propDamage,
            float explosionSize,
            Optional<Integer> fuseTime,
            float inkConsumption,
            int inkRecoveryCooldown,
            Optional<Float> throwVelocity,
            Optional<Float> throwAngle,
            Optional<Integer> holdTime,
            Optional<Float> mobility,
            Optional<CurlingDataRecord> curlingData,
            Optional<Boolean> isSecret
    )
    {
        public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.FLOAT.fieldOf("direct_damage")
                                .forGetter(DataRecord::directDamage),
                        Codec.FLOAT.fieldOf("indirect_damage")
                                .forGetter(DataRecord::indirectDamage),
                        Codec.FLOAT.fieldOf("prop_damage")
                                .forGetter(DataRecord::propDamage),
                        Codec.FLOAT.fieldOf("explosion_size")
                                .forGetter(DataRecord::explosionSize),
                        Codec.INT.optionalFieldOf("fuse_time")
                                .forGetter(DataRecord::fuseTime),
                        Codec.FLOAT.fieldOf("ink_consumption")
                                .forGetter(DataRecord::inkConsumption),
                        Codec.INT.fieldOf("ink_recovery_cooldown")
                                .forGetter(DataRecord::inkRecoveryCooldown),
                        Codec.FLOAT.optionalFieldOf("throw_velocity")
                                .forGetter(DataRecord::throwVelocity),
                        Codec.FLOAT.optionalFieldOf("throw_angle")
                                .forGetter(DataRecord::throwAngle),
                        Codec.INT.optionalFieldOf("hold_time")
                                .forGetter(DataRecord::holdTime),
                        Codec.FLOAT.optionalFieldOf("mobility").forGetter(DataRecord::mobility),
                        CurlingDataRecord.CODEC.optionalFieldOf("curling").forGetter(DataRecord::curlingData),
                        Codec.BOOL.optionalFieldOf("isSecret").forGetter(DataRecord::isSecret)
                ).apply(instance, DataRecord::new)
        );
    }

    record CurlingDataRecord(
            int cookTime,
            float contactDamage
    )
    {
        public static final Codec<CurlingDataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.INT.fieldOf("cook_time")
                                .forGetter(CurlingDataRecord::cookTime),
                        Codec.FLOAT.fieldOf("contact_damage")
                                .forGetter(CurlingDataRecord::contactDamage)
                ).apply(instance, CurlingDataRecord::new)
        );
    }
}
