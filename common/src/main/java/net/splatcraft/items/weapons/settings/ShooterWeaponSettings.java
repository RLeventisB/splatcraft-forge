package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.util.WeaponTooltip;

import java.util.List;

public class ShooterWeaponSettings extends AbstractWeaponSettings<ShooterWeaponSettings, ShooterWeaponSettings.DataRecord>
{
	public static final ShooterWeaponSettings DEFAULT = new ShooterWeaponSettings("default");
	public CommonRecords.ProjectileDataRecord projectileData = CommonRecords.ProjectileDataRecord.DEFAULT;
	public CommonRecords.ShotDataRecord shotData = CommonRecords.ShotDataRecord.DEFAULT;
	public boolean bypassesMobDamage = false;
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
	public List<WeaponTooltip<ShooterWeaponSettings>> tooltipsToRegister()
	{
		return List.of(
			new WeaponTooltip<>("range", WeaponTooltip.Metrics.BLOCKS, settings -> calculateAproximateRange(settings.projectileData), WeaponTooltip.RANKER_ASCENDING),
			new WeaponTooltip<>("damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.projectileData.baseDamage(), WeaponTooltip.RANKER_ASCENDING),
			new WeaponTooltip<>("fire_rate", WeaponTooltip.Metrics.BPS, settings -> settings.shotData.getFireRate(), WeaponTooltip.RANKER_DESCENDING)
		);
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
	public void processData(DataRecord data)
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
	public DataRecord getDataToSerialize()
	{
		return new DataRecord(projectileData, shotData, moveSpeed, bypassesMobDamage, isSecret);
	}
	@Override
	public float getSpeedForRender(ClientPlayerEntity player, ItemStack mainHandItem)
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
