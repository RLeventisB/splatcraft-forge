package net.splatcraft.forge.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.util.WeaponTooltip;

import java.util.Optional;

public class RollerWeaponSettings extends AbstractWeaponSettings<RollerWeaponSettings, RollerWeaponSettings.DataRecord>
{
    public static final RollerWeaponSettings DEFAULT = new RollerWeaponSettings("default");
    public String name;
    public boolean isBrush;
    public int rollSize;
    public int rollHitboxSize;
    public float rollConsumption;
    public int rollInkRecoveryCooldown;
    public float rollDamage;
    public float rollMobility;
    public float dashMobility;
    public float dashConsumption;
    public int dashTime = 1;
    public int swingProjectileCount;
    public float swingAttackAngle;
    public boolean allowJumpingOnCharge;
    public float swingMobility;
    public float swingConsumption;
    public int swingInkRecoveryCooldown;
    public float swingBaseDamage;
    public float swingLetalAngle;
    public float swingOffAnglePenalty = 0.5f;
    public int swingDamageDecayStartTick;
    public float swingDamageDecayPerTick;
    public float swingMinDamage;
    public float swingProjectileSpeed;
    public int swingTime;
    public int swingStraightTicks;
    public float swingHorizontalDrag = 1.0f;
    public float flingConsumption;
    public int flingInkRecoveryCooldown;
    public float flingBaseDamage;
    public int flingDamageDecayStartTick;
    public float flingDamageDecayPerTick;
    public float flingMinDamage;
    public float flingProjectileSpeed;
    public int flingTime;
    public int flingStraightTicks;
    public float flingHorizontalDrag = 1.0f;
    public boolean bypassesMobDamage = false;

    public RollerWeaponSettings(String name)
    {
        super(name);
    }

    public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
    {
        if (projectile.throwerAirborne)
        {
            return projectile.calculateDamageDecay(flingBaseDamage, flingDamageDecayStartTick, flingDamageDecayPerTick, flingMinDamage);
        }
        return projectile.calculateDamageDecay(swingBaseDamage, swingDamageDecayStartTick, swingDamageDecayPerTick, swingMinDamage);
    }

    @Override
    public WeaponTooltip<RollerWeaponSettings>[] tooltipsToRegister()
    {
        return new WeaponTooltip[]
                {
                        new WeaponTooltip<RollerWeaponSettings>("speed", WeaponTooltip.Metrics.BPT, settings -> settings.swingProjectileSpeed, WeaponTooltip.RANKER_ASCENDING),
                        new WeaponTooltip<RollerWeaponSettings>("mobility", WeaponTooltip.Metrics.MULTIPLIER, settings -> settings.dashMobility, WeaponTooltip.RANKER_ASCENDING),
                        new WeaponTooltip<RollerWeaponSettings>("direct_damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.rollDamage, WeaponTooltip.RANKER_ASCENDING)
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
        setBrush(data.isBrush.orElse(false));

        data.fullDamageToMobs.ifPresent(b -> bypassesMobDamage = b);
        data.isSecret.ifPresent(this::setSecret);

        RollDataRecord roll = data.roll;

        setRollSize(roll.inkSize);
        roll.hitboxSize.ifPresent(this::setRollHitboxSize);
        setRollConsumption(roll.inkConsumption);
        setRollInkRecoveryCooldown(roll.inkRecoveryCooldown);
        setRollDamage(roll.damage);
        setRollMobility(roll.mobility);

        roll.dashMobility.ifPresent(this::setDashMobility);
        roll.dashConsumption.ifPresent(this::setDashConsumption);
        roll.dashTime.ifPresent(this::setDashTime);

        SwingDataRecord swing = data.swing;

        setSwingProjectileCount(swing.projectileCount);
        setSwingAttackAngle(swing.attackAngle);
        setSwingLetalAngle(swing.letalAngle.orElse(16.0f));
        swing.offAnglePenalty.ifPresent(this::setSwingOffAnglePenalty);

        swing.allowJumpingOnCharge.ifPresent(this::setAllowJumpingOnCharge);
        setSwingMobility(swing.mobility);
        setSwingConsumption(swing.inkConsumption);
        setSwingInkRecoveryCooldown(swing.inkRecoveryCooldown);
        setSwingProjectileSpeed(swing.projectileSpeed);
        setSwingTime(swing.startupTime);
        setSwingBaseDamage(swing.baseDamage);
        swing.minDamage.ifPresent(this::setSwingMinDamage);
        setSwingDamageDecayStartTick(swing.damageDecayStartTick.orElse(0));
        setSwingDamageDecayPerTick(swing.damageDecayPerTick.orElse(0f));
        setSwingStraightTicks(swing.straightTicks.orElse(0));
        swing.horizontalDrag.ifPresent(this::setSwingHorizontalDrag);

        if (data.fling.isPresent())
        {
            FlingDataRecord fling = data.fling.get();
            setFlingConsumption(fling.inkConsumption);
            setFlingInkRecoveryCooldown(fling.inkRecoveryCooldown);
            setFlingProjectileSpeed(fling.projectileSpeed);
            setFlingTime(fling.startupTime);
            setFlingBaseDamage(fling.baseDamage);
            fling.minDamage.ifPresent(this::setFlingMinDamage);
            setFlingDamageDecayStartTick(fling.damageDecayStartTick.orElse(0));
            setFlingDamageDecayPerTick(fling.damageDecayPerTick.orElse(0f));
            setFlingStraightTicks(fling.straightTicks.orElse(0));
            fling.horizontalDrag.ifPresent(this::setFlingHorizontalDrag);
        }
    }

    @Override
    public DataRecord serialize()
    {
        return new DataRecord(Optional.of(isBrush), new RollDataRecord(rollSize, Optional.of(rollHitboxSize), rollConsumption, rollInkRecoveryCooldown, rollDamage, rollMobility, Optional.of(dashMobility), Optional.of(dashConsumption), Optional.of(dashTime)),
                new SwingDataRecord(swingProjectileCount, swingAttackAngle, Optional.of(allowJumpingOnCharge), swingMobility, swingConsumption, swingInkRecoveryCooldown, swingProjectileSpeed, swingTime, swingBaseDamage, Optional.of(swingLetalAngle), Optional.of(swingOffAnglePenalty), Optional.of(swingMinDamage), Optional.of(swingDamageDecayStartTick), Optional.of(swingDamageDecayPerTick), Optional.of(swingStraightTicks), Optional.of(swingHorizontalDrag)),
                Optional.of(new FlingDataRecord(flingConsumption, flingInkRecoveryCooldown, flingProjectileSpeed, flingTime, flingBaseDamage, Optional.of(flingMinDamage), Optional.of(flingDamageDecayStartTick), Optional.of(flingDamageDecayPerTick), Optional.of(flingStraightTicks), Optional.of(flingHorizontalDrag))),
                Optional.of(bypassesMobDamage), Optional.of(isSecret));
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

    public RollerWeaponSettings setRollSize(int rollSize)
    {
        this.rollSize = rollSize;
        this.rollHitboxSize = rollSize;
        return this;
    }

    public RollerWeaponSettings setRollHitboxSize(int rollHitboxSize)
    {
        this.rollHitboxSize = rollHitboxSize;
        return this;
    }

    public RollerWeaponSettings setRollConsumption(float rollConsumption)
    {
        this.rollConsumption = rollConsumption;
        this.dashConsumption = rollConsumption;
        return this;
    }

    public RollerWeaponSettings setRollInkRecoveryCooldown(int rollInkRecoveryCooldown)
    {
        this.rollInkRecoveryCooldown = rollInkRecoveryCooldown;
        return this;
    }

    public RollerWeaponSettings setRollDamage(float rollDamage)
    {
        this.rollDamage = rollDamage;
        return this;
    }

    public RollerWeaponSettings setRollMobility(float rollMobility)
    {
        this.rollMobility = rollMobility;
        this.dashMobility = rollMobility;
        return this;
    }

    public RollerWeaponSettings setDashMobility(float dashMobility)
    {
        this.dashMobility = dashMobility;
        return this;
    }

    public RollerWeaponSettings setDashConsumption(float dashConsumption)
    {
        this.dashConsumption = dashConsumption;
        return this;
    }

    public RollerWeaponSettings setDashTime(int dashTime)
    {
        this.dashTime = dashTime;
        return this;
    }

    public RollerWeaponSettings setSwingMobility(float swingMobility)
    {
        this.swingMobility = swingMobility;
        return this;
    }

    public RollerWeaponSettings setSwingConsumption(float swingConsumption)
    {
        this.swingConsumption = swingConsumption;
        this.flingConsumption = swingConsumption;
        return this;
    }

    public RollerWeaponSettings setSwingInkRecoveryCooldown(int swingInkRecoveryCooldown)
    {
        this.swingInkRecoveryCooldown = swingInkRecoveryCooldown;
        this.flingInkRecoveryCooldown = swingInkRecoveryCooldown;
        return this;
    }

    public RollerWeaponSettings setSwingBaseDamage(float swingBaseDamage)
    {
        this.swingBaseDamage = swingBaseDamage;
        this.swingMinDamage = swingBaseDamage;
        this.flingBaseDamage = swingBaseDamage;
        this.flingMinDamage = swingBaseDamage;
        return this;
    }

    public RollerWeaponSettings setSwingDamageDecayStartTick(int swingDamageDecayStartTick)
    {
        this.swingDamageDecayStartTick = swingDamageDecayStartTick;
        this.flingDamageDecayStartTick = swingDamageDecayStartTick;
        return this;
    }

    public RollerWeaponSettings setSwingDamageDecayPerTick(float swingDamageDecayPerTick)
    {
        this.swingDamageDecayPerTick = swingDamageDecayPerTick;
        this.flingDamageDecayPerTick = swingDamageDecayPerTick;
        return this;
    }

    public RollerWeaponSettings setSwingMinDamage(float swingMinDamage)
    {
        this.swingMinDamage = swingMinDamage;
        this.flingMinDamage = swingMinDamage;
        return this;
    }

    public RollerWeaponSettings setSwingProjectileSpeed(float swingProjectileSpeed)
    {
        this.swingProjectileSpeed = swingProjectileSpeed;
        this.flingProjectileSpeed = swingProjectileSpeed * (isBrush ? 1 : 1.3f);
        return this;
    }

    public RollerWeaponSettings setSwingTime(int swingTime)
    {
        this.swingTime = swingTime;
        this.flingTime = swingTime;
        return this;
    }

    public RollerWeaponSettings setSwingAttackAngle(float swingAttackAngle)
    {
        this.swingAttackAngle = swingAttackAngle;
        return this;
    }

    public RollerWeaponSettings setFlingConsumption(float flingConsumption)
    {
        this.flingConsumption = flingConsumption;
        return this;
    }

    public RollerWeaponSettings setFlingInkRecoveryCooldown(int flingInkRecoveryCooldown)
    {
        this.flingInkRecoveryCooldown = flingInkRecoveryCooldown;
        return this;
    }

    public RollerWeaponSettings setFlingBaseDamage(float flingBaseDamage)
    {
        this.flingBaseDamage = flingBaseDamage;
        return this;
    }

    public RollerWeaponSettings setFlingDamageDecayStartTick(int flingDamageDecayStartTick)
    {
        this.flingDamageDecayStartTick = flingDamageDecayStartTick;
        return this;
    }

    public RollerWeaponSettings setFlingDamageDecayPerTick(float flingDamageDecayPerTick)
    {
        this.flingDamageDecayPerTick = flingDamageDecayPerTick;
        return this;
    }

    public RollerWeaponSettings setFlingMinDamage(float flingMinDamage)
    {
        this.flingMinDamage = flingMinDamage;
        return this;
    }

    public RollerWeaponSettings setFlingProjectileSpeed(float flingProjectileSpeed)
    {
        this.flingProjectileSpeed = flingProjectileSpeed;
        return this;
    }

    public RollerWeaponSettings setFlingTime(int flingTime)
    {
        this.flingTime = flingTime;
        return this;
    }

    public RollerWeaponSettings setAllowJumpingOnCharge(boolean allowJumpingOnCharge)
    {
        this.allowJumpingOnCharge = allowJumpingOnCharge;
        return this;
    }

    public RollerWeaponSettings setSwingProjectileCount(int swingProjectileCount)
    {
        this.swingProjectileCount = swingProjectileCount;
        return this;
    }

    public RollerWeaponSettings setSwingLetalAngle(float swingLetalAngle)
    {
        this.swingLetalAngle = swingLetalAngle;
        return this;
    }

    public RollerWeaponSettings setSwingOffAnglePenalty(float swingOffAnglePenalty)
    {
        this.swingOffAnglePenalty = swingOffAnglePenalty;
        return this;
    }

    public RollerWeaponSettings setSwingStraightTicks(int swingStraightTicks)
    {
        this.swingStraightTicks = swingStraightTicks;
        return this;
    }

    public RollerWeaponSettings setFlingStraightTicks(int flingStraightTicks)
    {
        this.flingStraightTicks = flingStraightTicks;
        return this;
    }

    public RollerWeaponSettings setSwingHorizontalDrag(float swingHorizontalDrag)
    {
        this.swingHorizontalDrag = swingHorizontalDrag;
        return this;
    }

    public RollerWeaponSettings setFlingHorizontalDrag(float flingHorizontalDrag)
    {
        this.flingHorizontalDrag = flingHorizontalDrag;
        return this;
    }

    public record DataRecord(
            Optional<Boolean> isBrush,
            RollDataRecord roll,
            SwingDataRecord swing,
            Optional<FlingDataRecord> fling,
            Optional<Boolean> fullDamageToMobs,
            Optional<Boolean> isSecret
    )
    {
        public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.BOOL.optionalFieldOf("is_brush").forGetter(DataRecord::isBrush),
                        RollDataRecord.CODEC.fieldOf("roll").forGetter(DataRecord::roll),
                        SwingDataRecord.CODEC.fieldOf("swing").forGetter(DataRecord::swing),
                        FlingDataRecord.CODEC.optionalFieldOf("fling").forGetter(DataRecord::fling),
                        Codec.BOOL.optionalFieldOf("full_damage_to_mobs").forGetter(DataRecord::fullDamageToMobs),
                        Codec.BOOL.optionalFieldOf("is_secret").forGetter(DataRecord::isSecret)
                ).apply(instance, DataRecord::new)
        );
    }

    record RollDataRecord(
            int inkSize,
            Optional<Integer> hitboxSize,
            float inkConsumption,
            int inkRecoveryCooldown,
            float damage,
            float mobility,
            Optional<Float> dashMobility,
            Optional<Float> dashConsumption,
            Optional<Integer> dashTime
    )
    {
        public static final Codec<RollDataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.INT.fieldOf("ink_size").forGetter(RollDataRecord::inkSize),
                        Codec.INT.optionalFieldOf("hitbox_size").forGetter(RollDataRecord::hitboxSize),
                        Codec.FLOAT.fieldOf("ink_consumption").forGetter(RollDataRecord::inkConsumption),
                        Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(RollDataRecord::inkRecoveryCooldown),
                        Codec.FLOAT.fieldOf("damage").forGetter(RollDataRecord::damage),
                        Codec.FLOAT.fieldOf("mobility").forGetter(RollDataRecord::mobility),
                        Codec.FLOAT.optionalFieldOf("dash_mobility").forGetter(RollDataRecord::dashMobility),
                        Codec.FLOAT.optionalFieldOf("dash_consumption").forGetter(RollDataRecord::dashConsumption),
                        Codec.INT.optionalFieldOf("dash_time").forGetter(RollDataRecord::dashTime)
                ).apply(instance, RollDataRecord::new)
        );
    }

    record SwingDataRecord(
            int projectileCount,
            float attackAngle,
            Optional<Boolean> allowJumpingOnCharge,
            float mobility,
            float inkConsumption,
            int inkRecoveryCooldown,
            float projectileSpeed,
            int startupTime,
            float baseDamage,
            Optional<Float> letalAngle,
            Optional<Float> offAnglePenalty,
            Optional<Float> minDamage,
            Optional<Integer> damageDecayStartTick,
            Optional<Float> damageDecayPerTick,
            Optional<Integer> straightTicks,
            Optional<Float> horizontalDrag
    )
    {
        public static final Codec<SwingDataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.INT.fieldOf("swing_blob_count").forGetter(SwingDataRecord::projectileCount),
                        Codec.FLOAT.fieldOf("swing_attack_angle").forGetter(SwingDataRecord::attackAngle),
                        Codec.BOOL.optionalFieldOf("allow_jumping_on_charge").forGetter(SwingDataRecord::allowJumpingOnCharge),
                        Codec.FLOAT.fieldOf("mobility").forGetter(SwingDataRecord::mobility),
                        Codec.FLOAT.fieldOf("ink_consumption").forGetter(SwingDataRecord::inkConsumption),
                        Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(SwingDataRecord::inkRecoveryCooldown),
                        Codec.FLOAT.fieldOf("projectile_speed").forGetter(SwingDataRecord::projectileSpeed),
                        Codec.INT.fieldOf("startup_time").forGetter(SwingDataRecord::startupTime),
                        Codec.FLOAT.fieldOf("base_damage").forGetter(SwingDataRecord::baseDamage),
                        Codec.FLOAT.optionalFieldOf("letal_angle").forGetter(SwingDataRecord::letalAngle),
                        Codec.FLOAT.optionalFieldOf("offangle_penalty").forGetter(SwingDataRecord::offAnglePenalty),
                        Codec.FLOAT.optionalFieldOf("min_damage").forGetter(SwingDataRecord::minDamage),
                        Codec.INT.optionalFieldOf("damage_decay_start_tick").forGetter(SwingDataRecord::damageDecayStartTick),
                        Codec.FLOAT.optionalFieldOf("damage_decay_per_tick").forGetter(SwingDataRecord::damageDecayPerTick),
                        Codec.INT.optionalFieldOf("straight_ticks").forGetter(SwingDataRecord::straightTicks),
                        Codec.FLOAT.optionalFieldOf("horizontal_drag").forGetter(SwingDataRecord::horizontalDrag)

                ).apply(instance, SwingDataRecord::new)
        );
    }

    record FlingDataRecord(
            float inkConsumption,
            int inkRecoveryCooldown,
            float projectileSpeed,
            int startupTime,
            float baseDamage,
            Optional<Float> minDamage,
            Optional<Integer> damageDecayStartTick,
            Optional<Float> damageDecayPerTick,
            Optional<Integer> straightTicks,
            Optional<Float> horizontalDrag
    )
    {
        public static final Codec<FlingDataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.FLOAT.fieldOf("ink_consumption").forGetter(FlingDataRecord::inkConsumption),
                        Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(FlingDataRecord::inkRecoveryCooldown),
                        Codec.FLOAT.fieldOf("projectile_speed").forGetter(FlingDataRecord::projectileSpeed),
                        Codec.INT.fieldOf("startup_time").forGetter(FlingDataRecord::startupTime),
                        Codec.FLOAT.fieldOf("base_damage").forGetter(FlingDataRecord::baseDamage),
                        Codec.FLOAT.optionalFieldOf("min_damage").forGetter(FlingDataRecord::minDamage),
                        Codec.INT.optionalFieldOf("damage_decay_start_tick").forGetter(FlingDataRecord::damageDecayStartTick),
                        Codec.FLOAT.optionalFieldOf("damage_decay_per_tick").forGetter(FlingDataRecord::damageDecayPerTick),
                        Codec.INT.optionalFieldOf("straight_ticks").forGetter(FlingDataRecord::straightTicks),
                        Codec.FLOAT.optionalFieldOf("horizontal_drag").forGetter(FlingDataRecord::horizontalDrag)
                ).apply(instance, FlingDataRecord::new)
        );
    }
}
