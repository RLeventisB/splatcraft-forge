package net.splatcraft.client.renderer;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.splatcraft.Splatcraft;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.client.models.projectiles.BlasterInkProjectileModel;
import net.splatcraft.client.models.projectiles.InkProjectileModel;
import net.splatcraft.client.models.projectiles.RollerInkProjectileModel;
import net.splatcraft.client.models.projectiles.ShooterInkProjectileModel;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.TreeMap;

public class InkProjectileRenderer extends EntityRenderer<InkProjectileEntity> implements FeatureRendererContext<InkProjectileEntity, InkProjectileModel>
{
	private final TreeMap<String, InkProjectileModel> MODELS;
	public InkProjectileRenderer(EntityRendererFactory.Context context)
	{
		super(context);
		
		MODELS = new TreeMap<>()
		{{
			put(InkProjectileEntity.Types.DEFAULT, new InkProjectileModel(context.getPart(InkProjectileModel.LAYER_LOCATION)));
			put(InkProjectileEntity.Types.SHOOTER, new ShooterInkProjectileModel(context.getPart(ShooterInkProjectileModel.LAYER_LOCATION)));
			put(InkProjectileEntity.Types.CHARGER, new ShooterInkProjectileModel(context.getPart(ShooterInkProjectileModel.LAYER_LOCATION)));
			put(InkProjectileEntity.Types.BLASTER, new BlasterInkProjectileModel(context.getPart(BlasterInkProjectileModel.LAYER_LOCATION)));
			put(InkProjectileEntity.Types.ROLLER, new RollerInkProjectileModel(context.getPart(RollerInkProjectileModel.LAYER_LOCATION)));
		}};
	}
	@Override
	public void render(InkProjectileEntity entityIn, float entityYaw, float partialTicks, @NotNull MatrixStack matrixStackIn, @NotNull VertexConsumerProvider bufferIn, int packedLightIn)
	{
		if (entityIn.isInvisible())
			return;
		
		if (dispatcher.camera.getPos().squaredDistanceTo(entityIn.getLerpedPos(partialTicks)) >= 2D)
		{
			float visualSize = entityIn.getProjectileVisualSize();
			float scale = visualSize * (entityIn.getProjectileType().equals(InkProjectileEntity.Types.DEFAULT) ? 1 : 2.5f);
			InkColor color = ColorUtils.getColorLockedIfConfig(entityIn.getColor());
			
			boolean shinier = SplatcraftConfig.get("splatcraft.makeShinier");
			if (shinier)
			{
				byte[] colorValues = color.getRGBBytes();
				float[] hslValues = new float[3];
				Color.RGBtoHSB(colorValues[0], colorValues[1], colorValues[2], hslValues);
				hslValues[2] = MathHelper.lerp(0.9f, hslValues[2], 1);
				hslValues[1] = MathHelper.lerp(0.5f, hslValues[1], 0);
				
				color = InkColor.constructOrReuse(Color.HSBtoRGB(hslValues[0], hslValues[1], hslValues[2]) | 0xFF000000);
				packedLightIn = 0x00F00000;
			}
			
			//0.30000001192092896D
			matrixStackIn.push();
			matrixStackIn.translate(0.0D, visualSize / 4, 0.0D);
			matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevYaw, entityIn.getYaw()) - 180.0F));
			matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevPitch, entityIn.getPitch())));
			matrixStackIn.scale(scale, scale, scale);
			
			InkProjectileModel model = MODELS.getOrDefault(entityIn.getProjectileType(), MODELS.get(InkProjectileEntity.Types.DEFAULT));
			
			model.setAngles(entityIn, 0, 0, handleRotationFloat(entityIn, partialTicks), entityYaw, entityIn.getPitch());
			model.render(matrixStackIn, bufferIn.getBuffer(model.getLayer(getTexture(entityIn))), shinier ? LightmapTextureManager.MAX_LIGHT_COORDINATE : packedLightIn, OverlayTexture.DEFAULT_UV, color.getColorWithAlpha(255));
			matrixStackIn.pop();
			
			super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
		}
	}
	protected float handleRotationFloat(InkProjectileEntity livingBase, float partialTicks)
	{
		return (float) livingBase.age + partialTicks;
	}
	@Override
	public @NotNull InkProjectileModel getModel()
	{
		return MODELS.get(InkProjectileEntity.Types.DEFAULT);
	}
	@Override
	public @NotNull Identifier getTexture(InkProjectileEntity entity)
	{
		return Splatcraft.identifierOf("textures/entity/ink_projectile_" + entity.getProjectileType() + ".png");
	}
}
