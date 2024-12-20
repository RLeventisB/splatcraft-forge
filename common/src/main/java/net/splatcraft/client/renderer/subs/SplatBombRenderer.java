package net.splatcraft.client.renderer.subs;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.subs.SplatBombModel;
import net.splatcraft.entities.subs.SplatBombEntity;
import org.jetbrains.annotations.NotNull;

public class SplatBombRenderer extends SubWeaponRenderer<SplatBombEntity, SplatBombModel>
{
    private static final Identifier TEXTURE = Splatcraft.identifierOf("textures/item/weapons/sub/splat_bomb.png");
    private static final Identifier OVERLAY_TEXTURE = Splatcraft.identifierOf("textures/item/weapons/sub/splat_bomb_ink.png");
    private final SplatBombModel MODEL;

    public SplatBombRenderer(EntityRendererFactory.Context context)
    {
        super(context);
        MODEL = new SplatBombModel(context.getPart(SplatBombModel.LAYER_LOCATION));
    }

    @Override
    public void render(SplatBombEntity entityIn, float entityYaw, float partialTicks, @NotNull MatrixStack poseStack, @NotNull VertexConsumerProvider bufferIn, int packedLightIn)
    {

        poseStack.push();

        if (!entityIn.isItem)
        {
            poseStack.translate(0.0D, 0.2, 0.0D);
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevYaw, entityIn.getYaw()) *
                2 - 90f));
            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevPitch, entityIn.getPitch()) * 2 - 180f));

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
        poseStack.pop();
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
    public Identifier getInkTextureLocation(SplatBombEntity entity)
    {
        return OVERLAY_TEXTURE;
    }

    @Override
    public @NotNull Identifier getTexture(@NotNull SplatBombEntity entity)
    {
        return TEXTURE;
    }
}
