package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.util.DamageRangesRecord;
import net.splatcraft.util.WeaponTooltip;

import java.util.List;
import java.util.Optional;

import static net.splatcraft.items.weapons.settings.CommonRecords.*;

public class BlasterWeaponSettings extends AbstractWeaponSettings<BlasterWeaponSettings, BlasterWeaponSettings.DataRecord>
{
	public static final BlasterWeaponSettings DEFAULT = new BlasterWeaponSettings("default");
	public ProjectileDataRecord projectileData = ProjectileDataRecord.DEFAULT;
	public ShotDataRecord shotData = ShotDataRecord.DEFAULT;
	public DetonationRecord blasterData = DetonationRecord.DEFAULT;
	public boolean bypassesMobDamage = false;
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
	public List<WeaponTooltip<BlasterWeaponSettings>> tooltipsToRegister()
	{
		return List.of(
			new WeaponTooltip<>("range", WeaponTooltip.Metrics.BLOCKS, settings -> calculateAproximateRange(settings.projectileData), WeaponTooltip.RANKER_ASCENDING),
			new WeaponTooltip<>("direct_damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.projectileData.baseDamage(), WeaponTooltip.RANKER_ASCENDING),
			new WeaponTooltip<>("fire_rate", WeaponTooltip.Metrics.BPS, settings -> settings.shotData.getFireRate(), WeaponTooltip.RANKER_DESCENDING)
		);
	}
	@Override
	public Codec<DataRecord> getCodec()
	{
		return DataRecord.CODEC;
	}
	@Override
	public ShotDeviationDataRecord getShotDeviationData(ItemStack stack, LivingEntity entity)
	{
		return shotData.accuracyData();
	}
	@Override
	public void processData(DataRecord data)
	{
		projectileData = SplatcraftConvertors.convert(data.projectile);
		shotData = SplatcraftConvertors.convert(data.shot);
		blasterData = SplatcraftConvertors.convert(data.blast);
		
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
	public DataRecord getDataToSerialize()
	{
		return new DataRecord(projectileData, shotData, blasterData, moveSpeed, bypassesMobDamage, isSecret);
	}
	@Override
	public float getSpeedForRender(ClientPlayerEntity player, ItemStack mainHandItem)
	{
		return projectileData.speed();
	}
	public record DataRecord(
		ProjectileDataRecord projectile,
		ShotDataRecord shot,
		DetonationRecord blast,
		float mobility,
		boolean bypassesMobDamage,
		boolean isSecret
	)
	{
		public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				ProjectileDataRecord.CODEC.fieldOf("projectile").forGetter(DataRecord::projectile),
				ShotDataRecord.CODEC.fieldOf("shot").forGetter(DataRecord::shot),
				DetonationRecord.CODEC.fieldOf("blaster_data").forGetter(DataRecord::blast),
				Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::mobility),
				Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::bypassesMobDamage),
				Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
			).apply(instance, DataRecord::new)
		);
	}
	public record DetonationRecord(
		DamageRangesRecord damageRadiuses,
		DamageRangesRecord sparkDamageRadiuses,
		float explosionPaint,
		boolean newAttackId
	)
	{
		public static final Codec<DetonationRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				DamageRangesRecord.CODEC.fieldOf("damage_data").forGetter(DetonationRecord::damageRadiuses),
				DamageRangesRecord.CODEC.optionalFieldOf("spark_damage_data").forGetter(t -> Optional.ofNullable(t.sparkDamageRadiuses())),
				Codec.FLOAT.optionalFieldOf("explosion_paint_size").forGetter((DetonationRecord v) -> Optional.of(v.explosionPaint)),
				Codec.BOOL.optionalFieldOf("new_attack_id", false).forGetter(DetonationRecord::newAttackId)
			).apply(instance, DetonationRecord::create)
		);
		public static final DetonationRecord DEFAULT = new DetonationRecord(DamageRangesRecord.DEFAULT, DamageRangesRecord.DEFAULT, 0, false);
		public static DetonationRecord create(DamageRangesRecord damageRadiuses,
		                                      Optional<DamageRangesRecord> sparkDamageRadiuses,
		                                      Optional<Float> explosionPaint,
		                                      boolean newAttackId)
		{
			return new DetonationRecord(damageRadiuses, sparkDamageRadiuses.orElse(damageRadiuses.cloneWithMultiplier(0.5f, 0.5f)), explosionPaint.orElse(damageRadiuses.getMaxDistance()), newAttackId);
		}
	}
}
