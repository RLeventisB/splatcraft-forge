package net.splatcraft.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Author: MrCrayfish, from Obfuscate 1.17 <a href="https://github.com/MrCrayfish/Obfuscate/blob/1.17.X/src/main/java/com/mrcrayfish/obfuscate/client/event/PlayerModelEvent.java">...</a>
 */
@Mixin(PlayerEntityModel.class)
public class PlayerModelMixin<T extends LivingEntity> extends BipedEntityModel<T>
{
    @Shadow
    @Final
    public ModelPart leftSleeve;
    @Shadow
    @Final
    public ModelPart rightSleeve;
    @Shadow
    @Final
    public ModelPart leftPants;
    @Shadow
    @Final
    public ModelPart rightPants;
    @Shadow
    @Final
    public ModelPart jacket;
    @Shadow
    @Final
    private boolean thinArms;

    public PlayerModelMixin(ModelPart part)
    {
        super(part);
    }

    @Inject(method = "setAngles*", at = @At(value = "HEAD"))
    private void setRotationAnglesHead(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci)
    {
        if (!(entityIn instanceof PlayerEntity))
            return;

        splatcraft$resetRotationAngles();
        splatcraft$resetVisibilities();
    }

    @Inject(method = "setAngles*", at = @At(value = "TAIL"))
    private void setRotationAnglesTail(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci)
    {
        if (!(entityIn instanceof PlayerEntity player))
            return;

        PlayerEntityModel<T> model = (PlayerEntityModel<T>) (Object) this;
        PlayerPosingHandler.setupPlayerAngles(player, model, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true));

        splatcraft$setupRotationAngles();
    }

    @Unique
    private void splatcraft$setupRotationAngles()
    {
        rightSleeve.copyTransform(rightArm);
        leftSleeve.copyTransform(leftArm);
        rightPants.copyTransform(rightLeg);
        leftPants.copyTransform(leftLeg);
        jacket.copyTransform(body);
        hat.copyTransform(head);
    }

    /**
     * Resets all the rotations and rotation points back to their initial values. This makes it
     * so ever developer doesn't have to do it themselves.
     */
    @Unique
    private void splatcraft$resetRotationAngles()
    {
        splatcraft$resetAll(head);
        splatcraft$resetAll(hat);
        splatcraft$resetAll(body);
        splatcraft$resetAll(jacket);

        splatcraft$resetAll(rightArm);
        rightArm.pivotX = -5.0F;
        rightArm.pivotY = thinArms ? 2.5F : 2.0F;
        rightArm.pivotZ = 0.0F;

        splatcraft$resetAll(rightSleeve);
        rightSleeve.pivotX = -5.0F;
        rightSleeve.pivotY = thinArms ? 2.5F : 2.0F;
        rightSleeve.pivotZ = 10.0F;

        splatcraft$resetAll(leftArm);
        leftArm.pivotX = 5.0F;
        leftArm.pivotY = thinArms ? 2.5F : 2.0F;
        leftArm.pivotZ = 0.0F;

        splatcraft$resetAll(leftSleeve);
        leftSleeve.pivotX = 5.0F;
        leftSleeve.pivotY = thinArms ? 2.5F : 2.0F;
        leftSleeve.pivotZ = 0.0F;

        splatcraft$resetAll(leftLeg);
        leftLeg.pivotX = 1.9F;
        leftLeg.pivotY = 12.0F;
        leftLeg.pivotZ = 0.0F;

        splatcraft$resetAll(leftPants);
        leftPants.copyTransform(leftLeg);

        splatcraft$resetAll(rightLeg);
        rightLeg.pivotX = -1.9F;
        rightLeg.pivotY = 12.0F;
        rightLeg.pivotZ = 0.0F;

        splatcraft$resetAll(rightPants);
        rightPants.copyTransform(rightLeg);
    }

    /**
     * Resets the rotation angles and points to zero for the given model renderer
     *
     * @param part the model part to reset
     */
    @Unique
    private void splatcraft$resetAll(ModelPart part)
    {
        part.pitch = 0.0F;
        part.yaw = 0.0F;
        part.roll = 0.0F;
        part.pivotX = 0.0F;
        part.pivotY = 0.0F;
        part.pivotZ = 0.0F;
    }

    @Unique
    private void splatcraft$resetVisibilities()
    {
        head.visible = true;
        body.visible = true;
        rightArm.visible = true;
        leftArm.visible = true;
        rightLeg.visible = true;
        leftLeg.visible = true;
    }
}