package net.splatcraft.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.InkSquidModel;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

public class InkSquidColorLayer extends RenderLayer<LivingEntity, InkSquidModel>
{
    private static final ResourceLocation TEXTURE = Splatcraft.identifierOf("textures/entity/ink_squid.png");
    private final InkSquidModel model;

    public InkSquidColorLayer(RenderLayerParent<LivingEntity, InkSquidModel> renderer, EntityModelSet modelSet)
    {
        super(renderer);
        model = new InkSquidModel(modelSet.bakeLayer(InkSquidModel.LAYER_LOCATION));
    }

    protected static <T extends LivingEntity> void coloredCutoutModelCopyLayerRender(@NotNull EntityModel<T> parentModel, @NotNull EntityModel<T> model, @NotNull ResourceLocation textureLoc, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTickTime, float ageInTicks, float netHeadYaw, float headPitch, int color)
    {
        if (!entity.isInvisible())
        {
            parentModel.copyPropertiesTo(model);
            renderColoredCutoutModel(model, textureLoc, poseStack, bufferSource, packedLight, entity, color);
        }
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, @NotNull LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTickTime, float ageInTicks, float netHeadYaw, float headPitch)
    {
        InkColor color = ColorUtils.getColorLockedIfConfig(ColorUtils.getEntityColor(entity));
        coloredCutoutModelCopyLayerRender(getParentModel(), model, TEXTURE, poseStack, bufferSource, packedLight, entity, limbSwing, limbSwingAmount, packedLight, ageInTicks, netHeadYaw, headPitch, color.getColorWithAlpha(255));
    }
}
