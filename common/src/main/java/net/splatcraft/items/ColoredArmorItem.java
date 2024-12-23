package net.splatcraft.items;

import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.dummys.ISplatcraftForgeItemDummy;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.ColorUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ColoredArmorItem extends ArmorItem implements IColoredItem, ISplatcraftForgeItemDummy
{
	public ColoredArmorItem(RegistryEntry<ArmorMaterial> material, ArmorItem.Type armorType, Item.Settings settings)
	{
		super(material, armorType, settings);
		SplatcraftItems.inkColoredItems.add(this);
		
		CauldronBehavior.registerBehavior();
		CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map().put(this, CauldronBehavior.CLEAN_DYEABLE_ITEM);
	}
	public ColoredArmorItem(RegistryEntry<ArmorMaterial> material, ArmorItem.Type armorType)
	{
		this(material, armorType, new Item.Settings().maxCount(1).component(DataComponentTypes.DYED_COLOR, new DyedColorComponent(0, false)));
	}
	@Override
	public void appendTooltip(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Text> tooltip, @NotNull TooltipType flag)
	{
		super.appendTooltip(stack, context, tooltip, flag);
		
		if (I18n.hasTranslation(getTranslationKey() + ".tooltip"))
			tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.GRAY));
		
		if (ColorUtils.isColorLocked(stack))
			tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));
		else
			tooltip.add(Text.translatable("item.splatcraft.tooltip.matches_color").formatted(Formatting.GRAY));
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull World world, @NotNull Entity entity, int itemSlot, boolean isSelected)
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
		BlockPos pos = entity.getBlockPos().down();
		
		if (entity.getWorld().getBlockState(pos).getBlock() instanceof InkwellBlock)
		{
			if (ColorUtils.getInkColor(stack) != ColorUtils.getInkColorOrInverted(entity.getWorld(), pos))
			{
				ColorUtils.withInkColor(entity.getStack(), ColorUtils.getInkColorOrInverted(entity.getWorld(), pos));
				ColorUtils.withColorLocked(entity.getStack(), true);
			}
		}
		
		return false;
	}
}