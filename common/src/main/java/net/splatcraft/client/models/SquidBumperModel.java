package net.splatcraft.client.models;// Made with Blockbench 4.7.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.splatcraft.Splatcraft;
import net.splatcraft.entities.SquidBumperEntity;
import org.jetbrains.annotations.NotNull;

public class SquidBumperModel extends EntityModel<SquidBumperEntity>
{
    // This layer location should be baked with EntityRendererFactory.Context in the entity renderer and passed into this model's constructor
    public static final EntityModelLayer LAYER_LOCATION = new EntityModelLayer(Splatcraft.identifierOf("squid_bumper"), "main");
    private final ModelPart Base;
    private final ModelPart Bumper;
    private float scale = 1;

    public SquidBumperModel(ModelPart root)
    {
        Base = root.getChild("Base");
        Bumper = root.getChild("Bumper");
    }

    public static TexturedModelData createBodyLayer()
    {
        ModelData meshdefinition = new ModelData();
        ModelPartData partdefinition = meshdefinition.getRoot();

        ModelPartData Base = partdefinition.addChild("Base", ModelPartBuilder.create().uv(0, 46).cuboid(-5.0F, -2.0F, -5.0F, 10.0F, 2.0F, 10.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        ModelPartData Bumper = partdefinition.addChild("Bumper", ModelPartBuilder.create().uv(0, 0).cuboid(-7.0F, -16.0F, -7.0F, 14.0F, 14.0F, 14.0F, new Dilation(0.0F))
            .uv(0, 28).cuboid(-6.0F, -22.0F, -6.0F, 12.0F, 6.0F, 12.0F, new Dilation(0.0F))
            .uv(56, 1).cuboid(-5.0F, -27.0F, -5.0F, 10.0F, 5.0F, 10.0F, new Dilation(0.0F))
            .uv(56, 17).cuboid(-4.0F, -30.0F, -4.0F, 8.0F, 3.0F, 8.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        ModelPartData Left_Side = Bumper.addChild("Left_Side", ModelPartBuilder.create().uv(72, 28).cuboid(-11.3308F, -12.0465F, -1.5F, 10.0F, 10.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(3.3308F, -12.7034F, 0.5F, 0.0F, 0.0F, 0.7854F));

        ModelPartData Right_Side = Bumper.addChild("Right_Side", ModelPartBuilder.create().uv(48, 28).mirrored().cuboid(1.3261F, -12.0465F, -1.5F, 10.0F, 10.0F, 2.0F, new Dilation(0.0F)).mirrored(false), ModelTransform.of(-3.3308F, -12.7034F, 0.5F, 0.0F, 0.0F, -0.7854F));

        return TexturedModelData.of(meshdefinition, 128, 128);
    }

    @Override
    public void setAngles(@NotNull SquidBumperEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {

    }

    @Override
    public void animateModel(@NotNull SquidBumperEntity entityIn, float limbSwing, float limbSwingAmount, float partialTick)
    {
        super.animateModel(entityIn, limbSwing, limbSwingAmount, partialTick);

        scale = entityIn.getBumperScale(MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true));
        Bumper.yaw = MathHelper.RADIANS_PER_DEGREE * MathHelper.lerp(partialTick, entityIn.headYaw, entityIn.prevHeadYaw) + (float) Math.PI;

        Base.pitch = 0.0F;
        Base.yaw = 0.0F;
        Base.roll = 0.0F;

        float scale = entityIn.getBumperScale(partialTick);

        Bumper.pivotY = 24;

        if (entityIn.getInkHealth() <= 0f)
        {
            Bumper.pivotY *= 1 / scale;
        }
    }

    @Override
    public void render(@NotNull MatrixStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color)
    {
        Base.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);

        poseStack.push();
        poseStack.scale(scale, scale, scale);
        Bumper.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        poseStack.pop();
    }

    public void renderBase(MatrixStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color)
    {
        Base.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    public void renderBumper(MatrixStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color)
    {
        Bumper.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}