package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.entities.ExtraSaveData.ChargeExtraData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.entities.InkProjectileEntity.ExtraDataList;
import net.splatcraft.items.weapons.settings.CommonRecords.ProjectileDataRecord;
import net.splatcraft.items.weapons.settings.CommonRecords.ShotDeviationDataRecord;
import net.splatcraft.items.weapons.settings.SplatlingWeaponSettings.DataRecord;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.util.WeaponTooltip;
import net.splatcraft.util.WeaponTooltip.Metrics;

import java.util.List;
import java.util.Optional;

public class SplatlingWeaponSettings extends AbstractWeaponSettings<SplatlingWeaponSettings, DataRecord>
{
	public static final SplatlingWeaponSettings DEFAULT = new SplatlingWeaponSettings("default");
	public SplatlingShotDataRecord firstChargeLevelShot = SplatlingShotDataRecord.DEFAULT;
	public ProjectileDataRecord firstChargeLevelProjectile = ProjectileDataRecord.DEFAULT;
	public SplatlingShotDataRecord secondChargeLevelShot = SplatlingShotDataRecord.DEFAULT;
	public ProjectileDataRecord secondChargeLevelProjectile = ProjectileDataRecord.DEFAULT;
	public ChargeDataRecord chargeData = ChargeDataRecord.DEFAULT;
	public boolean bypassesMobDamage = false;
	public float inkConsumption;
	public int inkRecoveryCooldown;
	public SplatlingWeaponSettings(String name)
	{
		super(name);
	}
	private static float calculateSplatlingAproxRange(ProjectileDataRecord projSettings, float speed)
	{
		return calculateAproximateRange(projSettings.straightShotTicks(), projSettings.horizontalDrag(), speed, projSettings.delaySpeedMult(), projSettings.lifeTicks());
	}
	@Override
	public float calculateDamage(InkProjectileEntity projectile, ExtraDataList list)
	{
		ChargeExtraData data = list.getFirstExtraData(ChargeExtraData.class);
		if (data == null) // oh no
			return 0;
		if (data.charge >= 1)
			return projectile.calculateDamageDecay(secondChargeLevelProjectile.baseDamage(), secondChargeLevelProjectile.damageDecayStartTick(), secondChargeLevelProjectile.damageDecayPerTick(), secondChargeLevelProjectile.minDamage());
		return projectile.calculateDamageDecay(firstChargeLevelProjectile.baseDamage(), firstChargeLevelProjectile.damageDecayStartTick(), firstChargeLevelProjectile.damageDecayPerTick(), firstChargeLevelProjectile.minDamage());
	}
	@Override
	public List<WeaponTooltip<SplatlingWeaponSettings>> tooltipsToRegister()
	{
		return List.of(
			new WeaponTooltip<>("range", Metrics.BLOCKS, settings ->
				Math.max(calculateSplatlingAproxRange(settings.firstChargeLevelProjectile, settings.firstChargeLevelShot.projectileSpeed),
					calculateSplatlingAproxRange(settings.secondChargeLevelProjectile, settings.secondChargeLevelShot.projectileSpeed)), WeaponTooltip.RANKER_ASCENDING),
			new WeaponTooltip<>("charge_speed", Metrics.SECONDS, settings -> (settings.chargeData.firstChargeTime + settings.chargeData.secondChargeTime) / 20f, WeaponTooltip.RANKER_DESCENDING),
			new WeaponTooltip<>("mobility", Metrics.MULTIPLIER, settings -> settings.moveSpeed, WeaponTooltip.RANKER_ASCENDING)
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
		return stack.getOrDefault(SplatcraftComponents.CHARGE_DATA, SplatcraftComponents.ChargeData.DEFAULT).charge() > 1 ? secondChargeLevelShot.accuracyData : firstChargeLevelShot.accuracyData;
	}
	@Override
	public void processData(DataRecord data)
	{
		firstChargeLevelProjectile = data.projectile;
		firstChargeLevelShot = data.shot;
		secondChargeLevelProjectile = data.secondChargeLevelProjectile.orElse(data.projectile);
		secondChargeLevelShot = data.secondChargeLevelShot.orElse(data.shot);

		setMoveSpeed(data.moveSpeed);
		setSecret(data.isSecret);
		setBypassesMobDamage(data.bypassesMobDamage);

		setInkConsumption(data.inkConsumption);
		setInkRecoveryCooldown(data.inkRecoveryCooldown);
	}
	@Override
	public DataRecord getDataToSerialize()
	{
		return new DataRecord(firstChargeLevelProjectile, firstChargeLevelShot, Optional.ofNullable(secondChargeLevelProjectile), Optional.ofNullable(secondChargeLevelShot), chargeData, inkConsumption, inkRecoveryCooldown, moveSpeed, (bypassesMobDamage), (isSecret));
	}
	@Override
	public float getSpeedForRender(Player player, ItemStack mainHandItem)
	{
		return firstChargeLevelShot.projectileSpeed();
	}
	public SplatlingWeaponSettings setBypassesMobDamage(boolean bypassesMobDamage)
	{
		this.bypassesMobDamage = bypassesMobDamage;
		return this;
	}
	public SplatlingWeaponSettings setInkConsumption(float inkConsumption)
	{
		this.inkConsumption = inkConsumption;
		return this;
	}
	public SplatlingWeaponSettings setInkRecoveryCooldown(int inkRecoveryCooldown)
	{
		this.inkRecoveryCooldown = inkRecoveryCooldown;
		return this;
	}
	public float getDualieOffhandFiringOffset(boolean secondChargeLevel) // ok this would be funny to implement
	{
		return firstChargeLevelShot.repeatTicks / 2;
	}
	public record DataRecord(
		ProjectileDataRecord projectile,
		SplatlingShotDataRecord shot,
		Optional<ProjectileDataRecord> secondChargeLevelProjectile,
		Optional<SplatlingShotDataRecord> secondChargeLevelShot,
		ChargeDataRecord charge,
		float inkConsumption,
		int inkRecoveryCooldown,
		float moveSpeed,
		boolean bypassesMobDamage,
		boolean isSecret
	)
	{
		public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				ProjectileDataRecord.CODEC.fieldOf("projectile").forGetter(DataRecord::projectile),
				SplatlingShotDataRecord.CODEC.fieldOf("shot").forGetter(DataRecord::shot),
				ProjectileDataRecord.CODEC.optionalFieldOf("second_charge_projectile").forGetter(v -> v.secondChargeLevelProjectile),
				SplatlingShotDataRecord.CODEC.optionalFieldOf("second_charge_shot").forGetter(v -> v.secondChargeLevelShot),
				ChargeDataRecord.CODEC.fieldOf("charge").forGetter(DataRecord::charge),
				Codec.FLOAT.fieldOf("max_ink_consumption").forGetter(DataRecord::inkConsumption),
				Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(DataRecord::inkRecoveryCooldown),
				Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::moveSpeed),
				Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::bypassesMobDamage),
				Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
			).apply(instance, DataRecord::create)
		);
		public static DataRecord create(ProjectileDataRecord projectile, SplatlingShotDataRecord shot, Optional<ProjectileDataRecord> secondChargeLevelProjectile, Optional<SplatlingShotDataRecord> secondChargeLevelShot, ChargeDataRecord charge, float inkConsumption, int inkRecoveryCooldown, float moveSpeed, boolean bypassesMobDamage, boolean isSecret)
		{
			ChargeDataRecord parsedCharge = new ChargeDataRecord(charge.firstChargeTime,
				charge.secondChargeTime,
				charge.emptyTankFirstChargeTime,
				charge.emptyTankSecondChargeTime,
				charge.firingDuration,
				charge.fullFiringDuration,
				Optional.of(charge.moveSpeed.orElse(moveSpeed)),
				charge.chargeStorageTime,
				charge.canRechargeWhileFiring);
			return new DataRecord(projectile, shot, secondChargeLevelProjectile, secondChargeLevelShot, parsedCharge, inkConsumption, inkRecoveryCooldown, moveSpeed, bypassesMobDamage, isSecret);
		}
	}
	public record ChargeDataRecord(
		int firstChargeTime,
		int secondChargeTime,
		int emptyTankFirstChargeTime,
		int emptyTankSecondChargeTime,
		int firingDuration,
		int fullFiringDuration,
		Optional<Float> moveSpeed,
		int chargeStorageTime,
		boolean canRechargeWhileFiring
	)
	{
		public static final Codec<ChargeDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.INT.fieldOf("first_charge_time_ticks").forGetter(ChargeDataRecord::firstChargeTime),
				Codec.INT.fieldOf("second_charge_time_ticks").forGetter(ChargeDataRecord::secondChargeTime),
				Codec.INT.optionalFieldOf("empty_tank_first_charge_time_ticks").forGetter(v -> Optional.of(v.emptyTankFirstChargeTime)),
				Codec.INT.optionalFieldOf("empty_tank_second_charge_time_ticks").forGetter(v -> Optional.of(v.emptyTankSecondChargeTime)),
				Codec.INT.fieldOf("first_charge_firing_duration").forGetter(ChargeDataRecord::firingDuration),
				Codec.INT.optionalFieldOf("fully_charged_firing_duration").forGetter((t) -> Optional.of(t.fullFiringDuration())),
				Codec.FLOAT.optionalFieldOf("mobility_while_charging").forGetter(ChargeDataRecord::moveSpeed),
				Codec.INT.optionalFieldOf("charge_storage_ticks", 0).forGetter(ChargeDataRecord::chargeStorageTime),
				Codec.BOOL.optionalFieldOf("can_recharge_while_firing", false).forGetter(ChargeDataRecord::canRechargeWhileFiring)
			).apply(instance, ChargeDataRecord::create)
		);
		public static final ChargeDataRecord DEFAULT = new ChargeDataRecord(0, 0, 0, 0, 10, 20, Optional.empty(), 0, false);
		public static ChargeDataRecord create(int firstChargeTime,
		                                      int secondChargeTime,
		                                      Optional<Integer> emptyTankFirstChargeTime,
		                                      Optional<Integer> emptyTankSecondChargeTime,
		                                      int firstFiringDuration,
		                                      Optional<Integer> fullFiringDuration,
		                                      Optional<Float> moveSpeed,
		                                      int chargeStorageTime,
		                                      boolean canRechargeWhileFiring)
		{
			return new ChargeDataRecord(firstChargeTime,
				secondChargeTime,
				emptyTankFirstChargeTime.orElse(firstChargeTime * 6),
				emptyTankSecondChargeTime.orElse(secondChargeTime * 6),
				firstFiringDuration,
				fullFiringDuration.orElse(firstFiringDuration * 2),
				moveSpeed,
				chargeStorageTime,
				canRechargeWhileFiring);
		}
	}
	public record SplatlingShotDataRecord(
		float startupTicks,
		float repeatTicks,
		float endlagTicks,
		int projectileCount,
		float projectileSpeed,
		ShotDeviationDataRecord accuracyData,
		float pitchCompensation
	)
	{
		public static final Codec<SplatlingShotDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.optionalFieldOf("startup_ticks", 0f).forGetter(SplatlingShotDataRecord::startupTicks),
				Codec.FLOAT.fieldOf("repeat_ticks").forGetter(SplatlingShotDataRecord::repeatTicks),
				Codec.FLOAT.fieldOf("endlag_ticks").forGetter(SplatlingShotDataRecord::repeatTicks),
				Codec.INT.optionalFieldOf("shot_count", 1).forGetter(SplatlingShotDataRecord::projectileCount),
				Codec.FLOAT.optionalFieldOf("projectile_speed", 0f).forGetter(SplatlingShotDataRecord::projectileSpeed),
				ShotDeviationDataRecord.CODEC.optionalFieldOf("accuracy_data", ShotDeviationDataRecord.PERFECT_DEFAULT).forGetter(SplatlingShotDataRecord::accuracyData),
				Codec.FLOAT.optionalFieldOf("pitch_compensation", 0f).forGetter(SplatlingShotDataRecord::pitchCompensation)
			).apply(instance, SplatlingShotDataRecord::new)
		);
		public static final SplatlingShotDataRecord DEFAULT = new SplatlingShotDataRecord(0, 1, 0, 1, 1, ShotDeviationDataRecord.DEFAULT, 0);
	}
}
