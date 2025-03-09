package net.splatcraft.client.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
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
			ExtraCodecs.POSITIVE_FLOAT.fieldOf("scale").forGetter(InkSplashParticleData::getScale)
		).apply(instance, InkExplosionParticleData::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, InkExplosionParticleData> PACKET_CODEC = StreamCodec.composite(
		ByteBufCodecs.FLOAT, InkSplashParticleData::getRed,
		ByteBufCodecs.FLOAT, InkSplashParticleData::getGreen,
		ByteBufCodecs.FLOAT, InkSplashParticleData::getBlue,
		ByteBufCodecs.FLOAT, InkSplashParticleData::getScale,
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
