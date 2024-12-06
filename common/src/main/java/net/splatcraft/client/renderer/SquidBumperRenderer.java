package net.splatcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.MathHelper;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.layer.SquidBumperColorLayer;
import net.splatcraft.client.models.SquidBumperModel;
import net.splatcraft.entities.SquidBumperEntity;
import org.jetbrains.annotations.NotNull;

public class SquidBumperRenderer extends LivingEntityRenderer<SquidBumperEntity, SquidBumperModel> implements RenderLayerParent<SquidBumperEntity, SquidBumperModel>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/entity/squid_bumper_overlay.png");

    public SquidBumperRenderer(EntityRendererProvider.Context context)
    {
        super(context, new SquidBumperModel(context.bakeLayer(SquidBumperModel.LAYER_LOCATION)), 0.5f);
        addLayer(new SquidBumperColorLayer(this, context.getModelSet()));
        //addLayer(new SquidBumperOverlayLayer(this, context.getModelSet()));
    }

    @Override
    protected boolean shouldShowName(SquidBumperEntity entity)
    {
        return !entity.hasCustomName() && !(entity.getInkHealth() >= 20) || super.shouldShowName(entity) && (entity.shouldShowName() || entity == this.entityRenderDispatcher.crosshairPickEntity);
    }

    @Override
    protected void renderNameTag(SquidBumperEntity entityIn, @NotNull Component displayNameIn, @NotNull PoseStack PoseStackIn, @NotNull MultiBufferSource bufferIn, int packedLightIn)
    {
        if (entityIn.hasCustomName())
        {
            super.renderNameTag(entityIn, displayNameIn, PoseStackIn, bufferIn, packedLightIn);
        }
        else
        {
            float health = 20 - entityIn.getInkHealth();
            super.renderNameTag(entityIn, Component.literal((health >= 20 ? ChatFormatting.DARK_RED : "") + String.format("%.1f", health)), PoseStackIn, bufferIn, packedLightIn);
        }
    }

    @Override
    protected void setupRotations(SquidBumperEntity entityLiving, @NotNull PoseStack PoseStackIn, float ageInTicks, float rotationYaw, float partialTicks)
    {
        //PoseStackIn.rotate(Vector3f.YP.rotationDegrees(180.0F - rotationYaw));
        float punchTime = (float) (entityLiving.getWorld().getGameTime() - entityLiving.punchCooldown) + partialTicks;
        float hurtTime = (float) (entityLiving.getWorld().getGameTime() - entityLiving.hurtCooldown) + partialTicks;

        if (punchTime < 5.0F)
        {
            PoseStackIn.mulPose(Axis.YP.rotationDegrees(MathHelper.sin(punchTime / 1.5F * (float) Math.PI) * 3.0F));
        }
        if (hurtTime < 5.0F)
        {
            PoseStackIn.mulPose(Axis.ZP.rotationDegrees(MathHelper.sin(hurtTime / 1.5F * (float) Math.PI) * 3.0F));
        }
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SquidBumperEntity entity)
    {
        return TEXTURE;
    }
}
