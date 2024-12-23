package net.splatcraft.items;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.splatcraft.blocks.InkedBlock;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.client.gui.stagepad.StageSelectionScreen;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.SubWeaponItem;
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
		super(new Item.Settings().maxCount(1));
		SplatcraftItems.inkColoredItems.add(this);
	}
	public static void resetUseAction()
	{
		clientUseAction = OPEN_MAIN_MENU;
	}
	@Override
	public @NotNull TypedActionResult<ItemStack> use(World world, PlayerEntity player, @NotNull Hand hand)
	{
		ItemStack itemstack = player.getStackInHand(hand);
		player.incrementStat(Stats.USED.getOrCreateStat(this));
		
		if (world.isClient())
			clientUseAction.apply(world, player, hand, itemstack, null);
		
		return TypedActionResult.success(itemstack, world.isClient());
	}
	@Override
	public @NotNull ActionResult useOnBlock(ItemUsageContext context)
	{
		if (context.getWorld().isClient())
			clientUseAction.apply(context.getWorld(), context.getPlayer(), context.getHand(), context.getStack(), context.getBlockPos());
		
		return ActionResult.success(context.getWorld().isClient());
	}
	@Environment(EnvType.CLIENT)
	public void openMenu(ItemStack itemStack)
	{
		MinecraftClient.getInstance().setScreen(new StageSelectionScreen(itemStack.getName()));
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull World world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);
		
		if (entity instanceof PlayerEntity player)
		{
			if (!ColorUtils.isColorLocked(stack) && ColorUtils.getInkColor(stack) != ColorUtils.getEntityColor(player)
				&& EntityInfoCapability.hasCapability(player))
				ColorUtils.withInkColor(stack, ColorUtils.getEntityColor(player));
		}
	}
	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity)
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
		else if ((!(stack.getItem() instanceof SubWeaponItem) || !SubWeaponItem.singleUse(stack))
			&& InkedBlock.causesClear(entity.getWorld(), pos, entity.getWorld().getBlockState(pos)) && ColorUtils.getInkColor(stack).getColor() != 0xFFFFFF)
		{
			ColorUtils.withInkColor(stack, InkColor.constructOrReuse(0xFFFFFF));
			ColorUtils.withColorLocked(stack, false);
		}
		
		return false;
	}
	public interface UseAction
	{
		void apply(World world, PlayerEntity player, Hand hand, ItemStack stack, @Nullable BlockPos pos);
	}
}