package net.splatcraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec2f;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class CodecUtils
{
	public static final Codec<Hand> HAND_NULL_IS_MAIN_CODEC = new Codec<>()
	{
		@Override
		public <T> DataResult<Pair<Hand, T>> decode(DynamicOps<T> ops, T input)
		{
			DataResult<Boolean> result = ops.getBooleanValue(input);
			if (result.isSuccess())
				return DataResult.success(Pair.of(result.getOrThrow() ? Hand.MAIN_HAND : Hand.OFF_HAND, input));
			return DataResult.error(() -> "Invalid input.");
		}
		@Override
		public <T> DataResult<T> encode(Hand input, DynamicOps<T> ops, T prefix)
		{
			return DataResult.success(ops.createBoolean(Objects.equals(input, Hand.MAIN_HAND)));
		}
	};
	public static final Codec<Vec2f> VEC_2_CODEC = RecordCodecBuilder.create(inst -> inst.group(
		Codec.FLOAT.fieldOf("x").forGetter(v -> v.x),
		Codec.FLOAT.fieldOf("y").forGetter(v -> v.y)
	).apply(inst, Vec2f::new));
	public static <K, V> Codec<Object2ObjectOpenHashMap<K, V>> hashMapCodec(Codec<K> keyCodec, Codec<V> valueCodec)
	{
		return new UnboundedHashMapCodec<>(keyCodec, valueCodec);
	}
	public static <E> Codec<ObjectArrayList<E>> arrayList(Codec<E> codec)
	{
		return new ArrayListCodec<>(codec);
	}
	// this is literally UnboundedMapCodec but it gives the map raw.
	public record UnboundedHashMapCodec<K, V>(
		Codec<K> keyCodec,
		Codec<V> elementCodec
	) implements Codec<Object2ObjectOpenHashMap<K, V>>
	{
		@Override
		public <T> DataResult<Pair<Object2ObjectOpenHashMap<K, V>, T>> decode(final DynamicOps<T> ops, final T input)
		{
			return ops.getMap(input).setLifecycle(Lifecycle.stable()).flatMap(map -> decode(ops, map)).map(r -> Pair.of(r, input));
		}
		@Override
		public <T> DataResult<T> encode(final Object2ObjectOpenHashMap<K, V> input, final DynamicOps<T> ops, final T prefix)
		{
			return encode(input, ops, ops.mapBuilder()).build(prefix);
		}
		@Override
		public String toString()
		{
			return "UnboundedHashMapCodec[" + keyCodec + " -> " + elementCodec + ']';
		}
		private <T> DataResult<Object2ObjectOpenHashMap<K, V>> decode(final DynamicOps<T> ops, final MapLike<T> input)
		{
			final Object2ObjectOpenHashMap<K, V> read = new Object2ObjectOpenHashMap<>();
			final Stream.Builder<Pair<T, T>> failed = Stream.builder();
			
			final DataResult<Unit> result = input.entries().reduce(
				DataResult.success(Unit.INSTANCE, Lifecycle.stable()),
				(r, pair) ->
				{
					final DataResult<K> key = keyCodec().parse(ops, pair.getFirst());
					final DataResult<V> value = elementCodec().parse(ops, pair.getSecond());
					
					final DataResult<Pair<K, V>> entryResult = key.apply2stable(Pair::of, value);
					final Optional<Pair<K, V>> entry = entryResult.resultOrPartial();
					if (entry.isPresent())
					{
						final V existingValue = read.putIfAbsent(entry.get().getFirst(), entry.get().getSecond());
						if (existingValue != null)
						{
							failed.add(pair);
							return r.apply2stable((u, p) -> u, DataResult.error(() -> "Duplicate entry for key: '" + entry.get().getFirst() + "'"));
						}
					}
					if (entryResult.isError())
					{
						failed.add(pair);
					}
					
					return r.apply2stable((u, p) -> u, entryResult);
				},
				(r1, r2) -> r1.apply2stable((u1, u2) -> u1, r2)
			);
			
			final T errors = ops.createMap(failed.build());
			
			return result.map(unit -> read).setPartial(read).mapError(e -> e + " missed input: " + errors);
		}
		<T> RecordBuilder<T> encode(final Object2ObjectMap<K, V> input, final DynamicOps<T> ops, final RecordBuilder<T> prefix)
		{
			for (final Map.Entry<K, V> entry : input.entrySet())
			{
				prefix.add(keyCodec().encodeStart(ops, entry.getKey()), elementCodec().encodeStart(ops, entry.getValue()));
			}
			return prefix;
		}
	}
	// yes this one is too stolen from ListCodec
	public record ArrayListCodec<E>(Codec<E> elementCodec) implements Codec<ObjectArrayList<E>>
	{
		@Override
		public <T> DataResult<T> encode(final ObjectArrayList<E> input, final DynamicOps<T> ops, final T prefix)
		{
			final ListBuilder<T> builder = ops.listBuilder();
			for (final E element : input)
			{
				builder.add(elementCodec.encodeStart(ops, element));
			}
			return builder.build(prefix);
		}
		@Override
		public <T> DataResult<Pair<ObjectArrayList<E>, T>> decode(final DynamicOps<T> ops, final T input)
		{
			return ops.getList(input).setLifecycle(Lifecycle.stable()).flatMap(stream ->
			{
				final DecoderState<T> decoder = new DecoderState<>(ops);
				stream.accept(decoder::accept);
				return decoder.build();
			});
		}
		@Override
		public String toString()
		{
			return "ArrayListCodec[" + elementCodec + ']';
		}
		private class DecoderState<T>
		{
			private static final DataResult<Unit> INITIAL_RESULT = DataResult.success(Unit.INSTANCE, Lifecycle.stable());
			private final DynamicOps<T> ops;
			private final ObjectArrayList<E> elements = new ObjectArrayList<>();
			private final Stream.Builder<T> failed = Stream.builder();
			private DataResult<Unit> result = INITIAL_RESULT;
			private DecoderState(final DynamicOps<T> ops)
			{
				this.ops = ops;
			}
			public void accept(final T value)
			{
				final DataResult<Pair<E, T>> elementResult = elementCodec.decode(ops, value);
				elementResult.error().ifPresent(error -> failed.add(value));
				elementResult.resultOrPartial().ifPresent(pair -> elements.add(pair.getFirst()));
				result = result.apply2stable((result, element) -> result, elementResult);
			}
			public DataResult<Pair<ObjectArrayList<E>, T>> build()
			{
				final T errors = ops.createList(failed.build());
				final Pair<ObjectArrayList<E>, T> pair = Pair.of(elements, errors);
				return result.map(ignored -> pair).setPartial(pair);
			}
		}
	}
}
