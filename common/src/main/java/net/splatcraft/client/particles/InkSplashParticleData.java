package net.splatcraft.client.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.splatcraft.registries.SplatcraftParticleTypes;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class InkSplashParticleData implements ParticleOptions
{
	public static final MapCodec<InkSplashParticleData> CODEC = RecordCodecBuilder.mapCodec((instance) ->
		instance.group(
			Codec.FLOAT.fieldOf("r").forGetter(InkSplashParticleData::getRed),
			Codec.FLOAT.fieldOf("g").forGetter(InkSplashParticleData::getGreen),
			Codec.FLOAT.fieldOf("b").forGetter(InkSplashParticleData::getBlue),
			ExtraCodecs.POSITIVE_FLOAT.fieldOf("scale").forGetter(InkSplashParticleData::getScale)
		).apply(instance, InkSplashParticleData::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, InkSplashParticleData> PACKET_CODEC = StreamCodec.composite(
		ByteBufCodecs.FLOAT, InkSplashParticleData::getRed,
		ByteBufCodecs.FLOAT, InkSplashParticleData::getGreen,
		ByteBufCodecs.FLOAT, InkSplashParticleData::getBlue,
		ByteBufCodecs.FLOAT, InkSplashParticleData::getScale,
		InkSplashParticleData::new);
	protected final float red;
	protected final float green;
	protected final float blue;
	protected final float scale;
	public InkSplashParticleData(InkColor color, float scale)
	{
		this(ColorUtils.getColorLockedIfConfig(color).getRGB(), scale);
	}
	private InkSplashParticleData(float[] rgb, float scale)
	{
		this(rgb[0], rgb[1], rgb[2], scale);
	}
	public InkSplashParticleData(float red, float green, float blue, float scale)
	{
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.scale = scale;
	}
	@Override
	public @NotNull ParticleType<?> getType()
	{
		return SplatcraftParticleTypes.INK_SPLASH;
	}
	@Override
	public @NotNull String toString()
	{
		return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f", BuiltInRegistries.PARTICLE_TYPE.getResourceKey(getType()), red, green, blue, scale);
	}
	public float getRed()
	{
		return red;
	}
	public float getGreen()
	{
		return green;
	}
	public float getBlue()
	{
		return blue;
	}
	public float getScale()
	{
		return scale;
	}
}
