package net.splatcraft.items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.dummys.ISplatcraftForgeItemDummy;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.ColorUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ColoredArmorItem extends ArmorItem implements IColoredItem, ISplatcraftForgeItemDummy
{
	public ColoredArmorItem(Holder<ArmorMaterial> material, ArmorItem.Type armorType, Item.Properties settings)
	{
		super(material, armorType, settings);
		SplatcraftItems.inkColoredItems.add(this);
		
		CauldronInteraction.bootStrap();
		CauldronInteraction.WATER.map().put(this, CauldronInteraction.DYED_ITEM);
	}
	public ColoredArmorItem(Holder<ArmorMaterial> material, ArmorItem.Type armorType)
	{
		this(material, armorType, new Item.Properties().stacksTo(1).component(DataComponents.DYED_COLOR, new DyedItemColor(0, false)));
	}
	@Override
	public void appendHoverText(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
	{
		super.appendHoverText(stack, context, tooltip, flag);
		
		if (I18n.exists(getDescriptionId() + ".tooltip"))
			tooltip.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
		
		if (ColorUtils.isColorLocked(stack))
			tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));
		else
			tooltip.add(Component.translatable("item.splatcraft.tooltip.matches_color").withStyle(ChatFormatting.GRAY));
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);
		
		if (entity instanceof LivingEntity player && !ColorUtils.isColorLocked(stack) && ColorUtils.getInkColor(stack) != ColorUtils.getEntityColor(player)
			&& EntityInfoCapability.hasCapability(player))
		{
			ColorUtils.withInkColor(stack, ColorUtils.getEntityColor(player));
		}
	}
	@Override
	public boolean phOnEntityItemUpdate(ItemStack stack, ItemEntity entity)
	{
		BlockPos pos = entity.blockPosition().below();
		
		if (entity.level().getBlockState(pos).getBlock() instanceof InkwellBlock)
		{
			if (ColorUtils.getInkColor(stack) != ColorUtils.getEffectiveColor(entity.level(), pos))
			{
				ColorUtils.withInkColor(entity.getItem(), ColorUtils.getEffectiveColor(entity.level(), pos));
				ColorUtils.withColorLocked(entity.getItem(), true);
			}
		}
		
		return false;
	}
}