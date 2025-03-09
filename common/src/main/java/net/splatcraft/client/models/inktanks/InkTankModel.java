package net.splatcraft.client.models.inktanks;// Made with Blockbench 4.8.3
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;
import net.splatcraft.Splatcraft;

public class InkTankModel extends AbstractInkTankModel
{
	// This layer location should be baked with EntityRendererFactory.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Splatcraft.identifierOf("ink_tank"), "main");
	public InkTankModel(ModelPart root)
	{
		super(root, 7, "head", "body", "body:torso", "body:torso:ink_tank");
	}
	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		
		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 112).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		
		PartDefinition torso = body.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 0).addBox(-4.75F, -0.25F, -2.5F, 9.0F, 12.0F, 5.0F, new CubeDeformation(0.0F))
			.texOffs(31, 0).addBox(-1.0F, 3.0F, 2.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.25F, 0.0F));
		
		PartDefinition inkTank = torso.addOrReplaceChild("ink_tank", CubeListBuilder.create().texOffs(31, 2).addBox(-0.5F, 1.75F, 2.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
			.texOffs(0, 19).addBox(-2.0F, 3.25F, 3.25F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
			.texOffs(16, 19).addBox(-2.0F, 11.25F, 3.25F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
			.texOffs(0, 24).addBox(1.0F, 4.25F, 3.25F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
			.texOffs(6, 24).addBox(1.0F, 4.25F, 6.25F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
			.texOffs(10, 24).addBox(-2.0F, 4.25F, 6.25F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
			.texOffs(14, 24).addBox(-2.0F, 4.25F, 3.25F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
			.texOffs(12, 39).addBox(0.0F, 9.25F, 6.25F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
			.texOffs(12, 39).addBox(0.0F, 7.25F, 6.25F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
			.texOffs(12, 39).addBox(0.0F, 5.25F, 6.25F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
			.texOffs(0, 33).addBox(-1.0F, 2.25F, 4.25F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
			.texOffs(8, 33).addBox(-3.5F, 2.5F, 4.25F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.75F, 0.75F));
		
		for (int i = 0; i < 7; i++)
		{
			inkTank.addOrReplaceChild("ink_piece_" + i, CubeListBuilder.create().texOffs(116, 30)
				.addBox(-1.5F, -12F, 4.5F, 3, 1, 3, new CubeDeformation(0)), PartPose.offset(0.0F, 23.25f, -0.75F));
		}
		
		return LayerDefinition.create(meshdefinition, 128, 128);
	}
	@Override
	public void setupAnim(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch)
	{
	
	}
}