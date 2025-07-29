package net.splatcraft.client.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.splatcraft.registries.SplatcraftParticleTypes;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class InkHitParticleData implements ParticleOptions
{
	public static final MapCodec<InkHitParticleData> CODEC = RecordCodecBuilder.mapCodec((instance) ->
		instance.group(
			Codec.INT.fieldOf("color").forGetter(InkHitParticleData::getColor),
			Codec.FLOAT.fieldOf("scale").forGetter(InkHitParticleData::getScale)
		).apply(instance, InkHitParticleData::new));
	public static final StreamCodec<ByteBuf, InkHitParticleData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.INT, InkHitParticleData::getColor,
		ByteBufCodecs.FLOAT, InkHitParticleData::getScale,
		InkHitParticleData::new);
	protected final int color;
	protected final float scale;
	public InkHitParticleData(InkColor color, float scale)
	{
		this(ColorUtils.getColorLockedIfConfig(color).getColor(), scale);
	}
	public InkHitParticleData(int color, float scale)
	{
		this.color = color;
		this.scale = scale;
	}
	@Override
	public @NotNull ParticleType<?> getType()
	{
		return SplatcraftParticleTypes.INK_HIT;
	}
	@Override
	public @NotNull String toString()
	{
		return String.format(Locale.ROOT, "%s %d", BuiltInRegistries.PARTICLE_TYPE.getResourceKey(getType()), color);
	}
	public int getColor()
	{
		return color;
	}
	public float getScale()
	{
		return scale;
	}
}
