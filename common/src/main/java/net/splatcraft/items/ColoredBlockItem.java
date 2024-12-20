package net.splatcraft.items;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stat.Stats;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.splatcraft.blocks.InkedBlock;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ColoredBlockItem extends BlockItem implements IColoredItem, ISplatcraftForgeItemDummy
{
    private Item clearItem;
    private boolean matchColor = true;

    public ColoredBlockItem(Block block, Item.Settings properties, Item clearItem)
    {
        super(block, properties);
        SplatcraftItems.inkColoredItems.add(this);
        InkwellBlock.inkCoatingRecipes.put(clearItem, this);
        this.clearItem = clearItem;

        if (clearItem == null)
            return;

        CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map().put(this, ((state, level, pos, player, hand, stack) ->
        {
            if (equals(clearItem) && !ColorUtils.getInkColor(stack).isValid())
                return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            ItemStack itemstack1 = new ItemStack(clearItem, 1);

            player.incrementStat(Stats.USE_CAULDRON);

            if (!player.isCreative())
            {
                stack.decrement(1);
                LeveledCauldronBlock.decrementFluidLevel(state, level, pos);
            }

            if (stack.isEmpty())
            {
                player.setStackInHand(hand, itemstack1);
            }
            else if (!player.getInventory().insertStack(itemstack1))
            {
                player.dropItem(itemstack1, false);
            }

            return ItemActionResult.SUCCESS;
        }));
    }

    public ColoredBlockItem(Block block, int stackSize, @Nullable Item clearItem)
    {
        this(block, new Settings().maxCount(stackSize), clearItem);
    }

    public ColoredBlockItem(Block block, int stackSize)
    {
        this(block, new Settings().maxCount(stackSize), null);
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
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type)
    {
        super.appendTooltip(stack, context, tooltip, type);

        if (I18n.hasTranslation(getTranslationKey() + ".tooltip"))
            tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.GRAY));

        boolean inverted = ColorUtils.isInverted(stack);
        if (ColorUtils.isColorLocked(stack))
        {
            tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));
            if (inverted)
                tooltip.add(Text.translatable("item.splatcraft.tooltip.inverted").fillStyle(Style.EMPTY.withItalic(true).withColor(Formatting.DARK_PURPLE)));
        }
        else if (matchColor)
            tooltip.add(Text.translatable("item.splatcraft.tooltip.matches_color" + (inverted ? ".inverted" : "")).formatted(Formatting.GRAY));
    }

    public ColoredBlockItem clearsToSelf()
    {
        clearItem = this;
        return this;
    }

    @Override
    protected boolean postPlacement(@NotNull BlockPos pos, World levelIn, @Nullable PlayerEntity player, @NotNull ItemStack stack, @NotNull BlockState state)
    {
        MinecraftServer server = levelIn.getServer();
        if (server == null)
            return false;

        InkColor color = ColorUtils.getInkColor(stack);

        if (color.isValid())
            ColorUtils.setInkColor(levelIn.getBlockEntity(pos), color);
        ColorUtils.setInverted(levelIn, pos, ColorUtils.isInverted(stack));

        return super.postPlacement(pos, levelIn, player, stack, state);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull World world, @NotNull Entity entityIn, int itemSlot, boolean isSelected)
    {
        super.inventoryTick(stack, world, entityIn, itemSlot, isSelected);

        if (matchColor && (!ColorUtils.getInkColor(stack).isValid() || !ColorUtils.isColorLocked(stack)))
        {
            ColorUtils.setInkColor(stack, entityIn instanceof PlayerEntity && EntityInfoCapability.hasCapability((LivingEntity) entityIn) ?
                ColorUtils.getEntityColor((PlayerEntity) entityIn) : ColorUtils.getDefaultColor());
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity)
    {
        BlockPos pos = entity.getBlockPos();

        if (entity.getWorld().getBlockState(pos.down()).getBlock() instanceof InkwellBlock)
        {
            if (ColorUtils.getInkColor(stack) != ColorUtils.getInkColorOrInverted(entity.getWorld(), pos.down()))
            {
                ColorUtils.setInkColor(entity.getStack(), ColorUtils.getInkColorOrInverted(entity.getWorld(), pos.down()));
                ColorUtils.setColorLocked(entity.getStack(), true);
            }
        }
        else if (!(equals(clearItem) && !ColorUtils.doesStackHaveColorData(stack)) &&
            clearItem != null && InkedBlock.causesClear(entity.getWorld(), pos, entity.getWorld().getBlockState(pos), Direction.UP))
        {
            entity.setStack(new ItemStack(clearItem, stack.getCount()));
        }

        return false;
    }
}