package net.splatcraft.handlers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.RollerItem;
import net.splatcraft.items.weapons.SlosherItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.RollerWeaponSettings;
import net.splatcraft.items.weapons.settings.SlosherWeaponSettings;
import net.splatcraft.items.weapons.subs.SubWeaponItem;
import net.splatcraft.util.action.EntityAction;

import java.util.Optional;

public class PlayerPosingHandler
{
	@SuppressWarnings("all")
	@Environment(EnvType.CLIENT)
	public static void setupPlayerAngles(PlayerEntity player, PlayerEntityModel model, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float partialTicks)
	{
		if (model == null || player == null || !EntityInfoCapability.hasCapability(player) || EntityInfoCapability.isSquid(player))
			return;
		
		EntityInfo playerInfo = EntityInfoCapability.get(player);
		
		Hand activeHand = player.getActiveHand();
		Arm handSide = player.getMainArm();
		
		if (activeHand == null)
			return;
		
		ModelPart mainHand = activeHand == Hand.MAIN_HAND && handSide == Arm.LEFT || activeHand == Hand.OFF_HAND && handSide == Arm.RIGHT ? model.leftArm : model.rightArm;
		ModelPart offHand = mainHand.equals(model.leftArm) ? model.rightArm : model.leftArm;
		
		ItemStack mainStack = player.getStackInHand(activeHand);
		ItemStack offStack = player.getStackInHand(Hand.values()[(activeHand.ordinal() + 1) % Hand.values().length]);
		int useTime = player.getItemUseTimeLeft();
		
		if (!(mainStack.getItem() instanceof WeaponBaseItem<?> weaponBaseItem))
		{
			return;
		}
		
		if (useTime > 0 || player.getItemCooldownManager().isCoolingDown(mainStack.getItem())
			|| (playerInfo != null && playerInfo.getEntityAction() != null && playerInfo.getEntityAction().getTime() > 0))
		{
			useTime = mainStack.getItem().getMaxUseTime(mainStack, player) - useTime;
			float animTime;
			float angle;
			
			switch (weaponBaseItem.getPose(player, mainStack))
			{
				case TURRET_FIRE:
					model.body.roll += 0.1;
					
					model.leftLeg.pivotX -= 1f;
					model.leftLeg.pitch -= 0.23f;
					model.leftLeg.roll -= 0.07f;
					
					model.rightLeg.pivotX -= 1f;
					model.rightLeg.pitch += 0.14f;
					model.rightLeg.roll += 0.14f;
					
					offHand.pivotX -= 1f;
					offHand.yaw = 0.1F + model.getHead().yaw;
					offHand.pitch = -(MathHelper.HALF_PI) + model.getHead().pitch + 0.1f;
					offHand.roll -= 0.4f;
					
					mainHand.yaw = -0.1F + model.getHead().yaw;
					mainHand.pitch = -(MathHelper.HALF_PI) + model.getHead().pitch;
					mainHand.roll += 0.2f;
					break;
				case DUAL_FIRE:
					if (offStack.getItem() instanceof WeaponBaseItem && ((WeaponBaseItem) offStack.getItem()).getPose(player, offStack).equals(WeaponPose.DUAL_FIRE))
					{
						offHand.yaw = -0.1F + model.getHead().yaw;
						offHand.pitch = -(MathHelper.HALF_PI) + model.getHead().pitch;
					}
				case FIRE:
					mainHand.yaw = -0.1F + model.getHead().yaw;
					mainHand.pitch = -(MathHelper.HALF_PI) + model.getHead().pitch;
					break;
				case SUB_HOLD:
					if (!(mainStack.getItem() instanceof SubWeaponItem) || useTime < ((SubWeaponItem) mainStack.getItem()).getSettings(mainStack).dataRecord.holdTime())
					{
						mainHand.yaw = -0.1F + model.getHead().yaw;
						mainHand.pitch = ((float) Math.PI / 8F);
						mainHand.roll = ((float) Math.PI / 6F) * (mainHand == model.leftArm ? -1 : 1);
					}
					break;
				case SPLATLING:
					mainHand.yaw = -0.1F + model.getHead().yaw;
					mainHand.pitch = model.getHead().pitch;
					
					break;
				case BUCKET_SWING:
					SlosherWeaponSettings settings = ((SlosherItem) mainStack.getItem()).getSettings(mainStack);
					animTime = settings.shotData.endlagTicks();
					mainHand.yaw = 0;
					mainHand.pitch = -0.36f;
					
					if (EntityAction.hasEntityAction(player))
					{
						EntityAction action = EntityAction.getEntityAction(player);
						angle = (action.getTime() - partialTicks) / action.getMaxTime();
						mainHand.pitch = -0.36f + 0.5f + MathHelper.cos(angle) * 0.5f;
					}
					break;
				case BOW_CHARGE: // bro i aint done with the rollers and theres already a bow charge 😭😭😭😭 sorry
					if (mainHand == model.rightArm)
					{
						mainHand.yaw = -0.1F + model.getHead().yaw;
						offHand.yaw = 0.1F + model.getHead().yaw + 0.4F;
						
						mainHand.pitch = (-MathHelper.HALF_PI) + model.getHead().pitch;
						offHand.pitch = (-MathHelper.HALF_PI) + model.getHead().pitch;
					}
					else
					{
						offHand.yaw = -0.1F + model.getHead().yaw - 0.4F;
						mainHand.yaw = 0.1F + model.getHead().yaw;
						offHand.pitch = (-MathHelper.HALF_PI) + model.getHead().pitch;
						mainHand.pitch = (-MathHelper.HALF_PI) + model.getHead().pitch;
					}
					break;
				case ROLL:
				{
					mainHand.yaw = model.getHead().yaw;
					Optional<RollerItem.InitialSwingAction> optional = EntityAction.geSpecificEntityActionOptional(player, RollerItem.InitialSwingAction.class);
					if (optional.isEmpty())
					{
						mainHand.pitch = (0.5F - MathHelper.PI) * 0.1F;
					}
					else
					{
						RollerItem.InitialSwingAction action = optional.get();
						RollerWeaponSettings rollerSettings = ((RollerItem) mainStack.getItem()).getSettings(mainStack);
						RollerWeaponSettings.RollerAttackDataRecord attackData = action.isGrounded() ? rollerSettings.swingData.attackData() : rollerSettings.flingData.attackData();
						
						float currentFrame = action.getTime() - partialTicks;
						float timeFromSwing = currentFrame - (action.attackFrame + 1);
						float startupTime = action.getMaxTime() - action.attackFrame;
						if (timeFromSwing > 0) // is on the startup
						{
							float swingProgress = (action.getMaxTime() - currentFrame) / startupTime;
							mainHand.pitch = (-1f + 1f / (float) Math.pow(1.4, 1 + swingProgress * 10f)) * 3;
						}
						else
						{
							// ok this becomes confusing but this value goes from 0 to -1 depending on how much time passed from 1 frame before the swing
							float movingDownProgress = timeFromSwing;
							// approximately, when movingDownProgress < -3.7, it uses the (0.5 - pi) * 0.1 value.
							mainHand.pitch = Math.min(-3 - movingDownProgress, (0.5F - MathHelper.PI) * 0.1F);
						}
					}
				}
				break;
				case BRUSH:
				{
					mainHand.pitch = 0.1F * 0.5F - ((float) Math.PI / 10F);
					Optional<RollerItem.InitialSwingAction> optional = EntityAction.geSpecificEntityActionOptional(player, RollerItem.InitialSwingAction.class);
					if (EntityAction.hasEntityAction(player))
					{
						RollerItem.InitialSwingAction action = optional.get();
						RollerWeaponSettings rollerSettings = ((RollerItem) mainStack.getItem()).getSettings(mainStack);
						RollerWeaponSettings.RollerAttackDataRecord attackData = action.isGrounded() ? rollerSettings.swingData.attackData() : rollerSettings.flingData.attackData();
						animTime = attackData.startupTime();
						angle = (float) -((action.getMaxTime() - action.getTime() + partialTicks) / animTime * Math.PI / 2f) + ((float) Math.PI) / 1.8f;
						
						mainHand.yaw = model.getHead().yaw + MathHelper.cos(angle);
					}
					else mainHand.yaw = model.getHead().yaw;
				}
				break;
			}
		}
	}
	public enum WeaponPose
	{
		NONE,
		FIRE,
		DUAL_FIRE,
		TURRET_FIRE,
		ROLL,
		BRUSH,
		BOW_CHARGE,
		BUCKET_SWING,
		SPLATLING,
		SUB_HOLD
	}
}
