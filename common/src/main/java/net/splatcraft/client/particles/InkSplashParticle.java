package net.splatcraft.client.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkSplashParticle extends TextureSheetParticle
{
	private final SpriteSet spriteProvider;
	public InkSplashParticle(ClientLevel level, double x, double y, double z, double motionX, double motionY, double motionZ, InkSplashParticleData data, SpriteSet sprite)
	{
		super(level, x, y, z);
		
		xd = motionX;
		yd = motionY;
		zd = motionZ;
		
		rCol = Math.max(0.018f, data.getRed() - 0.018f);
		gCol = Math.max(0.018f, data.getGreen() - 0.018f);
		bCol = Math.max(0.018f, data.getBlue() - 0.018f);
		
		quadSize = (random.nextFloat() * 0.5F + 0.5F) * 0.66f * data.getScale();
		gravity = 0;
		lifetime = 5;
		
		spriteProvider = sprite;
		setSpriteFromAge(sprite);
	}
	@Override
	public void tick()
	{
		super.tick();
		if (level.getBlockState(new BlockPos((int) x, (int) y, (int) z)).liquid())
		{
			remove();
		}
		else
		{
			setSpriteFromAge(spriteProvider);
		}
	}
	@Override
	public @NotNull ParticleRenderType getRenderType()
	{
		return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}
	@Override
	public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks)
	{
		boolean firstPerson = Minecraft.getInstance().options.getCameraType().isFirstPerson();
		if (firstPerson)
		{
			double dist = squaredDistanceTo(camera.getPosition(), partialTicks);
			if (dist < 4)
			{
				if (dist < 1)
					return;
				setAlpha((float) (Math.sqrt(dist) - 1));
				super.render(buffer, camera, partialTicks);
			}
			else
				super.render(buffer, camera, partialTicks);
		}
		else
			super.render(buffer, camera, partialTicks);
	}
	protected double squaredDistanceTo(Vec3 pos, float partialTicks)
	{
		Vec3 particlePos = new Vec3(Mth.lerp(partialTicks, xo, x), Mth.lerp(partialTicks, yo, y), Mth.lerp(partialTicks, zo, z));
		
		return pos.distanceToSqr(particlePos);
	}
	@OnlyIn(Dist.CLIENT)
	public static class Factory implements ParticleProvider<InkSplashParticleData>
	{
		private final SpriteSet spriteSet;
		public Factory(SpriteSet sprite)
		{
			spriteSet = sprite;
		}
		@Nullable
		@Override
		public Particle createParticle(@NotNull InkSplashParticleData typeIn, @NotNull ClientLevel levelIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
		{
			return new InkSplashParticle(levelIn, x, y, z, xSpeed, ySpeed, zSpeed, typeIn, spriteSet);
		}
	}
}
