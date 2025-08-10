package net.splatcraft.client.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.client.renderer.SplatcraftRenderTypes;
import net.splatcraft.util.GraphicsUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class InkHitParticle extends TextureSheetParticle
{
	private final SpriteSet spriteProvider;
	public InkHitParticle(ClientLevel level, double x, double y, double z, int color, float scale, SpriteSet provider)
	{
		super(level, x, y, z, 0, 0, 0);
		
		rCol = FastColor.ARGB32.red(color) / 255f;
		gCol = FastColor.ARGB32.green(color) / 255f;
		bCol = FastColor.ARGB32.blue(color) / 255f;
		gravity = 0;
		lifetime = 5;
		roll = level.random.nextFloat() * Mth.TWO_PI;
		oRoll = roll;
		quadSize *= scale;
		
		spriteProvider = provider;
		setSpriteFromAge(provider);
	}
	@Override
	public void tick()
	{
		xo = x;
		yo = y;
		zo = z;
		if (age++ >= lifetime || level.getBlockState(BlockPos.containing(x, y, z)).liquid())
		{
			remove();
		}
		else
		{
			setSpriteFromAge(spriteProvider);
		}
	}
	@Override
	protected void renderRotatedQuad(@NotNull VertexConsumer buffer, @NotNull Quaternionf quaternion, float x, float y, float z, float partialTicks)
	{
		Matrix4f projectionMatrix = GraphicsUtils.getProjectionMatrix(partialTicks);
		Vector4f screenPos = GraphicsUtils.relativeWorldToScreenSpace(new Vec3(x, y, z), projectionMatrix);
		// todo: properly learn matrices and only get the w component (which represents the scale
		// dependant on the distance) instead of calculating every component
		
		float size = getQuadSize(partialTicks) * Mth.sqrt(screenPos.w);
		float u0 = getU0();
		float u1 = getU1();
		float v0 = getV0();
		float v1 = getV1();
		int packedLight = getLightColor(partialTicks);
		renderVertex(buffer, quaternion, x, y, z, 1.0F, -1.0F, size, u1, v1, packedLight);
		renderVertex(buffer, quaternion, x, y, z, 1.0F, 1.0F, size, u1, v0, packedLight);
		renderVertex(buffer, quaternion, x, y, z, -1.0F, 1.0F, size, u0, v0, packedLight);
		renderVertex(buffer, quaternion, x, y, z, -1.0F, -1.0F, size, u0, v1, packedLight);
	}
	private void renderVertex(VertexConsumer buffer, Quaternionf quaternion, float x, float y, float z, float xOffset, float yOffset, float quadSize, float u, float v, int packedLight)
	{
		Vector3f vector3f = (new Vector3f(xOffset, yOffset, 0.0F)).rotate(quaternion).mul(quadSize).add(x, y, z);
		buffer.addVertex(vector3f.x(), vector3f.y(), vector3f.z()).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(packedLight);
	}
	@Override
	protected int getLightColor(float partialTick)
	{
		return LightTexture.FULL_BRIGHT;
	}
	public @NotNull ParticleRenderType getRenderType()
	{
		return SplatcraftRenderTypes.getInkFrontRendertype();
	}
	@OnlyIn(Dist.CLIENT)
	public static class Factory implements ParticleProvider<InkHitParticleData>
	{
		private final SpriteSet provider;
		public Factory(SpriteSet sprite)
		{
			provider = sprite;
		}
		@Nullable
		@Override
		public Particle createParticle(InkHitParticleData data, @NotNull ClientLevel levelIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
		{
			// todo: add a config for this
			return new InkHitParticle(levelIn, x, y, z, data.getColor(), data.getScale(), provider);
		}
	}
}

