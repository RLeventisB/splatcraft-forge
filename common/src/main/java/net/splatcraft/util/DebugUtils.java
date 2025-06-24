package net.splatcraft.util;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.PrimitiveCodec;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.util.StringRepresentable;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class DebugUtils
{
	public static <T, B> String dumpCodecFormat(Codec<T> codec)
	{
		if (codec instanceof MapCodec.MapCodecCodec<T> mapCodecCodec)
		{
			RecordBuilder<JsonElement> codecFormatMap = processCodec(mapCodecCodec.codec());
			JsonElement result = codecFormatMap.build(JsonOps.INSTANCE.empty()).getOrThrow();

			final Gson gson = new GsonBuilder().setPrettyPrinting().create();
			return gson.toJson(result);
		}

		return "";
	}
	private static RecordBuilder<JsonElement> processCodec(MapCodec<?> mapCodec)
	{
		RecordBuilder<JsonElement> map = JsonOps.INSTANCE.mapBuilder();
		Optional<Object> builderOptional = getField("val$builder", mapCodec);
		builderOptional.ifPresent(builder ->
		{
			@NotNull List<CodecData> stringCodecPair = loopUntilDecoderHasName(builder);
			stringCodecPair.forEach(data ->
			{
				if (data.codec instanceof MapCodec.MapCodecCodec<?> mapCodecCodec)
				{
					map.add(data.fieldName, processCodec(mapCodecCodec.codec()).build(JsonOps.INSTANCE.empty()));
				}
				else
				{
					map.add(data.fieldName, JsonOps.INSTANCE.createString(data.toString()));
				}
			});
		});
		return map;
	}
	private static @NotNull List<CodecData> loopUntilDecoderHasName(Object obj)
	{
		Optional<Object> decoder = getField("decoder", obj);
		if (decoder.isPresent())
		{
			Optional<Object> name = getField("val$name", decoder.get());
			if (name.isEmpty())
				name = getField("name", decoder.get());
			// if a name exists, we are in an FieldDecoder which has a name and a codec! or an optional field codec
			if (name.isPresent())
			{
				String fieldName = null;
				Codec<?> codec = null;
				boolean optional = false;
				String extraDescription = "";

				if (name.get() instanceof String nameString) // optional field codec that has a special format for some reason (its because it doesnt have a fallback value)
				{
					Codec<?> actualCodec = (Codec<?>) getField("elementCodec", decoder.get()).get();
					fieldName = nameString;
					codec = actualCodec;
					optional = true;
				}
				else
				{
					Optional<Object> fieldNameOptional = getField("arg$2", name.get());
					Optional<Object> codecOptional = getField("arg$1", name.get());
					if (fieldNameOptional.isPresent() && codecOptional.isPresent())
					{
						fieldName = (String) fieldNameOptional.get();
						codec = (Codec<?>) codecOptional.get();
						optional = false;
					}
					else if (fieldNameOptional.isEmpty() && codecOptional.isPresent()) // probably an optional map codec
					{
						fieldNameOptional = getField("name", codecOptional.get());
						Optional<Object> defaultValue = getField("val$decoder", decoder.get()).flatMap(v -> getField("val$function", v)).flatMap(v -> getField("arg$1", v));
						if (fieldNameOptional.isPresent() && defaultValue.isPresent())
						{
							Codec<?> actualCodec = (Codec<?>) getField("elementCodec", codecOptional.get()).get();
							fieldName = (String) fieldNameOptional.get();
							codec = actualCodec;
							optional = true;
							extraDescription = "(Defaults to " + defaultValue.get() + ")";
						}
					}
				}
				if (fieldName == null)
					return List.of();

				return List.of(new CodecData(fieldName, codec, optional, extraDescription).sanitizeCodec());
			}
			else
			{
				return processCompactedData(decoder.get());
			}
		}
		return List.of();
	}
	private static @NotNull List<CodecData> processCompactedData(Object decoder)
	{
		// search for f1, f2, f3, f4, and function
		// f1, f2, f3, f4 are appended values, and function is another one that has a decoder and may follow the same format
		Optional<Object> what = getField("val$a", decoder);
		if (what.isPresent())
			return loopUntilDecoderHasName(what.get());
		else
		{
			ImmutableList.Builder<CodecData> adjacentPairs = ImmutableList.builder();
			Optional<Object> function = getField("val$function", decoder);
			if (function.isPresent())
			{
				@NotNull List<CodecData> foundForFunction = loopUntilDecoderHasName(function.get());
				if (foundForFunction.isEmpty()) // if this is true, the function is weird and has a this$0 variable that has the actual data
				{
					Optional<Object> thisOptional = getField("this$0", decoder);
					if (thisOptional.isPresent())
						adjacentPairs.addAll(processCompactedData(thisOptional.get()));
				}
				else
					adjacentPairs.addAll(foundForFunction);

				for (int i = 1; i < 5; i++)
				{
					Optional<Object> adjacentOptional = getField("val$f" + i, decoder);
					if (adjacentOptional.isPresent())
					{
						@NotNull List<CodecData> elements = loopUntilDecoderHasName(adjacentOptional.get());
						if (!elements.isEmpty())
							adjacentPairs.addAll(elements);
					}
				}
				return adjacentPairs.build();
			}
		}
		return List.of();
	}
	public static Optional<Object> getField(String name, Object obj)
	{
		try
		{
			Field declaredField = obj.getClass().getDeclaredField(name);
			return Optional.ofNullable(ReflectionUtil.getFieldValue(declaredField, obj));
		}
		catch (NoSuchFieldException e)
		{
			return Optional.empty();
		}
	}
	public record CodecData(String fieldName, Codec<?> codec, boolean optional, String extraDescription)
	{

		public CodecData sanitizeCodec()
		{
			if (codec instanceof PrimitiveCodec<?> ||
				codec instanceof StringRepresentable.StringRepresentableCodec<?> ||
				codec instanceof MapCodec.MapCodecCodec<?>)
			{
				return this;
			}
			// codecs that are xmapped or something similar go here
			// i have literally never used recursive codecs so idk how to process them btw
			Optional<Object> encoder = getField("val$encoder", codec);
			Optional<Object> decoder = getField("val$decoder", codec);
			if (encoder.isPresent() && decoder.isPresent())
			{
				for (CodecModDetectors detector : CodecModDetectors.values())
				{
					Pair<Codec<?>, String> result = detector.modAction.apply((Encoder<?>) encoder.get(), (Decoder<?>) decoder.get());
					if (result != null && result.getFirst() != null)
						return new CodecData(fieldName, result.getFirst(), optional, result.getSecond().isEmpty() ? extraDescription : extraDescription + ", " + result.getSecond());
				}
			}
			return this; // codec is modified but the validator is not implemented or smth
		}
		enum CodecModDetectors
		{
			RANGE((encoder, decoder) ->
			{
				Optional<Object> encoderFunction = getField("val$function", encoder);
				Optional<Object> decoderFunction = getField("val$function", decoder);
				Optional<Object> originalCodec = getField("this$0", decoder);
				if (encoderFunction.isPresent() && encoderFunction.equals(decoderFunction) && originalCodec.isPresent())
				{
/*
					Optional<Object> minInclusive = getField("arg$1", encoderFunction.get());
					Optional<Object> maxInclusive = getField("arg$2", encoderFunction.get());
					if (minInclusive.isEmpty() || maxInclusive.isEmpty())
					{
						return null;
					}
					if (!(minInclusive.get() instanceof Number minNumber) || !(maxInclusive.get() instanceof Number maxNumber))
					{
						return null;
					}

					return Pair.of((Codec<?>) originalCodec.get(), "[" + minInclusive + ", " + maxInclusive + "]");
*/

					return Pair.of((Codec<?>) originalCodec.get(), "");
				}
				return null;
			});
			private final BiFunction<Encoder<?>, Decoder<?>, Pair<Codec<?>, String>> modAction;
			CodecModDetectors(BiFunction<Encoder<?>, Decoder<?>, Pair<Codec<?>, String>> modAction)
			{
				this.modAction = modAction;
			}
			static boolean checkIfMethodsAreEqual(Object method1, Object method2)
			{
				return Arrays.equals(getMethodByteCode(method1), getMethodByteCode(method2));
			}
			static Integer[] getMethodByteCode(Object method)
			{
				IntArrayList list = new IntArrayList();
				ClassReader reader;
				try
				{

					String name = method.getClass().getName();
					int index = name.indexOf("$$");
					reader = new ClassReader(name.substring(0, index));
				}
				catch (IOException e)
				{
					return new Integer[0];
				}
				reader.accept(new ClassVisitor(Opcodes.ASM9)
				{
					@Override
					public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions)
					{
						return new MethodVisitor(Opcodes.ASM9)
						{
							@Override
							public void visitInsn(int opcode)
							{
								list.add(opcode);
								super.visitInsn(opcode);
							}
						};
					}
				}, ClassReader.EXPAND_FRAMES);
				return list.toArray(Integer[]::new);
			}
		}
		@Override
		public @NotNull String toString()
		{
			String codecText = null;
			if (codec instanceof StringRepresentable.EnumCodec<?> enumCodec)
			{
				Optional<Object> enumValues = getField("resolver", enumCodec).flatMap(v -> getField("arg$1", v));
				if (enumValues.isPresent() && enumValues.get() instanceof Enum<?>[] values)
					codecText = Arrays.toString(values);
			}
			if (codecText == null)
				codecText = codec.toString();
			if (optional)
				codecText = "[" + codecText + "]";
			if (!extraDescription.isEmpty())
				codecText = codecText + " " + extraDescription;
			return codecText;
		}
	}
}
