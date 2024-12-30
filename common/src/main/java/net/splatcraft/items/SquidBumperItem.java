package net.splatcraft.items;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.splatcraft.blocks.InkedBlock;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.dummys.ISplatcraftForgeItemDummy;
import net.splatcraft.entities.SquidBumperEntity;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftEntities;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SquidBumperItem extends Item implements IColoredItem, ISplatcraftForgeItemDummy
{
	public SquidBumperItem()
	{
		super(new Settings().maxCount(16).component(SplatcraftComponents.ITEM_COLOR_DATA, SplatcraftComponents.ItemColorData.DEFAULT.get()));
		SplatcraftItems.inkColoredItems.add(this);
	}
	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type)
	{
		super.appendTooltip(stack, context, tooltip, type);
		
		if (ColorUtils.isColorLocked(stack))
			tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getEffectiveColor(stack), true));
		else
			tooltip.add(Text.translatable("item.splatcraft.tooltip.matches_color" + (ColorUtils.isInverted(stack) ? ".inverted" : "")).formatted(Formatting.GRAY));
	}
	//	@Override
//	public void fillItemCategory(@NotNull ItemGroup group, @NotNull DefaultedList<ItemStack> items)
//	{
//		if (allowdedIn(group))
//		{
//			items.add(ColorUtils.setColorLocked(new ItemStack(this), false));
//			items.add(ColorUtils.setInverted(ColorUtils.setColorLocked(new ItemStack(this), false), true));
//		}
//	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull World world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);
		
		if (entity instanceof LivingEntity livingEntity && !ColorUtils.isColorLocked(stack) && ColorUtils.getInkColor(stack) != ColorUtils.getEntityColor(livingEntity)
			&& EntityInfoCapability.hasCapability(livingEntity))
		{
			ColorUtils.withInkColor(stack, ColorUtils.getEntityColor(livingEntity));
		}
	}
	@Override
	public boolean phOnEntityItemUpdate(ItemStack stack, ItemEntity entity)
	{
		BlockPos pos = entity.getBlockPos().down();
		
		if (entity.getWorld().getBlockState(pos).getBlock() instanceof InkwellBlock)
		{
			if (ColorUtils.getInkColor(stack) != ColorUtils.getEffectiveColor(entity.getWorld(), pos))
			{
				ColorUtils.withInkColor(entity.getStack(), ColorUtils.getEffectiveColor(entity.getWorld(), pos));
				ColorUtils.withColorLocked(entity.getStack(), true);
			}
		}
		else if (InkedBlock.causesClear(entity.getWorld(), pos, entity.getWorld().getBlockState(pos)) && ColorUtils.isColorLocked(stack))
		{
			ColorUtils.withInkColor(stack, InkColor.constructOrReuse(0xFFFFFF));
			ColorUtils.withColorLocked(stack, false);
		}
		
		return false;
	}
	@Override
	public @NotNull ActionResult useOnBlock(ItemUsageContext context)
	{
		if (context.getSide() == Direction.DOWN)
			return ActionResult.FAIL;
		
		World world = context.getWorld();
		BlockPos pos = new ItemPlacementContext(context).getBlockPos();
		ItemStack stack = context.getStack();
		
		Vec3d vector3d = Vec3d.ofBottomCenter(pos);
		Box axisalignedbb = SplatcraftEntities.SQUID_BUMPER.get().getDimensions().getBoxAt(vector3d);
		if (world.isSpaceEmpty(null, axisalignedbb) && world.getOtherEntities(null, axisalignedbb).isEmpty())
		{
			if (world instanceof ServerWorld serverLevel)
			{
				SquidBumperEntity bumper = SplatcraftEntities.SQUID_BUMPER.get().create(serverLevel, null, pos, SpawnReason.SPAWN_EGG, true, true);
				if (bumper != null)
				{
					bumper.setColor(ColorUtils.getEffectiveColor(stack));
					float f = (float) MathHelper.floor((MathHelper.wrapDegrees(context.getPlayerYaw() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
					bumper.refreshPositionAndAngles(bumper.getX(), bumper.getY(), bumper.getZ(), f, 0);
					bumper.setHeadYaw(f);
					bumper.prevHeadYaw = f;
					world.spawnEntity(bumper);
					world.playSound(null, bumper.getX(), bumper.getY(), bumper.getZ(), SplatcraftSounds.squidBumperPlace, SoundCategory.BLOCKS, 0.75F, 0.8F);
				}
			}
			stack.decrement(1);
			return ActionResult.success(world.isClient());
		}
		
		return ActionResult.FAIL;
	}
}