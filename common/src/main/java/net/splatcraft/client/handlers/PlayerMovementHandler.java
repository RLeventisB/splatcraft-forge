package net.splatcraft.client.handlers;

import dev.architectury.event.events.common.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.splatcraft.Splatcraft;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.data.capabilities.playerinfo.EntityInfo;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.DualieItem;
import net.splatcraft.items.weapons.RollerItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.mixin.accessors.EntityAccessor;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.SquidInputPacket;
import net.splatcraft.registries.SplatcraftAttributes;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.PlayerCooldown;

import java.util.HashMap;

public class PlayerMovementHandler
{
	public static final HashMap<ClientPlayerEntity, Input> unmodifiedInput = new HashMap<>();
	private static final EntityAttributeModifier INK_SWIM_SPEED = new EntityAttributeModifier(Splatcraft.identifierOf("ink_movement_boost"), 0D, EntityAttributeModifier.Operation.ADD_VALUE);
	private static final EntityAttributeModifier SQUID_SWIM_SPEED = new EntityAttributeModifier(Splatcraft.identifierOf("squid_swim_speed"), 0.2D, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	private static final EntityAttributeModifier ENEMY_INK_SPEED = new EntityAttributeModifier(Splatcraft.identifierOf("enemy_ink_penalty"), -0.5D, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	public static void registerEvents()
	{
		TickEvent.PLAYER_POST.register((player) ->
		{
			Vec3d deltaMovement = player.getVelocity();
			if (SplatcraftConfig.<Boolean>get("splatcraft.limitFallSpeed") && deltaMovement.y < -0.5)
			{
				player.setVelocity(deltaMovement.x, -0.5, deltaMovement.z);
			}
		});
		TickEvent.PLAYER_PRE.register(PlayerMovementHandler::playerMovement);
	}
	@Environment(EnvType.CLIENT)
	public static void playerMovement(PlayerEntity player)
	{
		EntityInfo playerInfo = EntityInfoCapability.get(player);
		if (playerInfo == null)
			playerInfo = new EntityInfo();
		
		boolean hasCooldown = PlayerCooldown.hasPlayerCooldown(player);
		PlayerCooldown cooldown = hasCooldown ? PlayerCooldown.getPlayerCooldown(player) : null;
		
		EntityAttributeInstance speedAttribute = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
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
				speedAttribute.addTemporaryModifier(ENEMY_INK_SPEED);
		}
		
		ItemStack useStack = player.getActiveItem();
		if (hasCooldown)
			useStack = cooldown.storedStack;
		else if (useStack.isEmpty())
			useStack = player.getItemCooldownManager().isCoolingDown(player.getMainHandStack().getItem()) ? player.getMainHandStack() :
				player.getItemCooldownManager().isCoolingDown(player.getOffHandStack().getItem()) ? player.getOffHandStack() : ItemStack.EMPTY;
		
		if (useStack.getItem() instanceof WeaponBaseItem<?> weapon && weapon.hasSpeedModifier(player, useStack))
		{
			EntityAttributeModifier mod = weapon.getSpeedModifier(player, useStack);
			if (!speedAttribute.hasModifier(mod.id()))
				speedAttribute.addTemporaryModifier(mod);
		}
		
		if (playerInfo.isSquid())
		{
			if (InkBlockUtils.canSquidSwim(player) && !speedAttribute.hasModifier(INK_SWIM_SPEED.id()) && player.isOnGround())
				speedAttribute.addTemporaryModifier(INK_SWIM_SPEED);
//                if (!swimAttribute.hasModifier(SQUID_SWIM_SPEED.id()))
//                    swimAttribute.addTemporaryModifier(SQUID_SWIM_SPEED);
		}
		
		if (hasCooldown && cooldown.getSlotIndex() >= 0)
			player.getInventory().selectedSlot = cooldown.getSlotIndex();
		
		if (!player.getAbilities().flying)
			if (speedAttribute.hasModifier(INK_SWIM_SPEED.id()))
				player.updateVelocity((float) player.getAttributeValue(SplatcraftAttributes.inkSwimSpeed) * (player.isOnGround() ? 1 : 0.75f), new Vec3d(player.sidewaysSpeed, 0.0f, player.forwardSpeed).normalize());
	}
	public static void onInputUpdate(ClientPlayerEntity player, Input input)
	{
		EntityInfo playerInfo = EntityInfoCapability.get(player);
		if (playerInfo == null)
			playerInfo = new EntityInfo();
		
		Input clonedInput = new Input();
		clonedInput.movementSideways = input.movementSideways;
		clonedInput.movementForward = input.movementForward;
		clonedInput.pressingForward = input.pressingForward;
		clonedInput.pressingBack = input.pressingBack;
		clonedInput.pressingLeft = input.pressingLeft;
		clonedInput.pressingRight = input.pressingRight;
		clonedInput.jumping = input.jumping;
		clonedInput.sneaking = input.sneaking;
		unmodifiedInput.put(player, clonedInput);
		
		float speedMod = !input.sneaking ? playerInfo.isSquid() && InkBlockUtils.canSquidHide(player) ? 15f : 2f : 1f;
		
		input.movementForward *= speedMod;
		//input = player.movementInput;
		input.movementSideways *= speedMod;
		//input = player.movementInput;
		
		if (playerInfo.isSquid())
		{
			handleSquidMovement(playerInfo, player, input.movementSideways, input.movementForward, input.jumping, input.sneaking, input);
		}
		
		if (player.isUsingItem())
		{
			ItemStack stack = player.getActiveItem();
			if (!stack.isEmpty())
			{
				if (stack.getItem() instanceof WeaponBaseItem)
				{
					input.movementSideways *= 5.0F;
					input.movementForward *= 5.0F;
					
					if (stack.getItem() instanceof DualieItem && (input.movementSideways != 0 || input.movementForward != 0))
					{
						input.jumping = false;
					}
				}
			}
		}
		
		if (PlayerCooldown.hasPlayerCooldown(player))
		{
			PlayerCooldown cooldown = PlayerCooldown.getPlayerCooldown(player);
			
			if (!cooldown.canMove())
			{
				input.movementForward = 0;
				input.movementSideways = 0;
				input.jumping = false;
			}
			else if (cooldown.storedStack.getItem() instanceof RollerItem rollerItem)
			{
				input.movementForward = Math.min(1, Math.abs(input.movementForward)) * Math.signum(input.movementForward) * rollerItem.getSettings(cooldown.storedStack).swingData.mobility();
				input.movementSideways = Math.min(1, Math.abs(input.movementSideways)) * Math.signum(input.movementSideways) * rollerItem.getSettings(cooldown.storedStack).swingData.mobility();
			}
			if (cooldown.forceCrouch() && cooldown.getTime() >= 1)
			{
				input.sneaking = !player.getAbilities().flying;
			}
		}
	}
	private static void handleSquidMovement(EntityInfo playerInfo, PlayerEntity player, float movementSideways, float movementForward, boolean jumping, boolean sneaking, Input input)
	{
		if (playerInfo.getClimbedDirection().isPresent())
		{
			Direction oldClimbedDirection = playerInfo.getClimbedDirection().get();
			Direction climbedDirection = InkBlockUtils.getSquidClimbingDirection(player, movementSideways, movementForward, oldClimbedDirection);
			
			if (climbedDirection != null && !player.isOnGround()) // if player is still swimming on a wall
			{
				playerInfo.setClimbedDirection(climbedDirection);
				Vec3d deltaMovement = player.getVelocity();
				if (deltaMovement.y < 0.4f && (movementForward != 0 || movementSideways != 0)) // handle input on wall
				{
					float yaw = player.getHeadYaw();
					Vec3d vec3 =
						EntityAccessor.invokeMovementInputToVelocity(new Vec3d(0f, movementForward, 0f), 0.12f, yaw).add(
							EntityAccessor.invokeMovementInputToVelocity(new Vec3d(movementSideways, 0f, 0f), 0.02f, yaw)
						);
					
					deltaMovement = deltaMovement.add(vec3);
				}
				if (sneaking) // set minimum y velocity to 0 if shifting
					deltaMovement = new Vec3d(deltaMovement.x, Math.max(0, deltaMovement.y), deltaMovement.z);
				
				if (climbedDirection.getAxis() != oldClimbedDirection.getAxis()) // if player swam to another wall, rotate velocity
				{
					deltaMovement = deltaMovement.rotateY(MathHelper.RADIANS_PER_DEGREE * (climbedDirection.asRotation() - oldClimbedDirection.asRotation()));
				}
				
				if (climbedDirection.getAxis() == Direction.Axis.X) // set velocity perpendicular to the wall to 0 because YOU CANNOT ESCAPE THE WALL (unless you press back).
				{
					double parallelMovement = deltaMovement.x;
					if (Math.abs(parallelMovement) < 0.6)
						deltaMovement = new Vec3d(0, deltaMovement.y, deltaMovement.z);
				}
				else
				{
					double parallelMovement = deltaMovement.z;
					if (Math.abs(parallelMovement) < 0.6)
						deltaMovement = new Vec3d(deltaMovement.x, deltaMovement.y, 0);
				}
				
				if (deltaMovement.y <= -0.3D) // limit gravity
				{
					deltaMovement = new Vec3d(deltaMovement.x, -0.3D, deltaMovement.z);
				}
				
				if (jumping) // squid surge
				{
					deltaMovement = deltaMovement.multiply(1f / (1f + playerInfo.getSquidSurgeCharge() / 2f));
					
					if (playerInfo.getSquidSurgeCharge() < 30)
						playerInfo.setSquidSurgeCharge(playerInfo.getSquidSurgeCharge() + 1);
				}
				else // stop squid surge
				{
					if (playerInfo.getSquidSurgeCharge() >= 30) // do squid surge logic
					{
						deltaMovement = new Vec3d(0, 10, 0);
					}
					playerInfo.setSquidSurgeCharge(0f);
				}
				
				if (input != null) // set input as 0 because movement was handled!! i think i should've used the event thingy though
				{
					input.movementForward = 0;
					input.movementSideways = 0;
				}
				
				player.fallDistance = 0.0F;
				player.setVelocity(deltaMovement);
			}
			else
			{
				playerInfo.setClimbedDirection(null);
			}
		}
		
		if (playerInfo.getClimbedDirection().isEmpty())
		{
			playerInfo.setSquidSurgeCharge(0f);
			Direction newDirection = InkBlockUtils.canSquidClimb(player, movementSideways, movementForward, player.getYaw());
			if (newDirection != null)
			{
				player.requestTeleportOffset(0, 0.01, 0);
				player.setOnGround(false);
				playerInfo.setClimbedDirection(newDirection);
			}
		}
		SplatcraftPacketHandler.sendToServer(new SquidInputPacket(
			playerInfo.getClimbedDirection(),
			playerInfo.getSquidSurgeCharge()));
	}
}
