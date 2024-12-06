package net.splatcraft.client.renderer.subs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.MathHelper;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.subs.BurstBombModel;
import net.splatcraft.entities.subs.BurstBombEntity;
import org.jetbrains.annotations.NotNull;

public class BurstBombRenderer extends SubWeaponRenderer<BurstBombEntity, BurstBombModel>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/item/weapons/sub/burst_bomb.png");
    private static final ResourceLocation OVERLAY_TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/item/weapons/sub/burst_bomb_ink.png");
    private final BurstBombModel MODEL;

    public BurstBombRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        MODEL = new BurstBombModel(context.bakeLayer(BurstBombModel.LAYER_LOCATION));
    }

    @Override
    public void render(BurstBombEntity entityIn, float entityYaw, float partialTicks, @NotNull PoseStack PoseStackIn, @NotNull MultiBufferSource bufferIn, int packedLightIn)
    {

        PoseStackIn.pushPose();
        if (!entityIn.isItem)
        {
            //PoseStackIn.translate(0.0D, 0.2/*0.15000000596046448D*/, 0.0D);
            PoseStackIn.mulPose(Axis.YP.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot()) - 180.0F));
            PoseStackIn.mulPose(Axis.XP.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot()) + 90F));
            PoseStackIn.scale(1, -1, 1);
        }
        super.render(entityIn, entityYaw, partialTicks, PoseStackIn, bufferIn, packedLightIn);
        PoseStackIn.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BurstBombEntity entity)
    {
        return TEXTURE;
    }

    @Override
    public BurstBombModel getModel()
    {
        return MODEL;
    }

    @Override
    public ResourceLocation getInkTextureLocation(BurstBombEntity entity)
    {
        return OVERLAY_TEXTURE;
    }
}
