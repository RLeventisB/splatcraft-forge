package net.splatcraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.MathHelper;

import java.util.*;
import java.util.stream.Collectors;

// file loosely based on https://github.com/WeaponMechanics/MechanicsMain/blob/master/WeaponMechanics/src/main/java/me/deecaad/weaponmechanics/weapon/damage/DamageDropoff.java
// thanks for teaching what the hell is a tree map!!
public class DamageRangesRecord
{
	public static final Codec<DamageRangesRecord> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Codec.list(DamageRangeRecord.CODEC).fieldOf("points").forGetter(DamageRangesRecord ->
				DamageRangesRecord.damageValues().entrySet().stream().map(v -> new DamageRangeRecord(v.getKey(), v.getValue())).collect(Collectors.toList())),
			Codec.BOOL.optionalFieldOf("lerp_between", true).forGetter(DamageRangesRecord::lerpBetween)
		).apply(instance, DamageRangesRecord::create)
	);
	public static final DamageRangesRecord DEFAULT = new DamageRangesRecord(new TreeMap<>(Collections.emptyMap()), false);
	private final TreeMap<Float, Float> damageValues;
	private final boolean lerpBetween;
	public DamageRangesRecord(
		TreeMap<Float, Float> damageValues,
		boolean lerpBetween
	)
	{
		this.damageValues = damageValues;
		this.lerpBetween = lerpBetween;
	}
	// yes this is from https://github.com/WeaponMechanics/MechanicsMain/blob/master/WeaponMechanics/src/main/java/me/deecaad/weaponmechanics/weapon/damage/DamageDropoff.java
	// go check it out it's a cool plugin (but if they made the trails free it would be cooler >:( im poor)
	public static DamageRangesRecord create(List<DamageRangeRecord> damageValues, boolean lerpBetween)
	{
		TreeMap<Float, Float> distances = new TreeMap<>();
		for (var pair : damageValues)
		{
			distances.put(pair.maxRange(), pair.damage());
		}
		return new DamageRangesRecord(distances, lerpBetween);
	}
	public static DamageRangesRecord createSimpleLerped(float closeDamage, float farDamage, float maxRange)
	{
		return new DamageRangesRecord(new TreeMap<>()
		{
			{
				put(0f, closeDamage);
				put(maxRange, farDamage);
			}
		}, true);
	}
	public static DamageRangesRecord createSimpleLerped(float closeDamage, float maxRange)
	{
		return new DamageRangesRecord(new TreeMap<>()
		{
			{
				put(0f, closeDamage);
				put(maxRange, 0f);
			}
		}, true);
	}
	public static DamageRangesRecord empty()
	{
		return new DamageRangesRecord(new TreeMap<>(), false);
	}
	public static DamageRangesRecord fromBuffer(ByteBuf buffer)
	{
		int size = buffer.readInt();
		TreeMap<Float, Float> damageValues = new TreeMap<>();
		for (int i = 0; i < size; i++)
		{
			damageValues.put(buffer.readFloat(), buffer.readFloat());
		}
		return new DamageRangesRecord(damageValues, buffer.readBoolean());
	}
	public float getDamage(float distance)
	{
		if (!lerpBetween)
		{
			Map.Entry<Float, Float> floor = damageValues.ceilingEntry(distance);
			return floor == null ? 0 : floor.getValue();
		}
		Map.Entry<Float, Float> floor = damageValues.floorEntry(distance);
		Map.Entry<Float, Float> ceiling = damageValues.ceilingEntry(distance);
		
		if (ceiling == null)
			return floor == null ? 0f : floor.getValue();
		
		// "Smooth Dropoff" is just a linear dropoff between the 2 points.
		// Using inverse-lerp and lerp like this is called a remap
		// function. 't' is a percentage of how far between 'distance' is
		// between 'floor' and 'ceiling'. Then we just use that percentage
		// to interpolate  -deecad
		
		// ok -me
		
		// oops i forgot to credit
		float floorDistance = floor == null ? 0 : floor.getKey();
		float floorDamage = floor == null ? ceiling.getValue() : floor.getValue();
		
		return MathHelper.lerp(MathHelper.getLerpProgress(distance, floorDistance, ceiling.getKey()), floorDamage, ceiling.getValue());
	}
	public float getMaxDistance()
	{
		return damageValues.isEmpty() ? 0 : damageValues.lastKey();
	}
	public float getMaxRegisteredDamage()
	{
		return damageValues.isEmpty() ? 0 : damageValues.firstEntry().getValue();
	}
	public boolean isInsignificant()
	{
		return damageValues.isEmpty() || getMaxDistance() == 0;
	}
	public DamageRangesRecord cloneWithMultiplier(float rangeMultiplier, float damageMultiplier)
	{
		if (rangeMultiplier == 1 && damageMultiplier == 1)
			return new DamageRangesRecord(damageValues, lerpBetween);
		TreeMap<Float, Float> map = new TreeMap<>();
		for (var pair : damageValues.entrySet())
		{
			map.put(pair.getKey() * rangeMultiplier, pair.getValue() * damageMultiplier);
		}
		return new DamageRangesRecord(map, lerpBetween);
	}
	public void writeToBuffer(ByteBuf buffer)
	{
		buffer.writeInt(damageValues.size());
		for (var pair : damageValues.entrySet())
		{
			buffer.writeFloat(pair.getKey());
			buffer.writeFloat(pair.getValue());
		}
		buffer.writeBoolean(lerpBetween);
	}
	public TreeMap<Float, Float> damageValues()
	{
		return damageValues;
	}
	public boolean lerpBetween()
	{
		return lerpBetween;
	}
	@Override
	public boolean equals(Object obj)
	{
		if (obj == this) return true;
		if (obj == null || obj.getClass() != getClass()) return false;
		var that = (DamageRangesRecord) obj;
		return Objects.equals(damageValues, that.damageValues) &&
			lerpBetween == that.lerpBetween;
	}
	@Override
	public int hashCode()
	{
		return Objects.hash(damageValues, lerpBetween);
	}
	@Override
	public String toString()
	{
		return "DamageRangesRecord[" +
			"damageValues=" + damageValues + ", " +
			"lerpBetween=" + lerpBetween + ']';
	}
	public Shifted withShift(float shift)
	{
		return new Shifted(this, shift);
	}
	public record DamageRangeRecord(float maxRange, float damage)
	{
		public static final Codec<DamageRangeRecord> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.fieldOf("max_range").forGetter(v -> v.maxRange),
				Codec.FLOAT.fieldOf("damage").forGetter(v -> v.damage)
			).apply(instance, DamageRangeRecord::new)
		);
	}
	public class Shifted extends DamageRangesRecord
	{
		private final float shift;
		public Shifted(TreeMap<Float, Float> damageValues, boolean lerpBetween, float shift)
		{
			super(damageValues, lerpBetween);
			this.shift = shift;
		}
		public Shifted(DamageRangesRecord damageRanges, float shift)
		{
			this(damageRanges.damageValues, damageRanges.lerpBetween, shift);
		}
		@Override
		public float getDamage(float distance)
		{
			return super.getDamage(distance + shift);
		}
	}
}
