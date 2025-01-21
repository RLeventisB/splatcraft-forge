package net.splatcraft.client.renderer;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.projectiles.InkDropModel;
import net.splatcraft.client.models.projectiles.ShooterInkProjectileModel;
import net.splatcraft.entities.InkDropEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

public class InkDropRenderer extends EntityRenderer<InkDropEntity> implements FeatureRendererContext<InkDropEntity, InkDropModel>
{
	private static final Identifier TEXTURE = Splatcraft.identifierOf("textures/entity/ink_projectile_shooter.png");
	private final InkDropModel MODEL;
	public InkDropRenderer(EntityRendererFactory.Context context)
	{
		super(context);
		
		MODEL = new InkDropModel(context.getPart(ShooterInkProjectileModel.LAYER_LOCATION));
	}
	@Override
	public void render(InkDropEntity entityIn, float entityYaw, float partialTicks, @NotNull MatrixStack matrixStackIn, @NotNull VertexConsumerProvider bufferIn, int packedLightIn)
	{
		if (entityIn.isInvisible())
			return;
		
		if (dispatcher.camera.getPos().squaredDistanceTo(entityIn.getLerpedPos(partialTicks)) >= 12.25D)
		{
			float size = InkDropEntity.DROP_SIZE;
			InkColor color = ColorUtils.getColorLockedIfConfig(entityIn.getColor());
			
			int rgb = color.getColorWithAlpha(255);
			
			//0.30000001192092896D
			matrixStackIn.push();
			matrixStackIn.translate(0, size / 2, 0);
			matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entityYaw - 180.0F));
			matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entityIn.getPitch(partialTicks) - 90.0F));
			matrixStackIn.scale(size, size, (float) (size + size * entityIn.getVelocity().length()));
			
			InkDropModel model = MODEL;
			
			model.setAngles(entityIn, 0, 0, handleRotationFloat(entityIn, partialTicks), entityYaw, entityIn.getPitch(partialTicks));
			model.render(matrixStackIn, bufferIn.getBuffer(model.getLayer(getTexture(entityIn))), packedLightIn, OverlayTexture.DEFAULT_UV, rgb);
			matrixStackIn.pop();
			
			super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
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
	public @NotNull Identifier getTexture(@NotNull InkDropEntity entity)
	{
		return TEXTURE;
	}
}
