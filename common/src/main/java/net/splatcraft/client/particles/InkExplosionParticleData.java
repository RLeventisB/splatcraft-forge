package net.splatcraft.client.particles;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.splatcraft.registries.SplatcraftParticleTypes;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

public class InkExplosionParticleData extends ScalableColoredParticleData
{
	public static final MapCodec<InkExplosionParticleData> CODEC = createCodec(InkExplosionParticleData::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, InkExplosionParticleData> STREAM_CODEC = createStreamCodec(InkExplosionParticleData::new);
	public InkExplosionParticleData(InkColor color, float scale)
	{
		super(color, scale);
	}
	public InkExplosionParticleData(int color, float scale)
	{
		super(color, scale);
	}
	public InkExplosionParticleData(float red, float green, float blue, float scale)
	{
		super(red, green, blue, scale);
	}
	public InkExplosionParticleData(float[] rgb, float scale)
	{
		super(rgb, scale);
	}
	@Override
	public @NotNull ParticleType<?> getType()
	{
		return SplatcraftParticleTypes.INK_EXPLOSION;
	}
	@Override
	public @NotNull String toString()
	{
		return createFormattedString(getColor(), getScale());
	}
}
