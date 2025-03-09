package net.splatcraft.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
@Mixin(PlayerModel.class)
public class PlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T>
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
	private boolean slim;
	public PlayerModelMixin(ModelPart part)
	{
		super(part);
	}
	@Inject(method = "setupAnim*", at = @At(value = "HEAD"))
	private void setRotationAnglesHead(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci)
	{
		if (!(entityIn instanceof Player))
			return;
		
		splatcraft$resetRotationAngles();
		splatcraft$resetVisibilities();
	}
	@Inject(method = "setupAnim*", at = @At(value = "TAIL"))
	private void setRotationAnglesTail(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci)
	{
		if (!(entityIn instanceof Player player))
			return;
		
		PlayerModel<T> model = (PlayerModel<T>) (Object) this;
		PlayerPosingHandler.setupPlayerAngles(player, model, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
		
		splatcraft$setupRotationAngles();
	}
	@Unique
	private void splatcraft$setupRotationAngles()
	{
		rightSleeve.copyFrom(rightArm);
		leftSleeve.copyFrom(leftArm);
		rightPants.copyFrom(rightLeg);
		leftPants.copyFrom(leftLeg);
		jacket.copyFrom(body);
		hat.copyFrom(head);
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
		rightArm.x = -5.0F;
		rightArm.y = slim ? 2.5F : 2.0F;
		rightArm.z = 0.0F;
		
		splatcraft$resetAll(rightSleeve);
		rightSleeve.x = -5.0F;
		rightSleeve.y = slim ? 2.5F : 2.0F;
		rightSleeve.z = 10.0F;
		
		splatcraft$resetAll(leftArm);
		leftArm.x = 5.0F;
		leftArm.y = slim ? 2.5F : 2.0F;
		leftArm.z = 0.0F;
		
		splatcraft$resetAll(leftSleeve);
		leftSleeve.x = 5.0F;
		leftSleeve.y = slim ? 2.5F : 2.0F;
		leftSleeve.z = 0.0F;
		
		splatcraft$resetAll(leftLeg);
		leftLeg.x = 1.9F;
		leftLeg.y = 12.0F;
		leftLeg.z = 0.0F;
		
		splatcraft$resetAll(leftPants);
		leftPants.copyFrom(leftLeg);
		
		splatcraft$resetAll(rightLeg);
		rightLeg.x = -1.9F;
		rightLeg.y = 12.0F;
		rightLeg.z = 0.0F;
		
		splatcraft$resetAll(rightPants);
		rightPants.copyFrom(rightLeg);
	}
	/**
	 * Resets the rotation angles and points to zero for the given model renderer
	 *
	 * @param part the model part to reset
	 */
	@Unique
	private void splatcraft$resetAll(ModelPart part)
	{
		part.xRot = 0.0F;
		part.yRot = 0.0F;
		part.zRot = 0.0F;
		part.x = 0.0F;
		part.y = 0.0F;
		part.z = 0.0F;
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