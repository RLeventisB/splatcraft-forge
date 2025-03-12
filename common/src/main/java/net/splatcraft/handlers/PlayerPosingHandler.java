package net.splatcraft.handlers;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
	@OnlyIn(Dist.CLIENT)
	public static void setupPlayerAngles(Player player, PlayerModel model, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float partialTicks)
	{
		if (model == null || player == null || !EntityInfoCapability.hasCapability(player) || EntityInfoCapability.isSquid(player))
			return;
		
		EntityInfo playerInfo = EntityInfoCapability.get(player);
		
		InteractionHand activeHand = player.getUsedItemHand();
		HumanoidArm handSide = player.getMainArm();
		
		if (activeHand == null)
			return;
		
		ModelPart mainHand = activeHand == InteractionHand.MAIN_HAND && handSide == HumanoidArm.LEFT || activeHand == InteractionHand.OFF_HAND && handSide == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
		ModelPart offHand = mainHand.equals(model.leftArm) ? model.rightArm : model.leftArm;
		
		ItemStack mainStack = player.getItemInHand(activeHand);
		ItemStack offStack = player.getItemInHand(InteractionHand.values()[(activeHand.ordinal() + 1) % InteractionHand.values().length]);
		int useTime = player.getUseItemRemainingTicks();
		
		if (!(mainStack.getItem() instanceof WeaponBaseItem<?> weaponBaseItem))
		{
			return;
		}
		
		if (useTime > 0 || player.getCooldowns().isOnCooldown(mainStack.getItem())
			|| (playerInfo != null && playerInfo.getEntityAction() != null && playerInfo.getEntityAction().getTime() > 0))
		{
			useTime = mainStack.getItem().getUseDuration(mainStack, player) - useTime;
			
			switch (weaponBaseItem.getPose(player, mainStack))
			{
				case TURRET_FIRE:
					model.body.zRot += 0.1;
					
					model.leftLeg.x -= 1f;
					model.leftLeg.xRot -= 0.23f;
					model.leftLeg.zRot -= 0.07f;
					
					model.rightLeg.x -= 1f;
					model.rightLeg.xRot += 0.14f;
					model.rightLeg.zRot += 0.14f;
					
					offHand.x -= 1f;
					offHand.yRot = 0.1F + model.getHead().yRot;
					offHand.xRot = -(Mth.HALF_PI) + model.getHead().xRot + 0.1f;
					offHand.zRot -= 0.4f;
					
					mainHand.yRot = -0.1F + model.getHead().yRot;
					mainHand.xRot = -(Mth.HALF_PI) + model.getHead().xRot;
					mainHand.zRot += 0.2f;
					break;
				case DUAL_FIRE:
					if (offStack.getItem() instanceof WeaponBaseItem && ((WeaponBaseItem) offStack.getItem()).getPose(player, offStack).equals(WeaponPose.DUAL_FIRE))
					{
						offHand.yRot = -0.1F + model.getHead().yRot;
						offHand.xRot = -(Mth.HALF_PI) + model.getHead().xRot;
					}
				case FIRE:
					mainHand.yRot = -0.1F + model.getHead().yRot;
					mainHand.xRot = -(Mth.HALF_PI) + model.getHead().xRot;
					break;
				case SUB_HOLD:
					if (!(mainStack.getItem() instanceof SubWeaponItem) || useTime < ((SubWeaponItem) mainStack.getItem()).getSettings(mainStack).dataRecord.holdTime())
					{
						mainHand.yRot = -0.1F + model.getHead().yRot;
						mainHand.xRot = ((float) Math.PI / 8F);
						mainHand.zRot = ((float) Math.PI / 6F) * (mainHand == model.leftArm ? -1 : 1);
					}
					break;
				case SPLATLING:
					mainHand.yRot = -0.1F + model.getHead().yRot;
					mainHand.xRot = model.getHead().xRot;
					
					break;
				case BUCKET_SWING:
				{
					// todo: fix this lol, maybe with a taylor series that makes a slope when the player attacks
					SlosherWeaponSettings settings = ((SlosherItem) mainStack.getItem()).getSettings(mainStack);
					float animTime = settings.shotData.endlagTicks();
					mainHand.yRot = 0;
					mainHand.xRot = -0.36f;
					
					if (EntityAction.hasEntityAction(player))
					{
						EntityAction action = EntityAction.getEntityAction(player);
						float angle = (action.getTime() - partialTicks) / action.getMaxTime();
						mainHand.xRot = -0.36f + 0.5f + Mth.cos(angle) * 0.5f;
					}
				}
				break;
				case BOW_CHARGE: // bro i aint done with the rollers and theres already a bow charge 😭😭😭😭 sorry
					if (mainHand == model.rightArm)
					{
						mainHand.yRot = -0.1F + model.getHead().yRot;
						offHand.yRot = 0.1F + model.getHead().yRot + 0.4F;
						
						mainHand.xRot = (-Mth.HALF_PI) + model.getHead().xRot;
						offHand.xRot = (-Mth.HALF_PI) + model.getHead().xRot;
					}
					else
					{
						offHand.yRot = -0.1F + model.getHead().yRot - 0.4F;
						mainHand.yRot = 0.1F + model.getHead().yRot;
						offHand.xRot = (-Mth.HALF_PI) + model.getHead().xRot;
						mainHand.xRot = (-Mth.HALF_PI) + model.getHead().xRot;
					}
					break;
				case ROLLER_SWING:
				{
					mainHand.yRot = model.getHead().yRot;
					Optional<RollerItem.InitialSwingAction> optional = EntityAction.getSpecificEntityActionOptional(player, RollerItem.InitialSwingAction.class);
					optional.ifPresentOrElse(action ->
					{
						RollerWeaponSettings rollerSettings = ((RollerItem) mainStack.getItem()).getSettings(mainStack);
						RollerWeaponSettings.RollerAttackDataRecord attackData = rollerSettings.getAttackData(action.isGrounded()).attackData();
						
						float currentFrame = action.getTime() - partialTicks;
						float timeFromSwing = currentFrame - (action.attackFrame + 1);
						float startupTime = action.getMaxTime() - action.attackFrame;
						if (timeFromSwing > 0) // is on the startup
						{
							float swingProgress = (action.getMaxTime() - currentFrame) / startupTime;
							mainHand.xRot = (-1f + 1f / (float) Math.pow(1.4, 1 + swingProgress * 10f)) * 3;
						}
						else
						{
							// ok this becomes confusing but this value goes from 0 to -1 depending on how much time passed from 1 frame before the swing
							float movingDownProgress = timeFromSwing;
							// approximately, when movingDownProgress < -3.7, it uses the (0.5 - pi) * 0.1 value.
							mainHand.xRot = Math.min(-3 - movingDownProgress, (0.5F - Mth.PI) * 0.1F);
						}
					}, () -> mainHand.xRot = (0.5F - Mth.PI) * 0.1F);
				}
				break;
				case BRUSH:
				{
					mainHand.xRot = (0.3F - Mth.PI) * 0.1f;
					Optional<RollerItem.InitialSwingAction> optional = EntityAction.getSpecificEntityActionOptional(player, RollerItem.InitialSwingAction.class);
					optional.ifPresentOrElse(action ->
					{
						RollerWeaponSettings rollerSettings = ((RollerItem) mainStack.getItem()).getSettings(mainStack);
						RollerWeaponSettings.RollerAttackDataRecord attackData = rollerSettings.swingData.attackData();
						float animTime = attackData.attackTime();
						float angle = (float) -((action.getMaxTime() - action.getTime() - partialTicks) / animTime * Mth.PI / 2f) + ((float) Mth.PI) / 1.8f;
						
						mainHand.yRot = model.getHead().yRot + Mth.cos(angle);
					}, () -> mainHand.yRot = model.getHead().yRot);
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
		ROLLER_SWING,
		BRUSH,
		BOW_CHARGE,
		BUCKET_SWING,
		SPLATLING,
		SUB_HOLD
	}
}
