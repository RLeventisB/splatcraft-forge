package net.splatcraft.client.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkCloudParticle extends TextureSheetParticle
{
	public static final int CLOUD_LIFE_TIME = 40;
	public InkCloudParticle(ClientLevel level, double x, double y, double z, double motionX, double motionY, double motionZ, InkCloudParticleData data, SpriteSet sprite)
	{
		super(level, x, y, z);
		
		xd = motionX;
		yd = motionY;
		zd = motionZ;
		
		rCol = data.getRed();
		gCol = data.getGreen();
		bCol = data.getBlue();
		
		quadSize = (random.nextFloat() * 0.2F + 0.8F) * data.getScale();
		gravity = 0;
		lifetime = CLOUD_LIFE_TIME;
		
		pickSprite(sprite);
	}
	@Override
	public @NotNull ParticleRenderType getRenderType()
	{
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}
	@Override
	public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks)
	{
		double dist = squaredDistanceTo(camera.getPosition(), partialTicks);
		float lifeTimeAlpha = Mth.square((age + partialTicks) / CLOUD_LIFE_TIME * 2 - 1f);
		lifeTimeAlpha *= Mth.square(lifeTimeAlpha); // stupid way to write x^6 however i dont want to do *= 5 times (and there is no Math.pow for floats! why.)
		lifeTimeAlpha = Mth.clamp(1 - lifeTimeAlpha, 0, 1);
		if (dist < 1f)
			return;
		
		if (dist < 5)
			setAlpha((((float) dist - 1f) / 4f) * lifeTimeAlpha);
		else
			setAlpha(lifeTimeAlpha);
		
		super.render(buffer, camera, partialTicks);
	}
	protected double squaredDistanceTo(Vec3 pos, float partialTicks)
	{
		Vec3 particlePos = new Vec3(Mth.lerp(partialTicks, xo, x), Mth.lerp(partialTicks, yo, y), Mth.lerp(partialTicks, zo, z));
		
		return pos.distanceToSqr(particlePos);
	}
	@OnlyIn(Dist.CLIENT)
	public static class Factory implements ParticleProvider<InkCloudParticleData>
	{
		private final SpriteSet spriteSet;
		public Factory(SpriteSet sprite)
		{
			spriteSet = sprite;
		}
		@Nullable
		@Override
		public Particle createParticle(@NotNull InkCloudParticleData data, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
		{
			return new InkCloudParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, data, spriteSet);
		}
	}
}
