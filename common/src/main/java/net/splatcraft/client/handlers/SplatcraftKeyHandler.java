package net.splatcraft.client.handlers;

import com.google.common.collect.Iterables;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.handlers.ShootingHandler;
import net.splatcraft.items.SpecialProviderItem;
import net.splatcraft.items.weapons.subs.SubWeaponItem;
import net.splatcraft.mixin.accessors.MinecraftClientAccessor;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.RequestSpecialUsageDataPacket;
import net.splatcraft.network.c2s.SwapSlotWithOffhandPacket;
import net.splatcraft.network.c2s.UpdateChargeStatePacket;
import net.splatcraft.platform.Services;
import net.splatcraft.platform.event.TickEvents;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.PlayerCharge;
import net.splatcraft.util.action.EntityAction;
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
	private static boolean usingSubWeaponHotkey;
	@OnlyIn(Dist.CLIENT)
	public static void registerBindingsAndEvents()
	{
		Services.PLATFORM.registerKeyMapping(SUB_WEAPON_KEYBIND.key);
		Services.PLATFORM.registerKeyMapping(SPECIAL_WEAPON_KEYBIND.key);
		Services.PLATFORM.registerKeyMapping(SQUID_KEYBIND.key);
		Services.PLATFORM.registerListener(TickEvents.ClientBefore.class, SplatcraftKeyHandler::onClientTick);
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

		if (player == null || player.isSpectator() || !EntityInfoCapability.hasCapability(player))
		{
			return;
		}

		tickKeys(mc);

		if (!SHOOT_KEYBIND.active && PlayerCharge.hasCharge(player) && EntityInfoCapability.isSquid(player)) //Resets weapon charge when player is in swim form and not holding down right click. Used to void Charge Storage for Splatlings and Chargers.
		{
			PlayerCharge.getCharge(player).reset();
			SplatcraftPacketHandler.sendToServer(new UpdateChargeStatePacket(false));
		}

		tickAutoSquidDelay(player);

		if ((EntityAction.hasActionAnd(player, v -> !(SQUID_KEYBIND.active && v.isCancellable())))
			|| CommonUtils.anyWeaponOnCooldown(player) || ShootingHandler.isDoingShootingAction(player))
		{
			return;
		}

		ToggleableKey last = !pressState.isEmpty() ? Iterables.getLast(pressState) : null;

		EntityInfo info = EntityInfoCapability.get(player);
		if (SHOOT_KEYBIND.equals(last) || SUB_WEAPON_KEYBIND.equals(last))
		{
			// Unsquid so we can actually fire
			ClientUtils.setSquid(info, false);
		}

		Inventory inventory = player.getInventory();
		if (SUB_WEAPON_KEYBIND.equals(last))
		{
			ItemStack sub = CommonUtils.getItemInInventory(player, itemStack -> itemStack.getItem() instanceof SubWeaponItem);

			if (sub.isEmpty() || (info.isSquid() && !player.level().noBlockCollision(player,
				new AABB(player.getX() + -0.3, player.getY(), player.getZ() + -0.3, player.getX() + 0.3, player.getY() + 0.6, player.getZ() + 0.3))))
			{
				player.displayClientMessage(Component.translatable("status.cant_use"), true);
			}
			else
			{
				ClientUtils.setSquid(info, false);

				if (SUB_WEAPON_KEYBIND.pressed)
				{
					if (!player.getItemInHand(InteractionHand.OFF_HAND).equals(sub))
					{
						slot = inventory.findSlotMatchingItem(sub);
						SplatcraftPacketHandler.sendToServer(new SwapSlotWithOffhandPacket(slot, false));

						ItemStack stack = player.getOffhandItem();
						player.setItemInHand(InteractionHand.OFF_HAND, inventory.getItem(slot));
						inventory.setItem(slot, stack);
						player.releaseUsingItem();
					}
					else if (!usingSubWeaponHotkey) slot = -1;

					usingSubWeaponHotkey = true;
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
				slot = -1;
			}
		}

		if (SPECIAL_WEAPON_KEYBIND.equals(last))
		{
			Pair<ItemStack, Integer> providerPair = CommonUtils.getStackAndIndexInInventory(player, stack -> stack.getItem() instanceof SpecialProviderItem);
			if (providerPair.getFirst().isEmpty())
			{
				player.displayClientMessage(Component.translatable("status.cant_use"), true);
			}
			else
			{
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

				}
				else
				{
					SQUID_KEYBIND.active = false;
					SplatcraftPacketHandler.sendToServer(new RequestSpecialUsageDataPacket(weaponPair.getSecond(), providerPair.getSecond()));
				}
			}
		}

		if (player.getVehicle() == null &&
			player.level().noBlockCollision(player,
				new AABB(player.getX() + -0.3, player.getY(), player.getZ() + -0.3, player.getX() + 0.3, player.getY() + 0.6, player.getZ() + 0.3)))
		{
			if (SQUID_KEYBIND.equals(last) || !SQUID_KEYBIND.active)
			{
				ClientUtils.setSquid(info, SQUID_KEYBIND.active);
			}
		}
	}
	private static void tickAutoSquidDelay(Player player)
	{
		if (!Minecraft.getInstance().isPaused())
		{
			Optional<EntityAction> optional = EntityAction.getEntityActionOptional(player);
			if (SHOOT_KEYBIND.active || SUB_WEAPON_KEYBIND.active || optional.isPresent())
			{
				//autosquid delay set to 5 seconds for chargeables if cooldown hasn't been received yet
				// i think its better that actions manage their own squid endlag tho
				/*squidAndSubDelay = optional.map(
					entityAction -> (int) (entityAction.getTime() + 10)
				).orElseGet(
					() -> (player.getUseItem().getItem() instanceof IChargeableWeapon ? 20 : 5)
				);*/
			}
			else if (squidAndSubDelay > 0)
			{
				squidAndSubDelay--;
			}
		}
	}
	private static void tickKeys(Minecraft mc)
	{
		boolean canHold = canHoldKeys(mc);

		SHOOT_KEYBIND.tick(KeyMode.HOLD, canHold);
		updatePressState(SHOOT_KEYBIND, squidAndSubDelay);

		SQUID_KEYBIND.tick(SplatcraftConfig.get("splatcraft.squidKeyMode"), canHold);
		updatePressState(SQUID_KEYBIND, 0);

		SUB_WEAPON_KEYBIND.tick(KeyMode.HOLD, canHold);
		updatePressState(SUB_WEAPON_KEYBIND, squidAndSubDelay);

		SPECIAL_WEAPON_KEYBIND.tick(KeyMode.HOLD, canHold);
		updatePressState(SPECIAL_WEAPON_KEYBIND, 0);
	}
	private static void updatePressState(ToggleableKey key, int releaseDelay)
	{
		if (key.active)
		{
			if (!pressState.contains(key))
			{
				pressState.add(key);
			}
		}
		else if (releaseDelay <= 0)
		{
			pressState.remove(key);
		}
	}
	private static boolean canHoldKeys(Minecraft mc)
	{
		return mc.screen == null && mc.getOverlay() == null;
	}
	@SuppressWarnings("all") // VanillaCopy
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
	public static void setSquidDelay(LivingEntity entity, float delay)
	{
		int delayInt = (int) delay;
		if (delayInt > squidAndSubDelay && entity instanceof LocalPlayer)
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
			boolean isKeyDown = key.isDown() && canHold;
			pressed = isKeyDown && !previousKeyDown;
			released = !isKeyDown && previousKeyDown;
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
		public KeyState getKeybindState()
		{
			if (active)
			{
				if (pressed)
					return KeyState.JUST_PRESSED;
				return KeyState.PRESSED;
			}
			else
			{
				if (released)
					return KeyState.JUST_RELEASED;
				return KeyState.RELEASED;
			}
		}
		public enum KeyState
		{
			RELEASED,
			JUST_PRESSED,
			PRESSED,
			JUST_RELEASED
		}
	}
}
