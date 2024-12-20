package net.splatcraft.client.models.inktanks;// Made with Blockbench 4.8.3
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.splatcraft.Splatcraft;

public class ArmoredInkTankModel extends AbstractInkTankModel
{
    // This layer location should be baked with EntityRendererFactory.Context in the entity renderer and passed into this model's constructor
    public static final EntityModelLayer LAYER_LOCATION = new EntityModelLayer(Splatcraft.identifierOf("armoredinktankmodel"), "main");

    public ArmoredInkTankModel(ModelPart root)
    {
        super(root);
        ModelPart Ink_Tank = root.getChild("body").getChild("Torso").getChild("Ink_Tank");

        for (int i = 0; i < 6; i++)
            inkPieces.add(Ink_Tank.getChild("InkPiece_" + i));
    }

    public static TexturedModelData createBodyLayer()
    {
        ModelData meshdefinition = new ModelData();
        ModelPartData partdefinition = meshdefinition.getRoot();

        createEmptyMesh(partdefinition);

        Dilation deformation = new Dilation(1);
        ModelPartData body = partdefinition.addChild("body", ModelPartBuilder.create().uv(16, 0).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, deformation), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        partdefinition.addChild("right_arm", ModelPartBuilder.create().uv(40, 0).cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, deformation), ModelTransform.pivot(-5.0F, 2.0F, 0.0F));
        partdefinition.addChild("left_arm", ModelPartBuilder.create().uv(40, 0).mirrored().cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, deformation), ModelTransform.pivot(5.0F, 2.0F, 0.0F));

        ModelPartData Torso = body.addChild("Torso", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -0.25F, 0.0F));

        ModelPartData Ink_Tank = Torso.addChild("Ink_Tank", ModelPartBuilder.create().uv(0, 19).cuboid(-2.0F, 3.25F, 3.25F, 4.0F, 1.0F, 4.0F, new Dilation(0.0F))
            .uv(16, 19).cuboid(-2.0F, 10.25F, 3.25F, 4.0F, 1.0F, 4.0F, new Dilation(0.0F))
            .uv(0, 24).cuboid(1.0F, 4.25F, 3.25F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F))
            .uv(6, 24).cuboid(1.0F, 4.25F, 6.25F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F))
            .uv(10, 24).cuboid(-2.0F, 4.25F, 6.25F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F))
            .uv(14, 24).cuboid(-2.0F, 4.25F, 3.25F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -2.25F, -1.225F));

        for (int i = 0; i < 6; i++)
        {
            Ink_Tank.addChild("InkPiece_" + i, ModelPartBuilder.create().uv(116, 0)
                .cuboid(-1.5F, -13.0F, 4.5F, 3, 1, 3, new Dilation(0f)), ModelTransform.pivot(0.0F, 23.25f, -0.75F));
        }

        return TexturedModelData.of(meshdefinition, 128, 128);
    }
}