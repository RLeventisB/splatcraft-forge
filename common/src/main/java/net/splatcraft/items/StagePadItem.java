package net.splatcraft.items;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.blocks.InkedBlock;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.client.gui.stagepad.StageSelectionScreen;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.dummys.ISplatcraftForgeItemDummy;
import net.splatcraft.items.weapons.subs.SubWeaponItem;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StagePadItem extends Item implements IColoredItem, ISplatcraftForgeItemDummy
{
	public static final UseAction OPEN_MAIN_MENU = ((level, player, hand, stack, pos) ->
		((StagePadItem) stack.getItem()).openMenu(stack));
	public static UseAction clientUseAction = OPEN_MAIN_MENU;
	public StagePadItem()
	{
		super(new Item.Properties().stacksTo(1));
		SplatcraftItems.inkColoredItems.add(this);
	}
	public static void resetUseAction()
	{
		clientUseAction = OPEN_MAIN_MENU;
	}
	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(Level world, Player player, @NotNull InteractionHand hand)
	{
		ItemStack itemstack = player.getItemInHand(hand);
		player.awardStat(Stats.ITEM_USED.get(this));
		
		if (world.isClientSide())
			clientUseAction.apply(world, player, hand, itemstack, null);
		
		return InteractionResultHolder.sidedSuccess(itemstack, world.isClientSide());
	}
	@Override
	public @NotNull InteractionResult useOn(UseOnContext context)
	{
		if (context.getLevel().isClientSide())
			clientUseAction.apply(context.getLevel(), context.getPlayer(), context.getHand(), context.getItemInHand(), context.getClickedPos());
		
		return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
	}
	@OnlyIn(Dist.CLIENT)
	public void openMenu(ItemStack itemStack)
	{
		Minecraft.getInstance().setScreen(new StageSelectionScreen(itemStack.getHoverName()));
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);
		
		if (entity instanceof Player player)
		{
			if (!ColorUtils.isColorLocked(stack) && ColorUtils.getInkColor(stack) != ColorUtils.getEntityColor(player)
				&& EntityInfoCapability.hasCapability(player))
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
		else if ((!(stack.getItem() instanceof SubWeaponItem) || !SubWeaponItem.singleUse(stack))
			&& InkedBlock.causesClear(entity.level(), pos, entity.level().getBlockState(pos)) && ColorUtils.getInkColor(stack).getColor() != 0xFFFFFF)
		{
			ColorUtils.withInkColor(stack, InkColor.constructOrReuse(0xFFFFFF));
			ColorUtils.withColorLocked(stack, false);
		}
		
		return false;
	}
	public interface UseAction
	{
		void apply(Level world, Player player, InteractionHand hand, ItemStack stack, @Nullable BlockPos pos);
	}
}