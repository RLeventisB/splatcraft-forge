package net.splatcraft.client.models.subs;// Made with Blockbench 4.7.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.AbstractSubWeaponModel;
import net.splatcraft.entities.subs.SplatBombEntity;
import org.jetbrains.annotations.NotNull;

public class SplatBombModel extends AbstractSubWeaponModel<SplatBombEntity>
{
    // This layer location should be baked with EntityRendererFactory.Context in the entity renderer and passed into this model's constructor
    public static final EntityModelLayer LAYER_LOCATION = new EntityModelLayer(Splatcraft.identifierOf("splatbombmodel"), "main");
    private final ModelPart Main;

    public SplatBombModel(ModelPart root)
    {
        Main = root.getChild("Main");
    }

    public static TexturedModelData createBodyLayer()
    {
        ModelData meshdefinition = new ModelData();
        ModelPartData partdefinition = meshdefinition.getRoot();

        ModelPartData Main = partdefinition.addChild("Main", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -4.0F, 0.0F));

        ModelPartData bone13 = Main.addChild("bone13", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 3.4F, 0.3F));

        ModelPartData bone = bone13.addChild("bone", ModelPartBuilder.create().uv(18, 2).cuboid(-3.0F, -1.0F, 3.0F, 6.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, -1.0F));

        ModelPartData bone2 = bone13.addChild("bone2", ModelPartBuilder.create().uv(18, 0).cuboid(0.0F, -0.5F, -1.0F, 6.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-3.0F, -0.5F, 2.0F, 0.0F, 1.0472F, 0.0F));

        ModelPartData bone3 = bone13.addChild("bone3", ModelPartBuilder.create().uv(0, 18).cuboid(-6.0F, -0.5F, -1.0F, 6.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(3.0F, -0.5F, 2.0F, 0.0F, -1.0472F, 0.0F));

        ModelPartData bone7 = Main.addChild("bone7", ModelPartBuilder.create(), ModelTransform.pivot(-0.25F, 3.65F, -0.7F));

        ModelPartData bone6 = bone7.addChild("bone6", ModelPartBuilder.create(), ModelTransform.of(3.0F, -0.75F, 3.0F, 0.0F, 1.5708F, 0.0F));

        ModelPartData cube_r1 = bone6.addChild("cube_r1", ModelPartBuilder.create().uv(12, 12).cuboid(-6.75F, -0.9F, -0.5F, 7.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(4.8754F, -0.5F, -2.8148F, 0.0F, 0.0F, 1.0472F));

        ModelPartData bone5 = bone7.addChild("bone5", ModelPartBuilder.create(), ModelTransform.of(-3.0F, -0.75F, 3.0F, 0.0F, 0.5236F, 0.0F));

        ModelPartData cube_r2 = bone5.addChild("cube_r2", ModelPartBuilder.create().uv(12, 14).cuboid(-0.25F, -0.9F, -0.5F, 7.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.3207F, -0.5F, 0.1852F, 0.0F, 0.0F, -1.0472F));

        ModelPartData bone4 = bone7.addChild("bone4", ModelPartBuilder.create(), ModelTransform.of(3.0F, -0.75F, 3.0F, 0.0F, -0.5236F, 0.0F));

        ModelPartData cube_r3 = bone4.addChild("cube_r3", ModelPartBuilder.create().uv(12, 16).cuboid(-6.75F, -0.9F, -0.5F, 7.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 1.0472F));

        ModelPartData bone8 = bone7.addChild("bone8", ModelPartBuilder.create(), ModelTransform.of(-3.0F, -0.75F, 3.0F, 0.0F, 0.5236F, 0.0F));

        ModelPartData cube_r4 = bone8.addChild("cube_r4", ModelPartBuilder.create().uv(6, 22).cuboid(-1.9F, -0.95F, -1.0F, 2.0F, 2.0F, 2.0F, new Dilation(-0.1F)), ModelTransform.of(0.3207F, -0.5F, 0.1852F, 0.0F, 0.0F, -0.5236F));

        ModelPartData bone9 = bone7.addChild("bone9", ModelPartBuilder.create(), ModelTransform.of(3.5F, -0.75F, 3.0F, 0.0F, -0.5236F, 0.0F));

        ModelPartData cube_r5 = bone9.addChild("cube_r5", ModelPartBuilder.create().uv(18, 20).cuboid(-0.1433F, -0.925F, -0.9352F, 2.0F, 2.0F, 2.0F, new Dilation(-0.1F)), ModelTransform.of(-0.3207F, -0.5F, 0.1852F, 0.0F, 0.0F, 0.5236F));

        ModelPartData bone10 = bone7.addChild("bone10", ModelPartBuilder.create(), ModelTransform.of(0.275F, -0.75F, -2.3F, 0.0F, -1.5708F, 0.0F));

        ModelPartData cube_r6 = bone10.addChild("cube_r6", ModelPartBuilder.create().uv(0, 20).cuboid(-1.9084F, -0.95F, -1.1F, 2.0F, 2.0F, 2.0F, new Dilation(-0.1F)), ModelTransform.of(0.3207F, -0.5F, 0.1852F, 0.0F, 0.0F, -0.5236F));

        ModelPartData bone11 = bone7.addChild("bone11", ModelPartBuilder.create().uv(12, 18).cuboid(-0.825F, -8.2F, 0.35F, 2.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData bone17 = Main.addChild("bone17", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 4.0F, 0.0F));

        ModelPartData bone14 = bone17.addChild("bone14", ModelPartBuilder.create(), ModelTransform.of(0.0F, -1.0F, 0.0F, 0.0F, -1.0472F, 0.0F));

        ModelPartData cube_r7 = bone14.addChild("cube_r7", ModelPartBuilder.create().uv(12, 6).cuboid(-3.05F, -6.5981F, -0.434F, 6.0F, 6.0F, 0.0F, new Dilation(0.0F)), ModelTransform.of(0.4919F, 0.2349F, -1.1981F, -0.2618F, 0.0F, 0.0F));

        ModelPartData bone15 = bone17.addChild("bone15", ModelPartBuilder.create(), ModelTransform.of(-2.8F, -1.0F, 0.0F, 0.0F, -1.0472F, 0.0F));

        ModelPartData cube_r8 = bone15.addChild("cube_r8", ModelPartBuilder.create().uv(0, 12).cuboid(-3.0F, -6.4731F, -0.434F, 6.0F, 6.0F, 0.0F, new Dilation(0.0F)), ModelTransform.of(0.4919F, 0.2349F, -1.1981F, -0.2618F, 2.0944F, 0.0F));

        ModelPartData bone16 = bone17.addChild("bone16", ModelPartBuilder.create(), ModelTransform.of(-2.8F, -1.0F, 0.0F, 0.0F, -1.0472F, 0.0F));

        ModelPartData cube_r9 = bone16.addChild("cube_r9", ModelPartBuilder.create().uv(0, 6).cuboid(-4.45F, -5.7981F, -2.884F, 6.0F, 6.0F, 0.0F, new Dilation(0.0F)), ModelTransform.of(0.4919F, 0.2349F, -1.1981F, -0.2618F, -2.0944F, 0.0F));

        ModelPartData bone18 = bone17.addChild("bone18", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -0.1F, -3.4F, 6.0F, 0.0F, 6.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -0.7F, 0.0F));

        return TexturedModelData.of(meshdefinition, 32, 32);
    }

    @Override
    public void setAngles(@NotNull SplatBombEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {

    }

    @Override
    public void render(@NotNull MatrixStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color)
    {
        Main.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}