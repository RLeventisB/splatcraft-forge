package net.splatcraft.forge.client.renderer.subs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.client.models.subs.SuctionBombModel;
import net.splatcraft.forge.entities.subs.SuctionBombEntity;
import org.jetbrains.annotations.NotNull;

public class SuctionBombRenderer extends SubWeaponRenderer<SuctionBombEntity, SuctionBombModel>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/item/weapons/sub/suction_bomb.png");
    private static final ResourceLocation OVERLAY_TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/item/weapons/sub/suction_bomb_ink.png");
    private final SuctionBombModel MODEL;

    public SuctionBombRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        MODEL = new SuctionBombModel(context.bakeLayer(SuctionBombModel.LAYER_LOCATION));
    }

    @Override
    public void render(SuctionBombEntity entityIn, float entityYaw, float partialTicks, @NotNull PoseStack PoseStackIn, @NotNull MultiBufferSource bufferIn, int packedLightIn)
    {

        PoseStackIn.pushPose();
        if (!entityIn.isItem)
        {
            PoseStackIn.translate(0.0D, 0.15/*0.15000000596046448D*/, 0.0D);
            PoseStackIn.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot()) - 180.0F));
            PoseStackIn.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot()) + 90F));
            PoseStackIn.translate(0.0D, -0.15/*0.15000000596046448D*/, 0.0D);
            PoseStackIn.scale(1, -1, 1);

            float f = entityIn.getFlashIntensity(partialTicks);
            float f1 = 1.0F + Mth.sin(f * 100.0F) * f * 0.01F;
            f = Mth.clamp(f, 0.0F, 1.0F);
            f = f * f;
            f = f * f;
            float f2 = (1.0F + f * 0.4F) * f1;
            float f3 = (1.0F + f * 0.1F) / f1;
            PoseStackIn.scale(f2, f3, f2);
        }

        super.render(entityIn, entityYaw, partialTicks, PoseStackIn, bufferIn, packedLightIn);
        PoseStackIn.popPose();
    }

    protected float getOverlayProgress(SuctionBombEntity livingEntityIn, float partialTicks)
    {
        float f = livingEntityIn.getFlashIntensity(partialTicks);
        return (int) (f * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(f, 0.5F, 1.0F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SuctionBombEntity entity)
    {
        return TEXTURE;
    }

    @Override
    public SuctionBombModel getModel()
    {
        return MODEL;
    }

    @Override
    public ResourceLocation getInkTextureLocation(SuctionBombEntity entity)
    {
        return OVERLAY_TEXTURE;
    }
}
