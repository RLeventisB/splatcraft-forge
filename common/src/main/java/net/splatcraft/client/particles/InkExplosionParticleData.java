package net.splatcraft.client.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.dynamic.Codecs;
import net.splatcraft.registries.SplatcraftParticleTypes;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

public class InkExplosionParticleData extends InkSplashParticleData
{
	public static final MapCodec<InkExplosionParticleData> CODEC = RecordCodecBuilder.mapCodec((instance) ->
		instance.group(
			Codec.FLOAT.fieldOf("r").forGetter(InkSplashParticleData::getRed),
			Codec.FLOAT.fieldOf("g").forGetter(InkSplashParticleData::getGreen),
			Codec.FLOAT.fieldOf("b").forGetter(InkSplashParticleData::getBlue),
			Codecs.POSITIVE_FLOAT.fieldOf("scale").forGetter(InkSplashParticleData::getScale)
		).apply(instance, InkExplosionParticleData::new));
	public static final PacketCodec<RegistryByteBuf, InkExplosionParticleData> PACKET_CODEC = PacketCodec.tuple(
		PacketCodecs.FLOAT, InkSplashParticleData::getRed,
		PacketCodecs.FLOAT, InkSplashParticleData::getGreen,
		PacketCodecs.FLOAT, InkSplashParticleData::getBlue,
		PacketCodecs.FLOAT, InkSplashParticleData::getScale,
		InkExplosionParticleData::new);
	public InkExplosionParticleData(InkColor color, float scale)
	{
		super(color, scale);
	}
	public InkExplosionParticleData(float red, float green, float blue, float scale)
	{
		super(red, green, blue, scale);
	}
	@Override
	public @NotNull ParticleType<?> getType()
	{
		return SplatcraftParticleTypes.INK_EXPLOSION;
	}
}
