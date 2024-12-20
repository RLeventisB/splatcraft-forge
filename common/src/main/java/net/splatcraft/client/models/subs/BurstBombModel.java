package net.splatcraft.client.models.subs;// Made with Blockbench 4.7.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.AbstractSubWeaponModel;
import net.splatcraft.entities.subs.BurstBombEntity;
import org.jetbrains.annotations.NotNull;

public class BurstBombModel extends AbstractSubWeaponModel<BurstBombEntity>
{
    // This layer location should be baked with EntityRendererFactory.Context in the entity renderer and passed into this model's constructor
    public static final EntityModelLayer LAYER_LOCATION = new EntityModelLayer(Splatcraft.identifierOf("burstbombmodel"), "main");
    private final ModelPart bone;

    public BurstBombModel(ModelPart root)
    {
        bone = root.getChild("bone");
    }

    public static TexturedModelData createBodyLayer()
    {
        ModelData meshdefinition = new ModelData();
        ModelPartData partdefinition = meshdefinition.getRoot();

        partdefinition.addChild("bone", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -2.5F, -3.0F, 6.0F, 5.0F, 6.0F, new Dilation(0.0F))
            .uv(12, 12).cuboid(-2.0F, -3.5F, -2.0F, 4.0F, 1.0F, 4.0F, new Dilation(0.0F))
            .uv(0, 11).cuboid(-2.0F, 2.5F, -2.0F, 4.0F, 1.0F, 4.0F, new Dilation(0.0F))
            .uv(0, 16).cuboid(-1.5F, -5.5F, -1.5F, 3.0F, 1.0F, 3.0F, new Dilation(0.0F))
            .uv(12, 17).cuboid(-1.0F, -4.5F, -1.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -3.5F, 0.0F));

        return TexturedModelData.of(meshdefinition, 32, 32);
    }

    @Override
    public void setAngles(@NotNull BurstBombEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {

    }

    @Override
    public void render(@NotNull MatrixStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color)
    {
        bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}