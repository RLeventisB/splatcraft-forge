package net.splatcraft.client.models.inktanks;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AbstractInkTankModel extends BipedEntityModel<LivingEntity>
{
    protected List<ModelPart> inkPieces = new ArrayList<>();
    protected float inkBarY = 0;

    public AbstractInkTankModel(ModelPart root)
    {
        super(root);
    }

    public static void createEmptyMesh(ModelPartData partdefinition)
    {
        partdefinition.addChild("head", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        partdefinition.addChild("hat", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        partdefinition.addChild("body", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        partdefinition.addChild("right_arm", ModelPartBuilder.create(), ModelTransform.pivot(-5.0F, 2.0F, 0.0F));
        partdefinition.addChild("left_arm", ModelPartBuilder.create(), ModelTransform.pivot(5.0F, 2.0F, 0.0F));
        partdefinition.addChild("right_leg", ModelPartBuilder.create(), ModelTransform.pivot(-1.9F, 12.0F, 0.0F));
        partdefinition.addChild("left_leg", ModelPartBuilder.create(), ModelTransform.pivot(1.9F, 12.0F, 0.0F));
    }

    public void setInkLevels(float inkPctg)
    {
        for (int i = 1; i <= inkPieces.size(); i++)
        {
            ModelPart box = inkPieces.get(i - 1);
            if (inkPctg == 0)
            {
                box.visible = false;
                continue;
            }
            box.visible = true;
            box.pivotY = 23.25F - Math.min(i * inkPctg, i);
        }
    }

    @Override
    public void setAngles(@NotNull LivingEntity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        super.setAngles(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }
}
