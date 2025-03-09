package net.splatcraft.util;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec2;
import net.splatcraft.Splatcraft;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
		final IntFunction<T> decoder = (index) -> values.get()[index];
		final ToIntFunction<T> encoder = Enum::ordinal;
		return ByteBufCodecs.idMapper(decoder, encoder);
	}
	public static <K, V> Codec<Object2ObjectOpenHashMap<K, V>> hashMapCodec(Codec<K> keyCodec, Codec<V> valueCodec)
	{
		return hashMapCodec(keyCodec, valueCodec, false);
	}
	public static <K, V> Codec<Object2ObjectOpenHashMap<K, V>> hashMapCodec(Codec<K> keyCodec, Codec<V> valueCodec, boolean unsafeConvert)
	{
		return mapCodec(keyCodec, valueCodec, Object2ObjectOpenHashMap::new, unsafeConvert);
	}
	public static <K, V, M extends Map<K, V>> Codec<M> mapCodec(Codec<K> keyCodec, Codec<V> valueCodec, Supplier<M> mapCreator)
	{
		return mapCodec(keyCodec, valueCodec, mapCreator, true);
	}
	public static <K, V, M extends Map<K, V>> Codec<M> mapCodec(Codec<K> keyCodec, Codec<V> valueCodec, Supplier<M> mapCreator, boolean unsafeConvert)
	{
		return new MapCodecNotToBeConfusedWithAMapCodec<>(keyCodec, valueCodec, mapCreator, unsafeConvert);
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
	private static DataResult<ResourceLocation> validateId(String id, String defaultNamespace)
	{
		try
		{
			int i = id.indexOf(':');
			if (i >= 0)
			{
				String path = id.substring(i + 1);
				if (i != 0)
				{
					String namespace = id.substring(0, i);
					return DataResult.success(ResourceLocation.fromNamespaceAndPath(namespace, path));
				}
				else
				{
					return DataResult.success(ResourceLocation.fromNamespaceAndPath(defaultNamespace, path));
				}
			}
			
			return DataResult.success(ResourceLocation.fromNamespaceAndPath(defaultNamespace, id));
		}
		catch (ResourceLocationException var2)
		{
			return DataResult.error(() -> "Not a valid resource location: " + id + " " + var2.getMessage());
		}
	}
	public static class Codecs
	{
		public static final StreamCodec<ByteBuf, Instant> INSTANT_PACKET_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_LONG, Instant::getEpochSecond,
			ByteBufCodecs.VAR_INT, Instant::getNano,
			Instant::ofEpochSecond
		);
		public static final Codec<ResourceLocation> SPLATCRAFT_IDENTIFIER_CODEC = identifierCustomNamespace(Splatcraft.MODID);
		public static final StreamCodec<ByteBuf, InteractionHand> PACKET_HAND = new StreamCodec<>()
		{
			@Override
			public InteractionHand decode(ByteBuf buf)
			{
				return buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
			}
			@Override
			public void encode(ByteBuf buf, InteractionHand value)
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
	}
	public static final class MapCodecNotToBeConfusedWithAMapCodec<K, V, M extends Map<K, V>> implements Codec<M>
	{
		private final Codec<K> keyCodec;
		private final Codec<V> elementCodec;
		private final Supplier<M> mapCreator;
		private final Supplier<String> classNameSupplier;
		private final boolean unsafeConvert;
		public MapCodecNotToBeConfusedWithAMapCodec(Codec<K> keyCodec, Codec<V> elementCodec, Supplier<M> mapCreator, boolean unsafeConvert)
		{
			this.keyCodec = keyCodec;
			this.elementCodec = elementCodec;
			this.mapCreator = mapCreator;
			classNameSupplier = Suppliers.memoize(() -> mapCreator.get().getClass().getSimpleName());
			this.unsafeConvert = unsafeConvert;
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
			return classNameSupplier.get() + "Codec[" + keyCodec + " -> " + elementCodec + (unsafeConvert ? ", unsafeConvert" : "") + ']';
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
}
