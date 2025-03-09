package net.splatcraft.client.handlers;

import dev.architectury.event.events.common.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.Splatcraft;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.DualieItem;
import net.splatcraft.items.weapons.RollerItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.AbstractWeaponSettings;
import net.splatcraft.mixin.accessors.EntityAccessor;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.SquidInputPacket;
import net.splatcraft.registries.SplatcraftAttributes;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.action.EntityAction;

import java.util.HashMap;
import java.util.Optional;

public class PlayerMovementHandler
{
	public static final HashMap<Player, Input> unmodifiedInput = new HashMap<>();
	private static final AttributeModifier INK_SWIM_SPEED = new AttributeModifier(Splatcraft.identifierOf("ink_movement_boost"), 0D, AttributeModifier.Operation.ADD_VALUE);
	private static final AttributeModifier SQUID_SWIM_SPEED = new AttributeModifier(Splatcraft.identifierOf("squid_swim_speed"), 0.2D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	private static final AttributeModifier ENEMY_INK_SPEED = new AttributeModifier(Splatcraft.identifierOf("enemy_ink_penalty"), -0.5D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	public static void registerEvents()
	{
		TickEvent.PLAYER_POST.register((player) ->
		{
			Vec3 deltaMovement = player.getDeltaMovement();
			if (Boolean.TRUE.equals(SplatcraftConfig.get("splatcraft.limitFallSpeed")) && deltaMovement.y < -0.5)
			{
				player.setDeltaMovement(deltaMovement.x, -0.5, deltaMovement.z);
			}
		});
		TickEvent.PLAYER_PRE.register(PlayerMovementHandler::playerMovement);
	}
	public static void playerMovement(Player player)
	{
		EntityInfo playerInfo = EntityInfoCapability.get(player);
		if (playerInfo == null)
			playerInfo = new EntityInfo();
		
		Optional<EntityAction> action = EntityAction.getEntityActionOptional(player);
		
		AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
//            EntityAttributeInstance swimAttribute = player.getAttributeInstance(attributes.SWIM_SPEED.get());
		
		if (speedAttribute.hasModifier(INK_SWIM_SPEED.id()))
			speedAttribute.removeModifier(INK_SWIM_SPEED);
		if (speedAttribute.hasModifier(ENEMY_INK_SPEED.id()))
			speedAttribute.removeModifier(ENEMY_INK_SPEED);
//            if (swimAttribute.hasModifier(SQUID_SWIM_SPEED.id()))
//                swimAttribute.removeModifier(SQUID_SWIM_SPEED);
		
		if (speedAttribute.getModifier(SplatcraftItems.SPEED_MOD_IDENTIFIER) != null)
			speedAttribute.removeModifier(SplatcraftItems.SPEED_MOD_IDENTIFIER);
		
		if (InkBlockUtils.onEnemyInk(player))
		{
			//player.setVelocity(player.getVelocity().x, Math.min(player.getVelocity().y, 0.05f), player.getVelocity().z);
			if (!speedAttribute.hasModifier(ENEMY_INK_SPEED.id()))
				speedAttribute.addTransientModifier(ENEMY_INK_SPEED);
		}
		
		if (playerInfo.isSquid())
		{
			if (InkBlockUtils.canSquidSwim(player) && !speedAttribute.hasModifier(INK_SWIM_SPEED.id()) && player.onGround())
				speedAttribute.addTransientModifier(INK_SWIM_SPEED);
//                if (!swimAttribute.hasModifier(SQUID_SWIM_SPEED.id()))
//                    swimAttribute.addTemporaryModifier(SQUID_SWIM_SPEED);
		}
		
		action.ifPresent(v ->
		{
			if (v.getSlotIndex() >= 0)
				player.getInventory().selected = v.getSlotIndex();
		});
		
		tickWeaponMobilityAttribute(player, speedAttribute);
		
		if (!player.getAbilities().flying)
			if (speedAttribute.hasModifier(INK_SWIM_SPEED.id()))
				player.moveRelative((float) player.getAttributeValue(SplatcraftAttributes.inkSwimSpeed) * (player.onGround() ? 1 : 0.75f), new Vec3(player.xxa, 0.0f, player.zza).normalize());
	}
	private static void tickWeaponMobilityAttribute(LivingEntity entity, AttributeInstance speedAttribute)
	{
		ItemStack useStack = entity.getUseItem();
		if (speedAttribute.hasModifier(AbstractWeaponSettings.WEAPON_MOBILITY_ATTIBUTE_ID))
			speedAttribute.removeModifier(AbstractWeaponSettings.WEAPON_MOBILITY_ATTIBUTE_ID);
		
		if (useStack.getItem() instanceof WeaponBaseItem<?> weapon && weapon.hasSpeedModifier(entity, useStack))
		{
			var mod = weapon.getSpeedModifier(entity, useStack);
			if (!speedAttribute.hasModifier(mod.id()))
				speedAttribute.addTransientModifier(mod);
		}
	}
	@Environment(EnvType.CLIENT)
	public static void onInputUpdate(LocalPlayer player, Input input)
	{
		EntityInfoCapability.getOptional(player).ifPresent(info ->
		{
			Input clonedInput = unmodifiedInput.computeIfAbsent(player, v -> new Input());
			copyTo(input, clonedInput);
			
			if (CommonUtils.isEntityMatchImmobile(player, info))
			{
				input.leftImpulse = 0;
				input.forwardImpulse = 0;
				input.jumping = false;
				input.shiftKeyDown = false;
				return;
			}
			
			float speedMod = !input.shiftKeyDown ? info.isSquid() && InkBlockUtils.canSquidHide(player) ? 15f : 2f : 1f;
			
			input.forwardImpulse *= speedMod;
			input.leftImpulse *= speedMod;
			
			if (info.isSquid())
			{
				handleSquidMovement(info, player, input.leftImpulse, input.forwardImpulse, input.jumping, input.shiftKeyDown, input);
			}
			
			if (player.isUsingItem())
			{
				ItemStack stack = player.getUseItem();
				if (!stack.isEmpty())
				{
					if (stack.getItem() instanceof WeaponBaseItem)
					{
						input.leftImpulse *= 5.0F;
						input.forwardImpulse *= 5.0F;
					}
				}
			}
			
			EntityAction.getEntityActionOptional(player).ifPresent(action ->
			{
				if (!action.canMove())
				{
					if (!(action instanceof DualieItem.DodgeRollAction))
					{
						input.jumping = false;
						input.forwardImpulse = 0;
						input.leftImpulse = 0;
					}
				}
				else if (action.getStoredStack().getItem() instanceof RollerItem rollerItem)
				{
					input.forwardImpulse = Math.min(1, Math.abs(input.forwardImpulse)) * Math.signum(input.forwardImpulse) * rollerItem.getSettings(action.getStoredStack()).swingData.mobility();
					input.leftImpulse = Math.min(1, Math.abs(input.leftImpulse)) * Math.signum(input.leftImpulse) * rollerItem.getSettings(action.getStoredStack()).swingData.mobility();
				}
				if (action.forceCrouch())
				{
					input.shiftKeyDown = !player.getAbilities().flying;
				}
			});
		});
	}
	private static void copyTo(Input from, Input to)
	{
		to.leftImpulse = from.leftImpulse;
		to.forwardImpulse = from.forwardImpulse;
		to.up = from.up;
		to.down = from.down;
		to.left = from.left;
		to.right = from.right;
		to.jumping = from.jumping;
		to.shiftKeyDown = from.shiftKeyDown;
	}
	private static void handleSquidMovement(EntityInfo playerInfo, Player player, float movementSideways, float movementForward, boolean jumping, boolean sneaking, Input input)
	{
		if (playerInfo.getClimbedDirection().isPresent())
		{
			Direction oldClimbedDirection = playerInfo.getClimbedDirection().get();
			Direction climbedDirection = InkBlockUtils.getSquidClimbingDirection(player, movementSideways, movementForward, oldClimbedDirection);
			
			if (climbedDirection != null && !player.onGround()) // if player is still swimming on a wall
			{
				playerInfo.setClimbedDirection(climbedDirection);
				Vec3 deltaMovement = player.getDeltaMovement();
				if (deltaMovement.y < 0.4f && (movementForward != 0 || movementSideways != 0)) // handle input on wall
				{
					float yaw = player.getYHeadRot();
					Vec3 vec3 =
						EntityAccessor.invokeGetInputVector(new Vec3(0f, movementForward, 0f), 0.12f, yaw).add(
							EntityAccessor.invokeGetInputVector(new Vec3(movementSideways, 0f, 0f), 0.02f, yaw)
						);
					
					deltaMovement = deltaMovement.add(vec3);
				}
				if (sneaking) // set minimum y velocity to 0 if shifting
					deltaMovement = new Vec3(deltaMovement.x, Math.max(0, deltaMovement.y), deltaMovement.z);
				
				if (climbedDirection.getAxis() != oldClimbedDirection.getAxis()) // if player swam to another wall, rotate velocity
				{
					deltaMovement = deltaMovement.yRot(Mth.DEG_TO_RAD * (climbedDirection.toYRot() - oldClimbedDirection.toYRot()));
				}
				
				if (climbedDirection.getAxis() == Direction.Axis.X) // set velocity perpendicular to the wall to 0 because YOU CANNOT ESCAPE THE WALL (unless you press back).
				{
					double parallelMovement = deltaMovement.x;
					if (Math.abs(parallelMovement) < 0.6)
						deltaMovement = new Vec3(0, deltaMovement.y, deltaMovement.z);
				}
				else
				{
					double parallelMovement = deltaMovement.z;
					if (Math.abs(parallelMovement) < 0.6)
						deltaMovement = new Vec3(deltaMovement.x, deltaMovement.y, 0);
				}
				
				if (deltaMovement.y <= -0.3D) // limit gravity
				{
					deltaMovement = new Vec3(deltaMovement.x, -0.3D, deltaMovement.z);
				}
				
				if (jumping) // squid surge
				{
					deltaMovement = deltaMovement.scale(1f / (1f + playerInfo.getSquidSurgeCharge() / 2f));
					
					if (playerInfo.getSquidSurgeCharge() < 30)
						playerInfo.setSquidSurgeCharge(playerInfo.getSquidSurgeCharge() + 1);
				}
				else // stop squid surge
				{
					if (playerInfo.getSquidSurgeCharge() >= 30) // do squid surge logic
					{
						deltaMovement = new Vec3(0, 10, 0);
					}
					playerInfo.setSquidSurgeCharge(0f);
				}
				
				if (input != null) // set input as 0 because movement was handled!! i think i should've used the event thingy though
				{
					input.forwardImpulse = 0;
					input.leftImpulse = 0;
				}
				
				player.fallDistance = 0.0F;
				player.setDeltaMovement(deltaMovement);
			}
			else
			{
				playerInfo.setClimbedDirection(null);
			}
		}
		
		if (playerInfo.getClimbedDirection().isEmpty())
		{
			playerInfo.setSquidSurgeCharge(0f);
			Direction newDirection = InkBlockUtils.canSquidClimb(player, movementSideways, movementForward, player.getYRot());
			if (newDirection != null)
			{
				player.teleportRelative(0, 0.01, 0);
				player.setOnGround(false);
				playerInfo.setClimbedDirection(newDirection);
			}
		}
		SplatcraftPacketHandler.sendToServer(new SquidInputPacket(
			playerInfo.getClimbedDirection(),
			playerInfo.getSquidSurgeCharge()));
	}
}
