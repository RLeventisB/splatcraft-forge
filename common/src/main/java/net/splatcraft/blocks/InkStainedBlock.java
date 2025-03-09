package net.splatcraft.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkStainedBlock extends Block implements IColoredBlock, EntityBlock
{
	public static final BooleanProperty COLORED = BooleanProperty.create("colored");
	public InkStainedBlock(Properties properties)
	{
		super(properties);
		SplatcraftBlocks.inkColoredBlocks.add(this);
	}
	@Override
	public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader world, @NotNull BlockPos pos, @NotNull BlockState state)
	{
		InkColor color = getColor(world, pos);
		if (color.isValid())
			return ColorUtils.withInkColor(super.getCloneItemStack(world, pos, state), color);
		return ColorUtils.withColorLocked(ColorUtils.withInkColor(super.getCloneItemStack(world, pos, state), color), true);
	}
	@Override
	public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity entity, ItemStack stack)
	{
		if (stack.has(SplatcraftComponents.ITEM_COLOR_DATA) && world.getBlockEntity(pos) instanceof InkColorTileEntity)
		{
			ColorUtils.withInkColor(world.getBlockEntity(pos), ColorUtils.getEffectiveColor(stack));
		}
		super.setPlacedBy(world, pos, state, entity, stack);
	}
	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
	{
		return SplatcraftTileEntities.colorTileEntity.get().create(pos, state);
	}
	@Override
	public boolean canClimb()
	{
		return false;
	}
	@Override
	public boolean canSwim()
	{
		return false;
	}
	@Override
	public boolean canDamage()
	{
		return false;
	}
	@Nullable
	@Override
	public BlockState getStateForPlacement(@NotNull BlockPlaceContext context)
	{
		return super.getStateForPlacement(context);
	}
	@Override
	public boolean setColor(Level world, BlockPos pos, InkColor color)
	{
		return IColoredBlock.super.setColor(world, pos, color);
	}
	@Override
	public InkColor getColor(LevelReader world, BlockPos pos)
	{
		if (world.getBlockEntity(pos) instanceof InkColorTileEntity blockEntity)
		{
			return blockEntity.getInkColor();
		}
		return InkColor.INVALID;
	}
	@Override
	public boolean remoteColorChange(Level world, BlockPos pos, InkColor newColor)
	{
		BlockState state = world.getBlockState(pos);
		
		if (world.getBlockEntity(pos) instanceof InkColorTileEntity blockEntity && blockEntity.getInkColor() != newColor)
		{
			blockEntity.setColor(newColor);
			world.sendBlockUpdated(pos, state, state, 2);
			return true;
		}
		return false;
	}
	@Override
	public boolean remoteInkClear(Level world, BlockPos pos)
	{
		return false;
	}
	public static class WithUninkedVariant extends InkStainedBlock
	{
		public WithUninkedVariant(Properties properties)
		{
			super(properties);
			
			registerDefaultState(defaultBlockState().setValue(COLORED, false));
		}
		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
		{
			builder.add(COLORED);
		}
		@Override
		public boolean remoteColorChange(Level world, BlockPos pos, InkColor newColor)
		{
			if (!world.getBlockState(pos).getValue(COLORED))
				return false;
			
			return super.remoteColorChange(world, pos, newColor);
		}
		@Override
		public boolean setColor(Level world, BlockPos pos, InkColor color)
		{
			world.setBlockAndUpdate(pos, world.getBlockState(pos).setValue(COLORED, color.isValid()));
			return super.setColor(world, pos, color);
		}
		@Override
		public InkColor getColor(LevelReader world, BlockPos pos)
		{
			if (world.getBlockState(pos).getValue(COLORED))
				return super.getColor(world, pos);
			else return InkColor.INVALID;
		}
		@Override
		public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context)
		{
			return super.getStateForPlacement(context).setValue(COLORED, ColorUtils.getInkColor(context.getItemInHand()).isValid());
		}
	}
}
