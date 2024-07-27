package net.splatcraft.forge.items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.forge.blocks.InkwellBlock;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.registries.SplatcraftItemGroups;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.util.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ColoredArmorItem extends DyeableArmorItem implements IColoredItem
{
	public ColoredArmorItem(ArmorMaterial material, ArmorItem.Type type, Properties properties)
	{
		super(material, type, properties);
		SplatcraftItems.inkColoredItems.add(this);
		SplatcraftItemGroups.addWeaponItem(this);
		
		CauldronInteraction.WATER.put(this, CauldronInteraction.DYED_ITEM);
	}
	public ColoredArmorItem(ArmorMaterial material, ArmorItem.Type type)
	{
		this(material, type, new Properties().stacksTo(1));
	}
	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
	{
		super.appendHoverText(stack, level, tooltip, flag);
		
		if (I18n.exists(getDescriptionId() + ".tooltip"))
			tooltip.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
		
		if (ColorUtils.isColorLocked(stack))
			tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));
		else
			tooltip.add(Component.translatable("item.splatcraft.tooltip.matches_color").withStyle(ChatFormatting.GRAY));
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, level, entity, itemSlot, isSelected);
		
		if (entity instanceof Player player && !ColorUtils.isColorLocked(stack) && ColorUtils.getInkColor(stack) != ColorUtils.getPlayerColor(player)
			&& PlayerInfoCapability.hasCapability(player))
		{
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
		
		return false;
	}
	@Override
	public @NotNull InteractionResult useOn(UseOnContext context)
	{
		BlockState state = context.getLevel().getBlockState(context.getClickedPos());
		
		return super.useOn(context);
	}
	@Override
	public boolean hasCustomColor(@NotNull ItemStack stack)
	{
		return true;
	}
	@Override
	public int getColor(@NotNull ItemStack stack)
	{
		return ColorUtils.getInkColor(stack);
	}
	@Override
	public void setColor(@NotNull ItemStack stack, int color)
	{
		ColorUtils.setInkColor(stack, color);
	}
	@Override
	public void clearColor(@NotNull ItemStack stack)
	{
		ColorUtils.setInkColor(stack, -1);
		ColorUtils.setColorLocked(stack, false);
	}
}
