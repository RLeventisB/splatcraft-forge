package net.splatcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.layer.InkSquidColorLayer;
import net.splatcraft.client.models.InkSquidModel;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.entities.InkSquidEntity;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.Optional;

public class InkSquidRenderer extends LivingEntityRenderer<LivingEntity, InkSquidModel> implements RenderLayerParent<LivingEntity, InkSquidModel>
{
	private static final ResourceLocation TEXTURE = Splatcraft.identifierOf("textures/entity/ink_squid_overlay.png");
	private static EntityRendererProvider.Context context;
	public InkSquidRenderer(EntityRendererProvider.Context context)
	{
		super(context, new InkSquidModel(context.bakeLayer(InkSquidModel.LAYER_LOCATION)), 0.5f);
		addLayer(new InkSquidColorLayer(this, context.getModelSet()));

		if (InkSquidRenderer.context == null)
			InkSquidRenderer.context = context;
	}
	public static EntityRendererProvider.Context getContext()
	{
		return context;
	}
	private static void addVertexPair(VertexConsumer p_174308_, Matrix4f p_174309_, float p_174310_, float p_174311_, float p_174312_, int p_174313_, int p_174314_, int p_174315_, int p_174316_, float p_174317_, float p_174318_, float p_174319_, float p_174320_, int p_174321_, boolean p_174322_)
	{
		float f = (float) p_174321_ / 24.0F;
		int i = (int) Mth.lerp(f, (float) p_174313_, (float) p_174314_);
		int j = (int) Mth.lerp(f, (float) p_174315_, (float) p_174316_);
		int k = LightTexture.pack(i, j);
		float f1 = p_174321_ % 2 == (p_174322_ ? 1 : 0) ? 0.7F : 1.0F;
		float f2 = 0.5F * f1;
		float f3 = 0.4F * f1;
		float f4 = 0.3F * f1;
		float f5 = p_174310_ * f;
		float f6 = p_174311_ > 0.0F ? p_174311_ * f * f : p_174311_ - p_174311_ * (1.0F - f) * (1.0F - f);
		float f7 = p_174312_ * f;
		p_174308_.addVertex(p_174309_, f5 - p_174319_, f6 + p_174318_, f7 + p_174320_).setColor(f2, f3, f4, 1.0F).setLight(k);
		p_174308_.addVertex(p_174309_, f5 + p_174319_, f6 + p_174317_ - p_174318_, f7 - p_174320_).setColor(f2, f3, f4, 1.0F).setLight(k);
	}
	@Override
	protected boolean isBodyVisible(@NotNull LivingEntity entity)
	{
		return super.isBodyVisible(entity) && (entity.shouldShowName() || entity.hasCustomName() && entity == entityRenderDispatcher.crosshairPickEntity);
	}
	@Override
	protected void setupRotations(@NotNull LivingEntity entity, @NotNull PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale)
	{
		Optional<Direction> directionOptional = EntityInfoCapability.getOptional(entity).flatMap(EntityInfo::getClimbedDirection);
		if (directionOptional.isPresent())
		{
			yBodyRot = 0;
			Vec3i normal = directionOptional.get().getNormal(); // the squid floats without this piece of code,,,,, (the height of the model is 0.3 but 0.5 "buries" it and it looks better in my opinion)
			float offset = -0.5f * scale;
			poseStack.translate(normal.getX() * offset, 0, normal.getZ() * offset);
		}
		super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
	}
	@Override
	public void render(@NotNull LivingEntity entity, float p_115309_, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int light)
	{
		super.render(entity, p_115309_, partialTicks, poseStack, bufferSource, light);

		if (entity instanceof InkSquidEntity squid)
			renderLeash(squid, partialTicks, poseStack, bufferSource);
	}
	@Override
	public @NotNull ResourceLocation getTextureLocation(@NotNull LivingEntity entity)
	{
		return TEXTURE;
	}
	private void renderLeash(InkSquidEntity squid, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferSource)
	{
		Entity holder = squid.getLeashHolder();

		if (holder == null)
			return;

		matrixStack.pushPose();
		Vec3 vec3 = holder.getRopeHoldPosition(partialTicks);
		float d0 = (squid.getPreciseBodyRotation(partialTicks) * Mth.DEG_TO_RAD) + Mth.HALF_PI;
		Vec3 vec31 = squid.getLeashOffset(partialTicks);
		double d1 = Math.cos(d0) * vec31.z + Math.sin(d0) * vec31.x;
		double d2 = Math.sin(d0) * vec31.z - Math.cos(d0) * vec31.x;
		double d3 = Mth.lerp(partialTicks, squid.xo, squid.getX()) + d1;
		double d4 = Mth.lerp(partialTicks, squid.yo, squid.getY()) + vec31.y;
		double d5 = Mth.lerp(partialTicks, squid.zo, squid.getZ()) + d2;
		matrixStack.translate(d1, vec31.y, d2);
		float f = (float) (vec3.x - d3);
		float f1 = (float) (vec3.y - d4);
		float f2 = (float) (vec3.z - d5);
		VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.leash());
		Matrix4f matrix4f = matrixStack.last().pose();
		float f4 = (float) (Math.sqrt(f * f + f2 * f2) * 0.025F / 2.0F);
		float f5 = f2 * f4;
		float f6 = f * f4;
		BlockPos blockpos = BlockPos.containing(squid.getEyePosition(partialTicks));
		BlockPos blockpos1 = BlockPos.containing(holder.getEyePosition(partialTicks));
		int i = getBlockLightLevel(squid, blockpos);
		int j = getHolderBlockLightLevel(holder, blockpos1);
		int k = squid.level().getBrightness(LightLayer.SKY, blockpos);
		int l = squid.level().getBrightness(LightLayer.SKY, blockpos1);

		for (int i1 = 0; i1 <= 24; ++i1)
		{
			addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025F, 0.025F, f5, f6, i1, false);
		}

		for (int j1 = 24; j1 >= 0; --j1)
		{
			addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025F, 0.0F, f5, f6, j1, true);
		}

		matrixStack.popPose();
	}
	protected int getHolderBlockLightLevel(Entity entity, BlockPos pos)
	{
		return entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.BLOCK, pos);
	}
}
