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
	public void render(InkDropEntity entity, float entityYaw, float partialTicks, @NotNull MatrixStack matrixStack, @NotNull VertexConsumerProvider provider, int packetLight)
	{
		if (entity.isInvisible())
			return;
		
		double distance = dispatcher.camera.getPos().squaredDistanceTo(entity.getLerpedPos(partialTicks));
		if (distance >= 2)
		{
			float size = InkDropEntity.DROP_SIZE * entity.getImpactCoverage();
			InkColor color = ColorUtils.getColorLockedIfConfig(entity.getColor());
			
			int rgb = color.getColorWithAlpha((int) Math.min(255, distance));
			
			//0.30000001192092896D
			matrixStack.push();
			matrixStack.translate(0, size / 2, 0);
			matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entityYaw - 180.0F));
			matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getPitch(partialTicks) - 90.0F));
			matrixStack.scale(size, size, (float) (size + size * entity.getVelocity().length()));
			
			InkDropModel model = MODEL;
			
			model.setAngles(entity, 0, 0, handleRotationFloat(entity, partialTicks), entityYaw, entity.getPitch(partialTicks));
			model.render(matrixStack, provider.getBuffer(model.getLayer(getTexture(entity))), packetLight, OverlayTexture.DEFAULT_UV, rgb);
			matrixStack.pop();
			
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
	public @NotNull Identifier getTexture(@NotNull InkDropEntity entity)
	{
		return TEXTURE;
	}
}
