package net.splatcraft.client.handlers;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.Splatcraft;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.data.capabilities.structs.EntityInfo;
import net.splatcraft.items.weapons.DualieItem;
import net.splatcraft.items.weapons.RollerItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.AbstractWeaponSettings;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.SendSquidSurgePacket;
import net.splatcraft.platform.Components;
import net.splatcraft.platform.Services;
import net.splatcraft.platform.event.TickEvents;
import net.splatcraft.registries.SplatcraftAttributes;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.specials.BaseSpecialAction;
import net.splatcraft.util.action.specials.InkjetAction;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerMovementHandler
{
	public static final HashMap<Player, Input> unmodifiedInput = new HashMap<>();
	private static final AttributeModifier INK_SWIM_SPEED = new AttributeModifier(Splatcraft.identifierOf("ink_movement_boost"), 0D, AttributeModifier.Operation.ADD_VALUE);
	private static final AttributeModifier SQUID_SWIM_SPEED = new AttributeModifier(Splatcraft.identifierOf("squid_swim_speed"), 0.2D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	private static final AttributeModifier ENEMY_INK_SPEED = new AttributeModifier(Splatcraft.identifierOf("enemy_ink_penalty"), -0.5D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	private static final AttributeModifier SLOW_FALLING = new AttributeModifier(Splatcraft.identifierOf("slow_falling_dummy"), -0.07, AttributeModifier.Operation.ADD_VALUE);
	private static final ResourceLocation SPECIAL_BONUS_ID = Splatcraft.identifierOf("special_bonus");
	public static void registerEvents()
	{
		Services.PLATFORM.registerListener(TickEvents.PlayerBefore.class, PlayerMovementHandler::playerMovement);
		Services.PLATFORM.registerListener(TickEvents.PlayerAfter.class, player ->
		{
			Vec3 deltaMovement = player.getDeltaMovement();
			if (Boolean.TRUE.equals(SplatcraftConfig.get("splatcraft.limitFallSpeed")) && deltaMovement.y < -0.5)
			{
				player.setDeltaMovement(deltaMovement.x, -0.5, deltaMovement.z);
			}
		});
	}
	public static void playerMovement(Player player)
	{
		EntityInfo info = Components.ENTITY_INFO.getOrCreate(player);
		
		Optional<EntityAction> action = EntityAction.getEntityActionOptional(player);
		
		AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
		AttributeInstance gravityAttribute = player.getAttribute(Attributes.GRAVITY);
//		EntityAttributeInstance swimAttribute = player.getAttributeInstance(attributes.SWIM_SPEED.get());
		
		if (speedAttribute.hasModifier(INK_SWIM_SPEED.id()))
			speedAttribute.removeModifier(INK_SWIM_SPEED);
		if (speedAttribute.hasModifier(ENEMY_INK_SPEED.id()))
			speedAttribute.removeModifier(ENEMY_INK_SPEED);
		if (speedAttribute.hasModifier(SPECIAL_BONUS_ID))
			speedAttribute.removeModifier(SPECIAL_BONUS_ID);
		if (gravityAttribute.hasModifier(SPECIAL_BONUS_ID))
			gravityAttribute.removeModifier(SPECIAL_BONUS_ID);
//            if (swimAttribute.hasModifier(SQUID_SWIM_SPEED.id()))
//                swimAttribute.removeModifier(SQUID_SWIM_SPEED);
		
		if (speedAttribute.hasModifier(SplatcraftItems.SPEED_MOD_IDENTIFIER))
			speedAttribute.removeModifier(SplatcraftItems.SPEED_MOD_IDENTIFIER);
		
		if (InkBlockUtils.onEnemyInk(player))
		{
			//player.setVelocity(player.getVelocity().x, Math.min(player.getVelocity().y, 0.05f), player.getVelocity().z);
			if (!speedAttribute.hasModifier(ENEMY_INK_SPEED.id()))
				speedAttribute.addTransientModifier(ENEMY_INK_SPEED);
		}
		
		if (EntityAction.hasSpecificEntityAction(player, BaseSpecialAction.class))
		{
			BaseSpecialAction specialAction = EntityAction.getSpecificEntityAction(player, BaseSpecialAction.class);
			specialAction.mobility(player).ifPresent(bonus ->
			{
				speedAttribute.addOrUpdateTransientModifier(new AttributeModifier(SPECIAL_BONUS_ID, bonus - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			});
			if (specialAction instanceof InkjetAction)
			{
				gravityAttribute.addOrUpdateTransientModifier(new AttributeModifier(SPECIAL_BONUS_ID, -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
		}
		
		if (info.isSquid())
		{
			if (InkBlockUtils.canSquidSwim(player) && !speedAttribute.hasModifier(INK_SWIM_SPEED.id()) && player.onGround())
				speedAttribute.addTransientModifier(INK_SWIM_SPEED);
//                if (!swimAttribute.hasModifier(SQUID_SWIM_SPEED.id()))
//                    swimAttribute.addTemporaryModifier(SQUID_SWIM_SPEED);
		}
		
		action.ifPresent(v ->
		{
			if (v.getItemSlot() instanceof EntitySlot.PlayerInventorySlot playerSlot)
				player.getInventory().selected = playerSlot.getSlotIndex();
		});
		
		tickWeaponMobilityAttribute(player, speedAttribute);
		
		if (!player.getAbilities().flying)
			if (speedAttribute.hasModifier(INK_SWIM_SPEED.id()))
				player.moveRelative((float) player.getAttributeValue(SplatcraftAttributes.inkSwimSpeed) * (player.onGround() ? 1 : 0.75f), new Vec3(player.xxa, 0.0f, player.zza).normalize());
	}
	private static void tickWeaponMobilityAttribute(LivingEntity entity, AttributeInstance speedAttribute)
	{
		if (speedAttribute.hasModifier(AbstractWeaponSettings.WEAPON_MOBILITY_ATTIBUTE_ID))
			speedAttribute.removeModifier(AbstractWeaponSettings.WEAPON_MOBILITY_ATTIBUTE_ID);
		
		ItemStack useStack = entity.getMainHandItem();
		if (useStack.getItem() instanceof WeaponBaseItem<?> weapon && weapon.preventsChanging(useStack, entity) && weapon.hasSpeedModifier(entity, useStack))
		{
			var mod = weapon.getSpeedModifier(entity, useStack);
			if (!speedAttribute.hasModifier(mod.id()))
				speedAttribute.addTransientModifier(mod);
		}
		else
		{
			useStack = entity.getOffhandItem();
			if (useStack.getItem() instanceof WeaponBaseItem<?> weapon && weapon.preventsChanging(useStack, entity) && weapon.hasSpeedModifier(entity, useStack))
			{
				var mod = weapon.getSpeedModifier(entity, useStack);
				if (!speedAttribute.hasModifier(mod.id()))
					speedAttribute.addTransientModifier(mod);
			}
		}
	}
	@OnlyIn(Dist.CLIENT)
	public static void onInputUpdate(LocalPlayer player, Input input)
	{
		EntityInfo info = Components.ENTITY_INFO.getOrCreate(player);
		
		Input clonedInput = unmodifiedInput.computeIfAbsent(player, v -> new Input());
		copyTo(input, clonedInput);
		
		if (info.getMatchState(player).movementDisabled)
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
			handleSquidMovement(info, player, input.leftImpulse, input.forwardImpulse, player.jumping, player.isShiftKeyDown());
		}
		else if (info.getSquidSurgeState() != 0)
		{
			info.setSquidSurgeState(0);
			SplatcraftPacketHandler.sendToServer(new SendSquidSurgePacket(info.getClimbedDirection(), info.getSquidSurgeCharge()));
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
	private static void handleSquidMovement(EntityInfo entityInfo, LivingEntity entity, float movementSideways, float movementForward, boolean jumping, boolean sneaking)
	{
		Optional<Direction> climbDirectionOptional = InkBlockUtils.getSquidClimbDirection(entity, movementSideways, movementForward, entityInfo.getClimbedDirection());
		
		if (climbDirectionOptional.map(v -> v.getAxis() == Direction.Axis.Y).orElse(false))
			climbDirectionOptional = Optional.empty();
		
		entityInfo.setClimbedDirection(climbDirectionOptional);
		climbDirectionOptional.ifPresent(climbDirection ->
			{
				AttributeInstance gravity = entity.getAttribute(Attributes.GRAVITY);
				boolean falling = entity.getDeltaMovement().y <= 0.0D;
				if (falling && entity.hasEffect(MobEffects.SLOW_FALLING))
				{
					if (!gravity.hasModifier(SLOW_FALLING.id()))
						gravity.addTransientModifier(SLOW_FALLING);
					entity.fallDistance = 0.0F;
				}
				else if (gravity.hasModifier(SLOW_FALLING.id()))
					gravity.removeModifier(SLOW_FALLING);
				
				if (movementSideways != 0 || movementForward != 0)
				{
					Vec3 finalImpulse = getWallImpulse(climbDirection, movementSideways, movementForward, entity.getYRot());
					
					Vec3 deltaMovement = entity.getDeltaMovement();
					if (deltaMovement.y() < 0.4f)
					{
						if (finalImpulse.y > 0)
							deltaMovement = deltaMovement.add(0, finalImpulse.y * 0.06f, 0);
						else
							deltaMovement = deltaMovement.add(0, finalImpulse.y * 0.01f, 0);
					}
					if (deltaMovement.horizontalDistanceSqr() < 0.1f)
						deltaMovement = deltaMovement.add(finalImpulse.x * 0.01f, 0, finalImpulse.z * 0.01f);
					
					entity.setDeltaMovement(deltaMovement);
				}
				if (entity.getDeltaMovement().y() <= 0 && !sneaking)
					entity.moveRelative(0.035f, new Vec3(0.0f, 1, 0.0f));
				
				entity.addDeltaMovement(Vec3.atLowerCornerOf(climbDirection.getNormal()).scale(-0.03));
				if (sneaking)
					entity.setDeltaMovement(entity.getDeltaMovement().x, Math.max(0, entity.getDeltaMovement().y()), entity.getDeltaMovement().z);
			}
		);
		tickSquidSurge(entity, entityInfo, jumping);
		
		SplatcraftPacketHandler.sendToServer(new SendSquidSurgePacket(
			entityInfo.getClimbedDirection(),
			entityInfo.getSquidSurgeState()));
	}
	public static @NotNull Vec3 getWallImpulse(Direction climbDirection, float movementSideways, float movementForward, float yaw)
	{
//		return processImpulse(climbDirection, getRotatedImpulse(movementSideways, movementForward, yaw).normalize());
		Pair<Vector2f, Vector2f> rotatedImpulseSeparate = getRotatedImpulseSeparate(movementSideways, movementForward, yaw);
		Vec3 verticalPart = processImpulse(climbDirection, rotatedImpulseSeparate.getFirst());
		Vec3 horizontalPart = processImpulse(climbDirection, rotatedImpulseSeparate.getSecond());
		return horizontalPart.add(verticalPart).normalize();
	}
	public static @NotNull Vec3 processImpulse(Direction climbDirection, Vector2f horizontalImpulse)
	{
		Vec3 finalImpulse = new Vec3(0, 0, 0);
		switch (climbDirection)
		{
			case NORTH:
				finalImpulse = new Vec3(horizontalImpulse.x, horizontalImpulse.y, 0);
				break;
			case SOUTH:
				finalImpulse = new Vec3(horizontalImpulse.x, -horizontalImpulse.y, 0);
				break;
			case WEST:
				finalImpulse = new Vec3(0, horizontalImpulse.x, horizontalImpulse.y);
				break;
			case EAST:
				finalImpulse = new Vec3(0, -horizontalImpulse.x, horizontalImpulse.y);
				break;
		}
		return finalImpulse;
	}
	private static void tickSquidSurge(LivingEntity entity, EntityInfo entityInfo, boolean jumping)
	{
		AtomicReference<Vec3> deltaMovement = new AtomicReference<>(entity.getDeltaMovement());
		if (entityInfo.isDoingSquidSurge()) // swimming upwards
		{
			float squidSurgePower = entityInfo.getSquidSurgePower();
			if (entityInfo.getClimbedDirection().isPresent())
			{
				deltaMovement.set(new Vec3(0, 0.25 + squidSurgePower / 60f, 0));
				
				AABB extendedBox = entity.getBoundingBox().expandTowards(0, deltaMovement.get().y, 0);
				if (!entity.level().noCollision(entity, extendedBox))
					entityInfo.flagSquidSurgeEnd();
			}
			else
			{
				deltaMovement.set(new Vec3(0, 0.32 + squidSurgePower / 75f, 0));
				entityInfo.flagSquidSurgeEnd();
			}
		}
		else
		{
			if (entityInfo.canChargeSquidSurge())
			{
				entityInfo.getClimbedDirection().ifPresentOrElse(climbDirection ->
					{
						if (jumping && (deltaMovement.get().y < 0.3)) // charge squid surge
						{
							deltaMovement.set(deltaMovement.get().scale(1f / (1f + entityInfo.getSquidSurgeState() / 2f)));
							
							entityInfo.chargeSquidSurge();
						}
						else // release squid surge
						{
							if (entityInfo.flagSquidSurgeUsage()) // do squid surge logic
							{
								deltaMovement.set(new Vec3(0, 0.3, 0));
							}
							else
							{
								entityInfo.setSquidSurgeState(0);
							}
						}
					},
					() ->
					{
						entityInfo.setSquidSurgeState(0);
					});
			}
			else if (entityInfo.getSquidSurgeState() < 0) // is on cooldown
			{
				if (entityInfo.getSquidSurgeState() < -5)
				{
					float horizontalRestriction = 0.4f * (1f / (-entityInfo.getSquidSurgeState() + 5));
					deltaMovement.set(deltaMovement.get().multiply(horizontalRestriction, 1f, horizontalRestriction));
				}
				entityInfo.setSquidSurgeState(entityInfo.getSquidSurgeState() + 1);
			}
		}
		entity.setDeltaMovement(deltaMovement.get());
	}
	public static Vec3 getHorizontalImpulse(float movementSideways, float movementForward, float yaw)
	{
		final Vector2f rotatedImpulse = getRotatedImpulse(movementSideways, movementForward, yaw);
		return new Vec3(rotatedImpulse.x, 0, rotatedImpulse.y);
	}
	public static Vector2f getRotatedImpulse(float movementSideways, float movementForward, float yaw)
	{
		yaw = yaw * Mth.DEG_TO_RAD;
		float sin = (float) Math.sin(yaw);
		float cos = (float) Math.cos(yaw);
		// cos = x / a
		// sin = y / a
		// cos(a+b) * a = x * cos(b) - y * sin(b)
		// sin(a+b) * a = y * cos(b) + x * sin(b)
		return new Vector2f(movementSideways * cos - movementForward * sin, movementForward * cos + movementSideways * sin);
	}
	public static Pair<Vector2f, Vector2f> getRotatedImpulseSeparate(float movementSideways, float movementForward, float yaw)
	{
		yaw = yaw * Mth.DEG_TO_RAD;
		float sin = (float) Math.sin(yaw);
		float cos = (float) Math.cos(yaw);
		return Pair.of(
			new Vector2f(-movementForward * sin, movementForward * cos),
			new Vector2f(movementSideways * cos, movementSideways * sin)
		);
	}
	public static Vec3 getVerticalImpulse(float movementSideways, float movementForward, float yaw)
	{
		return new Vec3(0, -movementForward, movementSideways).xRot(-yaw * Mth.DEG_TO_RAD);
	}
}
