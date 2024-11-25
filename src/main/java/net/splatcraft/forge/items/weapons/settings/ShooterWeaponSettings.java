package net.splatcraft.forge.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.forge.data.SplatcraftConvertors;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.util.WeaponTooltip;

public class ShooterWeaponSettings extends AbstractWeaponSettings<ShooterWeaponSettings, ShooterWeaponSettings.DataRecord>
{
    public CommonRecords.ProjectileDataRecord projectileData = CommonRecords.ProjectileDataRecord.DEFAULT;
    public CommonRecords.ShotDataRecord shotData = CommonRecords.ShotDataRecord.DEFAULT;
    public boolean bypassesMobDamage = false;
    public static final ShooterWeaponSettings DEFAULT = new ShooterWeaponSettings("default");

    public ShooterWeaponSettings(String name)
    {
        super(name);
    }

    @Override
    public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
    {
        return projectile.calculateDamageDecay(projectileData.baseDamage(), projectileData.damageDecayStartTick(), projectileData.damageDecayPerTick(), projectileData.minDamage());
    }

    @Override
    public WeaponTooltip<ShooterWeaponSettings>[] tooltipsToRegister()
    {
        return new WeaponTooltip[]
            {
                new WeaponTooltip<ShooterWeaponSettings>("range", WeaponTooltip.Metrics.BLOCKS, settings -> calculateAproximateRange(settings.projectileData), WeaponTooltip.RANKER_ASCENDING),
                new WeaponTooltip<ShooterWeaponSettings>("damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.projectileData.baseDamage(), WeaponTooltip.RANKER_ASCENDING),
                new WeaponTooltip<ShooterWeaponSettings>("fire_rate", WeaponTooltip.Metrics.BPS, settings -> settings.shotData.getFireRate(), WeaponTooltip.RANKER_DESCENDING)
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
        projectileData = SplatcraftConvertors.convert(data.projectile);
        shotData = SplatcraftConvertors.convert(data.shot);

        setMoveSpeed(data.mobility);
        setSecret(data.isSecret);
        setBypassesMobDamage(data.bypassesMobDamage);
    }

    public ShooterWeaponSettings setBypassesMobDamage(boolean bypassesMobDamage)
    {
        this.bypassesMobDamage = bypassesMobDamage;
        return this;
    }

    @Override
    public DataRecord serialize()
    {
        return new DataRecord(projectileData, shotData, moveSpeed, bypassesMobDamage, isSecret);
    }

    @Override
    public float getSpeedForRender(LocalPlayer player, ItemStack mainHandItem)
    {
        return projectileData.speed();
    }

    public record DataRecord(
        CommonRecords.ProjectileDataRecord projectile,
        CommonRecords.ShotDataRecord shot,
        float mobility,
        boolean bypassesMobDamage,
        boolean isSecret
    )
    {
        public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                CommonRecords.ProjectileDataRecord.CODEC.fieldOf("projectile").forGetter(DataRecord::projectile),
                CommonRecords.ShotDataRecord.CODEC.fieldOf("shot").forGetter(DataRecord::shot),
                Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::mobility),
                Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::bypassesMobDamage),
                Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
            ).apply(instance, DataRecord::new)
        );
    }
}
