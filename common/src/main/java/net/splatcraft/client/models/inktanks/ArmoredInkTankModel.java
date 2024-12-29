package net.splatcraft.client.models.inktanks;// Made with Blockbench 4.8.3
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.LivingEntity;
import net.splatcraft.Splatcraft;

public class ArmoredInkTankModel extends AbstractInkTankModel
{
	// This layer location should be baked with EntityRendererFactory.Context in the entity renderer and passed into this model's constructor
	public static final EntityModelLayer LAYER_LOCATION = new EntityModelLayer(Splatcraft.identifierOf("armored_ink_tank"), "main");
	private final ModelPart leftArm, rightArm;
	public ArmoredInkTankModel(ModelPart root)
	{
		super(root, 6, "body", "left_arm", "right_arm", "body:torso", "body:torso:ink_tank");
		leftArm = root.getChild("left_arm");
		rightArm = root.getChild("right_arm");
	}
	public static TexturedModelData createBodyLayer()
	{
		ModelData meshdefinition = new ModelData();
		ModelPartData partdefinition = meshdefinition.getRoot();
		
		Dilation deformation = new Dilation(1);
		ModelPartData body = partdefinition.addChild("body", ModelPartBuilder.create().uv(16, 0).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, deformation), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
		partdefinition.addChild("right_arm", ModelPartBuilder.create().uv(40, 0).cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, deformation), ModelTransform.pivot(-5.0F, 2.0F, 0.0F));
		partdefinition.addChild("left_arm", ModelPartBuilder.create().uv(40, 0).mirrored().cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, deformation), ModelTransform.pivot(5.0F, 2.0F, 0.0F));
		
		ModelPartData torso = body.addChild("torso", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -0.25F, 0.0F));
		
		ModelPartData Ink_Tank = torso.addChild("ink_tank", ModelPartBuilder.create().uv(0, 19).cuboid(-2.0F, 3.25F, 3.25F, 4.0F, 1.0F, 4.0F, new Dilation(0.0F))
			.uv(16, 19).cuboid(-2.0F, 10.25F, 3.25F, 4.0F, 1.0F, 4.0F, new Dilation(0.0F))
			.uv(0, 24).cuboid(1.0F, 4.25F, 3.25F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F))
			.uv(6, 24).cuboid(1.0F, 4.25F, 6.25F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F))
			.uv(10, 24).cuboid(-2.0F, 4.25F, 6.25F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F))
			.uv(14, 24).cuboid(-2.0F, 4.25F, 3.25F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -2.25F, -1.225F));
		
		for (int i = 0; i < 6; i++)
		{
			Ink_Tank.addChild("ink_piece_" + i, ModelPartBuilder.create().uv(116, 0)
				.cuboid(-1.5F, -13.0F, 4.5F, 3, 1, 3, new Dilation(0f)), ModelTransform.pivot(0.0F, 23.25f, -0.75F));
		}
		
		return TexturedModelData.of(meshdefinition, 128, 128);
	}
	@Override
	public void setAngles(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch)
	{
	}
	@Override
	public <M extends EntityModel<? extends LivingEntity>> void notifyState(M contextModel)
	{
		if (contextModel instanceof BipedEntityModel<?> bipedModel)
		{
			leftArm.yaw = bipedModel.leftArm.yaw;
			leftArm.pitch = bipedModel.leftArm.pitch;
			leftArm.roll = bipedModel.leftArm.roll;
			rightArm.yaw = bipedModel.rightArm.yaw;
			rightArm.pitch = bipedModel.rightArm.pitch;
			rightArm.roll = bipedModel.rightArm.roll;
		}
	}
}