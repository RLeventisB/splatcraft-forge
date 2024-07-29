package net.splatcraft.forge.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.splatcraft.forge.entities.ExtraSaveData;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.util.WeaponTooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SlosherWeaponSettings extends AbstractWeaponSettings<SlosherWeaponSettings, SlosherWeaponSettings.DataRecord>
{
	public boolean bypassesMobDamage;
	public static final SlosherWeaponSettings DEFAULT = new SlosherWeaponSettings("default");
	public SlosherShotDataRecord shotData = SlosherShotDataRecord.DEFAULT;
	public SlosherProjectileDataRecord sampleProjectile;
	public float lowestStartup;
	public SlosherWeaponSettings(String name)
	{
		super(name);
	}
	@Override
	public float calculateDamage(float tickCount, boolean airborne, InkProjectileEntity.ExtraDataList list)
	{
		ExtraSaveData.SloshExtraData sloshData = list.getFirstExtraData(ExtraSaveData.SloshExtraData.class);
		if (sloshData != null)
		{
			return shotData.sloshes.get(sloshData.sloshDataIndex).projectile.directDamage;
		}
		return sampleProjectile.directDamage;
	}
	@Override
	public float getMinDamage()
	{
		return 0;
	}
	@Override
	public WeaponTooltip<SlosherWeaponSettings>[] tooltipsToRegister()
	{
		return new WeaponTooltip[]
			{
				new WeaponTooltip<SlosherWeaponSettings>("speed", WeaponTooltip.Metrics.BPT, settings -> settings.sampleProjectile.speed, WeaponTooltip.RANKER_ASCENDING),
				new WeaponTooltip<SlosherWeaponSettings>("damage", WeaponTooltip.Metrics.HEALTH, settings -> settings.sampleProjectile.directDamage, WeaponTooltip.RANKER_ASCENDING),
				new WeaponTooltip<SlosherWeaponSettings>("handling", WeaponTooltip.Metrics.TICKS, settings -> settings.shotData.endlagTicks, WeaponTooltip.RANKER_DESCENDING)
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
		shotData = data.shot;
		
		sampleProjectile = data.sampleProjectile.orElse(SlosherProjectileDataRecord.DEFAULT);
		
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
	public DataRecord serialize()
	{
		return new DataRecord(shotData, Optional.ofNullable(sampleProjectile), moveSpeed, bypassesMobDamage, isSecret);
	}
	public SlosherWeaponSettings setBypassesMobDamage(boolean bypassesMobDamage)
	{
		this.bypassesMobDamage = bypassesMobDamage;
		return this;
	}
	public record DataRecord(
		SlosherShotDataRecord shot,
		Optional<SlosherProjectileDataRecord> sampleProjectile,
		float mobility,
		boolean bypassesMobDamage,
		boolean isSecret
	)
	{
		public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				SlosherShotDataRecord.CODEC.fieldOf("shot").forGetter(DataRecord::shot),
				SlosherProjectileDataRecord.CODEC.optionalFieldOf("sample_projectile").forGetter(DataRecord::sampleProjectile),
				Codec.FLOAT.optionalFieldOf("mobility", 1f).forGetter(DataRecord::mobility),
				Codec.BOOL.optionalFieldOf("full_damage_to_mobs", false).forGetter(DataRecord::bypassesMobDamage),
				Codec.BOOL.optionalFieldOf("is_secret", false).forGetter(DataRecord::isSecret)
			).apply(instance, DataRecord::create)
		);
		private static DataRecord create(SlosherShotDataRecord shot, Optional<SlosherProjectileDataRecord> sampleProjectile, float mobility, boolean bypassesMobDamage, boolean isSecret)
		{
			if (sampleProjectile.isEmpty() && !shot.sloshes.isEmpty())
				sampleProjectile = Optional.ofNullable(shot.sloshes.get(0).projectile);
			return new DataRecord(shot, sampleProjectile, mobility, bypassesMobDamage, isSecret);
		}
	}
	public record SlosherProjectileDataRecord(
		float size,
		float visualSize,
		float speed,
		float horizontalDrag,
		float straightShotTicks,
		float gravity,
		float inkCoverageImpact,
		float inkDropCoverage,
		float distanceBetweenInkDrops,
		float directDamage,
		Optional<BlasterWeaponSettings.DetonationRecord> detonationData
	)
	{
		public static final Codec<SlosherProjectileDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.fieldOf("size").forGetter(SlosherProjectileDataRecord::size),
				Codec.FLOAT.optionalFieldOf("visual_size").forGetter(r -> Optional.of(r.visualSize)),
				Codec.FLOAT.fieldOf("speed").forGetter(SlosherProjectileDataRecord::speed),
				Codec.FLOAT.optionalFieldOf("horizontal_drag", 0.262144F).forGetter(SlosherProjectileDataRecord::horizontalDrag),
				Codec.FLOAT.optionalFieldOf("straight_shot_ticks", 0F).forGetter(SlosherProjectileDataRecord::straightShotTicks),
				Codec.FLOAT.optionalFieldOf("gravity", 0.175F).forGetter(SlosherProjectileDataRecord::gravity),
				Codec.FLOAT.optionalFieldOf("ink_coverage_on_impact").forGetter(r -> Optional.of(r.inkCoverageImpact)),
				Codec.FLOAT.optionalFieldOf("ink_drop_coverage").forGetter(r -> Optional.of(r.inkDropCoverage)),
				Codec.FLOAT.optionalFieldOf("distance_between_drops", 4F).forGetter(SlosherProjectileDataRecord::distanceBetweenInkDrops),
				Codec.FLOAT.fieldOf("direct_damage").forGetter(SlosherProjectileDataRecord::directDamage),
				BlasterWeaponSettings.DetonationRecord.CODEC.optionalFieldOf("detonation_data").forGetter(SlosherProjectileDataRecord::detonationData)
			).apply(instance, SlosherProjectileDataRecord::create)
		);
		public static final SlosherProjectileDataRecord DEFAULT = new SlosherProjectileDataRecord(0, 0, 0, 0.262144F, 0, 0.175F, 0, 0, 4, 0, Optional.empty());
		private static SlosherProjectileDataRecord create(float size, Optional<Float> visualSize, float speed, float horizontalDrag, float straightShotTicks, float gravity, Optional<Float> inkCoverageImpact, Optional<Float> inkDropCoverage, float distanceBetweenInkDrops, float directDamage, Optional<BlasterWeaponSettings.DetonationRecord> detonationData)
		{
			return new SlosherProjectileDataRecord(size, visualSize.orElse(size * 3), speed, horizontalDrag, straightShotTicks, gravity, inkCoverageImpact.orElse(size * 0.85f), inkDropCoverage.orElse(size * 0.75f), distanceBetweenInkDrops, directDamage, detonationData);
		}
	}
	public record SlosherShotDataRecord(
		int endlagTicks,
		List<SingularSloshShotData> sloshes,
		float pitchCompensation,
		float inkConsumption,
		int inkRecoveryCooldown
	)
	{
		public static final Codec<SlosherShotDataRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.INT.optionalFieldOf("endlag_ticks", 1).forGetter(SlosherShotDataRecord::endlagTicks),
				SingularSloshShotData.CODEC.listOf().fieldOf("sloshes_data").forGetter(SlosherShotDataRecord::sloshes),
				Codec.FLOAT.optionalFieldOf("pitch_compensation", 0f).forGetter(SlosherShotDataRecord::pitchCompensation),
				Codec.FLOAT.fieldOf("ink_consumption").forGetter(SlosherShotDataRecord::inkConsumption),
				Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(SlosherShotDataRecord::inkRecoveryCooldown)
			).apply(instance, SlosherShotDataRecord::new)
		);
		public static final SlosherShotDataRecord DEFAULT = new SlosherShotDataRecord(0, new ArrayList<>(), 0, 0, 0);
	}
	public record SingularSloshShotData(
		float startupTicks,
		int count,
		float delayBetweenProjectiles,
		float speedSubstract,
		float offsetAngle,
		SlosherProjectileDataRecord projectile
	)
	{
		public static final Codec<SingularSloshShotData> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.optionalFieldOf("startup_ticks", 0f).forGetter(SingularSloshShotData::startupTicks),
				Codec.INT.fieldOf("slosh_count").forGetter(SingularSloshShotData::count),
				Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("delay_between_projectiles", 1f).forGetter(SingularSloshShotData::delayBetweenProjectiles),
				Codec.FLOAT.optionalFieldOf("speed_substract_per_projectile", 0f).forGetter(SingularSloshShotData::speedSubstract),
				Codec.FLOAT.optionalFieldOf("offset_angle", 0f).forGetter(SingularSloshShotData::offsetAngle),
				SlosherProjectileDataRecord.CODEC.fieldOf("slosh_projectile").forGetter(SingularSloshShotData::projectile)
			).apply(instance, SingularSloshShotData::new)
		);
		public static final SingularSloshShotData DEFAULT = new SingularSloshShotData(0, 1, 1f, 0, 0, SlosherProjectileDataRecord.DEFAULT);
	}
}
