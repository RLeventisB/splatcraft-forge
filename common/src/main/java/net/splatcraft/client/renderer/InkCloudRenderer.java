package net.splatcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.handlers.RendererHandler;
import net.splatcraft.entities.InkCloudEntity;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class InkCloudRenderer extends EntityRenderer<InkCloudEntity>
{
	public static final List<InkCloudEntity> cloudsToRenderBorder = new ArrayList<>();
	public static final ResourceLocation INK_CLOUD_LOCATION = Splatcraft.identifierOf("ink_cloud");
	public InkCloudRenderer(EntityRendererProvider.Context context)
	{
		super(context);
	}
	@Override
	public boolean shouldRender(@NotNull InkCloudEntity entity, @NotNull Frustum camera, double camX, double camY, double camZ)
	{
		final Vector3f downOffset = entity.getDeltaMovement().toVector3f().negate().add(0, -1, 0).mul(100);
		Vector3f position = entity.position().toVector3f().sub((float) camX, (float) camY, (float) camZ);
		return super.shouldRender(entity, camera, camX, camY, camZ) || camera.intersection.testLineSegment(position, position.add(downOffset, new Vector3f()));
	}
	@Override
	public void render(@NotNull InkCloudEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack matrixStack, @NotNull MultiBufferSource buffer, int b)
	{
		InkColor cloudColor = entity.getColor();
		int colorHex = cloudColor.getColorWithAlpha(255);
		
		cloudsToRenderBorder.add(entity);
		
		if (entity.cloudParticleRenderData == null)
			return;
		
		AbstractTexture particleAtlas = ClientUtils.getClient().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
		if (!(particleAtlas instanceof TextureAtlas particleAtlasSecure))
		{
			return;
		}
		
		TextureAtlasSprite inkCloudSprite = particleAtlasSecure.getSprite(INK_CLOUD_LOCATION);
		float u0 = inkCloudSprite.getU0();
		float u1 = inkCloudSprite.getU1();
		float v0 = inkCloudSprite.getV0();
		float v1 = inkCloudSprite.getV1();
		
		VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_PARTICLES));
		Camera camera = ClientUtils.getClient().gameRenderer.getMainCamera();
		
		Vector3f entityPos = entity.getPosition(partialTicks).toVector3f();
		Quaternionf quaternion = new Quaternionf();
		SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ.setRotation(quaternion, camera, partialTicks);
		float cloudAlpha = Math.clamp(1f - (entity.tickCount - entity.duration - entity.formationTime + partialTicks) / InkCloudEntity.DISSAPEAR_TICKS, 0, 1);
		
		for (InkCloudEntity.ParticleData data : entity.cloudParticleRenderData)
		{
			float size = data.getSize();
			Vector3f relativePos = data.position.add(data.velocity.mul(partialTicks, new Vector3f()), new Vector3f());
			Vector3f absolutePos = relativePos.add(entityPos, new Vector3f());
			int packedLight = LevelRenderer.getLightColor(entity.level(), BlockPos.containing(absolutePos.x, absolutePos.y, absolutePos.z));
			
			float lifeTimeAlpha = Mth.square((data.getLifespan() + partialTicks) / InkCloudEntity.ParticleData.MAX_LIFESPAN * 2 - 1f);
			lifeTimeAlpha *= Mth.square(lifeTimeAlpha);
			lifeTimeAlpha = Mth.clamp(1 - lifeTimeAlpha, 0, 1);
			
			int col = FastColor.ARGB32.color((int) (255 * lifeTimeAlpha * cloudAlpha), colorHex);
			
			renderVertex(matrixStack, consumer, quaternion, relativePos, 1.0F, -1.0F, size, u1, v1, packedLight, col);
			renderVertex(matrixStack, consumer, quaternion, relativePos, 1.0F, 1.0F, size, u1, v0, packedLight, col);
			renderVertex(matrixStack, consumer, quaternion, relativePos, -1.0F, 1.0F, size, u0, v0, packedLight, col);
			renderVertex(matrixStack, consumer, quaternion, relativePos, -1.0F, -1.0F, size, u0, v1, packedLight, col);
		}
	}
	private void renderVertex(PoseStack poseStack, VertexConsumer consumer, Quaternionf quaternion, Vector3f absolutePos, float xOffset, float yOffset, float quadSize, float u, float v, int packedLight, int col)
	{
		Vector3f vector3f = (new Vector3f(xOffset, yOffset, 0.0F)).rotate(quaternion).mul(quadSize).add(absolutePos);
		consumer.addVertex(poseStack.last(), vector3f).setUv(u, v).setColor(col).setUv1(1, 1).setNormal(1, 0, 0).setLight(packedLight);
	}
	@Override
	public @NotNull ResourceLocation getTextureLocation(@NotNull InkCloudEntity entity)
	{
		return RendererHandler.MAGIC_PIXEL;
	}
}