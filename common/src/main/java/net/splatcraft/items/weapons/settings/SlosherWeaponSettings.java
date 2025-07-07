package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.entities.ExtraSaveData.SloshExtraData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.entities.InkProjectileEntity.ExtraDataList;
import net.splatcraft.items.weapons.settings.BlasterWeaponSettings.DetonationRecord;
import net.splatcraft.items.weapons.settings.CommonRecords.OptionalProjectileDataRecord;
import net.splatcraft.items.weapons.settings.CommonRecords.ProjectileDataRecord;
import net.splatcraft.items.weapons.settings.CommonRecords.ShotDeviationDataRecord;
import net.splatcraft.items.weapons.settings.SlosherWeaponSettings.DataRecord;
import net.splatcraft.util.structs.WeaponTooltip;
import net.splatcraft.util.structs.WeaponTooltip.Metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SlosherWeaponSettings extends AbstractWeaponSettings<SlosherWeaponSettings, DataRecord>
{
	public static final SlosherWeaponSettings DEFAULT = new SlosherWeaponSettings("default");
	public boolean bypassesMobDamage;
	public SlosherShotDataRecord shotData = SlosherShotDataRecord.DEFAULT;
	public ProjectileDataRecord baseProjectile = SingularSloshShotData.SLOSHER_PROJECTILE_DEFAULT;
	public float lowestStartup;
	public ProjectileDataRecord[] mergedProjectileData = new ProjectileDataRecord[0];
	public SlosherWeaponSettings(String name)
	{
		super(name);
	}
	private static float getDamage(ProjectileDataRecord projectileData, float relativeY)
	{
		float minDamageHeight = projectileData.damageDecayPerTick();
		float damageDecayStartHeight = projectileData.damageDecayStartTick();
		
		float damage = projectileData.baseDamage();
		if (relativeY < -minDamageHeight)
			damage = projectileData.minDamage();
		else if (relativeY < -damageDecayStartHeight)
			damage = Mth.lerp(Mth.inverseLerp(-relativeY, damageDecayStartHeight, minDamageHeight), projectileData.baseDamage(), projectileData.minDamage());
		return damage;
	}
	@Override
	public float calculateDamage(InkProjectileEntity projectile, ExtraDataList list)
	{
		SloshExtraData sloshData = list.getFirstExtraData(SloshExtraData.class);
		if (sloshData != null)
		{
			ProjectileDataRecord projectileData = getProjectileDataAtIndex(sloshData.sloshDataIndex);
			if (projectileData.minDamage() == projectileData.baseDamage())
				return projectileData.baseDamage();
			
			double relativeY = projectile.getY() - sloshData.spawnHeight;
			return getDamage(projectileData, (float) relativeY);
		}
		return baseProjectile.baseDamage();
	}
	@Override
	public List<WeaponTooltip<SlosherWeaponSettings>> tooltipsToRegister()
	{
		return List.of(
			new WeaponTooltip<>("speed", Metrics.BPT, settings -> settings.shotData.baseSpeed, WeaponTooltip.RANKER_ASCENDING),
			new WeaponTooltip<>("damage", Metrics.HEALTH, settings -> settings.baseProjectile.baseDamage(), WeaponTooltip.RANKER_ASCENDING),
			new WeaponTooltip<>("handling", Metrics.TICKS, settings -> settings.shotData.endlagTicks, WeaponTooltip.RANKER_DESCENDING)
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
		shotData = SplatcraftConvertors.convertDamage(data.shot);
		baseProjectile = SplatcraftConvertors.convertDamage(data.baseProjectile);
		mergedProjectileData = new ProjectileDataRecord[shotData.sloshes.size()];
		for (int i = 0; i < shotData.sloshes.size(); i++)
		{
			mergedProjectileData[i] = OptionalProjectileDataRecord.mergeWithBase(shotData.sloshes.get(i).projectileModifications, baseProjectile);
		}
		
		for (var slosh : shotData.sloshes)
		{
			if (slosh.startupTicks < lowestStartup)
				lowestStartup = slosh.startupTicks;
		}
		setMoveSpeed(data.mobility);
		setSecret(data.isSecret);
		setBypassesMobDamage(data.bypassesMobDamage);
	}
	@Override
	public DataRecord getDataToSerialize()
	{
		return new DataRecord(shotData, baseProjectile, moveSpeed, bypassesMobDamage, isSecret);
	}
	@Override
	public float getSpeedForRender(Player player, ItemStack mainHandItem)
	{
		return Float.POSITIVE_INFINITY;
	}
	public SlosherWeaponSettings setBypassesMobDamage(boolean bypassesMobDamage)
	{
		this.bypassesMobDamage = bypassesMobDamage;
		return this;
	}
	public ProjectileDataRecord getProjectileDataAtIndex(int sloshDataIndex)
	{
		return mergedProjectileData[sloshDataIndex];
	}
	public record DataRecord(
		SlosherShotDataRecord shot,
		ProjectileDataRecord baseProjectile,
		float mobility,
		boolean bypassesMobDamage,
		boolean isSecret
	)
	{
		public static final Codec<ProjectileDataRecord> BASE_SLOSHER_PROJECTILE_CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.fieldOf("size").forGetter(ProjectileDataRecord::size),
				Codec.FLOAT.optionalFieldOf("visual_size").forGetter(r -> Optional.of(r.visualSize())),
				Codec.FLOAT.optionalFieldOf("delay_speed_mult", 1f).forGetter(ProjectileDataRecord::delaySpeedMult),
				Codec.FLOAT.optionalFieldOf("horizontal_drag", 0.88f).forGetter(ProjectileDataRecord::horizontalDrag),
				Codec.FLOAT.optionalFieldOf("straight_shot_ticks", 0F).forGetter(ProjectileDataRecord::straightShotTicks),
				Codec.FLOAT.optionalFieldOf("gravity", 0.5f).forGetter(ProjectileDataRecord::gravity),
				Codec.FLOAT.optionalFieldOf("ink_coverage_on_impact").forGetter(r -> Optional.of(r.inkCoverageImpact())),
				Codec.FLOAT.optionalFieldOf("ink_drop_coverage", 0f).forGetter(ProjectileDataRecord::inkDropCoverage),
				Codec.FLOAT.optionalFieldOf("distance_between_drops", 4f).forGetter(ProjectileDataRecord::distanceBetweenInkDrops),
				Codec.FLOAT.fieldOf("direct_damage").forGetter(ProjectileDataRecord::baseDamage),
				Codec.FLOAT.optionalFieldOf("minimum_damage").forGetter(r -> Optional.of(r.minDamage())),
				Codec.FLOAT.optionalFieldOf("fall_damage_start", 1f).forGetter(ProjectileDataRecord::damageDecayStartTick),
				Codec.FLOAT.optionalFieldOf("fall_damage_end", 5f).forGetter(ProjectileDataRecord::damageDecayPerTick)
			).apply(instance, DataRecord::createSlosherProjectile)
		);
		public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				SlosherShotDataRecord.CODEC.fieldOf("shot").forGetter(DataRecord::shot),
				BASE_SLOSHER_PROJECTILE_CODEC.fieldOf("base_projectile").forGetter(DataRecord::baseProjectile),
				Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::mobility),
				Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::bypassesMobDamage),
				Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
			).apply(instance, DataRecord::create)
		);
		public static ProjectileDataRecord createSlosherProjectile(float size, Optional<Float> visualSize, Float delaySpeedMult, float horizontalDrag, float straightShotTicks, float gravity, Optional<Float> inkCoverageImpact, float inkDropCoverage, float distanceBetweenInkDrops, float directDamage, Optional<Float> minDamage, float heightDecayStart, float heightDecayEnd)
		{
			return ProjectileDataRecord.create(size, visualSize, 600, delaySpeedMult, horizontalDrag, straightShotTicks, gravity, inkCoverageImpact, Optional.of(inkDropCoverage), distanceBetweenInkDrops, directDamage, minDamage, heightDecayStart, heightDecayEnd);
		}
		private static DataRecord create(SlosherShotDataRecord shot, ProjectileDataRecord baseProjectile, float mobility, boolean bypassesMobDamage, boolean isSecret)
		{
			return new DataRecord(shot, baseProjectile, mobility, bypassesMobDamage, isSecret);
		}
	}
	public record SlosherShotDataRecord(
		float endlagTicks,
		int miscEndlagTicks,
		List<SingularSloshShotData> sloshes,
		float baseSpeed,
		float pitchCompensation,
		float inkConsumption,
		float inkRecoveryCooldown,
		boolean allowFlicking
	)
	{
		public static final Codec<SlosherShotDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.optionalFieldOf("endlag_ticks", 1f).forGetter(SlosherShotDataRecord::endlagTicks),
				Codec.INT.optionalFieldOf("other_actions_endlag_ticks", 1).forGetter(SlosherShotDataRecord::miscEndlagTicks),
				SingularSloshShotData.CODEC.listOf().fieldOf("sloshes_data").forGetter(SlosherShotDataRecord::sloshes),
				Codec.FLOAT.optionalFieldOf("base_speed", 1f).forGetter(SlosherShotDataRecord::baseSpeed),
				Codec.FLOAT.optionalFieldOf("pitch_compensation", 0f).forGetter(SlosherShotDataRecord::pitchCompensation),
				Codec.FLOAT.fieldOf("ink_consumption").forGetter(SlosherShotDataRecord::inkConsumption),
				Codec.FLOAT.fieldOf("ink_recovery_cooldown").forGetter(SlosherShotDataRecord::inkRecoveryCooldown),
				Codec.BOOL.optionalFieldOf("allow_flicking", true).forGetter(SlosherShotDataRecord::allowFlicking)
			).apply(instance, SlosherShotDataRecord::new)
		);
		public static final SlosherShotDataRecord DEFAULT = new SlosherShotDataRecord(1, 1, new ArrayList<>(), 1f, 0, 0, 0, true);
	}
	public record SingularSloshShotData(
		float startupTicks,
		int count,
		float delayBetweenProjectiles,
		Optional<Float> modifiedSpeed,
		float speedSubstract,
		float offsetAngle,
		Optional<OptionalProjectileDataRecord> projectileModifications,
		Optional<DetonationRecord> detonationData
	)
	{
		// this only renames some variables lol
		public static final Codec<OptionalProjectileDataRecord> PROJECTILE_MODIFICATIONS_CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.optionalFieldOf("size").forGetter(OptionalProjectileDataRecord::size),
				Codec.FLOAT.optionalFieldOf("visual_size").forGetter(OptionalProjectileDataRecord::visualSize),
				Codec.FLOAT.optionalFieldOf("lifespan").forGetter(OptionalProjectileDataRecord::lifeTicks),
				Codec.FLOAT.optionalFieldOf("delay_speed_mult").forGetter(OptionalProjectileDataRecord::delaySpeedMult),
				Codec.FLOAT.optionalFieldOf("horizontal_drag").forGetter(OptionalProjectileDataRecord::horizontalDrag),
				Codec.FLOAT.optionalFieldOf("straight_shot_ticks").forGetter(OptionalProjectileDataRecord::straightShotTicks),
				Codec.FLOAT.optionalFieldOf("gravity").forGetter(OptionalProjectileDataRecord::gravity),
				Codec.FLOAT.optionalFieldOf("ink_coverage_on_impact").forGetter(OptionalProjectileDataRecord::inkCoverageImpact),
				Codec.FLOAT.optionalFieldOf("ink_drop_coverage").forGetter(OptionalProjectileDataRecord::inkDropCoverage),
				Codec.FLOAT.optionalFieldOf("distance_between_drops").forGetter(OptionalProjectileDataRecord::distanceBetweenInkDrops),
				Codec.FLOAT.optionalFieldOf("direct_damage").forGetter(OptionalProjectileDataRecord::baseDamage),
				Codec.FLOAT.optionalFieldOf("minimum_damage").forGetter(OptionalProjectileDataRecord::minDamage),
				Codec.FLOAT.optionalFieldOf("fall_damage_start").forGetter(OptionalProjectileDataRecord::damageDecayStartTick),
				Codec.FLOAT.optionalFieldOf("fall_damage_end").forGetter(OptionalProjectileDataRecord::damageDecayPerTick)
			).apply(instance, OptionalProjectileDataRecord::new)
		);
		public static final Codec<SingularSloshShotData> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.optionalFieldOf("startup_ticks", 0f).forGetter(SingularSloshShotData::startupTicks),
				Codec.INT.fieldOf("slosh_count").forGetter(SingularSloshShotData::count),
				Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("delay_between_projectiles", 1f).forGetter(SingularSloshShotData::delayBetweenProjectiles),
				Codec.FLOAT.optionalFieldOf("overriden_base_speed").forGetter(SingularSloshShotData::modifiedSpeed),
				Codec.FLOAT.optionalFieldOf("speed_substract_per_projectile", 0f).forGetter(SingularSloshShotData::speedSubstract),
				Codec.FLOAT.optionalFieldOf("offset_angle", 0f).forGetter(SingularSloshShotData::offsetAngle),
				PROJECTILE_MODIFICATIONS_CODEC.optionalFieldOf("slosh_projectile_modifications").forGetter(SingularSloshShotData::projectileModifications),
				DetonationRecord.CODEC.optionalFieldOf("detonation_data").forGetter(SingularSloshShotData::detonationData)
			).apply(instance, SingularSloshShotData::new)
		);
		public static final ProjectileDataRecord SLOSHER_PROJECTILE_DEFAULT = new ProjectileDataRecord(0, 0, 600, 1f, 0.729f, 0, 0.225f, 0, 0, 4, 0, 0, 1f, 5f);
		public static final SingularSloshShotData DEFAULT = new SingularSloshShotData(0, 1, 1f, Optional.empty(), 0, 0, Optional.empty(), Optional.empty());
	}
}
