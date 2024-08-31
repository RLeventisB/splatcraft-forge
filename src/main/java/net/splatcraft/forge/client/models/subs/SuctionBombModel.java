package net.splatcraft.forge.client.models.subs;// Made with Blockbench 4.7.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.client.models.AbstractSubWeaponModel;
import net.splatcraft.forge.entities.subs.SuctionBombEntity;
import org.jetbrains.annotations.NotNull;

public class SuctionBombModel extends AbstractSubWeaponModel<SuctionBombEntity>
{
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(Splatcraft.MODID, "suctionbombmodel"), "main");
	private final ModelPart Main;
	private final ModelPart Neck;
	private final ModelPart Top;

	public SuctionBombModel(ModelPart root)
	{
		this.Main = root.getChild("Main");
		this.Neck = Main.getChild("Neck");
		this.Top = Neck.getChild("Top");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Main = partdefinition.addOrReplaceChild("Main", CubeListBuilder.create().texOffs(0, 10).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(14, 15).addBox(-1.0F, -4.25F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.2F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Neck = Main.addOrReplaceChild("Neck", CubeListBuilder.create().texOffs(12, 10).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.2F))
		.texOffs(0, 10).addBox(-0.5F, -1.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, -3.75F, 0.0F));

		PartDefinition Top = Neck.addOrReplaceChild("Top", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -7.7F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(-1.5F, -1.7F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-0.5F, -1.2F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offset(0.0F, -2.5F, 0.0F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void setupAnim(@NotNull SuctionBombEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void prepareMobModel(@NotNull SuctionBombEntity entityIn, float limbSwing, float limbSwingAmount, float partialTick)
	{
		super.prepareMobModel(entityIn, limbSwing, limbSwingAmount, partialTick);

		float f9 = (float)entityIn.shakeTime - partialTick;
		if(f9 >= 0)
		{
			float f10 = -Mth.sin(f9*3f)/6f * f9;
			Neck.xRot = f10/2f;
			Top.xRot = f10;
		}
		else
		{
			Neck.xRot = 0;
			Top.xRot = 0;
		}
	}

	@Override
	public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		Main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}