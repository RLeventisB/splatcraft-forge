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
import net.splatcraft.registries.SplatcraftParticleTypes;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class InkTerrainParticleData implements ParticleOptions
{
	public static final MapCodec<InkTerrainParticleData> CODEC = RecordCodecBuilder.mapCodec((instance) ->
		instance.group(
			Codec.FLOAT.fieldOf("r").forGetter(InkTerrainParticleData::getRed),
			Codec.FLOAT.fieldOf("g").forGetter(InkTerrainParticleData::getGreen),
			Codec.FLOAT.fieldOf("b").forGetter(InkTerrainParticleData::getBlue)
		).apply(instance, InkTerrainParticleData::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, InkTerrainParticleData> PACKET_CODEC = StreamCodec.composite(
		ByteBufCodecs.FLOAT, InkTerrainParticleData::getRed,
		ByteBufCodecs.FLOAT, InkTerrainParticleData::getGreen,
		ByteBufCodecs.FLOAT, InkTerrainParticleData::getBlue,
		InkTerrainParticleData::new);
	protected final float red;
	protected final float green;
	protected final float blue;
	public InkTerrainParticleData(InkColor color)
	{
		this(ColorUtils.getColorLockedIfConfig(color).getRGB());
	}
	private InkTerrainParticleData(float[] rgb)
	{
		this(rgb[0], rgb[1], rgb[2]);
	}
	public InkTerrainParticleData(float red, float green, float blue)
	{
		this.red = red;
		this.green = green;
		this.blue = blue;
	}
	@Override
	public @NotNull ParticleType<?> getType()
	{
		return SplatcraftParticleTypes.INK_TERRAIN;
	}
	@Override
	public @NotNull String toString()
	{
		return String.format(Locale.ROOT, "%s %.2f %.2f %.2f", BuiltInRegistries.PARTICLE_TYPE.getResourceKey(getType()), red, green, blue);
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
}
