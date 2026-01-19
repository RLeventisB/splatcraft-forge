package net.splatcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.splatcraft.Splatcraft;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.client.models.projectiles.BlasterInkProjectileModel;
import net.splatcraft.client.models.projectiles.InkProjectileModel;
import net.splatcraft.client.models.projectiles.RollerInkProjectileModel;
import net.splatcraft.client.models.projectiles.ShooterInkProjectileModel;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.TreeMap;

public class InkProjectileRenderer extends EntityRenderer<InkProjectileEntity> implements RenderLayerParent<InkProjectileEntity, InkProjectileModel>
{
	private final TreeMap<String, InkProjectileModel> MODELS;
	public InkProjectileRenderer(EntityRendererProvider.Context context)
	{
		super(context);

		MODELS = new TreeMap<>()
		{{
			put(InkProjectileEntity.Types.DEFAULT, new InkProjectileModel(context.bakeLayer(InkProjectileModel.LAYER_LOCATION)));
			put(InkProjectileEntity.Types.SHOOTER, new ShooterInkProjectileModel(context.bakeLayer(ShooterInkProjectileModel.LAYER_LOCATION)));
			put(InkProjectileEntity.Types.CHARGER, new ShooterInkProjectileModel(context.bakeLayer(ShooterInkProjectileModel.LAYER_LOCATION)));
			put(InkProjectileEntity.Types.BLASTER, new BlasterInkProjectileModel(context.bakeLayer(BlasterInkProjectileModel.LAYER_LOCATION)));
			put(InkProjectileEntity.Types.ROLLER, new RollerInkProjectileModel(context.bakeLayer(RollerInkProjectileModel.LAYER_LOCATION)));
		}};
	}
	@Override
	public void render(InkProjectileEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight)
	{
		if (entity.isInvisible())
			return;

		if (entityRenderDispatcher.shouldRenderHitBoxes())
		{
			// render collision sphere!!
			VertexConsumer builder = buffer.getBuffer(RenderType.LINE_STRIP);

			float radius = entity.getProjectileHitboxRadius();
			float step = 45 * (Mth.PI / 180);

			for (float pitch = -Mth.PI; pitch <= Mth.PI; pitch += step)
			{
				for (float yaw = -Mth.PI; yaw <= Mth.PI; yaw += step)
				{
					float x = Mth.sin(yaw) * -Mth.cos(pitch) * radius;
					float y = Mth.sin(pitch) * radius;
					float z = Mth.cos(yaw) * -Mth.cos(pitch) * radius;

					builder.addVertex(poseStack.last(), x, y, z).setColor(255, 255, 255, 255).setNormal(poseStack.last(), 1.0F, 0.0F, 0.0F);

					if (Math.abs(pitch) == 180) // only render one line, since we're on the top/bottom of the sphere
						break;
				}
			}
		}

		if (entityRenderDispatcher.camera.getPosition().distanceToSqr(entity.getPosition(partialTicks)) >= 2D)
		{
			float visualSize = entity.getProjectileVisualSize();
			float scale = visualSize * (entity.getProjectileType().equals(InkProjectileEntity.Types.DEFAULT) ? 1 : 2.5f);
			InkColor color = ColorUtils.getColorLockedIfConfig(entity.getColor());

			boolean shinier = SplatcraftConfig.get("splatcraft.makeShinier");
			if (shinier)
			{
				color = InkColor.constructOrReuse(ColorUtils.makeBrighter(color));
				packedLight = 0x00F00000;
			}

			//0.30000001192092896D
			poseStack.pushPose();
			poseStack.translate(0.0D, visualSize / 8, 0.0D);
			poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw - 180.0F));
			poseStack.mulPose(Axis.XP.rotationDegrees(entity.getViewXRot(partialTicks)));
			poseStack.scale(scale, scale, scale);

			InkProjectileModel model = MODELS.getOrDefault(entity.getProjectileType(), MODELS.get(InkProjectileEntity.Types.DEFAULT));

			model.setupAnim(entity, 0, 0, handleRotationFloat(entity, partialTicks), entityYaw, entity.getViewXRot(partialTicks));
			model.renderToBuffer(poseStack, buffer.getBuffer(model.renderType(getTextureLocation(entity))), shinier ? LightTexture.FULL_BRIGHT : packedLight, OverlayTexture.NO_OVERLAY, color.getColorWithAlpha(255));
			poseStack.popPose();

			super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
		}
	}
	protected float handleRotationFloat(InkProjectileEntity livingBase, float partialTicks)
	{
		return (float) livingBase.tickCount + partialTicks;
	}
	@Override
	public @NotNull InkProjectileModel getModel()
	{
		return MODELS.get(InkProjectileEntity.Types.DEFAULT);
	}
	@Override
	public @NotNull ResourceLocation getTextureLocation(InkProjectileEntity entity)
	{
		return Splatcraft.identifierOf("textures/entity/ink_projectile_" + entity.getProjectileType() + ".png");
	}
}
