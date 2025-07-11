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
import net.minecraft.world.ItemInteractionResult;
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
import net.splatcraft.dummys.ISplatcraftForgeItemDummy;
import net.splatcraft.platform.Components;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ColoredBlockItem extends BlockItem implements IColoredItem, ISplatcraftForgeItemDummy
{
	private Item clearItem;
	private boolean matchColor = true;
	public ColoredBlockItem(Block block, Item.Properties properties, Item clearItem)
	{
		super(block, properties.component(SplatcraftComponents.ITEM_COLOR_DATA, SplatcraftComponents.ItemColorData.DEFAULT));
		SplatcraftItems.inkColoredItems.add(this);
		InkwellBlock.inkCoatingRecipes.put(clearItem, this);
		this.clearItem = clearItem;
		
		if (clearItem == null)
			return;
		
		CauldronInteraction.WATER.map().put(this, ((state, level, pos, player, hand, stack) ->
		{
			if (equals(clearItem) && !ColorUtils.getInkColor(stack).isValid())
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			
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
			
			return ItemInteractionResult.SUCCESS;
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
	public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag type)
	{
		super.appendHoverText(stack, context, tooltip, type);
		
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
		
		InkColor color = ColorUtils.getEffectiveColor(stack, player);
		
		if (color.isValid())
			ColorUtils.withInkColor(levelIn.getBlockEntity(pos), color);
		
		return super.updateCustomBlockEntityTag(pos, levelIn, player, stack, state);
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, level, entity, itemSlot, isSelected);
		
		if (matchColor && (ColorUtils.getInkColor(stack).isInvalid() || !ColorUtils.isColorLocked(stack)))
		{
			ColorUtils.withInkColor(stack, entity instanceof LivingEntity living && Components.ENTITY_INFO.has(living) ?
				ColorUtils.getEntityColor(entity) : ColorUtils.getDefaultColor());
		}
	}
	@Override
	public boolean phOnEntityItemUpdate(ItemStack stack, ItemEntity entity)
	{
		BlockPos pos = entity.blockPosition();
		
		if (entity.level().getBlockState(pos.below()).getBlock() instanceof InkwellBlock)
		{
			if (ColorUtils.getInkColor(stack) != ColorUtils.getEffectiveColor(entity.level(), pos.below()))
			{
				ColorUtils.withInkColor(entity.getItem(), ColorUtils.getEffectiveColor(entity.level(), pos.below()));
				ColorUtils.withColorLocked(entity.getItem(), true);
			}
		}
		else if (!(equals(clearItem) && !ColorUtils.doesStackHaveColorData(stack)) &&
			clearItem != null && InkedBlock.causesClear(entity.level(), pos, entity.level().getBlockState(pos), Direction.UP))
		{
			entity.setItem(new ItemStack(clearItem, stack.getCount()));
		}
		
		return false;
	}
}