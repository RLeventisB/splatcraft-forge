package net.splatcraft.util.structs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class RangedValueCollection
{
	public static final Codec<RangedValueCollection> DAMAGE_CODEC = createCodec("max_range", "damage");
	private static Codec<RangedValueCollection> createCodec(String keyName, String valueName)
	{
		return createCodec(keyName, valueName, "points");
	}
	private static Codec<RangedValueCollection> createCodec(String keyName, String valueName, String listName)
	{
		final Codec<Map.Entry<Float, Float>> ENTRY_CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.fieldOf(keyName).forGetter(Map.Entry::getKey),
				Codec.FLOAT.fieldOf(valueName).forGetter(Map.Entry::getValue)
			).apply(instance, Map::entry)
		);
		
		return RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.list(ENTRY_CODEC).fieldOf(listName).forGetter(collection ->
					collection.rangedValues().entrySet().stream().map(v -> Map.entry(v.getKey(), v.getValue())).collect(Collectors.toList())),
				Codec.BOOL.optionalFieldOf("lerp_between", true).forGetter(RangedValueCollection::lerpBetween)
			).apply(instance, RangedValueCollection::new)
		);
	}
	public static StreamCodec<ByteBuf, RangedValueCollection> STREAM_CODEC;
	static
	{
		final StreamCodec<ByteBuf, Map.Entry<Float, Float>> ENTRY_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, Map.Entry::getKey,
			ByteBufCodecs.FLOAT, Map.Entry::getValue,
			Map::entry
		);
		
		STREAM_CODEC = StreamCodec.composite(
			ENTRY_CODEC.apply(ByteBufCodecs.list()), RangedValueCollection::getRangedValueList,
			ByteBufCodecs.BOOL, RangedValueCollection::lerpBetween,
			RangedValueCollection::new
		);
	}
	public static final RangedValueCollection EMPTY = new RangedValueCollection(Map.of(), false);
	private final TreeMap<Float, Float> rangedValues;
	private final boolean lerpBetween;
	public RangedValueCollection(TreeMap<Float, Float> rangedValues,
	                             boolean lerpBetween)
	{
		this.rangedValues = rangedValues;
		this.lerpBetween = lerpBetween;
	}
	public RangedValueCollection(Map<Float, Float> rangedValues,
	                             boolean lerpBetween)
	{
		this.rangedValues = new TreeMap<>(rangedValues);
		this.lerpBetween = lerpBetween;
	}
	public RangedValueCollection(List<Map.Entry<Float, Float>> rangedValues,
	                             boolean lerpBetween)
	{
		this.rangedValues = rangedValues.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> x, TreeMap::new));
		this.lerpBetween = lerpBetween;
	}
	public static RangedValueCollection createDamageSimpleLerped(float closeDamage, float farDamage, float maxRange)
	{
		return new RangedValueCollection(Map.of(
			0f, closeDamage,
			maxRange, farDamage
		), true);
	}
	public static RangedValueCollection createDamageSimpleLerped(float closeDamage, float maxRange)
	{
		return createDamageSimpleLerped(closeDamage, 0f, maxRange);
	}
	public static boolean isInsignificant(RangedValueCollection record)
	{
		return record == null || record.rangedValues.isEmpty() || record.getMaxKey() == 0;
	}
	public float getValue(float key)
	{
		if (!lerpBetween)
		{
			Map.Entry<Float, Float> floor = rangedValues.ceilingEntry(key);
			return floor == null ? 0 : floor.getValue();
		}
		Map.Entry<Float, Float> floor = rangedValues.floorEntry(key);
		Map.Entry<Float, Float> ceiling = rangedValues.ceilingEntry(key);
		
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
		
		return Mth.lerp(Mth.inverseLerp(key, floorDistance, ceiling.getKey()), floorDamage, ceiling.getValue());
	}
	public float getMaxKey()
	{
		return rangedValues.isEmpty() ? 0 : rangedValues.lastKey();
	}
	public float getMaxValue()
	{
		return rangedValues.isEmpty() ? 0 : rangedValues.firstEntry().getValue();
	}
	public RangedValueCollection cloneWithMultiplier(float rangeMultiplier, float damageMultiplier)
	{
		if (rangeMultiplier == 1 && damageMultiplier == 1)
			return new RangedValueCollection(rangedValues, lerpBetween);
		TreeMap<Float, Float> map = new TreeMap<>();
		for (var pair : rangedValues.entrySet())
		{
			map.put(pair.getKey() * rangeMultiplier, pair.getValue() * damageMultiplier);
		}
		return new RangedValueCollection(map, lerpBetween);
	}
	public List<Map.Entry<Float, Float>> getRangedValueList()
	{
		return rangedValues.entrySet().stream().toList();
	}
	public Shifted withShift(float shift)
	{
		return new Shifted(this, shift);
	}
	public TreeMap<Float, Float> rangedValues()
	{
		return rangedValues;
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
		var that = (RangedValueCollection) obj;
		return Objects.equals(rangedValues, that.rangedValues) &&
			lerpBetween == that.lerpBetween;
	}
	@Override
	public int hashCode()
	{
		return Objects.hash(rangedValues, lerpBetween);
	}
	@Override
	public String toString()
	{
		return "RangedValueCollection[" +
			"rangedValues=" + rangedValues + ", " +
			"lerpBetween=" + lerpBetween + ']';
	}
	public static class Shifted extends RangedValueCollection
	{
		private final float shift;
		public Shifted(TreeMap<Float, Float> rangedValues, boolean lerpBetween, float shift)
		{
			super(rangedValues, lerpBetween);
			this.shift = shift;
		}
		public Shifted(RangedValueCollection collection, float shift)
		{
			this(collection.rangedValues, collection.lerpBetween, shift);
		}
		@Override
		public float getValue(float key)
		{
			return super.getValue(key + shift);
		}
		@Override
		public Shifted withShift(float shift)
		{
			return new Shifted(rangedValues(), lerpBetween(), shift + this.shift);
		}
	}
}
