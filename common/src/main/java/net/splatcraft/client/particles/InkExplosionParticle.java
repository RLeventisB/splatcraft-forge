package net.splatcraft.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkExplosionParticle extends TextureSheetParticle
{
	private final SpriteSet spriteProvider;
	public InkExplosionParticle(ClientLevel level, double x, double y, double z, double motionX, double motionY, double motionZ, InkExplosionParticleData data, SpriteSet provider)
	{
		super(level, x, y, z, motionX, motionY, motionZ);
		
		xd = motionX;
		yd = motionY;
		zd = motionZ;
		
		setColor(Math.max(0.018f, data.getRed() - 0.018f),
			Math.max(0.018f, data.getGreen() - 0.018f),
			Math.max(0.018f, data.getBlue() - 0.018f));
		
		quadSize = 0.33F * (random.nextFloat() * 0.5F + 0.5F) * 2.0F * data.getScale();
		gravity = 0;
		lifetime = 6 + random.nextInt(4);
		
		spriteProvider = provider;
		setSpriteFromAge(provider);
	}
	@Override
	public void tick()
	{
		xo = x;
		yo = y;
		zo = z;
		if (age++ >= lifetime || level.getBlockState(new BlockPos((int) x, (int) y, (int) z)).liquid())
		{
			remove();
		}
		else
		{
			setSpriteFromAge(spriteProvider);
		}
	}
	@Override
	public ParticleRenderType getRenderType()
	{
		return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}
	@OnlyIn(Dist.CLIENT)
	public static class Factory implements ParticleProvider<InkExplosionParticleData>
	{
		private final SpriteSet provider;
		public Factory(SpriteSet sprite)
		{
			provider = sprite;
		}
		@Nullable
		@Override
		public Particle createParticle(@NotNull InkExplosionParticleData typeIn, @NotNull ClientLevel levelIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
		{
			return new InkExplosionParticle(levelIn, x, y, z, xSpeed, ySpeed, zSpeed, typeIn, provider);
		}
	}
}
