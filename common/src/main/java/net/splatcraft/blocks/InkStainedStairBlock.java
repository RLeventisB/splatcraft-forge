package net.splatcraft.blocks;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.splatcraft.blocks.InkStainedBlock.COLORED;

public class InkStainedStairBlock extends StairsBlock implements IColoredBlock, BlockEntityProvider
{
	public InkStainedStairBlock(BlockState parent, AbstractBlock.Settings properties)
	{
		super(parent, properties);
		SplatcraftBlocks.inkColoredBlocks.add(this);
	}
	@Override
	public @NotNull ItemStack getPickStack(@NotNull WorldView level, @NotNull BlockPos pos, @NotNull BlockState state)
	{
		InkColor color = getColor(level, pos);
		if (color.isInvalid())
			return ColorUtils.withInkColor(super.getPickStack(level, pos, state), color);
		return ColorUtils.withColorLocked(ColorUtils.withInkColor(super.getPickStack(level, pos, state), color), true);
	}
	@Override
	public void onPlaced(@NotNull World world, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity entity, ItemStack stack)
	{
		if (ColorUtils.doesStackHaveColorData(stack) && world.getBlockEntity(pos) instanceof InkColorTileEntity)
		{
			ColorUtils.withInkColor(world.getBlockEntity(pos), ColorUtils.getInkColor(stack));
		}
		super.onPlaced(world, pos, state, entity, stack);
	}
	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return SplatcraftTileEntities.colorTileEntity.get().instantiate(pos, state);
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
	public BlockState getPlacementState(ItemPlacementContext ctx)
	{
		return super.getPlacementState(ctx);
	}
	@Override
	public boolean setColor(World world, BlockPos pos, InkColor color)
	{
		return IColoredBlock.super.setColor(world, pos, color);
	}
	@Override
	public InkColor getColor(WorldView world, BlockPos pos)
	{
		if (world.getBlockEntity(pos) instanceof InkColorTileEntity blockEntity)
		{
			return blockEntity.getInkColor();
		}
		return InkColor.INVALID;
	}
	@Override
	public boolean remoteColorChange(World world, BlockPos pos, InkColor newColor)
	{
		BlockState state = world.getBlockState(pos);
		
		if (world.getBlockEntity(pos) instanceof InkColorTileEntity blockEntity && blockEntity.getInkColor() != newColor)
		{
			blockEntity.setColor(newColor);
			world.updateListeners(pos, state, state, 2);
			return true;
		}
		return false;
	}
	@Override
	public boolean remoteInkClear(World world, BlockPos pos)
	{
		return false;
	}
	public static class WithUninkedVariant extends InkStainedStairBlock
	{
		public WithUninkedVariant(BlockState parent, AbstractBlock.Settings settings)
		{
			super(parent, settings);
			
			setDefaultState(getDefaultState().with(COLORED, false));
		}
		@Override
		protected void appendProperties(StateManager.@NotNull Builder<Block, BlockState> builder)
		{
			super.appendProperties(builder);
			builder.add(COLORED);
		}
		@Override
		public InkColor getColor(WorldView world, BlockPos pos)
		{
			if (world.getBlockState(pos).get(COLORED))
				return super.getColor(world, pos);
			else return InkColor.INVALID;
		}
		@Override
		public boolean remoteColorChange(World world, BlockPos pos, InkColor newColor)
		{
			if (!world.getBlockState(pos).get(COLORED))
				return false;
			
			return super.remoteColorChange(world, pos, newColor);
		}
		@Override
		public boolean setColor(World world, BlockPos pos, InkColor color)
		{
			world.setBlockState(pos, world.getBlockState(pos).with(COLORED, color.isValid()));
			return super.setColor(world, pos, color);
		}
		@Override
		public @Nullable BlockState getPlacementState(@NotNull ItemPlacementContext context)
		{
			return super.getPlacementState(context).with(COLORED, ColorUtils.getInkColor(context.getStack()).isValid());
		}
	}
}
