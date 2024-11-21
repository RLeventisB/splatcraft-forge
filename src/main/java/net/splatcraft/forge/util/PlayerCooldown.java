package net.splatcraft.forge.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.commands.SuperJumpCommand;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.items.weapons.BlasterItem;
import net.splatcraft.forge.items.weapons.DualieItem;
import net.splatcraft.forge.items.weapons.SlosherItem;
import net.splatcraft.forge.items.weapons.WeaponBaseItem;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

import static net.splatcraft.forge.Splatcraft.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PlayerCooldown
{
    public static Supplier<IForgeRegistry<Class<? extends PlayerCooldown>>> REGISTRY;

    @SubscribeEvent
    public static void registerRegistry(final NewRegistryEvent event)
    {
        RegistryBuilder<Class<? extends PlayerCooldown>> registryBuilder = new RegistryBuilder<>();
        registryBuilder.setName(new ResourceLocation(MODID, "player_cooldowns"));
        REGISTRY = event.create(registryBuilder, (registry) ->
        {
            registry.register(new ResourceLocation(MODID, "superjump"), SuperJumpCommand.SuperJump.class);
            registry.register(new ResourceLocation(MODID, "sloshcooldown"), SlosherItem.SloshCooldown.class);
            registry.register(new ResourceLocation(MODID, "dodgerollcooldown"), DualieItem.DodgeRollCooldown.class);
            registry.register(new ResourceLocation(MODID, "blastercooldown"), BlasterItem.BlasterCooldown.class);
            registry.register(new ResourceLocation(MODID, "weaponfirecooldown"), WeaponBaseItem.WeaponFireCooldown.class);
        });
    }

    float maxTime;
    int slotIndex;
    InteractionHand hand;
    boolean canMove;
    boolean forceCrouch;
    boolean preventWeaponUse;
    boolean isGrounded;
    public ItemStack storedStack;
    float time;

    public boolean cancellable = false;

    public static final int OVERLOAD_LIMIT = -28800;

    public PlayerCooldown(ItemStack stack, float time, float maxTime, int slotIndex, InteractionHand hand, boolean canMove, boolean forceCrouch, boolean preventWeaponUse, boolean isGrounded)
    {
        this.storedStack = stack;
        this.time = time;
        this.maxTime = maxTime;
        this.slotIndex = slotIndex;
        this.hand = hand;
        this.canMove = canMove;
        this.forceCrouch = forceCrouch;
        this.preventWeaponUse = preventWeaponUse;
        this.isGrounded = isGrounded;
    }

    public PlayerCooldown(ItemStack stack, float time, int slotIndex, InteractionHand hand, boolean canMove, boolean forceCrouch, boolean preventWeaponUse, boolean isGrounded)
    {
        this(stack, time, time, slotIndex, hand, canMove, forceCrouch, preventWeaponUse, isGrounded);
    }

    public PlayerCooldown(CompoundTag nbt)
    {
        fromNbt(nbt);
    }

    public final void fromNbt(CompoundTag nbt)
    {
        this.storedStack = ItemStack.of(nbt.getCompound("StoredStack"));
        this.time = nbt.getFloat("Time");
        this.maxTime = nbt.getFloat("MaxTime");
        this.slotIndex = nbt.getInt("SlotIndex");
        this.hand = nbt.getBoolean("MainHand") ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        this.canMove = nbt.getBoolean("CanMove");
        this.forceCrouch = nbt.getBoolean("ForceCrouch");
        this.preventWeaponUse = nbt.getBoolean("PreventWeaponUse");
        this.isGrounded = nbt.getBoolean("IsGrounded");
    }

    public static PlayerCooldown readNBT(CompoundTag nbt)
    {
        if (nbt.contains("CooldownClass"))
        {
            Class<? extends PlayerCooldown> clazz = REGISTRY.get().getValue(new ResourceLocation(nbt.getString("CooldownClass")));
            try
            {
                if (clazz == null) throw new AssertionError();
                return clazz.getConstructor(CompoundTag.class).newInstance(nbt);
            }
            catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                   NoSuchMethodException e)
            {
                Splatcraft.LOGGER.error(new RuntimeException(e));
            }
        }

        return new PlayerCooldown(nbt);
    }

    public PlayerCooldown setCancellable()
    {
        this.cancellable = true;
        return this;
    }

    public static PlayerCooldown getPlayerCooldown(LivingEntity player)
    {
        PlayerInfo playerInfo = PlayerInfoCapability.get(player);
        if (playerInfo == null)
            return null;
        return playerInfo.getPlayerCooldown();
    }

    public static void setPlayerCooldown(Player player, PlayerCooldown playerCooldown)
    {
        PlayerInfoCapability.get(player).setPlayerCooldown(playerCooldown);
    }

    public static PlayerCooldown setCooldownTime(Player player, int time)
    {
        PlayerCooldown cooldown = PlayerInfoCapability.get(player).getPlayerCooldown();
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
        if (player == null || !PlayerInfoCapability.hasCapability(player))
            return false;
        PlayerCooldown cooldown = PlayerInfoCapability.get(player).getPlayerCooldown();
        return cooldown != null && cooldown.getTime() > 0;
    }

    public static boolean hasOverloadedPlayerCooldown(Player player)
    {
        if (player == null || !PlayerInfoCapability.hasCapability(player))
            return false;
        PlayerCooldown cooldown = PlayerInfoCapability.get(player).getPlayerCooldown();
        return cooldown != null;
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

    public InteractionHand getHand()
    {
        return hand;
    }

    public void setHand(InteractionHand hand)
    {
        this.hand = hand;
    }

    public void tick(Player player)
    {
    }

    public CompoundTag writeNBT(CompoundTag nbt)
    {
        nbt.putFloat("Time", time);
        nbt.putFloat("MaxTime", maxTime);
        nbt.putInt("SlotIndex", slotIndex);
        nbt.putBoolean("CanMove", canMove);
        nbt.putBoolean("ForceCrouch", forceCrouch);
        nbt.putBoolean("PreventWeaponUse", preventWeaponUse);
        nbt.putBoolean("IsGrounded", isGrounded);
        nbt.putBoolean("MainHand", hand.equals(InteractionHand.MAIN_HAND));
        if (storedStack.getItem() != Items.AIR)
        {
            nbt.put("StoredStack", storedStack.serializeNBT());
        }
        return nbt;
    }

    public void onStart(Player player)
    {
    }

    public void onEnd(Player player)
    {
    }
}
