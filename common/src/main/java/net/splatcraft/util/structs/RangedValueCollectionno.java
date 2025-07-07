package net.splatcraft.util.structs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

// file loosely based on https://github.com/WeaponMechanics/MechanicsMain/blob/master/WeaponMechanics/src/main/java/me/deecaad/weaponmechanics/weapon/damage/DamageDropoff.java
// thanks for teaching what the hell is a tree map!!
public interface RangedValueCollectionno<SELF, RANGEDVALUE extends RangedValueCollectionno.RangedValue>
{
	static <SELF extends RangedValueCollectionno<SELF, RANGEDVALUE>, RANGEDVALUE extends RangedValueCollectionno.RangedValue> Codec<SELF> createCodec(
		String listName,
		String lerpName,
		Codec<RANGEDVALUE> structCodec,
		Function<Map.Entry<Float, Float>, RANGEDVALUE> structConstructor,
		BiFunction<List<RANGEDVALUE>, Boolean, SELF> constructor
	)
	{
		Function<SELF, List<RANGEDVALUE>> objectListFunction = collection ->
			collection.rangedValues().entrySet().stream().map(structConstructor).collect(Collectors.toList());
		return RecordCodecBuilder.create(
			inst -> inst.group(
				Codec.list(structCodec).fieldOf(listName).forGetter(objectListFunction),
				Codec.BOOL.optionalFieldOf(lerpName, true).forGetter(SELF::lerpBetween)
			).apply(inst, constructor)
		);
	}
	static boolean isInsignificant(RangedValueCollectionno<?, ?> record)
	{
		return record == null || record.rangedValues().isEmpty() || record.getMaxKey() == 0;
	}
	TreeMap<Float, Float> rangedValues();
	boolean lerpBetween();
	default float getValue(float distance)
	{
		if (!lerpBetween())
		{
			Map.Entry<Float, Float> floor = rangedValues().ceilingEntry(distance);
			return floor == null ? 0 : floor.getValue();
		}
		Map.Entry<Float, Float> floor = rangedValues().floorEntry(distance);
		Map.Entry<Float, Float> ceiling = rangedValues().ceilingEntry(distance);
		
		if (ceiling == null)
			return floor == null ? 0f : floor.getValue();
		
		// "Smooth Dropoff" is just a linear dropoff between the 2 points.
		// Using inverse-lerp and lerp like this is called a remap
		// function. 't' is a percentage of how far between 'distance' is
		// between 'floor' and 'ceiling'. Then we just use that percentage
		// to interpolate  -deecad
		
		// ok -me
		
		float floorDistance = floor == null ? 0 : floor.getKey();
		float floorDamage = floor == null ? ceiling.getValue() : floor.getValue();
		
		return Mth.lerp(Mth.inverseLerp(distance, floorDistance, ceiling.getKey()), floorDamage, ceiling.getValue());
	}
	default float getMaxKey()
	{
		return rangedValues().isEmpty() ? 0 : rangedValues().lastKey();
	}
	default float getMaxValue()
	{
		return rangedValues().isEmpty() ? 0 : rangedValues().firstEntry().getValue();
	}
	default SELF cloneWithMultiplier(float keyMultiplier, float valueMultiplier)
	{
		if (keyMultiplier == 1 && valueMultiplier == 1)
			return mapConstructor().apply(rangedValues(), lerpBetween());
		
		TreeMap<Float, Float> map = new TreeMap<>();
		for (var pair : rangedValues().entrySet())
		{
			map.put(pair.getKey() * keyMultiplier, pair.getValue() * valueMultiplier);
		}
		return mapConstructor().apply(map, lerpBetween());
	}
	BiFunction<TreeMap<Float, Float>, Boolean, SELF> mapConstructor();
	default BiFunction<List<RANGEDVALUE>, Boolean, SELF> valueConstructor()
	{
		return (list, lerp) ->
		{
			final Collector<RANGEDVALUE, ?, TreeMap<Float, Float>> treeMapCollector = Collectors.toMap(RangedValue::key, RangedValue::value, (x, y) -> x, TreeMap::new);
			TreeMap<Float, Float> map = list.stream().collect(treeMapCollector);
			return mapConstructor().apply(map, lerp);
		};
	}
	interface RangedValue
	{
		float key();
		float value();
	}
}
