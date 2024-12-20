package net.splatcraft.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InkBandItem extends Item
{
    public InkBandItem()
    {
        super(new Item.Settings().maxCount(1));
    }

    @Override
    public void appendTooltip(@NotNull ItemStack stack, @Nullable TooltipContext context, @NotNull List<Text> tooltip, @NotNull TooltipType type)
    {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable(stack.getTranslationKey() + ".tooltip").formatted(Formatting.GRAY));
    }
}