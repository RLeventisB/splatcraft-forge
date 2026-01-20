package net.splatcraft.client.models.subs;// Made with Blockbench 4.11.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.AbstractSubWeaponModel;
import net.splatcraft.entities.subs.TorpedoEntity;
import org.jetbrains.annotations.NotNull;

public class TorpedoModel extends AbstractSubWeaponModel<TorpedoEntity>
{
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Splatcraft.identifierOf("torpedomodel"), "main");
	private final ModelPart open;
	private final ModelPart openPropeller;
	private final ModelPart closed;
	private boolean renderClosed;
	public TorpedoModel(ModelPart root)
	{
		open = root.getChild("open");
		openPropeller = root.getChild("open").getChild("propeller");
		closed = root.getChild("closed");
	}
	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition open = partdefinition.addOrReplaceChild("open", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -5.0F, -5.0F, 12.0F, 12.0F, 12.0F, new CubeDeformation(0.0F))
			.texOffs(0, 36).addBox(-5.0F, -3.0F, -7.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, -1.0F, -1.0F));

		PartDefinition propeller = open.addOrReplaceChild("propeller", CubeListBuilder.create().texOffs(0, 60).addBox(-8.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 1.0F, 8.0F));

		PartDefinition closed = partdefinition.addOrReplaceChild("closed", CubeListBuilder.create().texOffs(0, 36).addBox(-4.0F, -4.0F, -2.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
			.texOffs(48, 24).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -2.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}
	@Override
	public void prepareMobModel(@NotNull TorpedoEntity entity, float limbSwing, float limbSwingAmount, float partialTick)
	{
		renderClosed = !entity.isLockedIn();
		openPropeller.zRot = Mth.lerp(partialTick, entity.walkDistO, entity.walkDist);
	}
	@Override
	public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color)
	{
		if (renderClosed)
			closed.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
		else
			open.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}
}