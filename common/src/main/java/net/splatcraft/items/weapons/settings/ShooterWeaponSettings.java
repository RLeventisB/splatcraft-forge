package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.util.structs.WeaponTooltip;

import java.util.List;

public class ShooterWeaponSettings extends AbstractWeaponSettings<ShooterWeaponSettings, ShooterWeaponSettings.DataRecord>
{
	public static final ShooterWeaponSettings DEFAULT = new ShooterWeaponSettings("default");
	public CommonRecords.ProjectileDataRecord projectileData;
	public CommonRecords.ShotDataRecord shotData;
	public boolean bypassesMobDamage = false;
	public ShooterWeaponSettings(String name)
	{
		super(name);
	}
	@Override
	public List<WeaponTooltip<ShooterWeaponSettings>> tooltipsToRegister()
	{
		return List.of(
			new WeaponTooltip<>("range", WeaponTooltip.Metrics.BLOCKS, settings -> calculateAproximateRange(settings.projectileData, settings.shotData), WeaponTooltip.RANKER_ASCENDING),
			new WeaponTooltip<>("damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.projectileData.baseDamage(), WeaponTooltip.RANKER_ASCENDING),
			new WeaponTooltip<>("fire_rate", WeaponTooltip.Metrics.BPS, settings -> settings.shotData.repeatTicks(), WeaponTooltip.RANKER_DESCENDING)
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
		return shotData == null ? CommonRecords.ShotDeviationDataRecord.DEFAULT : shotData.accuracyData();
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
	public float getSpeedForRender(Player player, ItemStack mainHandItem)
	{
		return shotData.speed();
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
