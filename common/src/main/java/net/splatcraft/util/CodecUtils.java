package net.splatcraft.util;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.Products;
import com.mojang.datafixers.Products.P10;
import com.mojang.datafixers.Products.P2;
import com.mojang.datafixers.Products.P8;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.Splatcraft;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

public class CodecUtils
{
	public static <R> DataResult<R> exceptionCatchDataResult(Supplier<R> supplier)
	{
		try
		{
			return DataResult.success(supplier.get());
		}
		catch (Exception e)
		{
			return DataResult.error(() -> supplier + " threw an exception");
		}
	}
	public static <T extends Enum<T>> StreamCodec<ByteBuf, T> createEnumPacketCodec(final Supplier<T[]> values)
	{
		return createEnumPacketCodec(values.get(), ByIdMap.OutOfBoundsStrategy.WRAP);
	}
	public static <T extends Enum<T>> StreamCodec<ByteBuf, T> createEnumPacketCodec(final Supplier<T[]> values, final ByIdMap.OutOfBoundsStrategy boundsStrategy)
	{
		return createEnumPacketCodec(values.get(), boundsStrategy);
	}
	public static <T extends Enum<T>> StreamCodec<ByteBuf, T> createEnumPacketCodec(final T[] values)
	{
		return createEnumPacketCodec(values, ByIdMap.OutOfBoundsStrategy.WRAP);
	}
	public static <T extends Enum<T>> StreamCodec<ByteBuf, T> createEnumPacketCodec(final T[] values, final ByIdMap.OutOfBoundsStrategy boundsStrategy)
	{
		final IntFunction<T> decoder = ByIdMap.continuous(Enum::ordinal, values, boundsStrategy);
		final ToIntFunction<T> encoder = Enum::ordinal;
		return ByteBufCodecs.idMapper(decoder, encoder);
	}
	public static <K, V> Codec<Object2ObjectOpenHashMap<K, V>> hashMapCodec(Codec<K> keyCodec, Codec<V> valueCodec)
	{
		return mapCodec(keyCodec, valueCodec, Object2ObjectOpenHashMap::new);
	}
	public static <K, V, M extends Map<K, V>> Codec<M> mapCodec(Codec<K> keyCodec, Codec<V> valueCodec, Supplier<M> mapCreator)
	{
		return new MapCodecNotToBeConfusedWithAMapCodec<>(keyCodec, valueCodec, mapCreator);
	}
	public static <E> Codec<ObjectArrayList<E>> arrayList(Codec<E> codec)
	{
		return collection(codec, ObjectArrayList::new);
	}
	public static <E, C extends Collection<E>> Codec<C> collection(Codec<E> codec, Supplier<C> collectionCreator)
	{
		return new CollectionCodec<>(codec, collectionCreator);
	}
	public static Codec<ResourceLocation> identifierCustomNamespace(String defaultNamespace)
	{
		return Codec.STRING.comapFlatMap(id -> validateId(id, defaultNamespace), ResourceLocation::toString).stable();
	}
	public static StreamCodec<ByteBuf, ResourceLocation> identifierCustomNamespaceStreamCodec(String defaultNamespace)
	{
		return ByteBufCodecs.STRING_UTF8.map(id -> parse(id, defaultNamespace), ResourceLocation::toString);
	}
	private static DataResult<ResourceLocation> validateId(String id, String defaultNamespace)
	{
		try
		{
			return DataResult.success(parse(id, defaultNamespace));
		}
		catch (ResourceLocationException var2)
		{
			return DataResult.error(() -> "Not a valid resource location: " + id + " " + var2.getMessage());
		}
	}
	private static @NotNull ResourceLocation parse(String id, String defaultNamespace)
	{
		int i = id.indexOf(':');
		if (i >= 0)
		{
			String path = id.substring(i + 1);
			if (i != 0)
			{
				String namespace = id.substring(0, i);
				return ResourceLocation.fromNamespaceAndPath(namespace, path);
			}
			else
			{
				return ResourceLocation.fromNamespaceAndPath(defaultNamespace, path);
			}
		}
		
		return ResourceLocation.fromNamespaceAndPath(defaultNamespace, id);
	}
	public static <R> DataResult<R> dataResultOfOptional(final R result, Supplier<String> errorMessage)
	{
		return Optional.ofNullable(result).map(DataResult::success).orElseGet(() -> DataResult.error(errorMessage));
	}
	public static <R> DataResult<R> dataResultOfOptional(final Optional<R> result, Supplier<String> errorMessage)
	{
		return result.map(DataResult::success).orElseGet(() -> DataResult.error(errorMessage));
	}
	public static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> streamCodecComposite(final StreamCodec<? super B, T1> codec1,
	                                                                                        final Function<C, T1> getter1,
	                                                                                        final StreamCodec<? super B, T2> codec2,
	                                                                                        final Function<C, T2> getter2,
	                                                                                        final StreamCodec<? super B, T3> codec3,
	                                                                                        final Function<C, T3> getter3,
	                                                                                        final StreamCodec<? super B, T4> codec4,
	                                                                                        final Function<C, T4> getter4,
	                                                                                        final StreamCodec<? super B, T5> codec5,
	                                                                                        final Function<C, T5> getter5,
	                                                                                        final StreamCodec<? super B, T6> codec6,
	                                                                                        final Function<C, T6> getter6,
	                                                                                        final StreamCodec<? super B, T7> codec7,
	                                                                                        final Function<C, T7> getter7,
	                                                                                        final Function7<T1, T2, T3, T4, T5, T6, T7, C> factory)
	{
		return new StreamCodec<>()
		{
			public @NotNull C decode(@NotNull B buf)
			{
				T1 t1 = codec1.decode(buf);
				T2 t2 = codec2.decode(buf);
				T3 t3 = codec3.decode(buf);
				T4 t4 = codec4.decode(buf);
				T5 t5 = codec5.decode(buf);
				T6 t6 = codec6.decode(buf);
				T7 t7 = codec7.decode(buf);
				return factory.apply(t1, t2, t3, t4, t5, t6, t7);
			}
			public void encode(@NotNull B buf, @NotNull C input)
			{
				codec1.encode(buf, getter1.apply(input));
				codec2.encode(buf, getter2.apply(input));
				codec3.encode(buf, getter3.apply(input));
				codec4.encode(buf, getter4.apply(input));
				codec5.encode(buf, getter5.apply(input));
				codec6.encode(buf, getter6.apply(input));
				codec7.encode(buf, getter7.apply(input));
			}
		};
	}
	public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8> StreamCodec<B, C> streamCodecComposite(final StreamCodec<? super B, T1> codec1,
	                                                                                            final Function<C, T1> getter1,
	                                                                                            final StreamCodec<? super B, T2> codec2,
	                                                                                            final Function<C, T2> getter2,
	                                                                                            final StreamCodec<? super B, T3> codec3,
	                                                                                            final Function<C, T3> getter3,
	                                                                                            final StreamCodec<? super B, T4> codec4,
	                                                                                            final Function<C, T4> getter4,
	                                                                                            final StreamCodec<? super B, T5> codec5,
	                                                                                            final Function<C, T5> getter5,
	                                                                                            final StreamCodec<? super B, T6> codec6,
	                                                                                            final Function<C, T6> getter6,
	                                                                                            final StreamCodec<? super B, T7> codec7,
	                                                                                            final Function<C, T7> getter7,
	                                                                                            final StreamCodec<? super B, T8> codec8,
	                                                                                            final Function<C, T8> getter8,
	                                                                                            final Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> factory)
	{
		return new StreamCodec<>()
		{
			public @NotNull C decode(@NotNull B buf)
			{
				T1 t1 = codec1.decode(buf);
				T2 t2 = codec2.decode(buf);
				T3 t3 = codec3.decode(buf);
				T4 t4 = codec4.decode(buf);
				T5 t5 = codec5.decode(buf);
				T6 t6 = codec6.decode(buf);
				T7 t7 = codec7.decode(buf);
				T8 t8 = codec8.decode(buf);
				return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8);
			}
			public void encode(@NotNull B buf, @NotNull C input)
			{
				codec1.encode(buf, getter1.apply(input));
				codec2.encode(buf, getter2.apply(input));
				codec3.encode(buf, getter3.apply(input));
				codec4.encode(buf, getter4.apply(input));
				codec5.encode(buf, getter5.apply(input));
				codec6.encode(buf, getter6.apply(input));
				codec7.encode(buf, getter7.apply(input));
				codec8.encode(buf, getter8.apply(input));
			}
		};
	}
	public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9> StreamCodec<B, C> streamCodecComposite(final StreamCodec<? super B, T1> codec1,
	                                                                                                final Function<C, T1> getter1,
	                                                                                                final StreamCodec<? super B, T2> codec2,
	                                                                                                final Function<C, T2> getter2,
	                                                                                                final StreamCodec<? super B, T3> codec3,
	                                                                                                final Function<C, T3> getter3,
	                                                                                                final StreamCodec<? super B, T4> codec4,
	                                                                                                final Function<C, T4> getter4,
	                                                                                                final StreamCodec<? super B, T5> codec5,
	                                                                                                final Function<C, T5> getter5,
	                                                                                                final StreamCodec<? super B, T6> codec6,
	                                                                                                final Function<C, T6> getter6,
	                                                                                                final StreamCodec<? super B, T7> codec7,
	                                                                                                final Function<C, T7> getter7,
	                                                                                                final StreamCodec<? super B, T8> codec8,
	                                                                                                final Function<C, T8> getter8,
	                                                                                                final StreamCodec<? super B, T9> codec9,
	                                                                                                final Function<C, T9> getter9,
	                                                                                                final Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, C> factory)
	{
		return new StreamCodec<>()
		{
			public @NotNull C decode(@NotNull B buf)
			{
				T1 t1 = codec1.decode(buf);
				T2 t2 = codec2.decode(buf);
				T3 t3 = codec3.decode(buf);
				T4 t4 = codec4.decode(buf);
				T5 t5 = codec5.decode(buf);
				T6 t6 = codec6.decode(buf);
				T7 t7 = codec7.decode(buf);
				T8 t8 = codec8.decode(buf);
				T9 t9 = codec9.decode(buf);
				return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
			}
			public void encode(@NotNull B buf, @NotNull C input)
			{
				codec1.encode(buf, getter1.apply(input));
				codec2.encode(buf, getter2.apply(input));
				codec3.encode(buf, getter3.apply(input));
				codec4.encode(buf, getter4.apply(input));
				codec5.encode(buf, getter5.apply(input));
				codec6.encode(buf, getter6.apply(input));
				codec7.encode(buf, getter7.apply(input));
				codec8.encode(buf, getter8.apply(input));
				codec9.encode(buf, getter9.apply(input));
			}
		};
	}
	public static <B extends ByteBuf, I> StreamCodec.CodecOperation<B, I, I[]> arrayOf(IntFunction<I[]> arrayCreator)
	{
		return (codec) -> new StreamCodec<>()
		{
			@Override
			public I @NotNull [] decode(@NotNull B buf)
			{
				int size = VarInt.read(buf);
				I[] array = arrayCreator.apply(size);
				for (int i = 0; i < size; i++)
				{
					array[i] = codec.decode(buf);
				}
				
				return array;
			}
			@Override
			public void encode(@NotNull B buf, I @NotNull [] array)
			{
				VarInt.write(buf, array.length);
				for (I value : array)
				{
					codec.encode(buf, value);
				}
			}
		};
	}
	public static class Codecs
	{
		public static final StreamCodec<ByteBuf, Instant> INSTANT_STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_LONG, Instant::getEpochSecond,
			ByteBufCodecs.VAR_INT, Instant::getNano,
			Instant::ofEpochSecond
		);
		public static final Codec<ResourceLocation> SPLATCRAFT_IDENTIFIER_CODEC = identifierCustomNamespace(Splatcraft.MODID);
		public static final StreamCodec<ByteBuf, ResourceLocation> SPLATCRAFT_IDENTIFIER_STREAM_CODEC = identifierCustomNamespaceStreamCodec(Splatcraft.MODID);
		public static final StreamCodec<ByteBuf, InteractionHand> PACKET_HAND = new StreamCodec<>()
		{
			@Override
			public @NotNull InteractionHand decode(ByteBuf buf)
			{
				return buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
			}
			@Override
			public void encode(ByteBuf buf, @NotNull InteractionHand value)
			{
				buf.writeBoolean(Objects.equals(value, InteractionHand.MAIN_HAND));
			}
		};
		public static final Codec<InteractionHand> HAND_CODEC = new Codec<>()
		{
			@Override
			public <T> DataResult<Pair<InteractionHand, T>> decode(DynamicOps<T> ops, T input)
			{
				DataResult<Boolean> result = ops.getBooleanValue(input);
				if (result.isSuccess())
					return DataResult.success(Pair.of(result.getOrThrow() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, input));
				return DataResult.error(() -> "Invalid input.");
			}
			@Override
			public <T> DataResult<T> encode(InteractionHand input, DynamicOps<T> ops, T prefix)
			{
				return DataResult.success(ops.createBoolean(Objects.equals(input, InteractionHand.MAIN_HAND)));
			}
		};
		public static final Codec<Vec2> VEC_2_CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.FLOAT.fieldOf("x").forGetter(v -> v.x),
			Codec.FLOAT.fieldOf("y").forGetter(v -> v.y)
		).apply(inst, Vec2::new));
		public static final Codec<Vector2f> VECTOR2F_CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.FLOAT.fieldOf("x").forGetter(v -> v.x),
			Codec.FLOAT.fieldOf("y").forGetter(v -> v.y)
		).apply(inst, Vector2f::new));
		public static final Codec<Vector2f> VECTOR2F_LIST_CODEC = Codec.list(Codec.FLOAT, 2, 2).xmap(v -> new Vector2f(v.get(0), v.get(1)), v -> List.of(v.x(), v.y()));
		public static final Codec<Vector2f> VECTOR2F_SINGLE_NUMBER_CODEC = Codec.FLOAT.xmap(Vector2f::new, Vector2f::x);
		public static final StreamCodec<ByteBuf, Vec3> VEC_3_STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.DOUBLE, Vec3::x,
			ByteBufCodecs.DOUBLE, Vec3::y,
			ByteBufCodecs.DOUBLE, Vec3::z,
			Vec3::new);
		public static final Codec<Vector2f> VECTOR2_MULTI_CODEC = Codec.withAlternative(VECTOR2F_CODEC, Codec.withAlternative(VECTOR2F_LIST_CODEC, VECTOR2F_SINGLE_NUMBER_CODEC));
		public static final StreamCodec<ByteBuf, Vector2f> VECTOR2F_STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, Vector2f::x,
			ByteBufCodecs.FLOAT, Vector2f::y,
			Vector2f::new);
	}
	public static final class MapCodecNotToBeConfusedWithAMapCodec<K, V, M extends Map<K, V>> implements Codec<M>
	{
		private final Codec<K> keyCodec;
		private final Codec<V> elementCodec;
		private final Supplier<M> mapCreator;
		private final Supplier<String> classNameSupplier;
		public MapCodecNotToBeConfusedWithAMapCodec(Codec<K> keyCodec, Codec<V> elementCodec, Supplier<M> mapCreator)
		{
			this.keyCodec = keyCodec;
			this.elementCodec = elementCodec;
			this.mapCreator = mapCreator;
			classNameSupplier = Suppliers.memoize(() -> mapCreator.get().getClass().getSimpleName());
		}
		@Override
		public <T> DataResult<Pair<M, T>> decode(final DynamicOps<T> ops, final T input)
		{
			return ops.getMap(input).setLifecycle(Lifecycle.stable()).flatMap(map -> decode(ops, map)).map(r -> Pair.of(r, input));
		}
		@Override
		public <T> DataResult<T> encode(final M input, final DynamicOps<T> ops, final T prefix)
		{
			return encode(input, ops, ops.mapBuilder()).build(prefix);
		}
		@Override
		public String toString()
		{
			return classNameSupplier.get() + "Codec[" + keyCodec + " -> " + elementCodec + ']';
		}
		private <T> DataResult<M> decode(final DynamicOps<T> ops, final MapLike<T> input)
		{
			final M read = mapCreator.get();
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
		<T> RecordBuilder<T> encode(final M input, final DynamicOps<T> ops, final RecordBuilder<T> prefix)
		{
			for (final Map.Entry<K, V> entry : input.entrySet())
			{
				prefix.add(keyCodec().encodeStart(ops, entry.getKey()), elementCodec().encodeStart(ops, entry.getValue()));
			}
			return prefix;
		}
		public Codec<K> keyCodec()
		{
			return keyCodec;
		}
		public Codec<V> elementCodec()
		{
			return elementCodec;
		}
		public Supplier<M> mapCreator()
		{
			return mapCreator;
		}
		@Override
		public boolean equals(Object obj)
		{
			if (obj == this) return true;
			if (obj == null || obj.getClass() != getClass()) return false;
			var that = (MapCodecNotToBeConfusedWithAMapCodec) obj;
			return Objects.equals(keyCodec, that.keyCodec) &&
				Objects.equals(elementCodec, that.elementCodec) &&
				Objects.equals(mapCreator, that.mapCreator);
		}
		@Override
		public int hashCode()
		{
			return Objects.hash(keyCodec, elementCodec, mapCreator);
		}
	}
	public static final class CollectionCodec<E, C extends Collection<E>> implements Codec<C>
	{
		private final Codec<E> elementCodec;
		private final Supplier<C> collectionCreator;
		private final Supplier<String> classNameSupplier;
		public CollectionCodec(Codec<E> elementCodec, Supplier<C> collectionCreator)
		{
			this.elementCodec = elementCodec;
			this.collectionCreator = collectionCreator;
			classNameSupplier = Suppliers.memoize(() -> collectionCreator.get().getClass().getSimpleName());
		}
		@Override
		public <T> DataResult<T> encode(final C input, final DynamicOps<T> ops, final T prefix)
		{
			final ListBuilder<T> builder = ops.listBuilder();
			for (final E element : input)
			{
				builder.add(elementCodec.encodeStart(ops, element));
			}
			return builder.build(prefix);
		}
		@Override
		public <T> DataResult<Pair<C, T>> decode(final DynamicOps<T> ops, final T input)
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
			return classNameSupplier.get() + "Codec[" + elementCodec + ']';
		}
		public Codec<E> elementCodec()
		{
			return elementCodec;
		}
		public Supplier<C> collectionCreator()
		{
			return collectionCreator;
		}
		@Override
		public boolean equals(Object obj)
		{
			if (obj == this) return true;
			if (obj == null || obj.getClass() != getClass()) return false;
			var that = (CollectionCodec) obj;
			return Objects.equals(elementCodec, that.elementCodec) &&
				Objects.equals(collectionCreator, that.collectionCreator);
		}
		@Override
		public int hashCode()
		{
			return Objects.hash(elementCodec, collectionCreator);
		}
		private class DecoderState<T>
		{
			private static final DataResult<Unit> INITIAL_RESULT = DataResult.success(Unit.INSTANCE, Lifecycle.stable());
			private final DynamicOps<T> ops;
			private final C elements = collectionCreator.get();
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
			public DataResult<Pair<C, T>> build()
			{
				final T errors = ops.createList(failed.build());
				final Pair<C, T> pair = Pair.of(elements, errors);
				return result.map(ignored -> pair).setPartial(pair);
			}
		}
	}
	public static class MissingProducts
	{
		public static <F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> P10<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> and(P2<F, T1, T2> p2, P8<F, T3, T4, T5, T6, T7, T8, T9, T10> p8)
		{
			return new com.mojang.datafixers.Products.P10<>(p2.t1(),
				p2.t2(),
				p8.t1(),
				p8.t2(),
				p8.t3(),
				p8.t4(),
				p8.t5(),
				p8.t6(),
				p8.t7(),
				p8.t8());
		}
		public static <F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9> Products.P9<F, T1, T2, T3, T4, T5, T6, T7, T8, T9> and(Products.P4<F, T1, T2, T3, T4> p4, Products.P5<F, T5, T6, T7, T8, T9> p5)
		{
			return new Products.P9<>(p4.t1(),
				p4.t2(),
				p4.t3(),
				p4.t4(),
				p5.t1(),
				p5.t2(),
				p5.t3(),
				p5.t4(),
				p5.t5()
			);
		}
		public static <F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Products.P10<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> and(Products.P4<F, T1, T2, T3, T4> p4, Products.P6<F, T5, T6, T7, T8, T9, T10> p6)
		{
			return new Products.P10<>(p4.t1(),
				p4.t2(),
				p4.t3(),
				p4.t4(),
				p6.t1(),
				p6.t2(),
				p6.t3(),
				p6.t4(),
				p6.t5(),
				p6.t6()
			);
		}
		public static <F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Products.P11<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> and(Products.P4<F, T1, T2, T3, T4> p4, Products.P7<F, T5, T6, T7, T8, T9, T10, T11> p7)
		{
			return new Products.P11<>(p4.t1(),
				p4.t2(),
				p4.t3(),
				p4.t4(),
				p7.t1(),
				p7.t2(),
				p7.t3(),
				p7.t4(),
				p7.t5(),
				p7.t6(),
				p7.t7()
			);
		}
	}
}
