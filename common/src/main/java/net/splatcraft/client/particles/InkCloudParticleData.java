package net.splatcraft.client.particles;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.splatcraft.registries.SplatcraftParticleTypes;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

public class InkCloudParticleData extends ScalableColoredParticleData
{
	public static final MapCodec<InkCloudParticleData> CODEC = createCodec(InkCloudParticleData::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, InkCloudParticleData> STREAM_CODEC = createStreamCodec(InkCloudParticleData::new);
	public InkCloudParticleData(InkColor color, float scale)
	{
		super(color, scale);
	}
	public InkCloudParticleData(int color, float scale)
	{
		super(color, scale);
	}
	public InkCloudParticleData(float red, float green, float blue, float scale)
	{
		super(red, green, blue, scale);
	}
	public InkCloudParticleData(float[] rgb, float scale)
	{
		super(rgb, scale);
	}
	@Override
	public @NotNull ParticleType<?> getType()
	{
		return SplatcraftParticleTypes.INK_CLOUD;
	}
	@Override
	public @NotNull String toString()
	{
		return createFormattedString(getColor(), getScale());
	}
}
