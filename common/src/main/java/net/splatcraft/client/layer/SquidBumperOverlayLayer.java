package net.splatcraft.client.layer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.SquidBumperModel;
import net.splatcraft.entities.SquidBumperEntity;
import org.jetbrains.annotations.NotNull;

public class SquidBumperOverlayLayer extends FeatureRenderer<SquidBumperEntity, SquidBumperModel>
{
    private static final Identifier TEXTURE = Splatcraft.identifierOf("textures/entity/squid_bumper.png");
    private final SquidBumperModel model;

    public SquidBumperOverlayLayer(FeatureRendererContext<SquidBumperEntity, SquidBumperModel> renderer, EntityModelLoader modelSet)
    {
        super(renderer);
        model = new SquidBumperModel(modelSet.getModelPart(SquidBumperModel.LAYER_LOCATION));
    }

    @Override
    public void render(@NotNull MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight, @NotNull SquidBumperEntity entity, float limbSwing, float limbSwingAmount, float partialTickTime, float ageInTicks, float netHeadYaw, float headPitch)
    {
        getContextModel().copyStateTo(model);
        model.animateModel(entity, limbSwing, limbSwingAmount, headPitch);
        model.setAngles(entity, limbSwing, limbSwingAmount, partialTickTime, ageInTicks, netHeadYaw);

        VertexConsumer ivertexbuilder = bufferSource.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        model.render(poseStack, ivertexbuilder, packedLight, LivingEntityRenderer.getOverlay(entity, 0.0F), 0xFFFFFFFF);
    }
}
