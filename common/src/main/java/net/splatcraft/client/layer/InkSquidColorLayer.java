package net.splatcraft.client.layer;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.InkSquidModel;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

public class InkSquidColorLayer extends FeatureRenderer<LivingEntity, InkSquidModel>
{
    private static final Identifier TEXTURE = Splatcraft.identifierOf("textures/entity/ink_squid.png");
    private final InkSquidModel model;

    public InkSquidColorLayer(FeatureRendererContext<LivingEntity, InkSquidModel> renderer, EntityModelLoader modelSet)
    {
        super(renderer);
        model = new InkSquidModel(modelSet.getModelPart(InkSquidModel.LAYER_LOCATION));
    }

    protected static <T extends LivingEntity> void coloredCutoutModelCopyLayerRender(@NotNull EntityModel<T> parentModel, @NotNull EntityModel<T> model, @NotNull Identifier textureLoc, @NotNull MatrixStack poseStack, @NotNull VertexConsumerProvider bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTickTime, float ageInTicks, float netHeadYaw, float headPitch, int color)
    {
        if (!entity.isInvisible())
        {
            parentModel.copyStateTo(model);
            renderModel(model, textureLoc, poseStack, bufferSource, packedLight, entity, color);
        }
    }

    @Override
    public void render(@NotNull MatrixStack poseStack, @NotNull VertexConsumerProvider bufferSource, int packedLight, @NotNull LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTickTime, float ageInTicks, float netHeadYaw, float headPitch)
    {
        InkColor color = ColorUtils.getColorLockedIfConfig(ColorUtils.getEntityColor(entity));
        coloredCutoutModelCopyLayerRender(getContextModel(), model, TEXTURE, poseStack, bufferSource, packedLight, entity, limbSwing, limbSwingAmount, packedLight, ageInTicks, netHeadYaw, headPitch, color.getColorWithAlpha(255));
    }
}
