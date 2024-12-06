package net.splatcraft.items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.blocks.InkedBlock;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ColoredBlockItem extends BlockItem implements IColoredItem
{
    private Item clearItem;
    private boolean matchColor = true;

    public ColoredBlockItem(Block block, Properties properties, Item clearItem)
    {
        super(block, properties);
        SplatcraftItems.inkColoredItems.add(this);
        InkwellBlock.inkCoatingRecipes.put(clearItem, this);
        this.clearItem = clearItem;

        if (clearItem == null)
            return;

        CauldronInteraction.WATER.put(this, ((state, level, pos, player, hand, stack) ->
        {
            if (equals(clearItem) && ColorUtils.getInkColor(stack) < 0)
                return InteractionResult.PASS;

            ItemStack itemstack1 = new ItemStack(clearItem, 1);

            player.awardStat(Stats.USE_CAULDRON);

            if (!player.isCreative())
            {
                stack.shrink(1);
                LayeredCauldronBlock.lowerFillLevel(state, level, pos);
            }

            if (stack.isEmpty())
            {
                player.setItemInHand(hand, itemstack1);
            }
            else if (!player.getInventory().add(itemstack1))
            {
                player.drop(itemstack1, false);
            }

            return InteractionResult.SUCCESS;
        }));
    }

    public ColoredBlockItem(Block block, int stackSize, @Nullable Item clearItem)
    {
        this(block, new Properties().stacksTo(stackSize), clearItem);
    }

    public ColoredBlockItem(Block block, int stackSize)
    {
        this(block, new Properties().stacksTo(stackSize), null);
    }

    public ColoredBlockItem(Block block)
    {
        this(block, 64, null);
    }

    public ColoredBlockItem setMatchColor(boolean matchColor)
    {
        this.matchColor = matchColor;
        return this;
    }

    public boolean matchesColor()
    {
        return matchColor;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
    {
        super.appendHoverText(stack, level, tooltip, flag);

        if (I18n.exists(getDescriptionId() + ".tooltip"))
            tooltip.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));

        boolean inverted = ColorUtils.isInverted(stack);
        if (ColorUtils.isColorLocked(stack))
        {
            tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));
            if (inverted)
                tooltip.add(Component.translatable("item.splatcraft.tooltip.inverted").withStyle(Style.EMPTY.withItalic(true).withColor(ChatFormatting.DARK_PURPLE)));
        }
        else if (matchColor)
            tooltip.add(Component.translatable("item.splatcraft.tooltip.matches_color" + (inverted ? ".inverted" : "")).withStyle(ChatFormatting.GRAY));
    }

    public ColoredBlockItem clearsToSelf()
    {
        clearItem = this;
        return this;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(@NotNull BlockPos pos, Level levelIn, @Nullable Player player, @NotNull ItemStack stack, @NotNull BlockState state)
    {
        MinecraftServer server = levelIn.getServer();
        if (server == null)
            return false;

        int color = ColorUtils.getInkColor(stack);

        if (color != -1)
            ColorUtils.setInkColor(levelIn.getBlockEntity(pos), color);
        ColorUtils.setInverted(levelIn, pos, ColorUtils.isInverted(stack));

        return super.updateCustomBlockEntityTag(pos, levelIn, player, stack, state);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level levelIn, @NotNull Entity entityIn, int itemSlot, boolean isSelected)
    {
        super.inventoryTick(stack, levelIn, entityIn, itemSlot, isSelected);

        if (matchColor && (ColorUtils.getInkColor(stack) == -1 || !ColorUtils.isColorLocked(stack)))
        {
            ColorUtils.setInkColor(stack, entityIn instanceof Player && PlayerInfoCapability.hasCapability((LivingEntity) entityIn) ?
                ColorUtils.getPlayerColor((Player) entityIn) : ColorUtils.DEFAULT);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity)
    {
        BlockPos pos = entity.blockPosition();

        if (entity.getWorld().getBlockState(pos.below()).getBlock() instanceof InkwellBlock)
        {
            if (ColorUtils.getInkColor(stack) != ColorUtils.getInkColorOrInverted(entity.getWorld(), pos.below()))
            {
                ColorUtils.setInkColor(entity.getItem(), ColorUtils.getInkColorOrInverted(entity.getWorld(), pos.below()));
                ColorUtils.setColorLocked(entity.getItem(), true);
            }
        }
        else if (!(equals(clearItem) && ColorUtils.getInkColor(stack) < 0) &&
            clearItem != null && InkedBlock.causesClear(entity.getWorld(), pos, entity.getWorld().getBlockState(pos), Direction.UP))
        {
            entity.setItem(new ItemStack(clearItem, stack.getCount()));
        }

        return false;
    }
}