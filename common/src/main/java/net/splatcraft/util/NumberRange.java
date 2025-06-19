package net.splatcraft.util;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

// oh wait mojang has a class for these
// but it uses 3 times more ram and thats not acceptable >:(
public interface NumberRange<NUMTYPE extends Number>
{
	static <NUMTYPE extends Number, RANGE extends NumberRange<NUMTYPE>> Codec<RANGE> createSingleNumCodec(Codec<NUMTYPE> numberCodec, Function<NUMTYPE, RANGE> constructor)
	{
		return numberCodec.xmap(constructor, NumberRange::min);
	}
	static <NUMTYPE extends Number, RANGE extends NumberRange<NUMTYPE>> Codec<RANGE> createArrayCodec(Codec<NUMTYPE> numberCodec, BiFunction<NUMTYPE, NUMTYPE, RANGE> constructor)
	{
		return Codec.list(numberCodec, 2, 2).xmap(v -> constructor.apply(v.getFirst(), v.get(1)), v -> List.of(v.min(), v.max()));
	}
	static <NUMTYPE extends Number, RANGE extends NumberRange<NUMTYPE>> Products.P2<RecordCodecBuilder.Mu<RANGE>, NUMTYPE, NUMTYPE> objectCodecStart(RecordCodecBuilder.Instance<RANGE> inst, Codec<NUMTYPE> numberCodec)
	{
		return inst.group(
			numberCodec.fieldOf("min").forGetter(RANGE::min),
			numberCodec.fieldOf("max").forGetter(RANGE::max)
		);
	}
	static <NUMTYPE extends Number, RANGE extends NumberRange<NUMTYPE>> Codec<RANGE> createObjectCodec(Codec<NUMTYPE> numberCodec, BiFunction<NUMTYPE, NUMTYPE, RANGE> constructor)
	{
		return RecordCodecBuilder.create(inst -> objectCodecStart(inst, numberCodec).apply(inst, constructor));
	}
	NUMTYPE min();
	NUMTYPE max();
	NUMTYPE getValue(float progress);
	default NUMTYPE getRandom(RandomSource random)
	{
		return getValue(random.nextFloat());
	}
	default float average()
	{
		return (min().floatValue() + max().floatValue()) / 2f;
	}
	NumberRange<NUMTYPE> mapBoth(Function<NUMTYPE, NUMTYPE> mapper);
	record FloatRange(
		Float min,
		Float max
	) implements NumberRange<Float>
	{
		public static final FloatRange ZERO = new FloatRange(0f, 0f);
		public static final Codec<FloatRange> SINGLE_NUMBER_CODEC = createSingleNumCodec(Codec.FLOAT, FloatRange::ofValue);
		public static final Codec<FloatRange> OBJECT_PAIR_CODEC = createObjectCodec(Codec.FLOAT, FloatRange::new);
		public static final Codec<FloatRange> LIST_PAIR_CODEC = createArrayCodec(Codec.FLOAT, FloatRange::new);
		public static final Codec<FloatRange> CODEC = Codec.withAlternative(LIST_PAIR_CODEC, Codec.withAlternative(OBJECT_PAIR_CODEC, SINGLE_NUMBER_CODEC));
		public static FloatRange ofValue(float value)
		{
			return new FloatRange(value, value);
		}
		@Override
		public Float getValue(float progress)
		{
			return Mth.lerp(progress, min, max);
		}
		@Override
		public FloatRange mapBoth(Function<Float, Float> mapper)
		{
			return new FloatRange(mapper.apply(min), mapper.apply(max));
		}
	}
	record FloatRangeShifted(
		Float min,
		Float max,
		float minProgress,
		float maxProgress
	) implements NumberRange<Float>
	{
		public static final FloatRangeShifted ZERO = new FloatRangeShifted(0f, 0f, 0, 1f);
		public static final Codec<FloatRangeShifted> CODEC = RecordCodecBuilder.create(
			inst -> NumberRange.objectCodecStart(inst, Codec.FLOAT).and(
				inst.group(
					Codec.FLOAT.fieldOf("min_progress").forGetter(FloatRangeShifted::minProgress),
					Codec.FLOAT.fieldOf("max_progress").forGetter(FloatRangeShifted::maxProgress)
				)
			).apply(inst, FloatRangeShifted::new)
		);
		public static FloatRangeShifted create(float min, float max)
		{
			return new FloatRangeShifted(min, max, 0f, 1f);
		}
		@Override
		public Float getValue(float progress)
		{
			if (progress < minProgress)
				return min;
			if (progress > maxProgress)
				return max;
			return Mth.lerp((progress - minProgress) / (maxProgress - minProgress), min, max);
		}
		@Override
		public FloatRangeShifted mapBoth(Function<Float, Float> mapper)
		{
			return new FloatRangeShifted(mapper.apply(min), mapper.apply(max), minProgress, maxProgress);
		}
	}
	record IntRange(
		Integer min,
		Integer max
	) implements NumberRange<Integer>
	{
		public static final IntRange ZERO = new IntRange(0, 0);
		public static final Codec<IntRange> SINGLE_NUMBER_CODEC = createSingleNumCodec(Codec.INT, IntRange::ofValue);
		public static final Codec<IntRange> OBJECT_PAIR_CODEC = createObjectCodec(Codec.INT, IntRange::new);
		public static final Codec<IntRange> LIST_PAIR_CODEC = createArrayCodec(Codec.INT, IntRange::new);
		public static final Codec<IntRange> CODEC = Codec.withAlternative(LIST_PAIR_CODEC, Codec.withAlternative(OBJECT_PAIR_CODEC, SINGLE_NUMBER_CODEC));
		public static IntRange ofValue(int value)
		{
			return new IntRange(value, value);
		}
		@Override
		public Integer getValue(float progress)
		{
			return min + Math.round(progress * (float) (max - min));
		}
		@Override
		public IntRange mapBoth(Function<Integer, Integer> mapper)
		{
			return new IntRange(mapper.apply(min), mapper.apply(max));
		}
	}
}
