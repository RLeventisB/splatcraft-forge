package net.splatcraft.items;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class InkBandItem extends Item
{
    public InkBandItem()
    {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag type)
    {
        super.appendHoverText(stack, context, tooltip, type);
        tooltip.add(Component.translatable(stack.getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}