package net.splatcraft.client.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.splatcraft.registries.SplatcraftParticleTypes;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class InkTerrainParticleData implements ParticleEffect
{
	public static final MapCodec<InkTerrainParticleData> CODEC = RecordCodecBuilder.mapCodec((instance) ->
		instance.group(
			Codec.FLOAT.fieldOf("r").forGetter(InkTerrainParticleData::getRed),
			Codec.FLOAT.fieldOf("g").forGetter(InkTerrainParticleData::getGreen),
			Codec.FLOAT.fieldOf("b").forGetter(InkTerrainParticleData::getBlue)
		).apply(instance, InkTerrainParticleData::new));
	public static final PacketCodec<RegistryByteBuf, InkTerrainParticleData> PACKET_CODEC = PacketCodec.tuple(
		PacketCodecs.FLOAT, InkTerrainParticleData::getRed,
		PacketCodecs.FLOAT, InkTerrainParticleData::getGreen,
		PacketCodecs.FLOAT, InkTerrainParticleData::getBlue,
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
		return String.format(Locale.ROOT, "%s %.2f %.2f %.2f", Registries.PARTICLE_TYPE.getKey(getType()), red, green, blue);
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
