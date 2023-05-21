package net.splatcraft.forge.items.weapons;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.splatcraft.forge.SplatcraftConfig;
import net.splatcraft.forge.blocks.InkedBlock;
import net.splatcraft.forge.blocks.InkwellBlock;
import net.splatcraft.forge.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.handlers.PlayerPosingHandler;
import net.splatcraft.forge.items.IColoredItem;
import net.splatcraft.forge.items.InkTankItem;
import net.splatcraft.forge.items.weapons.settings.AbstractWeaponSettings;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.s2c.PlayerSetSquidClientPacket;
import net.splatcraft.forge.registries.SplatcraftGameRules;
import net.splatcraft.forge.registries.SplatcraftItemGroups;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.tileentities.InkColorTileEntity;
import net.splatcraft.forge.util.ClientUtils;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.PlayerCooldown;
import net.splatcraft.forge.util.WeaponTooltip;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WeaponBaseItem extends Item implements IColoredItem
{
    public static final int USE_DURATION = 72000;
    protected final List<WeaponTooltip> stats = new ArrayList<>();

    public AbstractWeaponSettings settings;

    public WeaponBaseItem(AbstractWeaponSettings settings) {
        super(new Properties().stacksTo(1).tab(SplatcraftItemGroups.GROUP_WEAPONS));
        SplatcraftItems.inkColoredItems.add(this);
        SplatcraftItems.weapons.add(this);
        this.settings = settings;

        CauldronInteraction.WATER.put(this, (state, level, pos, player, hand, stack) ->
        {
            if (ColorUtils.isColorLocked(stack) && !player.isCrouching()) {
                ColorUtils.setColorLocked(stack, false);

	            player.awardStat(Stats.USE_CAULDRON);

               if (!player.isCreative())
				   LayeredCauldronBlock.lowerFillLevel(state, level, pos);

			   return InteractionResult.SUCCESS;


	        } return InteractionResult.PASS;
        });
    }

    public static boolean reduceInk(LivingEntity player, Item item, float amount, int recoveryCooldown, boolean sendMessage) {
        if (!enoughInk(player, item, amount, recoveryCooldown, sendMessage, false)) return false;
        ItemStack tank = player.getItemBySlot(EquipmentSlot.CHEST);
        if (tank.getItem() instanceof InkTankItem)
            InkTankItem.setInkAmount(tank, InkTankItem.getInkAmount(tank) - amount);
        return true;
    }

    public static boolean enoughInk(LivingEntity player, Item item, float consumption, int recoveryCooldown, boolean sendMessage) {
        return enoughInk(player, item, consumption, recoveryCooldown, sendMessage, false);
    }

    public static boolean enoughInk(LivingEntity player, Item item, float consumption, int recoveryCooldown, boolean sendMessage, boolean sub) {
        ItemStack tank = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!SplatcraftGameRules.getLocalizedRule(player.level, player.blockPosition(), SplatcraftGameRules.REQUIRE_INK_TANK)
                || player instanceof Player && ((Player) player).isCreative()
                && SplatcraftGameRules.getBooleanRuleValue(player.level, SplatcraftGameRules.INFINITE_INK_IN_CREATIVE)) {
            return true;
        }
        if (tank.getItem() instanceof InkTankItem) {
            boolean enoughInk = InkTankItem.getInkAmount(tank) - consumption >= 0
                    && (item == null || ((InkTankItem) tank.getItem()).canUse(item));
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

    public static void sendNoInkMessage(LivingEntity entity, SoundEvent sound)
    {
        if (entity instanceof Player)
        {
            ((Player) entity).displayClientMessage(new TranslatableComponent("status.no_ink").withStyle(ChatFormatting.RED), true);
            if (sound != null)
            {
                entity.level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, 0.8F,
                        ((entity.level.getRandom().nextFloat() - entity.level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
            }
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
    {
        super.appendHoverText(stack, level, tooltip, flag);

        if (ColorUtils.isColorLocked(stack))
        {
            tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));
        } else
        {
            tooltip.add(new TextComponent(""));
        }

        for (WeaponTooltip stat : stats) {
            tooltip.add(stat.getTextComponent(stack, level).withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    public void addStat(WeaponTooltip stat) {
        stats.add(stat);
    }

    @Override
    public void fillItemCategory(@NotNull CreativeModeTab group, @NotNull NonNullList<ItemStack> list)
    {
        if (!settings.secret) {
            super.fillItemCategory(group, list);
        }
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, level, entity, itemSlot, isSelected);

        if (entity instanceof Player player) {
            if (!ColorUtils.isColorLocked(stack) && ColorUtils.getInkColor(stack) != ColorUtils.getPlayerColor(player)
                    && PlayerInfoCapability.hasCapability(player))
                ColorUtils.setInkColor(stack, ColorUtils.getPlayerColor(player));

            if (player.getCooldowns().isOnCooldown(stack.getItem())) {
                if (PlayerInfoCapability.isSquid(player)) {
                    PlayerInfoCapability.get(player).setIsSquid(false);
                    if (!level.isClientSide)
                        SplatcraftPacketHandler.sendToTrackers(new PlayerSetSquidClientPacket(player.getUUID(), false), player);
                }
                if(level.isClientSide())
                    SplatcraftKeyHandler.canUseHotkeys = false;
                player.setSprinting(false);
                player.getInventory().selected = itemSlot;
            }
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity)
    {
        BlockPos pos = entity.blockPosition().below();

        if (entity.level.getBlockState(pos).getBlock() instanceof InkwellBlock) {
            InkColorTileEntity te = (InkColorTileEntity) entity.level.getBlockEntity(pos);

            if (ColorUtils.getInkColor(stack) != ColorUtils.getInkColor(te)) {
                ColorUtils.setInkColor(entity.getItem(), ColorUtils.getInkColor(te));
                ColorUtils.setColorLocked(entity.getItem(), true);
            }
        } else if ((stack.getItem() instanceof SubWeaponItem && !SubWeaponItem.singleUse(stack) || !(stack.getItem() instanceof SubWeaponItem))
                && InkedBlock.causesClear(entity.level, pos, entity.level.getBlockState(pos)) && ColorUtils.getInkColor(stack) != 0xFFFFFF) {
            ColorUtils.setInkColor(stack, 0xFFFFFF);
            ColorUtils.setColorLocked(stack, false);
        }

        return false;
    }

    @Override
    public int getBarWidth(ItemStack stack)
    {
        try
        {
            return (int) (ClientUtils.getDurabilityForDisplay(stack) * 13);
        } catch (NoClassDefFoundError e)
        {
            return 13;
        }
    }

    @Override
    public int getBarColor(ItemStack stack)
    {
        return !SplatcraftConfig.Client.vanillaInkDurability.get() ? ColorUtils.getInkColor(stack) : super.getBarColor(stack);
    }


    @Override
    public boolean isBarVisible(ItemStack stack)
    {
        try
        {
            return ClientUtils.showDurabilityBar(stack);
        } catch (NoClassDefFoundError e)
        {
            return false;
        }
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack)
    {
        return USE_DURATION;
    }

    public final InteractionResultHolder<ItemStack> useSuper(Level level, Player player, InteractionHand hand)
    {
        return super.use(level, player, hand);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand)
    {
        if(!(player.isSwimming() && !player.isInWater()))
            player.startUsingItem(hand);
        return useSuper(level, player, hand);
    }

    @Override
    public void onUseTick(@NotNull Level p_219972_1_, @NotNull LivingEntity p_219972_2_, @NotNull ItemStack p_219972_3_, int p_219972_4_) {
        super.onUseTick(p_219972_1_, p_219972_2_, p_219972_3_, p_219972_4_);
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, LivingEntity entity, int timeLeft)
    {
        entity.stopUsingItem();
        super.releaseUsing(stack, level, entity, timeLeft);
    }

    public void weaponUseTick(Level level, LivingEntity entity, ItemStack stack, int timeLeft)
    {

    }

    public void onPlayerCooldownEnd(Level level, Player player, ItemStack stack, PlayerCooldown cooldown)
    {

    }

    public boolean hasSpeedModifier(LivingEntity entity, ItemStack stack)
    {
        return getSpeedModifier(entity, stack) != null;
    }

    public AttributeModifier getSpeedModifier(LivingEntity entity, ItemStack stack)
    {
        return null;
    }

    public PlayerPosingHandler.WeaponPose getPose()
    {
        return PlayerPosingHandler.WeaponPose.NONE;
    }
}
