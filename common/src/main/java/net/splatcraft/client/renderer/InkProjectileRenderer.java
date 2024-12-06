package net.splatcraft.forge.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.SplatcraftConfig;
import net.splatcraft.forge.client.models.projectiles.BlasterInkProjectileModel;
import net.splatcraft.forge.client.models.projectiles.InkProjectileModel;
import net.splatcraft.forge.client.models.projectiles.RollerInkProjectileModel;
import net.splatcraft.forge.client.models.projectiles.ShooterInkProjectileModel;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.util.ColorUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.TreeMap;

public class InkProjectileRenderer extends EntityRenderer<InkProjectileEntity> implements RenderLayerParent<InkProjectileEntity, InkProjectileModel>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/entity/shooter_projectile.png");
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
    public void render(InkProjectileEntity entityIn, float entityYaw, float partialTicks, @NotNull PoseStack matrixStackIn, @NotNull MultiBufferSource bufferIn, int packedLightIn)
    {
        if (entityIn.isInvisible())
            return;

        if (this.entityRenderDispatcher.camera.getPosition().distanceToSqr(entityIn.getPosition(partialTicks)) >= 2D)
        {
            float visualSize = entityIn.getProjectileVisualSize();
            float scale = visualSize * (entityIn.getProjectileType().equals(InkProjectileEntity.Types.DEFAULT) ? 1 : 2.5f);
            int color = entityIn.getColor();

            if (SplatcraftConfig.Client.colorLock.get())
                color = ColorUtils.getLockedColor(color);

            int rInt = (color & 0x00FF0000) >> 16;
            int gInt = (color & 0x0000FF00) >> 8;
            int bInt = color & 0x000000FF;

            boolean shinier = SplatcraftConfig.Client.makeShinier.get();
            if (shinier)
            {
                float[] values = new float[3];
                Color.RGBtoHSB(rInt, gInt, bInt, values);
                values[2] = Mth.lerp(0.9f, values[2], 1);
                values[1] = Mth.lerp(0.5f, values[1], 0);

                color = Color.HSBtoRGB(values[0], values[1], values[2]);
                rInt = (color & 0x00FF0000) >> 16;
                gInt = (color & 0x0000FF00) >> 8;
                bInt = color & 0x000000FF;
                packedLightIn = 0x00F00000;
            }

            float r = (rInt / 255f);
            float g = (gInt / 255f);
            float b = (bInt / 255f);

            //0.30000001192092896D
            matrixStackIn.pushPose();
            matrixStackIn.translate(0.0D, visualSize / 4, 0.0D);
            matrixStackIn.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot()) - 180.0F));
            matrixStackIn.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())));
            matrixStackIn.scale(scale, scale, scale);

            InkProjectileModel model = MODELS.getOrDefault(entityIn.getProjectileType(), MODELS.get(InkProjectileEntity.Types.DEFAULT));

            model.setupAnim(entityIn, 0, 0, this.handleRotationFloat(entityIn, partialTicks), entityYaw, entityIn.getXRot());
            model.renderToBuffer(matrixStackIn, bufferIn.getBuffer(model.renderType(getTextureLocation(entityIn))), shinier ? LightTexture.FULL_BRIGHT : packedLightIn, OverlayTexture.NO_OVERLAY, r, g, b, 1);
            matrixStackIn.popPose();

            super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
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
        return new ResourceLocation(Splatcraft.MODID, "textures/entity/ink_projectile_" + entity.getProjectileType() + ".png");
    }
}
