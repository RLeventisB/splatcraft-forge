package net.splatcraft.items.weapons;

import com.mojang.serialization.DataResult;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.splatcraft.Splatcraft;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.blocks.InkedBlock;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.handlers.DataHandler;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.handlers.ShootingHandler;
import net.splatcraft.items.IColoredItem;
import net.splatcraft.items.ISplatcraftForgeItemDummy;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.weapons.settings.*;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

public abstract class WeaponBaseItem<S extends AbstractWeaponSettings<S, ?>> extends Item implements IColoredItem, ISplatcraftForgeItemDummy
{
    public static final int USE_DURATION = 72000;
    private static final HashMap<Class<? extends AbstractWeaponSettings<?, ?>>, AbstractWeaponSettings<?, ?>> DEFAULTS = new HashMap<>() // a
    {{
        put(ShooterWeaponSettings.class, ShooterWeaponSettings.DEFAULT);
        put(BlasterWeaponSettings.class, BlasterWeaponSettings.DEFAULT);
        put(RollerWeaponSettings.class, RollerWeaponSettings.DEFAULT);
        put(ChargerWeaponSettings.class, ChargerWeaponSettings.DEFAULT);
        put(SlosherWeaponSettings.class, SlosherWeaponSettings.DEFAULT);
        put(DualieWeaponSettings.class, DualieWeaponSettings.DEFAULT);
        put(SubWeaponSettings.class, SubWeaponSettings.DEFAULT);
        put(SplatlingWeaponSettings.class, SplatlingWeaponSettings.DEFAULT);
    }};
    public Identifier settingsId;
    public boolean isSecret;

    public WeaponBaseItem(String settingsId)
    {
        super(new Item.Settings().maxCount(1));
        SplatcraftItems.inkColoredItems.add(this);
        SplatcraftItems.weapons.add(this);
        this.settingsId = settingsId.contains(":") ? Identifier.of(settingsId) : Splatcraft.identifierOf(settingsId);

        CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map().put(this, (state, level, pos, player, hand, stack) ->
        {
            if (ColorUtils.isColorLocked(stack) && !player.isSneaking())
            {
                ColorUtils.setColorLocked(stack, false);

                player.incrementStat(Stats.USE_CAULDRON);

                if (!player.isCreative())
                    LeveledCauldronBlock.decrementFluidLevel(state, level, pos);

                return ItemActionResult.success(level.isClient);
            }
            return ItemActionResult.FAIL;
        });
    }

    public static boolean reduceInk(LivingEntity player, Item item, float amount, float recoveryCooldown, boolean sendMessage)
    {
        return reduceInk(player, item, amount, recoveryCooldown, sendMessage, false);
    }

    public static boolean reduceInk(LivingEntity player, Item item, float amount, float recoveryCooldown, boolean sendMessage, boolean force)
    {
        if (!force && !enoughInk(player, item, amount, recoveryCooldown, sendMessage, false)) return false;
        ItemStack tank = player.getEquippedStack(EquipmentSlot.CHEST);
        if (tank.getItem() instanceof InkTankItem)
            InkTankItem.setInkAmount(tank, InkTankItem.getInkAmount(tank) - amount);
        return true;
    }

    public static boolean refundInk(LivingEntity player, float amount)
    {
        ItemStack tank = player.getEquippedStack(EquipmentSlot.CHEST);
        if (tank.getItem() instanceof InkTankItem inkTank)
            InkTankItem.setInkAmount(tank, Math.min(inkTank.capacity, InkTankItem.getInkAmount(tank) + amount));
        return true;
    }

    public static boolean enoughInk(LivingEntity player, Item item, float consumption, float recoveryCooldown, boolean sendMessage)
    {
        return enoughInk(player, item, consumption, recoveryCooldown, sendMessage, false);
    }

    public static boolean enoughInk(LivingEntity player, Item item, float consumption, float recoveryCooldown, boolean sendMessage, boolean sub)
    {
        ItemStack tank = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!SplatcraftGameRules.getLocalizedRule(player.getWorld(), player.getBlockPos(), SplatcraftGameRules.REQUIRE_INK_TANK)
            || player instanceof PlayerEntity plr && plr.isCreative()
            && SplatcraftGameRules.getBooleanRuleValue(player.getWorld(), SplatcraftGameRules.INFINITE_INK_IN_CREATIVE))
        {
            return true;
        }
        if (tank.getItem() instanceof InkTankItem tankItem)
        {
            boolean enoughInk = InkTankItem.getInkAmount(tank) - consumption >= 0
                && (item == null || tankItem.canUse(item));
            if (!sub || enoughInk)
                InkTankItem.setRecoveryCooldown(tank, recoveryCooldown);
            if (!enoughInk && sendMessage)
                sendNoInkMessage(player, sub ? SplatcraftSounds.noInkSub : SplatcraftSounds.noInkMain);
            return enoughInk;
        }
        if (sendMessage)
            sendNoInkMessage(player, sub ? SplatcraftSounds.noInkSub : SplatcraftSounds.noInkMain);
        return false;
    }

    public static boolean hasInkInTank(LivingEntity livingEntity, Item item)
    {
        ItemStack tank = livingEntity.getEquippedStack(EquipmentSlot.CHEST);
        if (!SplatcraftGameRules.getLocalizedRule(livingEntity.getWorld(), livingEntity.getBlockPos(), SplatcraftGameRules.REQUIRE_INK_TANK)
            || livingEntity instanceof PlayerEntity player && player.isCreative()
            && SplatcraftGameRules.getBooleanRuleValue(livingEntity.getWorld(), SplatcraftGameRules.INFINITE_INK_IN_CREATIVE))
        {
            return true;
        }

        return InkTankItem.getInkAmount(tank) > 0 && ((InkTankItem) tank.getItem()).canUse(item);
    }

    public static void sendNoInkMessage(LivingEntity entity, SoundEvent sound)
    {
        if (entity instanceof PlayerEntity player)
        {
            player.sendMessage(Text.translatable("status.no_ink").formatted(Formatting.RED), true);
            if (sound != null)
                playNoInkSound(entity, sound);
        }
    }

    public static void playNoInkSound(LivingEntity entity, SoundEvent sound)
    {
        entity.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundCategory.PLAYERS, 0.8F,
            ((entity.getWorld().getRandom().nextFloat() - entity.getWorld().getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
    }

    public abstract Class<S> getSettingsClass();

    public S getSettings(ItemStack stack)
    {
        // ok this method was confusing so i rewrote it for now
        ComponentMap components = stack.getComponents();
        Identifier id = components.contains(SplatcraftComponents.WEAPON_SETTING_ID) ? components.get(SplatcraftComponents.WEAPON_SETTING_ID) : settingsId;

        DataResult<AbstractWeaponSettings<?, ?>> result = CommonUtils.getFromMap(DataHandler.WeaponStatsListener.SETTINGS, id);
        if (result.isSuccess() && getSettingsClass().isInstance(result.getOrThrow()))
        {
            return getSettingsClass().cast(result.getOrThrow());
        }
        else
        {
            id = settingsId;
            result = CommonUtils.getFromMap(DataHandler.WeaponStatsListener.SETTINGS, id);
            if (result.isSuccess() && getSettingsClass().isInstance(result.getOrThrow()))
            {
                return getSettingsClass().cast(result.getOrThrow());
            }
            return (S) DEFAULTS.get(getSettingsClass());
        }
    }

    public <T extends WeaponBaseItem<?>> T setSecret(boolean secret)
    {
        isSecret = secret;
        return (T) this;
    }

    @Override
    public void appendTooltip(@NotNull ItemStack stack, @Nullable TooltipContext context, @NotNull List<Text> tooltip, @NotNull TooltipType type)
    {
        super.appendTooltip(stack, context, tooltip, type);

        if (ColorUtils.isColorLocked(stack))
        {
            tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));
        }
        else
        {
            tooltip.add(Text.literal(""));
        }

        if (stack.contains(DataComponentTypes.HIDE_TOOLTIP))
            getSettings(stack).addStatsToTooltip(tooltip, type);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull World world, @NotNull Entity entity, int itemSlot, boolean isSelected)
    {
        super.inventoryTick(stack, world, entity, itemSlot, isSelected);

        if (entity instanceof LivingEntity livingEntity)
        {
            CommonRecords.ShotDeviationDataRecord deviationData = getSettings(stack).getShotDeviationData(stack, livingEntity);
            if (deviationData != CommonRecords.ShotDeviationDataRecord.PERFECT_DEFAULT)
            {
                ShotDeviationHelper.tickDeviation(stack, deviationData, 1);
            }
        }
        if (entity instanceof PlayerEntity player)
        {
            if (!ColorUtils.isColorLocked(stack) && ColorUtils.getInkColor(stack) != ColorUtils.getEntityColor(player)
                && EntityInfoCapability.hasCapability(player))
                ColorUtils.setInkColor(stack, ColorUtils.getEntityColor(player));

            if (player.getItemCooldownManager().isCoolingDown(stack.getItem()))
            {
                if (EntityInfoCapability.isSquid(player))
                {
                    EntityInfoCapability.get(player).setIsSquid(false);
                    if (!world.isClient())
                    {
                        SplatcraftPacketHandler.sendToTrackers(new PlayerSetSquidS2CPacket(player.getUuid(), false), player);
                    }
                }

                player.setSprinting(false);
                if (PlayerInventory.isValidHotbarIndex(itemSlot))
                {
                    player.getInventory().selectedSlot = itemSlot;
                }
            }
        }
    }

    public ShootingHandler.FiringStatData getWeaponFireData(ItemStack itemStack, LivingEntity entity)
    {
        return ShootingHandler.FiringStatData.DEFAULT;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity)
    {
        BlockPos pos = entity.getBlockPos().down();

        if (entity.getWorld().getBlockState(pos).getBlock() instanceof InkwellBlock)
        {
            if (ColorUtils.getInkColor(stack) != ColorUtils.getInkColorOrInverted(entity.getWorld(), pos))
            {
                ColorUtils.setInkColor(entity.getStack(), ColorUtils.getInkColorOrInverted(entity.getWorld(), pos));
                ColorUtils.setColorLocked(entity.getStack(), true);
            }
        }
        else if ((!(stack.getItem() instanceof SubWeaponItem) || !SubWeaponItem.singleUse(stack))
            && InkedBlock.causesClear(entity.getWorld(), pos, entity.getWorld().getBlockState(pos)) && ColorUtils.getInkColor(stack) != InkColor.constructOrReuse(0xFFFFFF))
        {
            ColorUtils.setInkColor(stack, InkColor.constructOrReuse(0xFFFFFF));
            ColorUtils.setColorLocked(stack, false);
        }

        return false;
    }

    @Override
    public int getItemBarStep(@NotNull ItemStack stack)
    {
        try
        {
            return (int) (ClientUtils.getDurabilityForDisplay() * 13);
        }
        catch (NoClassDefFoundError e)
        {
            return 13;
        }
    }

    @Override
    public int getItemBarColor(@NotNull ItemStack stack)
    {
        return SplatcraftConfig.get("splatcraft.vanillaInkDurability") ? super.getItemBarColor(stack) : ColorUtils.getInkColor(stack).getColorWithAlpha(255);
    }

    @Override
    public boolean isItemBarVisible(@NotNull ItemStack stack)
    {
        try
        {
            return ClientUtils.showDurabilityBar(stack);
        }
        catch (NoClassDefFoundError e)
        {
            return false;
        }
    }

    @Override
    public int getMaxUseTime(@NotNull ItemStack stack, LivingEntity entity)
    {
        return USE_DURATION;
    }

    public final TypedActionResult<ItemStack> useSuper(World world, PlayerEntity player, Hand hand)
    {
        return super.use(world, player, hand);
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, PlayerEntity player, @NotNull Hand hand)
    {
        if (!(player.isSwimming() && !player.isSubmergedInWater()))
            player.setCurrentHand(hand);
        return useSuper(world, player, hand);
    }

    @Override
    public void onStoppedUsing(@NotNull ItemStack stack, @NotNull World world, LivingEntity entity, int timeLeft)
    {
        entity.stopUsingItem();
        super.onStoppedUsing(stack, world, entity, timeLeft);
    }

    public void weaponUseTick(World world, LivingEntity entity, ItemStack stack, int timeLeft)
    {

    }

    public void onPlayerCooldownEnd(World world, PlayerEntity player, ItemStack stack, PlayerCooldown cooldown)
    {

    }

    public void onPlayerCooldownTick(World world, PlayerEntity player, ItemStack stack, PlayerCooldown cooldown)
    {

    }

    public boolean hasSpeedModifier(LivingEntity entity, ItemStack stack)
    {
        return getSpeedModifier(entity, stack) != null;
    }

    public EntityAttributeModifier getSpeedModifier(LivingEntity entity, ItemStack stack)
    {
        return getSettings(stack).getSpeedModifier();
    }

    public PlayerPosingHandler.WeaponPose getPose(PlayerEntity player, ItemStack stack)
    {
        return PlayerPosingHandler.WeaponPose.NONE;
    }
}