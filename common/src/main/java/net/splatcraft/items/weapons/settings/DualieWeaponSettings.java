package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.items.weapons.DualieItem;
import net.splatcraft.items.weapons.settings.CommonRecords.*;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.WeaponTooltip;
import net.splatcraft.util.action.EntityAction;

import java.util.List;
import java.util.Optional;

public class DualieWeaponSettings extends AbstractWeaponSettings<DualieWeaponSettings, DualieWeaponSettings.DataRecord>
{
	public static final DualieWeaponSettings DEFAULT = new DualieWeaponSettings("default");
	public ProjectileDataRecord standardProjectileData = ProjectileDataRecord.DEFAULT, turretProjectileData = ProjectileDataRecord.DEFAULT;
	public ShotDataRecord standardShotData = ShotDataRecord.DEFAULT, turretShotData = ShotDataRecord.DEFAULT;
	public Optional<OptionalProjectileDataRecord> turretProjectileMods = Optional.empty();
	public Optional<OptionalShotDataRecord> turretShotMods = Optional.empty();
	public RollDataRecord rollData = RollDataRecord.DEFAULT;
	public boolean bypassesMobDamage = false;
	public DualieWeaponSettings(String name)
	{
		super(name);
	}
	@Override
	public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
	{
		ExtraSaveData.DualieExtraData dualieData = list.getFirstExtraData(ExtraSaveData.DualieExtraData.class);
		if (dualieData != null && dualieData.rollBullet)
		{
			return projectile.calculateDamageDecay(turretProjectileData.baseDamage(), turretProjectileData.damageDecayStartTick(), turretProjectileData.damageDecayPerTick(), turretProjectileData.minDamage());
		}
		
		return projectile.calculateDamageDecay(standardProjectileData.baseDamage(), standardProjectileData.damageDecayStartTick(), standardProjectileData.damageDecayPerTick(), standardProjectileData.minDamage());
	}
	@Override
	public List<WeaponTooltip<DualieWeaponSettings>> tooltipsToRegister()
	{
		return List.of(
			new WeaponTooltip<>("range", WeaponTooltip.Metrics.BLOCKS, settings -> calculateAproximateRange(settings.standardProjectileData, settings.standardShotData), WeaponTooltip.RANKER_ASCENDING),
			new WeaponTooltip<>("damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.standardProjectileData.baseDamage(), WeaponTooltip.RANKER_ASCENDING),
			new WeaponTooltip<>("roll_distance", WeaponTooltip.Metrics.BLOCKS, settings -> settings.rollData.rollDistance, WeaponTooltip.RANKER_ASCENDING) //i used desmos to get that 6 B)
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
		return turretShotData == null ? ShotDeviationDataRecord.DEFAULT : EntityAction.hasSpecificEntityAction(entity, DualieItem.DodgeRollAction.class) ? turretShotData.accuracyData() : standardShotData.accuracyData();
	}
	@Override
	public void processData(DataRecord data)
	{
		standardProjectileData = SplatcraftConvertors.convert(data.projectile);
		turretProjectileData = SplatcraftConvertors.convert(OptionalProjectileDataRecord.mergeWithBase(data.turretProjectile, data.projectile));
		turretProjectileMods = data.turretProjectile.map(SplatcraftConvertors::convert);
		standardShotData = SplatcraftConvertors.convert(data.shot);
		turretShotData = SplatcraftConvertors.convert(OptionalShotDataRecord.mergeWithBase(data.turretShot, data.shot));
		turretShotMods = data.turretShot.map(SplatcraftConvertors::convert);
		rollData = SplatcraftConvertors.convert(data.roll);
		
		setMoveSpeed(data.moveSpeed);
		setSecret(data.isSecret);
		setBypassesMobDamage(data.bypassesMobDamage);
	}
	@Override
	public DataRecord getDataToSerialize()
	{
		return new DataRecord(
			standardProjectileData,
			standardShotData,
			turretProjectileMods,
			turretShotMods,
			rollData,
			moveSpeed,
			bypassesMobDamage,
			isSecret);
	}
	@Override
	public float getSpeedForRender(Player player, ItemStack mainHandItem)
	{
		return getShotData(player).speed();
	}
	public DualieWeaponSettings setBypassesMobDamage(boolean bypassesMobDamage)
	{
		this.bypassesMobDamage = bypassesMobDamage;
		return this;
	}
	public ShotDataRecord getShotData(LivingEntity entity)
	{
		return CommonUtils.isRolling(entity) ? turretShotData : standardShotData;
	}
	public ProjectileDataRecord getProjectileData(LivingEntity entity)
	{
		return CommonUtils.isRolling(entity) ? turretProjectileData : standardProjectileData;
	}
	public record DataRecord(
		ProjectileDataRecord projectile,
		ShotDataRecord shot,
		Optional<OptionalProjectileDataRecord> turretProjectile,
		Optional<OptionalShotDataRecord> turretShot,
		RollDataRecord roll,
		float moveSpeed,
		boolean bypassesMobDamage,
		boolean isSecret
	)
	{
		public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				ProjectileDataRecord.CODEC.fieldOf("projectile").forGetter(DataRecord::projectile),
				ShotDataRecord.CODEC.fieldOf("shot").forGetter(DataRecord::shot),
				OptionalProjectileDataRecord.CODEC.optionalFieldOf("turret_projectile").forGetter(DataRecord::turretProjectile),
				OptionalShotDataRecord.CODEC.optionalFieldOf("turret_shot").forGetter(DataRecord::turretShot),
				RollDataRecord.CODEC.fieldOf("dodge_roll").forGetter(DataRecord::roll),
				Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::moveSpeed),
				Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::bypassesMobDamage),
				Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
			).apply(instance, DataRecord::new)
		);
	}
	public record RollDataRecord(
		float count,
		float rollDistance,
		float inkConsumption,
		float inkRecoveryCooldown,
		byte rollStartup,
		byte rollDuration,
		byte rollEndlag,
		int turretDuration,
		int lastRollTurretDuration,
		boolean canMove
	)
	{
		public static final Codec<RollDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.fieldOf("count").forGetter(RollDataRecord::count),
				Codec.FLOAT.fieldOf("distance_covered_by_roll").forGetter(RollDataRecord::rollDistance),
				Codec.FLOAT.fieldOf("ink_consumption").forGetter(RollDataRecord::inkConsumption),
				Codec.FLOAT.fieldOf("ink_recovery_cooldown").forGetter(RollDataRecord::inkRecoveryCooldown),
				Codec.BYTE.optionalFieldOf("roll_startup", (byte) 6).forGetter(RollDataRecord::rollStartup),
				Codec.BYTE.optionalFieldOf("roll_duration", (byte) 12).forGetter(RollDataRecord::rollDuration),
				Codec.BYTE.optionalFieldOf("roll_endlag", (byte) 6).forGetter(RollDataRecord::rollEndlag),
				Codec.INT.fieldOf("turret_duration").forGetter(RollDataRecord::turretDuration),
				Codec.INT.fieldOf("final_roll_turret_duration").forGetter(RollDataRecord::lastRollTurretDuration),
				Codec.BOOL.optionalFieldOf("allows_movement", false).forGetter(RollDataRecord::canMove)
			).apply(instance, RollDataRecord::new)
		);
		public static final RollDataRecord DEFAULT = new RollDataRecord(0, 0, 0, 0, (byte) 2, (byte) 4, (byte) 2, 0, 0, false);
		public float getRollImpulse()
		{
			// x is speed, this should be the value that should be found (i forgot blocks have 0.6 of friction, and also minecraft applies even more, so random number go (its 0.91 bc living entity references it or something))
			// rollDistance = x * roll_duration + x / (1 - 0.6)
			// rollDistance = x * (roll_duration + (1 / 0.4))
			// rollDistance / (roll_duration + 2.5) = x
			return rollDistance / (rollDuration + (2.5f));
		}
	}
}
