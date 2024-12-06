package net.splatcraft.client.renderer.subs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.MathHelper;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.subs.SplatBombModel;
import net.splatcraft.entities.subs.SplatBombEntity;
import org.jetbrains.annotations.NotNull;

public class SplatBombRenderer extends SubWeaponRenderer<SplatBombEntity, SplatBombModel>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/item/weapons/sub/splat_bomb.png");
    private static final ResourceLocation OVERLAY_TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/item/weapons/sub/splat_bomb_ink.png");
    private final SplatBombModel MODEL;

    public SplatBombRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        MODEL = new SplatBombModel(context.bakeLayer(SplatBombModel.LAYER_LOCATION));
    }

    @Override
    public void render(SplatBombEntity entityIn, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn)
    {

        poseStack.pushPose();

        if (!entityIn.isItem)
        {
            poseStack.translate(0.0D, 0.2, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot()) * 2 - 90f));
            poseStack.mulPose(Axis.XP.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot()) * 2 - 180f));

            float f = entityIn.getFlashIntensity(partialTicks);
            float f1 = 1.0F + MathHelper.sin(f * 100.0F) * f * 0.01F;
            f = MathHelper.clamp(f, 0.0F, 1.0F);
            f *= f;
            f *= f;
            float f2 = (1.0F + f * 0.4F) * f1;
            float f3 = (1.0F + f * 0.1F) / f1;
            poseStack.scale(f2, f3, f2);
        }

        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }

    @Override
    protected float getOverlayProgress(SplatBombEntity livingEntityIn, float partialTicks)
    {
        float f = livingEntityIn.getFlashIntensity(partialTicks);
        return (int) (f * 10.0F) % 2 == 0 ? 0.0F : MathHelper.clamp(f, 0.5F, 1.0F);
    }

    @Override
    public SplatBombModel getModel()
    {
        return MODEL;
    }

    @Override
    public ResourceLocation getInkTextureLocation(SplatBombEntity entity)
    {
        return OVERLAY_TEXTURE;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SplatBombEntity entity)
    {
        return TEXTURE;
    }
}
