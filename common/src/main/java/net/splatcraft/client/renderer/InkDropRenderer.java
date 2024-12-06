package net.splatcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.MathHelper;
import net.splatcraft.Splatcraft;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.client.models.projectiles.InkDropModel;
import net.splatcraft.client.models.projectiles.ShooterInkProjectileModel;
import net.splatcraft.entities.InkDropEntity;
import net.splatcraft.util.ColorUtils;
import org.jetbrains.annotations.NotNull;

public class InkDropRenderer extends EntityRenderer<InkDropEntity> implements RenderLayerParent<InkDropEntity, InkDropModel>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/entity/shooter_projectile.png");
    private final InkDropModel MODEL;

    public InkDropRenderer(EntityRendererProvider.Context context)
    {
        super(context);

        MODEL = new InkDropModel(context.bakeLayer(ShooterInkProjectileModel.LAYER_LOCATION));
    }

    @Override
    public void render(InkDropEntity entityIn, float entityYaw, float partialTicks, @NotNull PoseStack matrixStackIn, @NotNull MultiBufferSource bufferIn, int packedLightIn)
    {
        if (entityIn.isInvisible())
            return;

        if (this.entityRenderDispatcher.camera.getEntity().distanceToSqr(entityIn.getPosition(partialTicks)) >= 12.25D)
        {
            float scale = entityIn.getProjectileSize();
            int color = entityIn.getColor();

            if (SplatcraftConfig.Client.colorLock.get())
                color = ColorUtils.getLockedColor(color);

            float r = (float) (Math.floor((float) color / (256 * 256)) / 255f);
            float g = (float) (Math.floor((float) color / 256) % 256 / 255f);
            float b = (color % 256) / 255f;

            //0.30000001192092896D
            matrixStackIn.pushPose();
            matrixStackIn.translate(0.0D, scale, 0.0D);
            matrixStackIn.mulPose(Axis.YP.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot()) - 180.0F));
            matrixStackIn.mulPose(Axis.XP.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())));
            matrixStackIn.scale(scale, scale, (float) (scale * entityIn.getDeltaMovement().length()));

            InkDropModel model = MODEL;

            model.setupAnim(entityIn, 0, 0, this.handleRotationFloat(entityIn, partialTicks), entityYaw, entityIn.getXRot());
            model.renderToBuffer(matrixStackIn, bufferIn.getBuffer(model.renderType(getTextureLocation(entityIn))), packedLightIn, OverlayTexture.NO_OVERLAY, r, g, b, 1);
            matrixStackIn.popPose();

            super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        }
    }

    protected float handleRotationFloat(InkDropEntity livingBase, float partialTicks)
    {
        return (float) livingBase.tickCount + partialTicks;
    }

    @Override
    public @NotNull InkDropModel getModel()
    {
        return MODEL;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull InkDropEntity entity)
    {
        return new ResourceLocation(Splatcraft.MODID, "textures/entity/ink_projectile_shooter.png");
    }
}
