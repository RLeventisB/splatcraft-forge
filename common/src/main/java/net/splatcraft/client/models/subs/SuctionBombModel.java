package net.splatcraft.client.models.subs;// Made with Blockbench 4.7.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.AbstractSubWeaponModel;
import net.splatcraft.entities.subs.SuctionBombEntity;
import org.jetbrains.annotations.NotNull;

public class SuctionBombModel extends AbstractSubWeaponModel<SuctionBombEntity>
{
    // This layer location should be baked with EntityRendererFactory.Context in the entity renderer and passed into this model's constructor
    public static final EntityModelLayer LAYER_LOCATION = new EntityModelLayer(Splatcraft.identifierOf("suctionbombmodel"), "main");
    private final ModelPart Main;
    private final ModelPart Neck;
    private final ModelPart Top;

    public SuctionBombModel(ModelPart root)
    {
        Main = root.getChild("Main");
        Neck = Main.getChild("Neck");
        Top = Neck.getChild("Top");
    }

    public static TexturedModelData createBodyLayer()
    {
        ModelData meshdefinition = new ModelData();
        ModelPartData partdefinition = meshdefinition.getRoot();

        ModelPartData Main = partdefinition.addChild("Main", ModelPartBuilder.create().uv(0, 10).cuboid(-2.0F, -3.0F, -2.0F, 4.0F, 3.0F, 4.0F, new Dilation(0.0F))
            .uv(14, 15).cuboid(-1.0F, -4.25F, -1.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.2F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData Neck = Main.addChild("Neck", ModelPartBuilder.create().uv(12, 10).cuboid(-1.0F, -3.0F, -1.0F, 2.0F, 2.0F, 2.0F, new Dilation(-0.2F))
            .uv(0, 10).cuboid(-0.5F, -1.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.1F)), ModelTransform.pivot(0.0F, -3.75F, 0.0F));

        ModelPartData Top = Neck.addChild("Top", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0F, -7.7F, -2.0F, 4.0F, 6.0F, 4.0F, new Dilation(0.0F))
            .uv(12, 0).cuboid(-1.5F, -1.7F, -1.5F, 3.0F, 1.0F, 3.0F, new Dilation(0.0F))
            .uv(0, 0).cuboid(-0.5F, -1.2F, -0.5F, 1.0F, 2.0F, 1.0F, new Dilation(-0.1F)), ModelTransform.pivot(0.0F, -2.5F, 0.0F));

        return TexturedModelData.of(meshdefinition, 32, 32);
    }

    @Override
    public void setAngles(@NotNull SuctionBombEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {

    }

    @Override
    public void animateModel(@NotNull SuctionBombEntity entityIn, float limbSwing, float limbSwingAmount, float partialTick)
    {
        super.animateModel(entityIn, limbSwing, limbSwingAmount, partialTick);

        float f9 = (float) entityIn.shakeTime - partialTick;
        if (f9 >= 0)
        {
            float f10 = -MathHelper.sin(f9 * 3f) / 6f * f9;
            Neck.pitch = f10 / 2f;
            Top.pitch = f10;
        }
        else
        {
            Neck.pitch = 0;
            Top.pitch = 0;
        }
    }

    @Override
    public void render(@NotNull MatrixStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color)
    {
        Main.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}