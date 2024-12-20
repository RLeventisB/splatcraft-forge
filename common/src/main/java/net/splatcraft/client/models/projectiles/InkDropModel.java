package net.splatcraft.client.models.projectiles;// Made with Blockbench 4.8.3
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.splatcraft.Splatcraft;
import net.splatcraft.entities.InkDropEntity;
import org.jetbrains.annotations.NotNull;

public class InkDropModel extends EntityModel<InkDropEntity>
{
    // This layer location should be baked with EntityRendererFactory.Context in the entity renderer and passed into this model's constructor
    public static final EntityModelLayer LAYER_LOCATION = new EntityModelLayer(Splatcraft.identifierOf("shooterinkprojectilemodel"), "main");
    protected final ModelPart main;

    public InkDropModel(ModelPart root)
    {
        main = root.getChild("main");
    }

    public static TexturedModelData createBodyLayer()
    {
        ModelData meshdefinition = new ModelData();
        ModelPartData partdefinition = meshdefinition.getRoot();

        ModelPartData main = partdefinition.addChild("main", ModelPartBuilder.create().uv(0, 5).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData middle = main.addChild("middle", ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -1.0F, -0.75F, 2.0F, 2.0F, 3.0F, new Dilation(-0.3F)), ModelTransform.pivot(0.0F, -1.0F, 1.5F));

        ModelPartData back = middle.addChild("back", ModelPartBuilder.create().uv(7, 0).cuboid(-0.5F, -0.5F, -0.75F, 1.0F, 1.0F, 2.0F, new Dilation(-0.15F)), ModelTransform.pivot(0.0F, 0.0F, 2.0F));

        return TexturedModelData.of(meshdefinition, 16, 16);
    }

    @Override
    public void setAngles(@NotNull InkDropEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
    }

    @Override
    public void render(@NotNull MatrixStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color)
    {
        main.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}