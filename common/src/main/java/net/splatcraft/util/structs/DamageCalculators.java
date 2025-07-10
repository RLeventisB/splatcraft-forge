package net.splatcraft.util.structs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.splatcraft.entities.InkProjectileEntity;
import org.joml.Vector3f;

public class DamageCalculators
{
	public static class EmptyDamageCalculator implements DamageCalculator
	{
		public static final MapCodec<EmptyDamageCalculator> CODEC = MapCodec.unit(new EmptyDamageCalculator());
		public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList extraDataList)
		{
			return 0;
		}
	}
	public record BasicDamageCalculator(
		float baseDamage,
		float damageDecayStartTick,
		float damageDecayPerTick,
		float minDamage
	) implements DamageCalculator
	{
		public static final MapCodec<BasicDamageCalculator> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				Codec.FLOAT.fieldOf("base_damage").forGetter(BasicDamageCalculator::baseDamage),
				Codec.FLOAT.fieldOf("damage_decay_start_tick").forGetter(BasicDamageCalculator::damageDecayStartTick),
				Codec.FLOAT.fieldOf("damage_decay_per_tick").forGetter(BasicDamageCalculator::damageDecayPerTick),
				Codec.FLOAT.fieldOf("min_damage").forGetter(BasicDamageCalculator::minDamage)
			).apply(inst, BasicDamageCalculator::new)
		);
		public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList extraDataList)
		{
			return projectile.calculateDamageDecay(baseDamage(), damageDecayStartTick(), damageDecayPerTick(), minDamage());
		}
	}
	public record StaticDamageCalculator(
		float damage
	) implements DamageCalculator
	{
		public static final MapCodec<StaticDamageCalculator> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				Codec.FLOAT.fieldOf("damage").forGetter(StaticDamageCalculator::damage)
			).apply(inst, StaticDamageCalculator::new)
		);
		public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList extraDataList)
		{
			return damage;
		}
	}
	public record SlosherDamageCalculator(
		float spawnHeight,
		float baseDamage,
		float minDamage,
		float damageDecayStartHeight,
		float damageDecayMinHeight
	) implements DamageCalculator
	{
		public static final MapCodec<SlosherDamageCalculator> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				Codec.FLOAT.fieldOf("spawn_height").forGetter(SlosherDamageCalculator::spawnHeight),
				Codec.FLOAT.fieldOf("base_damage").forGetter(SlosherDamageCalculator::baseDamage),
				Codec.FLOAT.fieldOf("min_damage").forGetter(SlosherDamageCalculator::minDamage),
				Codec.FLOAT.fieldOf("damage_decay_start_height").forGetter(SlosherDamageCalculator::damageDecayStartHeight),
				Codec.FLOAT.fieldOf("damage_decay_min_height").forGetter(SlosherDamageCalculator::damageDecayMinHeight)
			).apply(inst, SlosherDamageCalculator::new)
		);
		public float getDamage(float relativeY)
		{
			float damage = baseDamage();
			if (relativeY < -damageDecayMinHeight)
				damage = minDamage();
			else if (relativeY < -damageDecayStartHeight)
				damage = Mth.lerp(Mth.inverseLerp(-relativeY, damageDecayStartHeight, damageDecayMinHeight), baseDamage, minDamage);
			return damage;
		}
		public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
		{
			if (minDamage == baseDamage)
				return baseDamage;
			
			float relativeY = (float) projectile.getY() - spawnHeight;
			return getDamage(relativeY);
		}
	}
	public record RollerDamageCalculator(
		RangedValueCollection damageRanges,
		Vector3f spawnPos,
		float damageFalloffPercentStartTick,
		float damageFalloffPercentPerTick,
		float maxDamageFalloffPercent
	) implements DamageCalculator
	{
		public static final MapCodec<RollerDamageCalculator> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				RangedValueCollection.DAMAGE_CODEC.fieldOf("damage_ranges").forGetter(RollerDamageCalculator::damageRanges),
				ExtraCodecs.VECTOR3F.fieldOf("spawn_pos").forGetter(RollerDamageCalculator::spawnPos),
				Codec.FLOAT.fieldOf("damage_falloff_percent_start_tick").forGetter(RollerDamageCalculator::damageFalloffPercentStartTick),
				Codec.FLOAT.fieldOf("damage_falloff_percent_per_tick").forGetter(RollerDamageCalculator::damageFalloffPercentPerTick),
				Codec.FLOAT.fieldOf("max_damage_falloff_percent").forGetter(RollerDamageCalculator::maxDamageFalloffPercent)
			).apply(inst, RollerDamageCalculator::new)
		);
		public float calculateDamage(InkProjectileEntity projectile, InkProjectileEntity.ExtraDataList list)
		{
			float distance = spawnPos.distance(projectile.position().toVector3f());
			
			float timeDamagePercent = projectile.calculateDamageDecay(1, damageFalloffPercentStartTick, damageFalloffPercentPerTick, maxDamageFalloffPercent);
			return damageRanges.getValue(distance) * timeDamagePercent;
		}
	}
}
