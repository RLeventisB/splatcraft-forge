package net.splatcraft.client.handlers;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.data.capabilities.structs.SquidInfo;
import net.splatcraft.handlers.ShootingHandler;
import net.splatcraft.handlers.WeaponHandler;
import net.splatcraft.items.SpecialProviderItem;
import net.splatcraft.items.weapons.IChargeableWeapon;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.subs.SubWeaponItem;
import net.splatcraft.mixin.accessors.MinecraftClientAccessor;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.RequestSpecialUsageDataPacket;
import net.splatcraft.network.c2s.SwapSlotWithOffhandPacket;
import net.splatcraft.network.s2c.VoidedChargePacket;
import net.splatcraft.platform.Components;
import net.splatcraft.platform.Services;
import net.splatcraft.platform.event.EventResult;
import net.splatcraft.platform.event.InteractionEvents;
import net.splatcraft.platform.event.TickEvents;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.EntityStoredCharge;
import net.splatcraft.util.action.ActionThatSetsSquid;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.specials.BaseSpecialAction;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

public class SplatcraftKeyHandler
{
	public static final ToggleableKey SHOOT_KEYBIND = new ToggleableKey(Minecraft.getInstance().options.keyUse);
	public static final ToggleableKey SQUID_KEYBIND = new ToggleableKey(new KeyMapping("key.squidForm", GLFW.GLFW_KEY_Z, "key.categories.splatcraft"));
	public static final ToggleableKey SUB_WEAPON_KEYBIND = new ToggleableKey(new KeyMapping("key.subWeaponHotkey", GLFW.GLFW_KEY_V, "key.categories.splatcraft"));
	public static final ToggleableKey SPECIAL_WEAPON_KEYBIND = new ToggleableKey(new KeyMapping("key.specialWeaponHotkey", GLFW.GLFW_KEY_B, "key.categories.splatcraft"));
	private static final ObjectArrayList<ToggleableKey> pressState = new ObjectArrayList<>();
	public static int squidAndSubDelay = 0; //delays automatically returning into squid form after firing for balancing reasons and to allow packet-based weapons to fire (chargers and splatlings)
	private static int slot = -1;
	private static boolean usingSubWeaponHotkey, queuedSubWeapon;
	@OnlyIn(Dist.CLIENT)
	public static void registerBindingsAndEvents()
	{
		Services.PLATFORM.registerKeyMapping(SUB_WEAPON_KEYBIND.key);
		Services.PLATFORM.registerKeyMapping(SPECIAL_WEAPON_KEYBIND.key);
		Services.PLATFORM.registerKeyMapping(SQUID_KEYBIND.key);
		Services.PLATFORM.registerListener(TickEvents.ClientBefore.class, SplatcraftKeyHandler::onClientTick);
		Services.PLATFORM.registerListener(InteractionEvents.RightClickItem.class, SplatcraftKeyHandler::onRightClick);
		Services.PLATFORM.registerListener(InteractionEvents.RightClickBlock.class, SplatcraftKeyHandler::onRightClick);
	}
	private static EventResult onRightClick(Player player, InteractionHand hand, Direction direction, ItemStack stack, Level level, BlockPos pos)
	{
		if (!stack.isEmpty() && player.isLocalPlayer() && !(stack.getItem() instanceof WeaponBaseItem<?>)) // add delay for placing blocks, or other things
			squidAndSubDelay = 5;
		return EventResult.PASS;
	}
	public static boolean isSubWeaponHotkeyDown()
	{
		return SUB_WEAPON_KEYBIND.active;
	}
	public static boolean isSquidKeyDown()
	{
		return !pressState.isEmpty() && Iterables.getLast(pressState).equals(SQUID_KEYBIND);
	}
	@OnlyIn(Dist.CLIENT)
	public static void onClientTick(Minecraft mc)
	{
		Player player = mc.player;
		
		if (player == null || player.isSpectator())
			return;
		
		SquidInfo info = Components.SQUID_INFO.get(player);
		
		if (info == null)
			return;
		
		tickKeys(player, mc);
		
		ToggleableKey lastPressedKey = getLastPressedKey();
		
		tickSquidAndCharge(player, info, lastPressedKey);
		
		tickSubWeapon(mc, player, lastPressedKey, info);
		
		if (SPECIAL_WEAPON_KEYBIND.pressed && !EntityStoredCharge.hasCharge(player))
			tickSpecialWeapon(player);
		
		tickAutoSquidDelay(player);
	}
	private static @Nullable ToggleableKey getLastPressedKey()
	{
		return Iterables.getLast(pressState, null);
	}
	private static void tickSpecialWeapon(Player player)
	{
		Inventory inventory = player.getInventory();
		Pair<ItemStack, Integer> providerPair = CommonUtils.getStackAndIndexInInventory(player, stack -> stack.getItem() instanceof SpecialProviderItem);
		if (providerPair.getFirst().isEmpty())
		{
			player.displayClientMessage(Component.translatable("status.no_special_provider").withStyle(ChatFormatting.RED), true);
			return;
		}
		
		if (WeaponHandler.getWeaponHand(player, Predicates.alwaysTrue()).isEmpty())
		{
			player.displayClientMessage(Component.translatable("status.need_weapon").withStyle(ChatFormatting.RED), true);
			return;
		}
		
		SpecialProviderItem providerItem = (SpecialProviderItem) providerPair.getFirst().getItem();
		SplatcraftComponents.SpecialProviderData providerData = providerItem.getData(providerPair.getFirst());
		Pair<ItemStack, Integer> weaponPair = null;
		if (providerData.testWeapon(inventory.getSelected()))
		{
			weaponPair = Pair.of(inventory.getSelected(), inventory.selected);
		}
		else if (providerData.testWeapon(inventory.getItem(Inventory.SLOT_OFFHAND)))
		{
			weaponPair = Pair.of(inventory.getItem(Inventory.SLOT_OFFHAND), Inventory.SLOT_OFFHAND);
		}
		if (weaponPair == null)
		{
			player.displayClientMessage(Component.translatable("status.provider_wrong_weapon").withStyle(ChatFormatting.RED), true);
			return;
		}
		
		SplatcraftPacketHandler.sendToServer(new RequestSpecialUsageDataPacket(weaponPair.getSecond(), providerPair.getSecond()));
	}
	private static void tickSubWeapon(Minecraft mc, Player player, ToggleableKey oldest, SquidInfo info)
	{
		if (SUB_WEAPON_KEYBIND.pressed)
			queuedSubWeapon = true;
		
		if (EntityAction.hasEntityActionAnd(player, EntityAction::preventWeaponUse) ||
			CommonUtils.anyWeaponOnCooldown(player) ||
			ShootingHandler.isDoingShootingAction(player) ||
			EntityStoredCharge.hasCharge(player)) // dont allow sub code to execute if the player has a charge or else everything breaks
			return;
		
		Inventory inventory = player.getInventory();
		
		if (SUB_WEAPON_KEYBIND.equals(oldest))
		{
			Pair<ItemStack, Integer> sub = CommonUtils.getStackAndIndexInInventory(player, itemStack -> itemStack.getItem() instanceof SubWeaponItem);
			
			if (sub.getSecond() == -1 || (info.isSquid() && !hasEnoughSpaceToTransformBack(player)))
			{
				player.displayClientMessage(Component.translatable("status.cant_use"), true);
			}
			else
			{
				if (queuedSubWeapon)
				{
					if (!player.getItemInHand(InteractionHand.OFF_HAND).equals(sub.getFirst()))
					{
						slot = sub.getSecond();
						SplatcraftPacketHandler.sendToServer(new SwapSlotWithOffhandPacket(slot, false));
						
						ItemStack stack = player.getOffhandItem();
						player.setItemInHand(InteractionHand.OFF_HAND, inventory.getItem(slot));
						inventory.setItem(slot, stack);
						player.releaseUsingItem();
					}
					else if (!usingSubWeaponHotkey) slot = -1;
					
					usingSubWeaponHotkey = true;
					queuedSubWeapon = false;
					startUsingItemInHand(InteractionHand.OFF_HAND);
				}
			}
		}
		else
		{
			if (SUB_WEAPON_KEYBIND.released && mc.gameMode != null && player.getUsedItemHand() == InteractionHand.OFF_HAND)
			{
				mc.gameMode.releaseUsingItem(player);
			}
			
			if (slot != -1)
			{
				ItemStack stack = player.getOffhandItem();
				player.setItemInHand(InteractionHand.OFF_HAND, inventory.getItem(slot));
				inventory.setItem(slot, stack);
				player.releaseUsingItem();
				
				SplatcraftPacketHandler.sendToServer(new SwapSlotWithOffhandPacket(slot, false));
				usingSubWeaponHotkey = false;
				squidAndSubDelay = 5;
				slot = -1;
			}
		}
	}
	public static boolean pressedSquidKeyWhileHoldingChargeable(LivingEntity entity)
	{
		return SQUID_KEYBIND.pressed &&
			WeaponHandler.getWeaponHand(entity, (x, y) -> y instanceof IChargeableWeapon && y.preventsChanging(x, entity)).isPresent();
	}
	public static boolean hasEnoughSpaceToTransformBack(LivingEntity entity)
	{
		return entity.level().noBlockCollision(entity,
			new AABB(entity.getX() + -0.3, entity.getY(), entity.getZ() + -0.3, entity.getX() + 0.3, entity.getY() + 0.6, entity.getZ() + 0.3));
	}
	public static void tickSquidAndCharge(LivingEntity entity, SquidInfo info, ToggleableKey oldest)
	{
		Optional<Boolean> forcedSquidMode = EntityAction.getSpecificEntityActionOptional(entity, ActionThatSetsSquid.class).flatMap(v -> v.isSquid(entity));
		if (forcedSquidMode.isPresent())
		{
			if (info.isSquid() != forcedSquidMode.get())
			{
				ClientUtils.setSquid(entity, forcedSquidMode.get(), false);
			}
			return;
		}
		
		boolean doingActionThatLeavesSquidMode = SHOOT_KEYBIND.equals(oldest) || SUB_WEAPON_KEYBIND.equals(oldest) || SPECIAL_WEAPON_KEYBIND.equals(oldest);
		if (info.isSquid())
		{
			//Resets weapon charge when player is in swim form and not holding down right click. Used to void Charge Storage for Splatlings and Chargers.
			if (EntityStoredCharge.hasCharge(entity) && !SHOOT_KEYBIND.active)
			{
				EntityStoredCharge.emptyStoredCharge(entity);
				for (InteractionHand hand : InteractionHand.values())
				{
					ItemStack stack = entity.getItemInHand(hand);
					if (stack.getItem() instanceof IChargeableWeapon chargeableWeapon)
						chargeableWeapon.setCharge(stack, 0f);
				}
				
				SplatcraftPacketHandler.sendToServer(new VoidedChargePacket());
			}
			
			if (!hasEnoughSpaceToTransformBack(entity))
				return;
			
			if (EntityStoredCharge.hasCharge(entity))
			{
				if (SQUID_KEYBIND.pressed)
					ClientUtils.setSquid(entity, false, true);
			}
			else
			{
				if (!SQUID_KEYBIND.active || doingActionThatLeavesSquidMode)
					ClientUtils.setSquid(entity, false, false);
			}
		}
		else
		{
			if (EntityAction.hasEntityActionAnd(entity, v -> !v.isCancellable(entity)) ||
				CommonUtils.anyWeaponOnCooldown(entity))
				return;
			
			if (pressedSquidKeyWhileHoldingChargeable(entity))
			{
				SQUID_KEYBIND.active = true;
				
				ClientUtils.setSquid(entity, true, true);
			}
			else
			{
				if (SQUID_KEYBIND.active &&
					WeaponHandler.getWeaponHand(entity, (x, y) -> y.preventsSquidForm(x, entity)).isEmpty() &&
					!doingActionThatLeavesSquidMode)
				{
					ClientUtils.setSquid(entity, true, false);
				}
			}
		}
	}
	@OnlyIn(Dist.CLIENT)
	private static void tickAutoSquidDelay(Player player)
	{
		if (!Minecraft.getInstance().isPaused())
		{
			if (squidAndSubDelay > 0)
			{
				// this is to fix whenever a player shoots again just in time the sub delay reaches 0, allowing them to literally cancel all endlag and enter squid form lol
				// in that case the shot is done server-side, were isUsingItem is true because of delay
				boolean isDoingAction = WeaponHandler.getUsingWeaponHand(player).isPresent() || EntityAction.hasEntityActionAnd(player, action -> !action.isCancellable(player));
				if (isDoingAction && squidAndSubDelay == 1 && !pressedSquidKeyWhileHoldingChargeable(player))
					return;
				
				squidAndSubDelay--;
			}
		}
	}
	@OnlyIn(Dist.CLIENT)
	private static void tickKeys(Player player, Minecraft mc)
	{
		boolean canHold = canHoldKeys(mc);
		
		SUB_WEAPON_KEYBIND.tick(KeyMode.HOLD, canHold);
		updatePressState(SUB_WEAPON_KEYBIND, squidAndSubDelay, !pressState.contains(SHOOT_KEYBIND) &&
			!player.isUsingItem());
		
		SHOOT_KEYBIND.tick(KeyMode.HOLD, canHold);
		updatePressState(SHOOT_KEYBIND, squidAndSubDelay, WeaponHandler.getUsingWeaponHand(player).isEmpty());
		
		boolean forcedHold = EntityAction.hasSpecificEntityActionAnd(player, BaseSpecialAction::setSquidKeyToHold, BaseSpecialAction.class);
		SQUID_KEYBIND.tick(forcedHold ? KeyMode.HOLD : SplatcraftConfig.get("splatcraft.squidKeyMode"), canHold);
		updatePressState(SQUID_KEYBIND, 0);
		
		SPECIAL_WEAPON_KEYBIND.tick(KeyMode.HOLD, canHold);
		updatePressState(SPECIAL_WEAPON_KEYBIND, 0);
	}
	private static void updatePressState(ToggleableKey key, int releaseDelay)
	{
		updatePressState(key, releaseDelay, true);
	}
	private static void updatePressState(ToggleableKey key, int releaseDelay, boolean extraReleaseCondition)
	{
		if (key.active)
		{
			if (!pressState.contains(key))
			{
				pressState.add(key);
			}
		}
		else if (releaseDelay <= 0 && extraReleaseCondition)
		{
			pressState.remove(key);
		}
	}
	@OnlyIn(Dist.CLIENT)
	private static boolean canHoldKeys(Minecraft mc)
	{
		return mc.screen == null && mc.getOverlay() == null;
	}
	@SuppressWarnings("all") // VanillaCopy
	@OnlyIn(Dist.CLIENT)
	public static void startUsingItemInHand(InteractionHand hand)
	{
		Minecraft mc = Minecraft.getInstance();
		if (!mc.gameMode.isDestroying())
		{
			((MinecraftClientAccessor) mc).setRightClickDelay(4);
			if (!mc.player.isHandsBusy())
			{
				CommonUtils.InteractionEventResultDummy inputEvent = CommonUtils.doPlayerUseItemForgeEvent(1, mc.options.keyUse, hand);
				if (inputEvent.isCanceled())
				{
					if (inputEvent.shouldSwingHand())
					{
						mc.player.swing(hand);
					}
					return;
				}
				ItemStack itemstack = mc.player.getItemInHand(hand);
				if (mc.hitResult != null)
				{
					switch (mc.hitResult.getType())
					{
						case ENTITY:
							EntityHitResult entityraytraceresult = (EntityHitResult) mc.hitResult;
							Entity entity = entityraytraceresult.getEntity();
							InteractionResult actionresulttype = mc.gameMode.interactAt(mc.player, entity, entityraytraceresult, hand);
							if (!actionresulttype.consumesAction())
							{
								actionresulttype = mc.gameMode.interact(mc.player, entity, hand);
							}
							
							if (actionresulttype.consumesAction())
							{
								if (actionresulttype.shouldSwing())
								{
									if (inputEvent.shouldSwingHand())
									{
										mc.player.swing(hand);
									}
								}
								
								return;
							}
							break;
						case BLOCK:
							BlockHitResult blockraytraceresult = (BlockHitResult) mc.hitResult;
							int i = itemstack.getCount();
							InteractionResult actionresulttype1 = mc.gameMode.useItemOn(mc.player, hand, blockraytraceresult);
							if (actionresulttype1.consumesAction())
							{
								if (actionresulttype1.shouldSwing())
								{
									if (inputEvent.shouldSwingHand())
									{
										mc.player.swing(hand);
									}
									if (!itemstack.isEmpty() && (itemstack.getCount() != i || mc.gameMode.hasInfiniteItems()))
									{
										mc.gameRenderer.itemInHandRenderer.itemUsed(hand);
									}
								}
								
								return;
							}
							
							if (actionresulttype1 == InteractionResult.FAIL)
							{
								return;
							}
					}
				}
				
				if (itemstack.isEmpty() && (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS))
				{
					CommonUtils.doForgeEmptyClickEvent(mc.player, hand);
				}
				
				if (!itemstack.isEmpty())
				{
					InteractionResult actionresulttype2 = mc.gameMode.useItem(mc.player, hand);
					if (actionresulttype2.consumesAction())
					{
						if (actionresulttype2.shouldSwing())
						{
							mc.player.swing(hand);
						}
						
						mc.gameRenderer.itemInHandRenderer.itemUsed(hand);
					}
				}
			}
		}
	}
	public static void setSquidDelayInternal(float delay)
	{
		int delayInt = (int) delay;
		if (delayInt > squidAndSubDelay)
			squidAndSubDelay = delayInt;
	}
	public enum KeyMode
	{
		HOLD,
		TOGGLE
	}
	public static class ToggleableKey
	{
		public final KeyMapping key;
		public boolean active;
		public boolean previousKeyDown;
		public boolean pressed;
		public boolean released;
		public ToggleableKey(KeyMapping key)
		{
			this.key = key;
		}
		public void tick(KeyMode mode, boolean canHold)
		{
			tick(mode, canHold, true);
		}
		public void tick(KeyMode mode, boolean canHold, boolean tickMode)
		{
			boolean isKeyDown = key.isDown() && canHold;
			pressed = isKeyDown && !previousKeyDown;
			released = !isKeyDown && previousKeyDown;
			
			if (!tickMode)
				return;
			
			switch (mode)
			{
				case HOLD -> active = isKeyDown;
				case TOGGLE ->
				{
					if (pressed)
					{
						active = !active;
					}
				}
			}
			previousKeyDown = isKeyDown;
		}
		public boolean isActive()
		{
			return active;
		}
	}
}
