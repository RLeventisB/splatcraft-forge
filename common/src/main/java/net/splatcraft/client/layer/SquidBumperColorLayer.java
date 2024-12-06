package net.splatcraft.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.Splatcraft;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.client.models.SquidBumperModel;
import net.splatcraft.entities.SquidBumperEntity;
import net.splatcraft.util.ColorUtils;
import org.jetbrains.annotations.NotNull;

public class SquidBumperColorLayer extends RenderLayer<SquidBumperEntity, SquidBumperModel>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/entity/squid_bumper.png");
    private final SquidBumperModel model;

    public SquidBumperColorLayer(RenderLayerParent<SquidBumperEntity, SquidBumperModel> renderer, EntityModelSet modelSet)
    {
        super(renderer);
        model = new SquidBumperModel(modelSet.bakeLayer(SquidBumperModel.LAYER_LOCATION));
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, @NotNull SquidBumperEntity entity, float limbSwing, float limbSwingAmount, float partialTickTime, float ageInTicks, float netHeadYaw, float headPitch)
    {
        int color = ColorUtils.getEntityColor(entity);
        if (SplatcraftConfig.Client.colorLock.get())
        {
            color = ColorUtils.getLockedColor(color);
        }
        float r = ((color & 0xFF0000) >> 16) / 255.0f;
        float g = ((color & 0x00FF00) >> 8) / 255.0f;
        float b = (color & 0x0000FF) / 255.0f;

        getParentModel().copyPropertiesTo(model);
        model.prepareMobModel(entity, limbSwing, limbSwingAmount, headPitch);
        model.setupAnim(entity, limbSwing, limbSwingAmount, partialTickTime, ageInTicks, netHeadYaw);

        VertexConsumer ivertexbuilder = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(poseStack, ivertexbuilder, packedLight, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), r, g, b, 1.0F);
    }
}
