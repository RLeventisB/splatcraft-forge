package net.splatcraft.client.models.inktanks;// Made with Blockbench 4.8.3
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.LivingEntity;
import net.splatcraft.Splatcraft;

public class ClassicInkTankModel extends AbstractInkTankModel
{
	// This layer location should be baked with EntityRendererFactory.Context in the entity renderer and passed into this model's constructor
	public static final EntityModelLayer LAYER_LOCATION = new EntityModelLayer(Splatcraft.identifierOf("classic_ink_tank"), "main");
	public ClassicInkTankModel(ModelPart root)
	{
		super(root, 7, "body", "body:torso", "body:torso:ink_tank");
	}
	public static TexturedModelData createBodyLayer()
	{
		ModelData meshdefinition = new ModelData();
		ModelPartData partdefinition = meshdefinition.getRoot();
		
		ModelPartData body = partdefinition.addChild("body", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
		
		ModelPartData torso = body.addChild("torso", ModelPartBuilder.create().uv(0, 0).cuboid(-4.75F, -0.25F, -2.5F, 9.0F, 12.0F, 5.0F, new Dilation(0.0F))
			.uv(30, 0).cuboid(-1.0F, 3.0F, 2.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -0.25F, 0.0F));
		
		ModelPartData Ink_Tank = torso.addChild("ink_tank", ModelPartBuilder.create().uv(0, 19).cuboid(-2.0F, 3.25F, 3.25F, 4.0F, 1.0F, 4.0F, new Dilation(0.0F))
			.uv(20, 28).cuboid(-1.5F, 2.25F, 3.75F, 3.0F, 1.0F, 3.0F, new Dilation(0.0F))
			.uv(22, 32).cuboid(-2.0F, 2.0F, 4.75F, 4.0F, 2.0F, 1.0F, new Dilation(0.0F))
			.uv(21, 35).cuboid(-0.5F, 2.0F, 3.25F, 1.0F, 2.0F, 4.0F, new Dilation(0.0F))
			.uv(16, 19).cuboid(-2.0F, 11.25F, 3.25F, 4.0F, 1.0F, 4.0F, new Dilation(0.0F))
			.uv(20, 24).cuboid(-1.5F, 11.75F, 3.75F, 3.0F, 1.0F, 3.0F, new Dilation(0.0F))
			.uv(0, 24).cuboid(1.0F, 4.25F, 3.25F, 1.0F, 7.0F, 1.0F, new Dilation(0.0F))
			.uv(6, 24).cuboid(1.0F, 4.25F, 6.25F, 1.0F, 7.0F, 1.0F, new Dilation(0.0F))
			.uv(10, 24).cuboid(-2.0F, 4.25F, 6.25F, 1.0F, 7.0F, 1.0F, new Dilation(0.0F))
			.uv(14, 24).cuboid(-2.0F, 4.25F, 3.25F, 1.0F, 7.0F, 1.0F, new Dilation(0.0F))
			.uv(12, 39).cuboid(0.0F, 9.25F, 6.25F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
			.uv(12, 39).cuboid(0.0F, 7.25F, 6.25F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
			.uv(12, 39).cuboid(0.0F, 5.25F, 6.25F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
			.uv(0, 33).cuboid(-1.0F, 0.75F, 4.25F, 2.0F, 2.0F, 2.0F, new Dilation(0.0F))
			.uv(8, 34).cuboid(-2.75F, 3.75F, 4.75F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.75F, 0.25F));
		
		for (int i = 0; i < 7; i++)
		{
			Ink_Tank.addChild("ink_piece_" + i, ModelPartBuilder.create().uv(52, 0)
				.cuboid(-1.5F, -12F, 4.5F, 3, 1, 3, new Dilation(0)), ModelTransform.pivot(0.0F, 23.25f, -0.75F));
		}
		
		return TexturedModelData.of(meshdefinition, 64, 64);
	}
	@Override
	public void setAngles(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch)
	{
	
	}
}