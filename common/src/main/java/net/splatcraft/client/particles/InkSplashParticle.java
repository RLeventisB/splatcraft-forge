package net.splatcraft.client.particles;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkSplashParticle extends SpriteBillboardParticle
{
	private final SpriteProvider spriteProvider;
	public InkSplashParticle(ClientWorld level, double x, double y, double z, double motionX, double motionY, double motionZ, InkSplashParticleData data, SpriteProvider sprite)
	{
		super(level, x, y, z);
		
		velocityX = motionX;
		velocityY = motionY;
		velocityZ = motionZ;
		
		red = Math.max(0.018f, data.getRed() - 0.018f);
		green = Math.max(0.018f, data.getGreen() - 0.018f);
		blue = Math.max(0.018f, data.getBlue() - 0.018f);
		
		scale = (random.nextFloat() * 0.5F + 0.5F) * 0.66f * data.getScale();
		gravityStrength = 0;
		maxAge = 5;
		
		spriteProvider = sprite;
		setSpriteForAge(sprite);
	}
	@Override
	public void tick()
	{
		super.tick();
		if (world.getBlockState(new BlockPos((int) x, (int) y, (int) z)).isLiquid())
		{
			markDead();
		}
		else
		{
			setSpriteForAge(spriteProvider);
		}
	}
	@Override
	public @NotNull ParticleTextureSheet getType()
	{
		return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
	}
	@Override
	public void buildGeometry(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks)
	{
		boolean firstPerson = MinecraftClient.getInstance().options.getPerspective().isFirstPerson();
		if (firstPerson)
		{
			double dist = squaredDistanceTo(camera.getPos(), partialTicks);
			if (dist < 4)
			{
				if (dist < 1)
					return;
				setAlpha((float) (Math.sqrt(dist) - 1));
				super.buildGeometry(buffer, camera, partialTicks);
			}
			else
				super.buildGeometry(buffer, camera, partialTicks);
		}
		else
			super.buildGeometry(buffer, camera, partialTicks);
	}
	protected double squaredDistanceTo(Vec3d pos, float partialTicks)
	{
		Vec3d particlePos = new Vec3d(MathHelper.lerp(partialTicks, prevPosX, x), MathHelper.lerp(partialTicks, prevPosY, y), MathHelper.lerp(partialTicks, prevPosZ, z));
		
		return pos.squaredDistanceTo(particlePos);
	}
	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<InkSplashParticleData>
	{
		private final SpriteProvider spriteSet;
		public Factory(SpriteProvider sprite)
		{
			spriteSet = sprite;
		}
		@Nullable
		@Override
		public Particle createParticle(@NotNull InkSplashParticleData typeIn, @NotNull ClientWorld levelIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
		{
			return new InkSplashParticle(levelIn, x, y, z, xSpeed, ySpeed, zSpeed, typeIn, spriteSet);
		}
	}
}
