package net.splatcraft.client.handlers;

import com.google.common.collect.Iterables;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.data.capabilities.playerinfo.EntityInfo;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.handlers.ShootingHandler;
import net.splatcraft.items.weapons.IChargeableWeapon;
import net.splatcraft.items.weapons.SubWeaponItem;
import net.splatcraft.mixin.accessors.MinecraftClientAccessor;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.SwapSlotWithOffhandPacket;
import net.splatcraft.network.c2s.UpdateChargeStatePacket;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.PlayerCharge;
import net.splatcraft.util.PlayerCooldown;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class SplatcraftKeyHandler
{
    private static final List<ToggleableKey> pressState = new ObjectArrayList<>();
    private static final ToggleableKey SHOOT_KEYBIND = new ToggleableKey(MinecraftClient.getInstance().options.useKey);
    private static final ToggleableKey SQUID_KEYBIND = new ToggleableKey(new KeyBinding("key.squidForm", GLFW.GLFW_KEY_Z, "key.categories.splatcraft"));
    private static final ToggleableKey SUB_WEAPON_KEYBIND = new ToggleableKey(new KeyBinding("key.subWeaponHotkey", GLFW.GLFW_KEY_V, "key.categories.splatcraft"));
    private static int autoSquidDelay = 0; //delays automatically returning into squid form after firing for balancing reasons and to allow packet-based weapons to fire (chargers and splatlings)
    private static boolean usingSubWeaponHotkey;
    private static int slot = -1;

    public static void registerBindingsAndEvents()
    {
        KeyMappingRegistry.register(SUB_WEAPON_KEYBIND.key);
        KeyMappingRegistry.register(SHOOT_KEYBIND.key);
        KeyMappingRegistry.register(SQUID_KEYBIND.key);
        ClientTickEvent.CLIENT_PRE.register(SplatcraftKeyHandler::onClientTick);
    }

    public static boolean isSubWeaponHotkeyDown()
    {
        return SUB_WEAPON_KEYBIND.active;
    }

    public static boolean isSquidKeyDown()
    {
        return !pressState.isEmpty() && Iterables.getLast(pressState).equals(SQUID_KEYBIND);
    }

    public static void onClientTick(MinecraftClient mc)
    {
        PlayerEntity player = mc.player;

        if (player == null || player.isSpectator() || !EntityInfoCapability.hasCapability(player))
        {
            return;
        }

        if (!mc.options.useKey.isPressed() && PlayerCharge.hasCharge(player) && EntityInfoCapability.isSquid(player)) //Resets weapon charge when player is in swim form and not holding down right click. Used to void Charge Storage for Splatlings and Chargers.
        {
            PlayerCharge.getCharge(player).reset();
            SplatcraftPacketHandler.sendToServer(new UpdateChargeStatePacket(false));
        }

        boolean canHold = canHoldKeys(mc);

        SHOOT_KEYBIND.tick(KeyMode.HOLD, canHold);
        updatePressState(SHOOT_KEYBIND, autoSquidDelay);

        EntityInfo info = EntityInfoCapability.get(player);
        KeyMode squidKeyMode = SplatcraftConfig.get("splatcraft.squidKeyMode");

        SQUID_KEYBIND.tick(squidKeyMode, canHold);
        updatePressState(SQUID_KEYBIND, 0);

        SUB_WEAPON_KEYBIND.tick(KeyMode.HOLD, canHold);
        updatePressState(SUB_WEAPON_KEYBIND, autoSquidDelay);

        if ((PlayerCooldown.hasPlayerCooldown(player) && !(PlayerCooldown.getPlayerCooldown(player).cancellable && SQUID_KEYBIND.active))
            || CommonUtils.anyWeaponOnCooldown(player) || ShootingHandler.isDoingShootingAction(player))
        {
            return;
        }

        ToggleableKey last = !pressState.isEmpty() ? Iterables.getLast(pressState) : null;

        if (!MinecraftClient.getInstance().isPaused())
        {
            int autoSquidMaxDelay = PlayerCooldown.hasPlayerCooldown(player) ? (int) (PlayerCooldown.getPlayerCooldown(player).getTime() + 10) :
                (player.getActiveItem().getItem() instanceof IChargeableWeapon ? 100 : 10); //autosquid delay set to 5 seconds for chargeables if cooldown hasn't been received yet
            autoSquidDelay = SHOOT_KEYBIND.active || SUB_WEAPON_KEYBIND.active || PlayerCooldown.hasPlayerCooldown(player) ? autoSquidMaxDelay :
                MathHelper.clamp(autoSquidDelay - 1, 0, autoSquidMaxDelay);
        }

        if (SHOOT_KEYBIND.equals(last) || SUB_WEAPON_KEYBIND.equals(last))
        {
            // Unsquid so we can actually fire
            ClientUtils.setSquid(info, false, true);
        }

        if (SUB_WEAPON_KEYBIND.equals(last))
        {
            ItemStack sub = CommonUtils.getItemInInventory(player, itemStack -> itemStack.getItem() instanceof SubWeaponItem);

            if (sub.isEmpty() || (info.isSquid() && player.getWorld().getBlockCollisions(player,
                new Box(-0.3 + player.getX(), player.getY(), -0.3 + player.getZ(), 0.3 + player.getX(), 0.6 + player.getY(), 0.3 + player.getZ())).iterator().hasNext()))
            {
                player.sendMessage(Text.translatable("status.cant_use"), true);
            }
            else
            {
                ClientUtils.setSquid(info, false);

                if (SUB_WEAPON_KEYBIND.pressed)
                {
                    if (!player.getStackInHand(Hand.OFF_HAND).equals(sub))
                    {
                        slot = player.getInventory().getSlotWithStack(sub);
                        SplatcraftPacketHandler.sendToServer(new SwapSlotWithOffhandPacket(slot, false));

                        ItemStack stack = player.getOffHandStack();
                        player.setStackInHand(Hand.OFF_HAND, player.getInventory().getStack(slot));
                        player.getInventory().setStack(slot, stack);
                        player.stopUsingItem();
                    }
                    else if (!usingSubWeaponHotkey) slot = -1;

                    usingSubWeaponHotkey = true;
                    startUsingItemInHand(Hand.OFF_HAND);
                }
            }
        }
        else
        {
            if (SUB_WEAPON_KEYBIND.released && mc.interactionManager != null && player.getActiveHand() == Hand.OFF_HAND)
            {
                mc.interactionManager.stopUsingItem(player);
            }

            if (slot != -1)
            {
                ItemStack stack = player.getOffHandStack();
                player.setStackInHand(Hand.OFF_HAND, player.getInventory().getStack(slot));
                player.getInventory().setStack(slot, stack);
                player.stopUsingItem();

                SplatcraftPacketHandler.sendToServer(new SwapSlotWithOffhandPacket(slot, false));
                usingSubWeaponHotkey = false;
                slot = -1;
            }
        }

        if (player.getVehicle() == null &&
            !player.getWorld().getBlockCollisions(player,
                new Box(-0.3 + player.getX(), player.getY(), -0.3 + player.getZ(), 0.3 + player.getX(), 0.6 + player.getY(), 0.3 + player.getZ())).iterator().hasNext())
        {
            if (SQUID_KEYBIND.equals(last) || !SQUID_KEYBIND.active)
            {
                ClientUtils.setSquid(info, SQUID_KEYBIND.active);
            }
        }
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

    private static boolean canHoldKeys(MinecraftClient mc)
    {
        return mc.currentScreen == null && mc.getOverlay() == null;
    }

    @SuppressWarnings("all") // VanillaCopy
    public static void startUsingItemInHand(Hand hand)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!mc.interactionManager.isBreakingBlock())
        {
            ((MinecraftClientAccessor) mc).setRightClickDelay(4);
            {
                CommonUtils.InteractionEventResultDummy inputEvent = CommonUtils.doPlayerUseItemForgeEvent(1, mc.options.useKey, hand);
                if (inputEvent.isCanceled())
                {
                    if (inputEvent.shouldSwingHand())
                    {
                        mc.player.swingHand(hand);
                    }
                    return;
                }
                ItemStack itemstack = mc.player.getStackInHand(hand);
                if (mc.crosshairTarget != null)
                {
                    switch (mc.crosshairTarget.getType())
                    {
                        case ENTITY:
                            EntityHitResult entityraytraceresult = (EntityHitResult) mc.crosshairTarget;
                            Entity entity = entityraytraceresult.getEntity();
                            ActionResult actionresulttype = mc.interactionManager.interactEntityAtLocation(mc.player, entity, entityraytraceresult, hand);
                            if (!actionresulttype.isAccepted())
                            {
                                actionresulttype = mc.interactionManager.interactEntity(mc.player, entity, hand);
                            }

                            if (actionresulttype.isAccepted())
                            {
                                if (actionresulttype.shouldSwingHand())
                                {
                                    if (inputEvent.shouldSwingHand())
                                    {
                                        mc.player.swingHand(hand);
                                    }
                                }

                                return;
                            }
                            break;
                        case BLOCK:
                            BlockHitResult blockraytraceresult = (BlockHitResult) mc.crosshairTarget;
                            int i = itemstack.getCount();
                            ActionResult actionresulttype1 = mc.interactionManager.interactBlock(mc.player, hand, blockraytraceresult);
                            if (actionresulttype1.isAccepted())
                            {
                                if (actionresulttype1.shouldSwingHand())
                                {
                                    if (inputEvent.shouldSwingHand())
                                    {
                                        mc.player.swingHand(hand);
                                    }
                                    if (!itemstack.isEmpty() && (itemstack.getCount() != i || mc.interactionManager.hasCreativeInventory()))
                                    {
                                        mc.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                                    }
                                }

                                return;
                            }

                            if (actionresulttype1 == ActionResult.FAIL)
                            {
                                return;
                            }
                    }
                }

                if (itemstack.isEmpty() && (mc.crosshairTarget == null || mc.crosshairTarget.getType() == HitResult.Type.MISS))
                {
                    CommonUtils.doForgeEmptyClickEvent(mc.player, hand);
                }

                if (!itemstack.isEmpty())
                {
                    ActionResult actionresulttype2 = mc.interactionManager.interactItem(mc.player, hand);
                    if (actionresulttype2.isAccepted())
                    {
                        if (actionresulttype2.shouldSwingHand())
                        {
                            mc.player.swingHand(hand);
                        }

                        mc.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                    }
                }
            }
        }
    }

    public enum KeyMode
    {
        HOLD,
        TOGGLE
    }

    private static class ToggleableKey
    {
        private final KeyBinding key;
        private boolean active;
        private boolean wasKeyDown;
        private boolean pressed;
        private boolean released;

        public ToggleableKey(KeyBinding key)
        {
            this.key = key;
        }

        public void tick(KeyMode mode, boolean canHold)
        {
            boolean isKeyDown = key.isPressed() && canHold;
            if (mode.equals(KeyMode.HOLD))
            {
                pressed = isKeyDown && !active;
                released = !isKeyDown && active;
                active = isKeyDown;
                return;
            }
            if (isKeyDown && !wasKeyDown)
            {
                active = !active;
            }

            pressed = isKeyDown && !wasKeyDown;
            released = !isKeyDown && wasKeyDown;
            wasKeyDown = isKeyDown;
        }
    }
}
