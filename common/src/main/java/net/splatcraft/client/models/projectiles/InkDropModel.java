package net.splatcraft.client.models.projectiles;// Made with Blockbench 4.8.3
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.Splatcraft;
import net.splatcraft.entities.InkDropEntity;
import org.jetbrains.annotations.NotNull;

public class InkDropModel extends EntityModel<InkDropEntity>
{
    // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(Splatcraft.MODID, "shooterinkprojectilemodel"), "main");
    protected final ModelPart main;

    public InkDropModel(ModelPart root)
    {
        this.main = root.getChild("main");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition main = partdefinition.addOrReplaceChild("main", CubeListBuilder.create().texOffs(0, 5).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition middle = main.addOrReplaceChild("middle", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -0.75F, 2.0F, 2.0F, 3.0F, new CubeDeformation(-0.3F)), PartPose.offset(0.0F, -1.0F, 1.5F));

        PartDefinition back = middle.addOrReplaceChild("back", CubeListBuilder.create().texOffs(7, 0).addBox(-0.5F, -0.5F, -0.75F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.15F)), PartPose.offset(0.0F, 0.0F, 2.0F));

        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    @Override
    public void setupAnim(@NotNull InkDropEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {

    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}