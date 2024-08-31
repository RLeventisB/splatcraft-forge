package net.splatcraft.forge.items;

import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.splatcraft.forge.registries.SplatcraftItemGroups;
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
		super(new Properties().stacksTo(1).rarity(rarity));
		SplatcraftItemGroups.addGeneralItem(this);
		this.isGlowing = isGlowing;
		this.isOmni = isOmni;
		
		filters.add(this);
	}
	public FilterItem()
	{
		this(Rarity.COMMON, false, false);
	}
	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag isAdvanced)
	{
		super.appendHoverText(stack, level, tooltip, isAdvanced);
		tooltip.add(Component.translatable("item.splatcraft.filter.tooltip").withStyle(ChatFormatting.GRAY));
	}
	@Override
	public boolean isFoil(@NotNull ItemStack stack)
	{
		return isGlowing;
	}
	public boolean isOmni()
	{
		return isOmni;
	}
}
