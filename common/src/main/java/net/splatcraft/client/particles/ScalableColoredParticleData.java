package net.splatcraft.client.particles;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.FastColor;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Collections;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public abstract class ScalableColoredParticleData implements ParticleOptions
{
	private final int color;
	private final float scale;
	private static final MapCodec<Integer> FROM_FLOAT_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
		Codec.FLOAT.fieldOf("red").forGetter(v -> FastColor.ARGB32.red(v) / 255f),
		Codec.FLOAT.fieldOf("green").forGetter(v -> FastColor.ARGB32.green(v) / 255f),
		Codec.FLOAT.fieldOf("blue").forGetter(v -> FastColor.ARGB32.blue(v) / 255f)
	).apply(inst, (r, g, b) -> FastColor.ARGB32.colorFromFloat(1f, r, g, b)));
	public static <T extends ScalableColoredParticleData> MapCodec<T> createCodec(BiFunction<Integer, Float, T> constructor)
	{
		return new MapCodec<>()
		{
			@Override
			public <T1> Stream<T1> keys(DynamicOps<T1> ops)
			{
				return Stream.of(
					ops.createString("r"),
					ops.createString("g"),
					ops.createString("b"),
					ops.createString("scale"),
					ops.createString("color")
				);
			}
			@Override
			public <T1> DataResult<T> decode(DynamicOps<T1> ops, MapLike<T1> input)
			{
				float scaleFinal;
				DataResult<Float> scaleResult = Codec.FLOAT.optionalFieldOf("scale", 1f).decode(ops, input);
				if (scaleResult.isSuccess())
				{
					scaleFinal = scaleResult.getOrThrow();
				}
				else
				{
					return scaleResult.map(v -> null);
				}
				
				int colorFinal;
				final MapCodec<Integer> colorCodec = Codec.mapEither(Codec.INT.fieldOf("color"), FROM_FLOAT_CODEC).xmap(Either::unwrap, Either::left);
				DataResult<Integer> colorResult = colorCodec.decode(ops, input);
				
				if (colorResult.isSuccess())
				{
					colorFinal = colorResult.getOrThrow();
				}
				else
				{
					return colorResult.map(v -> null);
				}
				
				return DataResult.success(constructor.apply(colorFinal, scaleFinal));
			}
			@Override
			public <T1> RecordBuilder<T1> encode(T input, DynamicOps<T1> ops, RecordBuilder<T1> prefix)
			{
				prefix.add("color", ops.createNumeric(input.getColor()));
				prefix.add("scale", ops.createNumeric(input.getScale()));
				return prefix;
			}
		};
	}
	public static <T extends ScalableColoredParticleData> StreamCodec<RegistryFriendlyByteBuf, T> createStreamCodec(BiFunction<Integer, Float, T> constructor)
	{
		return StreamCodec.composite(
			ByteBufCodecs.INT, ScalableColoredParticleData::getColor,
			ByteBufCodecs.FLOAT, ScalableColoredParticleData::getScale,
			constructor
		);
	}
	public ScalableColoredParticleData(InkColor color, float scale)
	{
		this(ColorUtils.getColorLockedIfConfig(color).getRGB(), scale);
	}
	public ScalableColoredParticleData(float[] rgb, float scale)
	{
		this(rgb[0], rgb[1], rgb[2], scale);
	}
	public ScalableColoredParticleData(float red, float green, float blue, float scale)
	{
		this(FastColor.ARGB32.colorFromFloat(1f, red, green, blue), scale);
	}
	public ScalableColoredParticleData(int color, float scale)
	{
		this.color = color;
		this.scale = scale;
	}
	public int getColor()
	{
		return color;
	}
	public float getRed()
	{
		return FastColor.ARGB32.red(color) / 255f;
	}
	public float getGreen()
	{
		return FastColor.ARGB32.green(color) / 255f;
	}
	public float getBlue()
	{
		return FastColor.ARGB32.blue(color) / 255f;
	}
	public float getScale()
	{
		return scale;
	}
	public String createFormattedString(Object... numberValues)
	{
		return String.format(Locale.ROOT, "%s " + String.join(" ", Collections.nCopies(numberValues.length, "%d")),
			ArrayUtils.addFirst(numberValues, BuiltInRegistries.PARTICLE_TYPE.getResourceKey(getType())));
	}
}
