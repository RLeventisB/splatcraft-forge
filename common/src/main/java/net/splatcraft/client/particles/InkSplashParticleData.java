package net.splatcraft.client.particles;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.splatcraft.registries.SplatcraftParticleTypes;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

public class InkSplashParticleData extends ScalableColoredParticleData
{
	public static final MapCodec<InkSplashParticleData> CODEC = createCodec(InkSplashParticleData::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, InkSplashParticleData> STREAM_CODEC = createStreamCodec(InkSplashParticleData::new);
	public InkSplashParticleData(InkColor color, float scale)
	{
		super(color, scale);
	}
	public InkSplashParticleData(int color, float scale)
	{
		super(color, scale);
	}
	public InkSplashParticleData(float red, float green, float blue, float scale)
	{
		super(red, green, blue, scale);
	}
	public InkSplashParticleData(float[] rgb, float scale)
	{
		super(rgb, scale);
	}
	@Override
	public @NotNull ParticleType<?> getType()
	{
		return SplatcraftParticleTypes.INK_SPLASH;
	}
	@Override
	public @NotNull String toString()
	{
		return createFormattedString(getColor(), getScale());
	}
}
