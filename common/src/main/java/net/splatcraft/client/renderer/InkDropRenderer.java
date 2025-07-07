package net.splatcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.projectiles.InkDropModel;
import net.splatcraft.client.models.projectiles.ShooterInkProjectileModel;
import net.splatcraft.entities.InkDropEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

public class InkDropRenderer extends EntityRenderer<InkDropEntity> implements RenderLayerParent<InkDropEntity, InkDropModel>
{
	private static final ResourceLocation TEXTURE = Splatcraft.identifierOf("textures/entity/ink_projectile_shooter.png");
	private final InkDropModel MODEL;
	public InkDropRenderer(EntityRendererProvider.Context context)
	{
		super(context);
		
		MODEL = new InkDropModel(context.bakeLayer(ShooterInkProjectileModel.LAYER_LOCATION));
	}
	@Override
	public void render(InkDropEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack matrixStack, @NotNull MultiBufferSource provider, int packetLight)
	{
		if (entity.isInvisible())
			return;
		
		double distance = entityRenderDispatcher.camera.getPosition().distanceToSqr(entity.getPosition(partialTicks));
		if (distance >= 2)
		{
			float size = InkDropEntity.DROP_SIZE * entity.getImpactCoverage();
			InkColor color = ColorUtils.getColorLockedIfConfig(entity.getColor());
			
			int rgb = color.getColorWithAlpha((int) Math.min(255, distance));
			
			//0.30000001192092896D
			matrixStack.pushPose();
			matrixStack.translate(0, entity.getBbHeight() / 2, 0);
			matrixStack.mulPose(Axis.YP.rotationDegrees(entityYaw - 180.0F));
			matrixStack.mulPose(Axis.XP.rotationDegrees(entity.getViewXRot(partialTicks)));
			matrixStack.scale(size, size, (float) (size + size * entity.getDeltaMovement().length()));
			
			InkDropModel model = MODEL;
			
			model.setupAnim(entity, 0, 0, handleRotationFloat(entity, partialTicks), entityYaw, entity.getViewXRot(partialTicks));
			model.renderToBuffer(matrixStack, provider.getBuffer(model.renderType(getTextureLocation(entity))), packetLight, OverlayTexture.NO_OVERLAY, rgb);
			matrixStack.popPose();
			
			super.render(entity, entityYaw, partialTicks, matrixStack, provider, packetLight);
		}
	}
	protected float handleRotationFloat(InkDropEntity livingBase, float partialTicks)
	{
		return livingBase.lifespan + partialTicks;
	}
	@Override
	public @NotNull InkDropModel getModel()
	{
		return MODEL;
	}
	@Override
	public @NotNull ResourceLocation getTextureLocation(@NotNull InkDropEntity entity)
	{
		return TEXTURE;
	}
}
