package net.splatcraft.forge.client.handlers;

import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.splatcraft.forge.SplatcraftConfig;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.items.weapons.IChargeableWeapon;
import net.splatcraft.forge.items.weapons.SubWeaponItem;
import net.splatcraft.forge.mixin.MinecraftClientAccessor;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.c2s.SwapSlotWithOffhandPacket;
import net.splatcraft.forge.network.c2s.UpdateChargeStatePacket;
import net.splatcraft.forge.util.ClientUtils;
import net.splatcraft.forge.util.CommonUtils;
import net.splatcraft.forge.util.PlayerCharge;
import net.splatcraft.forge.util.PlayerCooldown;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class SplatcraftKeyHandler {
    private static final List<ToggleableKey> pressState = new ObjectArrayList<>();

    private static ToggleableKey fireKey;
    private static int autoSquidDelay = 0; //delays automatically returning into squid form after firing for balancing reasons and to allow packet-based weapons to fire (chargers and splatlings)
    private static ToggleableKey squidKey;
    private static ToggleableKey subWeaponHotkey;

    private static int slot = -1;

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        fireKey = new ToggleableKey(Minecraft.getInstance().options.keyUse);

        KeyMapping squidMapping = new KeyMapping("key.squidForm", GLFW.GLFW_KEY_Z, "key.categories.splatcraft");
        event.register(squidMapping);
        squidKey = new ToggleableKey(squidMapping);

        KeyMapping subWeaponMapping = new KeyMapping("key.subWeaponHotkey", -1, "key.categories.splatcraft");
        event.register(subWeaponMapping);
        subWeaponHotkey = new ToggleableKey(subWeaponMapping);
    }

    public static boolean isSubWeaponHotkeyDown() {
        return subWeaponHotkey.active;
    }
    public static boolean isSquidKeyDown() {
        return !pressState.isEmpty() && Iterables.getLast(pressState).equals(squidKey);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || player.isSpectator() || !PlayerInfoCapability.hasCapability(player)) {
            return;
        }

        if(!mc.options.keyUse.isDown() && PlayerCharge.hasCharge(player) && PlayerInfoCapability.isSquid(player)) //Resets weapon charge when player is in swim form and not holding down right click. Used to void Charge Storage for Splatlings and Chargers.
        {
            PlayerCharge.getCharge(player).reset();
            SplatcraftPacketHandler.sendToServer(new UpdateChargeStatePacket(false));
        }

        boolean canHold = canHoldKeys(Minecraft.getInstance());

        fireKey.tick(KeyMode.HOLD, canHold);
        updatePressState(fireKey, autoSquidDelay);

        PlayerInfo info = PlayerInfoCapability.get(player);
        KeyMode squidKeyMode = SplatcraftConfig.Client.squidKeyMode.get();

        squidKey.tick(squidKeyMode, canHold);
        updatePressState(squidKey, 0);

        subWeaponHotkey.tick(KeyMode.HOLD, canHold);
        updatePressState(subWeaponHotkey, autoSquidDelay);

        if ((PlayerCooldown.hasPlayerCooldown(player) && !(PlayerCooldown.getPlayerCooldown(player).cancellable && squidKey.active))
                || CommonUtils.anyWeaponOnCooldown(player))
        {
            return;
        }


        ToggleableKey last = !pressState.isEmpty() ? Iterables.getLast(pressState) : null;

        if(!Minecraft.getInstance().isPaused())
        {
            int autoSquidMaxDelay = PlayerCooldown.hasPlayerCooldown(player) ? PlayerCooldown.getPlayerCooldown(player).getTime() + 10 :
                    (player.getUseItem().getItem() instanceof IChargeableWeapon ? 100 : 10); //autosquid delay set to 5 seconds for chargeables if cooldown hasn't been received yet
            SplatcraftKeyHandler.autoSquidDelay = fireKey.active || subWeaponHotkey.active || PlayerCooldown.hasPlayerCooldown(player) ? autoSquidMaxDelay :
                    Mth.clamp(SplatcraftKeyHandler.autoSquidDelay - 1, 0, autoSquidMaxDelay);
        }

        if (fireKey.equals(last) || subWeaponHotkey.equals(last)) {
            // Unsquid so we can actually fire
            ClientUtils.setSquid(info, false);
        }


        if (subWeaponHotkey.equals(last))
        {
            ItemStack sub = CommonUtils.getItemInInventory(player, itemStack -> itemStack.getItem() instanceof SubWeaponItem);

            if (sub.isEmpty() || (info.isSquid() && player.level().getBlockCollisions(
                    player,
                    new AABB(-0.3 + player.getX(), player.getY(), -0.3 + player.getZ(), 0.3 + player.getX(), 0.6 + player.getY(), 0.3 + player.getZ())).iterator().hasNext())) {
                player.displayClientMessage(Component.translatable("status.cant_use"), true);
            } else {
                ClientUtils.setSquid(info, false);

                if(subWeaponHotkey.pressed)
                {
                    if (!player.getItemInHand(InteractionHand.OFF_HAND).equals(sub)) {
                        slot = player.getInventory().findSlotMatchingItem(sub);
                        SplatcraftPacketHandler.sendToServer(new SwapSlotWithOffhandPacket(slot, false));

                        ItemStack stack = player.getOffhandItem();
                        player.setItemInHand(InteractionHand.OFF_HAND, player.getInventory().getItem(slot));
                        player.getInventory().setItem(slot, stack);
                        player.stopUsingItem();
                    } else slot = -1;

                    startUsingItemInHand(InteractionHand.OFF_HAND);
                }

            }
        } else {
            if (subWeaponHotkey.released && mc.gameMode != null && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
                mc.gameMode.releaseUsingItem(player);
            }

            if (slot != -1) {
                ItemStack stack = player.getOffhandItem();
                player.setItemInHand(InteractionHand.OFF_HAND, player.getInventory().getItem(slot));
                player.getInventory().setItem(slot, stack);
                player.stopUsingItem();

                SplatcraftPacketHandler.sendToServer(new SwapSlotWithOffhandPacket(slot, false));
                slot = -1;
            }
        }


        if (player.getVehicle() == null &&
                !player.level().getBlockCollisions(
                        player,
                        new AABB(-0.3 + player.getX(), player.getY(), -0.3 + player.getZ(), 0.3 + player.getX(), 0.6 + player.getY(), 0.3 + player.getZ())).iterator().hasNext()) {
            if (squidKey.equals(last) || !squidKey.active) {
                ClientUtils.setSquid(info, squidKey.active);
            }
        }
    }

    private static void updatePressState(ToggleableKey key, int releaseDelay) {
        if (key.active) {
            if (!pressState.contains(key)) {
                pressState.add(key);
            }
        } else if(releaseDelay <= 0)
        {
            pressState.remove(key);
        }
    }

    private static boolean canHoldKeys(Minecraft minecraft) {
        return minecraft.screen == null && minecraft.getOverlay() == null;
    }

    @SuppressWarnings("all") // VanillaCopy
    public static void startUsingItemInHand(InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.gameMode.isDestroying()) {
            ((MinecraftClientAccessor) mc).setRightClickDelay(4);
            {
                net.minecraftforge.client.event.InputEvent.ClickInputEvent inputEvent = net.minecraftforge.client.ForgeHooksClient.onClickInput(1, mc.options.keyUse, hand);
                if (inputEvent.isCanceled()) {
                    if (inputEvent.shouldSwingHand()) {
                        mc.player.swing(hand);
                    }
                    return;
                }
                ItemStack itemstack = mc.player.getItemInHand(hand);
                if (mc.hitResult != null) {
                    switch (mc.hitResult.getType()) {
                        case ENTITY:
                            EntityHitResult entityraytraceresult = (EntityHitResult) mc.hitResult;
                            Entity entity = entityraytraceresult.getEntity();
                            InteractionResult actionresulttype = mc.gameMode.interactAt(mc.player, entity, entityraytraceresult, hand);
                            if (!actionresulttype.consumesAction()) {
                                actionresulttype = mc.gameMode.interact(mc.player, entity, hand);
                            }

                            if (actionresulttype.consumesAction()) {
                                if (actionresulttype.shouldSwing()) {
                                    if (inputEvent.shouldSwingHand()) {
                                        mc.player.swing(hand);
                                    }
                                }

                                return;
                            }
                            break;
                        case BLOCK:
                            BlockHitResult blockraytraceresult = (BlockHitResult) mc.hitResult;
                            int i = itemstack.getCount();
                            InteractionResult actionresulttype1 = mc.gameMode.useItemOn(mc.player, mc.level, hand, blockraytraceresult);
                            if (actionresulttype1.consumesAction()) {
                                if (actionresulttype1.shouldSwing()) {
                                    if (inputEvent.shouldSwingHand()) {
                                        mc.player.swing(hand);
                                    }
                                    if (!itemstack.isEmpty() && (itemstack.getCount() != i || mc.gameMode.hasInfiniteItems())) {
                                        mc.gameRenderer.itemInHandRenderer.itemUsed(hand);
                                    }
                                }

                                return;
                            }

                            if (actionresulttype1 == InteractionResult.FAIL) {
                                return;
                            }
                    }
                }

                if (itemstack.isEmpty() && (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS)) {
                    net.minecraftforge.common.ForgeHooks.onEmptyClick(mc.player, hand);
                }

                if (!itemstack.isEmpty()) {
                    InteractionResult actionresulttype2 = mc.gameMode.useItem(mc.player, mc.level, hand);
                    if (actionresulttype2.consumesAction()) {
                        if (actionresulttype2.shouldSwing()) {
                            mc.player.swing(hand);
                        }

                        mc.gameRenderer.itemInHandRenderer.itemUsed(hand);
                    }
                }
            }

        }
    }

    public enum KeyMode {
        HOLD,
        TOGGLE
    }

    private static class ToggleableKey {
        private final KeyMapping key;
        private boolean active;
        private boolean wasKeyDown;
        private boolean pressed;
        private boolean released;

        public ToggleableKey(KeyMapping key) {
            this.key = key;
        }

        public void tick(KeyMode mode, boolean canHold)
        {
            boolean isKeyDown = key.isDown() && canHold;
            if (mode.equals(KeyMode.HOLD))
            {
                pressed = isKeyDown && !active;
                released = !isKeyDown && active;
                active = isKeyDown;
                return;
            }
            if (isKeyDown && !wasKeyDown) {
                active = !active;
            }

            pressed = isKeyDown && !wasKeyDown;
            released = !isKeyDown && wasKeyDown;
            wasKeyDown = isKeyDown;

        }
    }
}