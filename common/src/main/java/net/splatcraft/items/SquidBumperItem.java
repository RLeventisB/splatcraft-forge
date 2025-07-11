package net.splatcraft.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.blocks.InkedBlock;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.dummys.ISplatcraftForgeItemDummy;
import net.splatcraft.entities.SquidBumperEntity;
import net.splatcraft.platform.Components;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftEntities;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SquidBumperItem extends Item implements IColoredItem, ISplatcraftForgeItemDummy
{
	public SquidBumperItem()
	{
		super(new Properties().stacksTo(16).component(SplatcraftComponents.ITEM_COLOR_DATA, SplatcraftComponents.ItemColorData.DEFAULT));
		SplatcraftItems.inkColoredItems.add(this);
	}
	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag type)
	{
		super.appendHoverText(stack, context, tooltip, type);
		
		if (ColorUtils.isColorLocked(stack))
			tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getEffectiveColor(stack), true));
		else
			tooltip.add(Component.translatable("item.splatcraft.tooltip.matches_color" + (ColorUtils.isInverted(stack) ? ".inverted" : "")).withStyle(ChatFormatting.GRAY));
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
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, level, entity, itemSlot, isSelected);
		
		if (entity instanceof LivingEntity livingEntity &&
			!ColorUtils.isColorLocked(stack) &&
			ColorUtils.getInkColor(stack) != ColorUtils.getEntityColor(livingEntity) &&
			Components.ENTITY_INFO.has(livingEntity))
		{
			ColorUtils.withInkColor(stack, ColorUtils.getEntityColor(livingEntity));
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
		else if (InkedBlock.causesClear(entity.level(), pos, entity.level().getBlockState(pos)) && ColorUtils.isColorLocked(stack))
		{
			ColorUtils.withInkColor(stack, InkColor.constructOrReuse(0xFFFFFF));
			ColorUtils.withColorLocked(stack, false);
		}
		
		return false;
	}
	@Override
	public @NotNull InteractionResult useOn(UseOnContext context)
	{
		if (context.getClickedFace() == Direction.DOWN)
			return InteractionResult.FAIL;
		
		Level world = context.getLevel();
		BlockPos pos = new BlockPlaceContext(context).getClickedPos();
		ItemStack stack = context.getItemInHand();
		
		Vec3 vector3d = Vec3.atBottomCenterOf(pos);
		AABB axisalignedbb = SplatcraftEntities.SQUID_BUMPER.get().getDimensions().makeBoundingBox(vector3d);
		if (world.noCollision(null, axisalignedbb) && world.getEntities(null, axisalignedbb).isEmpty())
		{
			if (world instanceof ServerLevel serverLevel)
			{
				SquidBumperEntity bumper = SplatcraftEntities.SQUID_BUMPER.get().create(serverLevel, null, pos, MobSpawnType.SPAWN_EGG, true, true);
				if (bumper != null)
				{
					bumper.setColor(ColorUtils.getEffectiveColor(stack));
					float f = (float) Mth.floor((Mth.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
					bumper.moveTo(bumper.getX(), bumper.getY(), bumper.getZ(), f, 0);
					bumper.setYHeadRot(f);
					bumper.yHeadRotO = f;
					world.addFreshEntity(bumper);
					world.playSound(null, bumper.getX(), bumper.getY(), bumper.getZ(), SplatcraftSounds.squidBumperPlace, SoundSource.BLOCKS, 0.75F, 0.8F);
				}
			}
			stack.shrink(1);
			return InteractionResult.sidedSuccess(world.isClientSide());
		}
		
		return InteractionResult.FAIL;
	}
}