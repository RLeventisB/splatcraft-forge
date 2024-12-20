package net.splatcraft.client.models;// Made with Blockbench 4.7.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import org.jetbrains.annotations.NotNull;

public class InkSquidModel extends EntityModel<LivingEntity>
{
    // This layer location should be baked with EntityRendererFactory.Context in the entity renderer and passed into this model's constructor
    public static final EntityModelLayer LAYER_LOCATION = new EntityModelLayer(Splatcraft.identifierOf("inksquidmodel"), "main");
    private final ModelPart squid;
    private final ModelPart rightLimb;
    private final ModelPart leftLimb;

    public InkSquidModel(ModelPart root)
    {
        squid = root.getChild("squid");
        rightLimb = squid.getChild("RightLimb");
        leftLimb = squid.getChild("LeftLimb");
    }

    public static TexturedModelData createBodyLayer()
    {
        ModelData meshdefinition = new ModelData();
        ModelPartData partdefinition = meshdefinition.getRoot();

        ModelPartData squid = partdefinition.addChild("squid", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        ModelPartData Body = squid.addChild("Body", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, -2.0F, 8.0F, 4.0F, 4.0F, new Dilation(0.0F))
            .uv(0, 9).cuboid(-6.0F, -5.0F, -6.0F, 12.0F, 5.0F, 4.0F, new Dilation(0.0F))
            .uv(27, 0).cuboid(-5.0F, -4.0F, -8.0F, 10.0F, 4.0F, 2.0F, new Dilation(0.0F))
            .uv(32, 6).cuboid(-4.0F, -3.0F, -10.0F, 8.0F, 3.0F, 2.0F, new Dilation(0.0F))
            .uv(32, 12).cuboid(-2.0F, -2.0F, -12.0F, 4.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        Body.addChild("eyes", ModelPartBuilder.create().uv(18, 19).cuboid(-2.5F, -5.0F, -2.0F, 5.0F, 1.0F, 2.0F, new Dilation(0.0F))
            .uv(0, 19).cuboid(-3.0F, -4.5F, -2.25F, 6.0F, 1.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        Body.addChild("tentacles", ModelPartBuilder.create().uv(56, 0).cuboid(-2.6593F, -3.75F, 6.6593F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F))
            .uv(56, 0).cuboid(-1.495F, -3.75F, 5.495F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F))
            .uv(56, 0).cuboid(-0.1161F, -2.25F, 4.1161F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F))
            .uv(56, 0).cuboid(-1.495F, -2.25F, 5.495F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F))
            .uv(56, 0).cuboid(-0.1161F, -3.75F, 4.1161F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F))
            .uv(56, 0).cuboid(0.9875F, -3.75F, 2.9671F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(4.0F, 0.0F, -2.25F, 0.0F, -0.7854F, 0.0F));

        squid.addChild("LeftLimb", ModelPartBuilder.create().uv(0, 23).cuboid(0.0F, -3.0F, 0.0F, 2.0F, 3.0F, 3.0F, new Dilation(0.0F))
            .uv(0, 29).cuboid(-1.0F, -3.0F, 3.0F, 3.0F, 3.0F, 4.0F, new Dilation(0.0F)), ModelTransform.pivot(2.0F, 0.0F, 2.0F));

        squid.addChild("RightLimb", ModelPartBuilder.create().uv(10, 23).mirrored().cuboid(-2.0F, -3.0F, 0.0F, 2.0F, 3.0F, 3.0F, new Dilation(0.0F)).mirrored(false)
            .uv(14, 29).mirrored().cuboid(-2.0F, -3.0F, 3.0F, 3.0F, 3.0F, 4.0F, new Dilation(0.0F)).mirrored(false), ModelTransform.pivot(-2.0F, 0.0F, 2.0F));

        return TexturedModelData.of(meshdefinition, 64, 64);
    }

    @Override
    public void setAngles(@NotNull LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {

    }

    @Override
    public void copyStateTo(@NotNull EntityModel<LivingEntity> other)
    {
        super.copyStateTo(other);

        if (other instanceof InkSquidModel otherSquid)
        {
            otherSquid.squid.copyTransform(squid);
            otherSquid.leftLimb.copyTransform(leftLimb);
            otherSquid.rightLimb.copyTransform(rightLimb);
        }
    }

    @Override
    public void animateModel(@NotNull LivingEntity entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTickTime)
    {
        super.animateModel(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTickTime);
        boolean isSwimming = entitylivingbaseIn.isSwimming();

        if (!entitylivingbaseIn.hasVehicle())
        {
            InkOverlayInfo info = InkOverlayCapability.get(entitylivingbaseIn);

            double angle = isSwimming ? -(entitylivingbaseIn.getPitch() * Math.PI / 180F) : MathHelper.lerp(partialTickTime, info.getSquidRotO(), info.getSquidRot()) * 1.1f;
            squid.pitch = (float) -Math.min(Math.PI / 2, Math.max(-Math.PI / 2, angle));
        }

        if (entitylivingbaseIn.isOnGround() || isSwimming)
        {
            rightLimb.yaw = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount / (isSwimming ? 2.2f : 1.5f);
            leftLimb.yaw = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount / (isSwimming ? 2.2f : 1.5f);
        }
        else
        {
            if (Math.abs(Math.round(rightLimb.yaw * 100)) != 0)
            {
                rightLimb.yaw -= rightLimb.yaw / 8f;
            }
            if (Math.abs(Math.round(leftLimb.yaw * 100)) != 0)
            {
                leftLimb.yaw -= leftLimb.yaw / 8f;
            }
        }
    }

    @Override
    public void render(@NotNull MatrixStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color)
    {
        squid.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}