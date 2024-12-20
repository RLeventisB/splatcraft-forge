package net.splatcraft.items;

import com.google.common.collect.Lists;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FilterItem extends Item
{
    public static final ArrayList<FilterItem> filters = Lists.newArrayList();
    protected final boolean isGlowing;
    protected final boolean isOmni;

    public FilterItem(Rarity rarity, boolean isGlowing, boolean isOmni)
    {
        super(new Settings().maxCount(1).rarity(rarity));
        this.isGlowing = isGlowing;
        this.isOmni = isOmni;

        filters.add(this);
    }

    public FilterItem()
    {
        this(Rarity.COMMON, false, false);
    }

    @Override
    public void appendTooltip(@NotNull ItemStack stack, @Nullable TooltipContext context, @NotNull List<Text> tooltip, @NotNull TooltipType isAdvanced)
    {
        super.appendTooltip(stack, context, tooltip, isAdvanced);
        tooltip.add(Text.translatable("item.splatcraft.filter.tooltip").formatted(Formatting.GRAY));
    }

    @Override
    public boolean hasGlint(@NotNull ItemStack stack)
    {
        return isGlowing;
    }

    public boolean isOmni()
    {
        return isOmni;
    }
}