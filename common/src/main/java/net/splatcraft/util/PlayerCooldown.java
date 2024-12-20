package net.splatcraft.util;

import com.mojang.serialization.Lifecycle;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.splatcraft.Splatcraft;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.capabilities.playerinfo.EntityInfo;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.DualieItem;
import net.splatcraft.items.weapons.SlosherItem;

import java.lang.reflect.InvocationTargetException;

public class PlayerCooldown
{
    public static final int OVERLOAD_LIMIT = -28800;
    public static Registry<Class<? extends PlayerCooldown>> REGISTRY = new SimpleRegistry<>(RegistryKey.ofRegistry(Splatcraft.identifierOf("player_cooldowns")), Lifecycle.stable());
    public ItemStack storedStack;
    public boolean cancellable = false;
    float maxTime;
    int slotIndex;
    Hand hand;
    boolean canMove;
    boolean forceCrouch;
    boolean preventWeaponUse;
    boolean isGrounded;
    float time;

    public PlayerCooldown(ItemStack stack, float time, float maxTime, int slotIndex, Hand hand, boolean canMove, boolean forceCrouch, boolean preventWeaponUse, boolean isGrounded)
    {
        storedStack = stack;
        this.time = time;
        this.maxTime = maxTime;
        this.slotIndex = slotIndex;
        this.hand = hand;
        this.canMove = canMove;
        this.forceCrouch = forceCrouch;
        this.preventWeaponUse = preventWeaponUse;
        this.isGrounded = isGrounded;
    }

    public PlayerCooldown(ItemStack stack, float time, int slotIndex, Hand hand, boolean canMove, boolean forceCrouch, boolean preventWeaponUse, boolean isGrounded)
    {
        this(stack, time, time, slotIndex, hand, canMove, forceCrouch, preventWeaponUse, isGrounded);
    }

    public PlayerCooldown(RegistryWrapper.WrapperLookup wrapperLookup, NbtCompound nbt)
    {
        fromNbt(wrapperLookup, nbt);
    }

    public static void registerCooldowns()
    {
        Registry.register(REGISTRY, Splatcraft.identifierOf("superjump"), SuperJumpCommand.SuperJump.class);
        Registry.register(REGISTRY, Splatcraft.identifierOf("sloshcooldown"), SlosherItem.SloshCooldown.class);
        Registry.register(REGISTRY, Splatcraft.identifierOf("dodgerollcooldown"), DualieItem.DodgeRollCooldown.class);
    }

    public static PlayerCooldown readNBT(RegistryWrapper.WrapperLookup wrapperLookup, NbtCompound nbt)
    {
        if (nbt.contains("CooldownClass"))
        {
            Class<? extends PlayerCooldown> clazz = REGISTRY.get(Identifier.of(nbt.getString("CooldownClass")));
            try
            {
                if (clazz == null) throw new AssertionError();
                return clazz.getConstructor(RegistryWrapper.WrapperLookup.class, NbtCompound.class).newInstance(wrapperLookup, nbt);
            }
            catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                   NoSuchMethodException e)
            {
                Splatcraft.LOGGER.error(new RuntimeException(e));
            }
        }

        return new PlayerCooldown(wrapperLookup, nbt);
    }

    public static PlayerCooldown getPlayerCooldown(LivingEntity player)
    {
        EntityInfo playerInfo = EntityInfoCapability.get(player);
        if (playerInfo == null)
            return null;
        return playerInfo.getPlayerCooldown();
    }

    public static void setPlayerCooldown(LivingEntity player, PlayerCooldown playerCooldown)
    {
        EntityInfoCapability.get(player).setPlayerCooldown(playerCooldown);
    }

    public static PlayerCooldown setCooldownTime(LivingEntity player, int time)
    {
        PlayerCooldown cooldown = EntityInfoCapability.get(player).getPlayerCooldown();
        if (cooldown == null)
        {
            return null;
        }
        else
        {
            cooldown.setTime(time);
        }

        return cooldown;
    }

    public static boolean hasPlayerCooldown(LivingEntity player)
    {
        if (player == null || !EntityInfoCapability.hasCapability(player))
            return false;
        PlayerCooldown cooldown = EntityInfoCapability.get(player).getPlayerCooldown();
        return cooldown != null && cooldown.getTime() > 0;
    }

    public static boolean hasOverloadedPlayerCooldown(LivingEntity player)
    {
        if (player == null || !EntityInfoCapability.hasCapability(player))
            return false;
        PlayerCooldown cooldown = EntityInfoCapability.get(player).getPlayerCooldown();
        return cooldown != null;
    }

    public final void fromNbt(RegistryWrapper.WrapperLookup wrapperLookup, NbtCompound nbt)
    {
        storedStack = ItemStack.fromNbtOrEmpty(wrapperLookup, nbt.getCompound("StoredStack"));
        time = nbt.getFloat("Time");
        maxTime = nbt.getFloat("MaxTime");
        slotIndex = nbt.getInt("SlotIndex");
        hand = nbt.getBoolean("MainHand") ? Hand.MAIN_HAND : Hand.OFF_HAND;
        canMove = nbt.getBoolean("CanMove");
        forceCrouch = nbt.getBoolean("ForceCrouch");
        preventWeaponUse = nbt.getBoolean("PreventWeaponUse");
        isGrounded = nbt.getBoolean("IsGrounded");
    }

    public PlayerCooldown setCancellable()
    {
        cancellable = true;
        return this;
    }

    public boolean canMove()
    {
        return canMove;
    }

    public boolean forceCrouch()
    {
        return forceCrouch;
    }

    public boolean preventWeaponUse()
    {
        return preventWeaponUse;
    }

    public boolean isGrounded()
    {
        return isGrounded;
    }

    public float getTime()
    {
        return time;
    }

    public PlayerCooldown setTime(float v)
    {
        time = v;
        return this;
    }

    public float getMaxTime()
    {
        return maxTime;
    }

    public int getSlotIndex()
    {
        return slotIndex;
    }

    public Hand getHand()
    {
        return hand;
    }

    public void setHand(Hand hand)
    {
        this.hand = hand;
    }

    public void tick(LivingEntity player)
    {
    }

    public NbtCompound writeNBT(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
    {
        nbt.putFloat("Time", time);
        nbt.putFloat("MaxTime", maxTime);
        nbt.putInt("SlotIndex", slotIndex);
        nbt.putBoolean("CanMove", canMove);
        nbt.putBoolean("ForceCrouch", forceCrouch);
        nbt.putBoolean("PreventWeaponUse", preventWeaponUse);
        nbt.putBoolean("IsGrounded", isGrounded);
        nbt.putBoolean("MainHand", hand.equals(Hand.MAIN_HAND));
        if (storedStack.getItem() != Items.AIR)
        {
            nbt.put("StoredStack", storedStack.encode(registryLookup, nbt));
        }
        return nbt;
    }

    public void onStart(LivingEntity player)
    {
    }

    public void onEnd(LivingEntity player)
    {
    }
}
