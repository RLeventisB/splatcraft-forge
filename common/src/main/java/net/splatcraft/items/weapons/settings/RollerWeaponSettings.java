package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.items.weapons.settings.CommonRecords.ProjectileSizeRecord;
import net.splatcraft.util.structs.NumberRange.FloatRange;
import net.splatcraft.util.structs.RangedValueCollection;
import net.splatcraft.util.structs.WeaponTooltip;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

import static net.splatcraft.items.weapons.settings.CommonRecords.ShotDeviationDataRecord;
import static net.splatcraft.util.structs.NumberRange.IntRange;

public class RollerWeaponSettings extends AbstractWeaponSettings<RollerWeaponSettings, RollerWeaponSettings.DataRecord>
{
	public static final RollerWeaponSettings DEFAULT = new RollerWeaponSettings(DEFAULT_NAME);
	public String name;
	public boolean isBrush;
	public RollDataRecord rollData = RollDataRecord.DEFAULT;
	public boolean bypassesMobDamage = false;
	public SwingDataRecord swingData = SwingDataRecord.DEFAULT;
	public FlingDataRecord flingData = FlingDataRecord.DEFAULT;
	public RollerWeaponSettings(ResourceLocation name)
	{
		super(name);
	}
	@Override
	public List<WeaponTooltip<RollerWeaponSettings>> tooltipsToRegister()
	{
		return List.of(
			new WeaponTooltip<>("speed", WeaponTooltip.Metrics.BPT, settings -> settings.swingData.attackData.speedRange().max(), WeaponTooltip.RANKER_ASCENDING),
			new WeaponTooltip<>("mobility", WeaponTooltip.Metrics.MULTIPLIER, settings -> settings.rollData.dashMobility(), WeaponTooltip.RANKER_ASCENDING),
			new WeaponTooltip<>("direct_damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.rollData.damage, WeaponTooltip.RANKER_ASCENDING)
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
		return ShotDeviationDataRecord.PERFECT_DEFAULT;
	}
	@Override
	public void processData(DataRecord data)
	{
		isBrush = data.isBrush;

		bypassesMobDamage = data.fullDamageToMobs;
		isSecret = data.isSecret;

		rollData = SplatcraftConvertors.convert(data.roll);
		swingData = SplatcraftConvertors.convert(data.swing);
		if (!isBrush)
		{
			if (data.fling.isEmpty())
				throw new AssertionError("Error upon reading roller weapon settings! Fling (or vertical swing) data is not present.");
			flingData = SplatcraftConvertors.convert(data.fling.get());
		}
		else
			flingData = null;
	}
	@Override
	public DataRecord getDataToSerialize()
	{
		return new DataRecord(isBrush, rollData, swingData, Optional.ofNullable(flingData), bypassesMobDamage, isSecret);
	}
	@Override
	public float getSpeedForRender(Player player, ItemStack mainHandItem)
	{
		return 0;
	}
	public RollerWeaponSettings setName(String name)
	{
		this.name = name;
		return this;
	}
	public RollerWeaponSettings setBrush(boolean brush)
	{
		isBrush = brush;
		return this;
	}
	public RollerAttackDataBase getAttackData(boolean isGrounded)
	{
		if (isBrush)
			return swingData;
		return isGrounded ? swingData : flingData;
	}
	public interface RollerAttackDataBase
	{
		RollerProjectileDataRecord projectileData();
		RollerAttackDataRecord attackData();
	}
	public record DataRecord(
		boolean isBrush,
		RollDataRecord roll,
		SwingDataRecord swing,
		Optional<FlingDataRecord> fling,
		boolean fullDamageToMobs,
		boolean isSecret
	)
	{
		public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.BOOL.optionalFieldOf("is_brush", false).forGetter(DataRecord::isBrush),
				RollDataRecord.CODEC.fieldOf("roll").forGetter(DataRecord::roll),
				SwingDataRecord.CODEC.fieldOf("swing").forGetter(DataRecord::swing),
				FlingDataRecord.CODEC.optionalFieldOf("fling").forGetter(DataRecord::fling),
				Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::fullDamageToMobs),
				Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
			).apply(instance, DataRecord::new)
		);
	}
	public record RollerProjectileDataRecord(
		ProjectileSizeRecord size,
		float delaySpeedMult,
		float horizontalDrag,
		float straightShotTicks,
		float gravity,
		float inkCoverageImpact,
		float inkDropCoverage,
		float distanceBetweenInkDrops,
		float damageFalloffStartTick,
		float damageFalloffEndTick,
		float maxDamageFalloffPercent,
		RangedValueCollection damageRanges,
		Optional<RangedValueCollection> weakDamageRanges
	)
	{
		public static final Codec<RollerProjectileDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				ProjectileSizeRecord.CODEC.fieldOf("size").forGetter(RollerProjectileDataRecord::size),
				Codec.FLOAT.optionalFieldOf("delay_speed_mult", 0.7f).forGetter(RollerProjectileDataRecord::delaySpeedMult),
				Codec.FLOAT.optionalFieldOf("horizontal_drag", 0.64F).forGetter(RollerProjectileDataRecord::horizontalDrag),
				Codec.FLOAT.optionalFieldOf("straight_shot_ticks", 2f).forGetter(RollerProjectileDataRecord::straightShotTicks),
				Codec.FLOAT.optionalFieldOf("gravity", 0.4F).forGetter(RollerProjectileDataRecord::gravity),
				Codec.FLOAT.optionalFieldOf("ink_coverage_on_impact").forGetter(r -> Optional.of(r.inkCoverageImpact)),
				Codec.FLOAT.optionalFieldOf("ink_drop_coverage").forGetter(r -> Optional.of(r.inkDropCoverage)),
				Codec.FLOAT.optionalFieldOf("distance_between_drops", 30f).forGetter(RollerProjectileDataRecord::distanceBetweenInkDrops),
				Codec.FLOAT.optionalFieldOf("damage_falloff_start_tick", 25.0f).forGetter(RollerProjectileDataRecord::damageFalloffStartTick),
				Codec.FLOAT.optionalFieldOf("damage_falloff_end_tick", 45.0f).forGetter(RollerProjectileDataRecord::damageFalloffEndTick),
				Codec.FLOAT.optionalFieldOf("max_falloff_damage_percentage", 0.5f).forGetter(RollerProjectileDataRecord::maxDamageFalloffPercent),
				RangedValueCollection.DAMAGE_CODEC.fieldOf("damage_ranges").forGetter(RollerProjectileDataRecord::damageRanges),
				RangedValueCollection.DAMAGE_CODEC.optionalFieldOf("weak_damage_ranges").forGetter(RollerProjectileDataRecord::weakDamageRanges)

			).apply(instance, RollerProjectileDataRecord::create)
		);
		public static final RollerProjectileDataRecord DEFAULT = new RollerProjectileDataRecord(ProjectileSizeRecord.DEFAULT, 1f, 0.64f, 2f, 0.7f, 1f, 0.5f, 30, 25f, 45f, 0.5f, RangedValueCollection.EMPTY, Optional.empty());
		public static RollerProjectileDataRecord create(ProjectileSizeRecord size,
		                                                float delaySpeedMult,
		                                                float horizontalDrag,
		                                                float straightShotTicks,
		                                                float gravity,
		                                                Optional<Float> inkCoverageImpact,
		                                                Optional<Float> inkDropCoverage,
		                                                float distanceBetweenInkDrops,
		                                                float damageFalloffStartTick,
		                                                float damageFalloffEndTick,
		                                                float maxDamageFalloffPercent,
		                                                RangedValueCollection damageRanges,
		                                                Optional<RangedValueCollection> weakDamageRanges)
		{
			return new RollerProjectileDataRecord(size,
				delaySpeedMult,
				horizontalDrag,
				straightShotTicks,
				gravity,
				inkCoverageImpact.orElse(size.hitboxRadius() * 0.85f),
				inkDropCoverage.orElse(size.hitboxRadius() * 0.75f),
				distanceBetweenInkDrops,
				damageFalloffStartTick,
				damageFalloffEndTick,
				maxDamageFalloffPercent,
				damageRanges,
				weakDamageRanges);
		}
		public RangedValueCollection getDamageRanges(boolean weakBullet)
		{
			return weakBullet && weakDamageRanges.isPresent() ? weakDamageRanges.get() : damageRanges;
		}
		public float calculatePercentageFallofPerTick()
		{
			return maxDamageFalloffPercent / (damageFalloffEndTick - damageFalloffStartTick);
		}
	}
	public record RollDataRecord(
		float inkSize,
		float hitboxSize,
		float inkConsumption,
		int inkRecoveryCooldown,
		float damage,
		float mobility,
		float dashMobility,
		float dashConsumption,
		float dashTime
	)
	{
		public static final Codec<RollDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.fieldOf("ink_size").forGetter(RollDataRecord::inkSize),
				Codec.FLOAT.optionalFieldOf("hitbox_size").forGetter(v -> Optional.of(v.hitboxSize())),
				Codec.FLOAT.fieldOf("ink_consumption").forGetter(RollDataRecord::inkConsumption),
				Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(RollDataRecord::inkRecoveryCooldown),
				Codec.FLOAT.fieldOf("damage").forGetter(RollDataRecord::damage),
				Codec.FLOAT.fieldOf("mobility").forGetter(RollDataRecord::mobility),
				Codec.FLOAT.optionalFieldOf("dash_mobility").forGetter(v -> Optional.of(v.dashMobility())),
				Codec.FLOAT.optionalFieldOf("dash_consumption").forGetter(v -> Optional.of(v.dashConsumption())),
				Codec.FLOAT.optionalFieldOf("dash_time", 1f).forGetter(RollDataRecord::dashTime)
			).apply(instance, RollDataRecord::create)
		);
		public static final RollDataRecord DEFAULT = new RollDataRecord(3, 3, 1, 10, 20, 1, 2, 2, 10);
		private static @NotNull RollDataRecord create(Float inkSize, Optional<Float> hitboxSize, Float inkConsumption, Integer inkRecoveryCooldown, Float damage, Float mobility, Optional<Float> dashMobility, Optional<Float> dashConsumption, float dashTime)
		{
			return new RollDataRecord(inkSize, hitboxSize.orElse(inkSize * 0.9f), inkConsumption, inkRecoveryCooldown, damage, mobility, dashMobility.orElse(mobility), dashConsumption.orElse(inkConsumption), dashTime);
		}
	}
	public record SwingDataRecord(
		RollerProjectileDataRecord projectileData,
		RollerAttackDataRecord attackData,
		boolean allowJumpingOnCharge,
		float mobility,
		float attackAngle,
		float letalAngle,
		IntRange blobCount
	) implements RollerAttackDataBase
	{
		public static final Codec<SwingDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				RollerProjectileDataRecord.CODEC.fieldOf("projectile").forGetter(SwingDataRecord::projectileData),
				RollerAttackDataRecord.CODEC.fieldOf("attack_data").forGetter(SwingDataRecord::attackData),
				Codec.BOOL.optionalFieldOf("allow_jumping_on_charge", false).forGetter(SwingDataRecord::allowJumpingOnCharge),
				Codec.FLOAT.fieldOf("mobility").forGetter(SwingDataRecord::mobility),
				Codec.FLOAT.optionalFieldOf("swing_angle", 30f).forGetter(SwingDataRecord::attackAngle),
				Codec.FLOAT.optionalFieldOf("letal_angle", 16f).forGetter(SwingDataRecord::letalAngle),
				IntRange.CODEC.optionalFieldOf("brush_blob_count_range", new IntRange(2, 3)).forGetter(SwingDataRecord::blobCount)
			).apply(instance, SwingDataRecord::new)
		);
		public static final SwingDataRecord DEFAULT = new SwingDataRecord(RollerProjectileDataRecord.DEFAULT, RollerAttackDataRecord.DEFAULT, false, 0.5f, 18f, 16f, new IntRange(2, 3));
		public int calculateBrushProjectileCount()
		{
			return Math.round((attackAngle() * Mth.PI / 180f) * (attackData.speedRange.average()) * projectileData.straightShotTicks / (projectileData.size().hitboxRadius()));
		}
	}
	public record FlingDataRecord(
		RollerProjectileDataRecord projectileData,
		RollerAttackDataRecord attackData,
		float startPitchCompensation,
		float endPitchCompensation,
		Optional<Integer> forcedProjectileCount
	) implements RollerAttackDataBase
	{
		public static final Codec<FlingDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				RollerProjectileDataRecord.CODEC.fieldOf("projectile").forGetter(FlingDataRecord::projectileData),
				RollerAttackDataRecord.CODEC.fieldOf("attack_data").forGetter(FlingDataRecord::attackData),
				Codec.FLOAT.optionalFieldOf("start_pitch_compensation", -7.5f).forGetter(FlingDataRecord::startPitchCompensation),
				Codec.FLOAT.optionalFieldOf("end_pitch_compensation", -4f).forGetter(FlingDataRecord::endPitchCompensation),
				Codec.INT.optionalFieldOf("forced_projectile_count").forGetter(FlingDataRecord::forcedProjectileCount)
			).apply(instance, FlingDataRecord::new)
		);
		public static final FlingDataRecord DEFAULT = new FlingDataRecord(RollerProjectileDataRecord.DEFAULT, RollerAttackDataRecord.DEFAULT, -7.5f, 0f, Optional.empty());
		public int calculateProjectileCount()
		{
			return forcedProjectileCount.orElse(
				Math.round(
					(calculateAproximateRange(projectileData.straightShotTicks,
						projectileData.horizontalDrag,
						attackData.speedRange.max(),
						projectileData.delaySpeedMult,
						600)
						-
						calculateAproximateRange(
							projectileData.straightShotTicks,
							projectileData.horizontalDrag,
							attackData.speedRange.min(),
							projectileData.delaySpeedMult,
							600)
					) / projectileData.size.hitboxRadius())
			);
		}
	}
	public record RollerAttackDataRecord(
		float inkConsumption,
		float inkRecoveryCooldown,
		float startupTicks,
		float endlagTicks,
		float rollDelayTicks,
		float miscEndlagTicks,
		FloatRange speedRange
	)
	{
		public static final Codec<RollerAttackDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.fieldOf("ink_consumption").forGetter(RollerAttackDataRecord::inkConsumption),
				Codec.FLOAT.fieldOf("ink_recovery_cooldown").forGetter(RollerAttackDataRecord::inkRecoveryCooldown),
				Codec.FLOAT.fieldOf("startup_ticks").forGetter(RollerAttackDataRecord::startupTicks),
				Codec.FLOAT.optionalFieldOf("endlag_ticks", 10f).forGetter(RollerAttackDataRecord::endlagTicks),
				Codec.FLOAT.optionalFieldOf("attack_to_roll_ticks", 5f).forGetter(RollerAttackDataRecord::rollDelayTicks),
				Codec.FLOAT.optionalFieldOf("other_actions_endlag_ticks", 10f).forGetter(RollerAttackDataRecord::miscEndlagTicks),
				FloatRange.CODEC.fieldOf("speed_range").forGetter(RollerAttackDataRecord::speedRange)
			).apply(instance, RollerAttackDataRecord::new)
		);
		public static final RollerAttackDataRecord DEFAULT = new RollerAttackDataRecord(10f, 20, 10, 10f, 10, 10, FloatRange.ZERO);
		public float getTotalAttackTime()
		{
			return startupTicks + endlagTicks;
		}
		public float getRollDelay()
		{
			return startupTicks + rollDelayTicks;
		}
	}
}
