package net.splatcraft.client.models.subs;// Made with Blockbench 4.7.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.AbstractSubWeaponModel;
import net.splatcraft.entities.subs.CurlingBombEntity;
import org.jetbrains.annotations.NotNull;

public class CurlingBombModel extends AbstractSubWeaponModel<CurlingBombEntity>
{
    // This layer location should be baked with EntityRendererFactory.Context in the entity renderer and passed into this model's constructor
    public static final EntityModelLayer LAYER_LOCATION = new EntityModelLayer(Splatcraft.identifierOf("curlingbombmodel"), "main");
    private final ModelPart blades;
    private final ModelPart bumper1;
    private final ModelPart bumper2;
    private final ModelPart bumper3;
    private final ModelPart bumper4;
    private final ModelPart top;
    private final ModelPart bb_main;

    public CurlingBombModel(ModelPart root)
    {
        blades = root.getChild("blades");
        bumper1 = root.getChild("bumper1");
        bumper2 = root.getChild("bumper2");
        bumper3 = root.getChild("bumper3");
        bumper4 = root.getChild("bumper4");
        top = root.getChild("top");
        bb_main = root.getChild("bb_main");
    }

    public static TexturedModelData createBodyLayer()
    {
        ModelData meshdefinition = new ModelData();
        ModelPartData partdefinition = meshdefinition.getRoot();

        ModelPartData blades = partdefinition.addChild("blades", ModelPartBuilder.create().uv(26, 27).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 24.05F, 0.0F));

        ModelPartData bone3 = blades.addChild("bone3", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData cube_r1 = bone3.addChild("cube_r1", ModelPartBuilder.create().uv(2, 0).cuboid(-0.5F, 0.0F, -0.3F, 1.0F, 0.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -1.3F, 1.0F, 0.0F, 0.0F, 0.4363F));

        ModelPartData bone2 = blades.addChild("bone2", ModelPartBuilder.create(), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 2.0944F, 0.0F));

        ModelPartData cube_r2 = bone2.addChild("cube_r2", ModelPartBuilder.create().uv(2, 0).cuboid(-0.5F, 0.0F, -0.3F, 1.0F, 0.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -1.3F, 1.0F, 0.0F, 0.0F, 0.4363F));

        ModelPartData bone4 = blades.addChild("bone4", ModelPartBuilder.create(), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, -2.0944F, 0.0F));

        ModelPartData cube_r3 = bone4.addChild("cube_r3", ModelPartBuilder.create().uv(2, 0).cuboid(-0.5F, 0.0F, -0.3F, 1.0F, 0.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -1.3F, 1.0F, 0.0F, 0.0F, 0.4363F));

        ModelPartData bumper1 = partdefinition.addChild("bumper1", ModelPartBuilder.create().uv(24, 0).cuboid(-3.5F, -4.5F, -5.0F, 7.0F, 3.0F, 1.0F, new Dilation(0.0F))
            .uv(0, 13).cuboid(-0.5F, -3.5F, -4.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 24.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

        ModelPartData bumper2 = partdefinition.addChild("bumper2", ModelPartBuilder.create().uv(24, 0).cuboid(-3.5F, -4.5F, -5.0F, 7.0F, 3.0F, 1.0F, new Dilation(0.0F))
            .uv(0, 13).cuboid(-0.5F, -3.5F, -4.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 24.0F, 0.0F, 0.0F, 3.1416F, 0.0F));

        ModelPartData bumper3 = partdefinition.addChild("bumper3", ModelPartBuilder.create().uv(24, 0).cuboid(-3.5F, -4.5F, -5.0F, 7.0F, 3.0F, 1.0F, new Dilation(0.0F))
            .uv(0, 13).cuboid(-0.5F, -3.5F, -4.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        ModelPartData bumper4 = partdefinition.addChild("bumper4", ModelPartBuilder.create().uv(24, 0).cuboid(-3.5F, -4.5F, -5.0F, 7.0F, 3.0F, 1.0F, new Dilation(0.0F))
            .uv(0, 13).cuboid(-0.5F, -3.5F, -4.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 24.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        ModelPartData top = partdefinition.addChild("top", ModelPartBuilder.create().uv(0, 9).cuboid(-3.5F, -0.6F, -3.5F, 7.0F, 3.0F, 7.0F, new Dilation(-0.05F)), ModelTransform.pivot(0.0F, 20.0F, 0.0F));

        ModelPartData handle = top.addChild("handle", ModelPartBuilder.create().uv(0, 19).cuboid(-3.5346F, 0.2775F, -1.0F, 1.0F, 2.0F, 2.0F, new Dilation(0.0F))
            .uv(21, 13).cuboid(-3.5346F, -0.6225F, -1.0F, 5.0F, 1.0F, 2.0F, new Dilation(0.001F)), ModelTransform.pivot(2.5346F, -2.6775F, 0.0F));

        ModelPartData cube_r4 = handle.addChild("cube_r4", ModelPartBuilder.create().uv(22, 29).cuboid(-1.2F, -0.2F, -1.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F))
            .uv(21, 23).cuboid(-1.5F, -0.2F, -1.0F, 3.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3847F));

        ModelPartData cube_r5 = handle.addChild("cube_r5", ModelPartBuilder.create().uv(6, 29).cuboid(-0.925F, -2.25F, -1.0F, 1.0F, 1.0F, 2.0F, new Dilation(-0.001F)), ModelTransform.of(-2.8783F, 2.1816F, 0.0F, 0.0F, 0.0F, 0.2182F));

        ModelPartData cube_r6 = handle.addChild("cube_r6", ModelPartBuilder.create().uv(0, 9).cuboid(-0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-2.3706F, 2.0834F, 0.0F, 0.0F, 0.0F, -0.6109F));

        ModelPartData bb_main = partdefinition.addChild("bb_main", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -1.0F, -4.0F, 8.0F, 1.0F, 8.0F, new Dilation(0.0F))
            .uv(0, 19).cuboid(-3.5F, -4.5F, -3.5F, 7.0F, 3.0F, 7.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        return TexturedModelData.of(meshdefinition, 64, 64);
    }

    @Override
    public void setAngles(@NotNull CurlingBombEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
    }

    @Override
    public void animateModel(@NotNull CurlingBombEntity entityIn, float limbSwing, float limbSwingAmount, float partialTick)
    {
        super.animateModel(entityIn, limbSwing, limbSwingAmount, partialTick);

        blades.yaw = MathHelper.lerp(partialTick, entityIn.prevBladeRot, entityIn.bladeRot);

        top.pivotY = 20 - MathHelper.clamp(MathHelper.lerp(partialTick, entityIn.prevFuseTime, entityIn.fuseTime) - 50, 0, .95f) * 3f;
    }

    @Override
    public void render(@NotNull MatrixStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color)
    {
        blades.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        bumper1.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        bumper2.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        bumper3.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        bumper4.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        top.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        bb_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}