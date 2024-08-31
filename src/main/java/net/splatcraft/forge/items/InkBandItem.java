package net.splatcraft.forge.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.splatcraft.forge.registries.SplatcraftItemGroups;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InkBandItem extends Item
{
	public InkBandItem()
	{
		super(new Item.Properties().stacksTo(1));
		SplatcraftItemGroups.addGeneralItem(this);
	}
	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flags)
	{
		super.appendHoverText(stack, level, tooltip, flags);
		tooltip.add(Component.translatable(stack.getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
	}
}
