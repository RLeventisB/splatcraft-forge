package net.splatcraft.forge.items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.splatcraft.forge.blocks.InkedBlock;
import net.splatcraft.forge.blocks.InkwellBlock;
import net.splatcraft.forge.client.handlers.JumpLureHudHandler;
import net.splatcraft.forge.commands.SuperJumpCommand;
import net.splatcraft.forge.data.Stage;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.items.weapons.SubWeaponItem;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.s2c.SendJumpLureDataPacket;
import net.splatcraft.forge.registries.SplatcraftGameRules;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.util.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JumpLureItem extends Item implements IColoredItem
{
    public JumpLureItem()
    {
        super(new Properties().stacksTo(1));
        SplatcraftItems.inkColoredItems.add(this);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flags)
    {
        super.appendHoverText(stack, level, tooltip, flags);
        if (I18n.exists(getDescriptionId() + ".tooltip"))
            tooltip.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        boolean inverted = ColorUtils.isInverted(stack);
        if (ColorUtils.isColorLocked(stack))
        {
            tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));
            if (inverted)
                tooltip.add(Component.translatable("item.splatcraft.tooltip.inverted").withStyle(Style.EMPTY.withItalic(true).withColor(ChatFormatting.DARK_PURPLE)));
        }
        else
            tooltip.add(Component.translatable("item.splatcraft.tooltip.matches_color" + (inverted ? ".inverted" : "")).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public int getUseDuration(@NotNull ItemStack p_41454_)
    {
        return 72000;
    }

    public static void activate(ServerPlayer player, UUID targetUUID, int color)
    {
        Vec3 target;
        if (targetUUID == null)
        {
            BlockPos spawnPos = SuperJumpCommand.getSpawnPadPos(player);
            if (spawnPos != null)
            {
                target = new Vec3(spawnPos.getCenter().x(), spawnPos.getY() + SuperJumpCommand.blockHeight(spawnPos, player.level()), spawnPos.getCenter().z());
                if (!SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.GLOBAL_SUPERJUMPING) && !SuperJumpCommand.canSuperJumpTo(player, target))
                {
                    player.sendSystemMessage(Component.literal("Spawn Pad outside of stage bounds!")); //TODO better feedback
                    return;
                }
            }
            else
            {
                player.sendSystemMessage(Component.literal("No valid Spawn Pad was found!")); //TODO better feedback
                return;
            }
        }
        else
        {
            Player targetPlayer = player.level().getPlayerByUUID(targetUUID);

            if (targetPlayer == null || !hasMatchingLure(targetPlayer, color) || (!SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.GLOBAL_SUPERJUMPING)
                && !SuperJumpCommand.canSuperJumpTo(player, targetPlayer.position())))
            {
                player.sendSystemMessage(Component.literal("A communication error has occurred.")); //TODO better feedback
                return;
            }
            else
                target = targetPlayer.position();
        }
        SuperJumpCommand.superJump(player, target);
    }

    public static boolean hasMatchingLure(Player targetPlayer, int color)
    {
        for (int i = 0; i < targetPlayer.getInventory().getContainerSize(); i++)
            if (targetPlayer.getInventory().getItem(i).getItem() instanceof JumpLureItem &&
                ColorUtils.colorEquals(targetPlayer.level(), targetPlayer.blockPosition(), color, ColorUtils.getInkColorOrInverted(targetPlayer.getInventory().getItem(i))))
                return true;
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    private void releaseLure(LivingEntity entity)
    {
        if (entity.equals(Minecraft.getInstance().player))
            JumpLureHudHandler.releaseLure();
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack p_41452_)
    {
        return UseAnim.SPEAR;
    }

    public static List<? extends Player> getAvailableCandidates(Player player, int color)
    {
        ArrayList<Player> players = new ArrayList<>();
        if (SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.GLOBAL_SUPERJUMPING))
        {
            players.addAll(player.level().players());
        }
        else
        {
            ArrayList<Stage> stages = Stage.getStagesForPosition(player.level(), player.position());
            for (Stage stage : stages)
                players.addAll(player.level().getEntitiesOfClass(Player.class, stage.getBounds()));
        }
        players.removeIf(target ->
            player.equals(target) || !hasMatchingLure(target, color)
                && !SuperJumpCommand.canSuperJumpTo(player, target.position()));
        return players;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand)
    {
        if (level.isClientSide())
        {
            JumpLureHudHandler.clickedThisFrame = false;
            return super.use(level, player, hand);
        }

        int color = ColorUtils.getInkColorOrInverted(player.getItemInHand(hand));
        ArrayList<UUID> players = new ArrayList<>(getAvailableCandidates(player, color).stream().map(Entity::getUUID).toList());

        ServerPlayer serverPlayer = (ServerPlayer) player;

        BlockPos spawnPadPos = SuperJumpCommand.getSpawnPadPos(serverPlayer);

        if (!SplatcraftGameRules.getLocalizedRule(level, player.blockPosition(), SplatcraftGameRules.GLOBAL_SUPERJUMPING) && !SuperJumpCommand.canSuperJumpTo(player, new Vec3(spawnPadPos.getX(), spawnPadPos.getY(), spawnPadPos.getZ())))
            spawnPadPos = null;

        if (spawnPadPos == null && players.isEmpty())
        {
            player.displayClientMessage(Component.translatable("status.no_superjump_targets").withStyle(ChatFormatting.RED), true);
            return super.use(level, player, hand);
        }
        SplatcraftPacketHandler.sendToPlayer(new SendJumpLureDataPacket(color, spawnPadPos != null,
            players, spawnPadPos), serverPlayer);

        player.startUsingItem(hand);
        return super.use(level, player, hand);
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity, int useTime)
    {
        super.releaseUsing(stack, level, entity, useTime);
        if (level.isClientSide())
            releaseLure(entity);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int itemSlot, boolean isSelected)
    {
        super.inventoryTick(stack, level, entity, itemSlot, isSelected);

        if (entity instanceof Player player)
        {
            if (!ColorUtils.isColorLocked(stack) && ColorUtils.getInkColor(stack) != ColorUtils.getPlayerColor(player)
                && PlayerInfoCapability.hasCapability(player))
                ColorUtils.setInkColor(stack, ColorUtils.getPlayerColor(player));
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity)
    {
        BlockPos pos = entity.blockPosition().below();

        if (entity.level().getBlockState(pos).getBlock() instanceof InkwellBlock)
        {
            if (ColorUtils.getInkColor(stack) != ColorUtils.getInkColorOrInverted(entity.level(), pos))
            {
                ColorUtils.setInkColor(entity.getItem(), ColorUtils.getInkColorOrInverted(entity.level(), pos));
                ColorUtils.setColorLocked(entity.getItem(), true);
            }
        }
        else if ((stack.getItem() instanceof SubWeaponItem && !SubWeaponItem.singleUse(stack) || !(stack.getItem() instanceof SubWeaponItem))
            && InkedBlock.causesClear(entity.level(), pos, entity.level().getBlockState(pos)) && ColorUtils.getInkColor(stack) != 0xFFFFFF)
        {
            ColorUtils.setInkColor(stack, 0xFFFFFF);
            ColorUtils.setColorLocked(stack, false);
        }
        return false;
    }
}