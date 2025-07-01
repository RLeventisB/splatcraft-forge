package net.splatcraft.util;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

// mojang doesnt have a class for this! i think!
public interface VariableNumber<NUMTYPE extends Number>
{
	static <NUMTYPE extends Number, VARIABLE extends VariableNumber<NUMTYPE>> Codec<VARIABLE> createSingleNumCodec(Codec<NUMTYPE> numberCodec, Function<NUMTYPE, VARIABLE> constructor)
	{
		return numberCodec.xmap(constructor, VariableNumber::average);
	}
	static <NUMTYPE extends Number, VARIABLE extends VariableNumber<NUMTYPE>> Codec<VARIABLE> createArrayCodec(Codec<NUMTYPE> numberCodec, BiFunction<NUMTYPE, NUMTYPE, VARIABLE> constructor)
	{
		return Codec.list(numberCodec, 2, 2).xmap(v -> constructor.apply(v.getFirst(), v.get(1)), v -> List.of(v.average(), v.variance()));
	}
	static <NUMTYPE extends Number, VARIABLE extends VariableNumber<NUMTYPE>> Products.P2<RecordCodecBuilder.Mu<VARIABLE>, NUMTYPE, NUMTYPE> objectCodecStart(RecordCodecBuilder.Instance<VARIABLE> inst, Codec<NUMTYPE> numberCodec)
	{
		return inst.group(
			numberCodec.fieldOf("average").forGetter(VARIABLE::average),
			numberCodec.fieldOf("variance").forGetter(VARIABLE::variance)
		);
	}
	static <NUMTYPE extends Number, VARIABLE extends VariableNumber<NUMTYPE>> Codec<VARIABLE> createObjectCodec(Codec<NUMTYPE> numberCodec, BiFunction<NUMTYPE, NUMTYPE, VARIABLE> constructor)
	{
		return RecordCodecBuilder.create(inst -> objectCodecStart(inst, numberCodec).apply(inst, constructor));
	}
	NUMTYPE average();
	NUMTYPE variance();
	NUMTYPE getValue(float value);
	default NUMTYPE getValue(RandomSource random)
	{
		return getValue(random.nextFloat() * 2 - 1);
	}
	VariableNumber<NUMTYPE> mapBoth(Function<NUMTYPE, NUMTYPE> mapper);
	record VariableFloat(
		Float average,
		Float variance
	) implements VariableNumber<Float>
	{
		public static final VariableFloat ZERO = new VariableFloat(0f, 0f);
		public static final Codec<VariableFloat> SINGLE_NUMBER_CODEC = getSingleNumberCodec(0f);
		public static final Codec<VariableFloat> OBJECT_PAIR_CODEC = createObjectCodec(Codec.FLOAT, VariableFloat::new);
		public static final Codec<VariableFloat> LIST_PAIR_CODEC = createArrayCodec(Codec.FLOAT, VariableFloat::new);
		public static final Codec<VariableFloat> CODEC = Codec.withAlternative(LIST_PAIR_CODEC, Codec.withAlternative(OBJECT_PAIR_CODEC, SINGLE_NUMBER_CODEC));
		public static Codec<VariableFloat> getCodec(float defaultVariance)
		{
			return Codec.withAlternative(LIST_PAIR_CODEC, Codec.withAlternative(OBJECT_PAIR_CODEC, getSingleNumberCodec(defaultVariance)));
		}
		public static Codec<VariableFloat> getSingleNumberCodec(float defaultVariance)
		{
			return createSingleNumCodec(Codec.FLOAT, v -> new VariableFloat(v, defaultVariance));
		}
		@Override
		public Float getValue(float value)
		{
			return average + variance * value;
		}
		@Override
		public VariableFloat mapBoth(Function<Float, Float> mapper)
		{
			return new VariableFloat(mapper.apply(average), mapper.apply(variance));
		}
	}
	record VariableInt(
		Integer average,
		Integer variance
	) implements VariableNumber<Integer>
	{
		public static final VariableInt ZERO = new VariableInt(0, 0);
		public static final Codec<VariableInt> SINGLE_NUMBER_CODEC = getSingleNumberCodec(0);
		public static final Codec<VariableInt> OBJECT_PAIR_CODEC = createObjectCodec(Codec.INT, VariableInt::new);
		public static final Codec<VariableInt> LIST_PAIR_CODEC = createArrayCodec(Codec.INT, VariableInt::new);
		public static final Codec<VariableInt> CODEC = Codec.withAlternative(LIST_PAIR_CODEC, Codec.withAlternative(OBJECT_PAIR_CODEC, SINGLE_NUMBER_CODEC));
		public static Codec<VariableInt> getCodec(int defaultVariance)
		{
			return Codec.withAlternative(LIST_PAIR_CODEC, Codec.withAlternative(OBJECT_PAIR_CODEC, getSingleNumberCodec(defaultVariance)));
		}
		public static Codec<VariableInt> getSingleNumberCodec(int defaultVariance)
		{
			return createSingleNumCodec(Codec.INT, v -> new VariableInt(v, defaultVariance));
		}
		public Integer getValue(float value)
		{
			return (int) (average + variance * value);
		}
		@Override
		public VariableInt mapBoth(Function<Integer, Integer> mapper)
		{
			return new VariableInt(mapper.apply(average), mapper.apply(variance));
		}
	}
}
