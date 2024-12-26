package net.splatcraft.blocks;

import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.splatcraft.dummys.ISplatcraftForgeBlockDummy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmptyInkwellBlock extends TransparentBlock implements ISplatcraftForgeBlockDummy
{
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	private static final VoxelShape SHAPE = VoxelShapes.union(
		createCuboidShape(0, 0, 0, 16, 12, 16),
		createCuboidShape(1, 12, 1, 14, 13, 14),
		createCuboidShape(0, 13, 0, 16, 16, 16));
	public EmptyInkwellBlock(AbstractBlock.Settings properties)
	{
		super(properties.nonOpaque());
		setDefaultState(getStateManager().getDefaultState().with(WATERLOGGED, false));
	}
	@Override
	public VoxelShape getOutlineShape(@NotNull BlockState state, @NotNull BlockView levelIn, @NotNull BlockPos pos, @NotNull ShapeContext context)
	{
		return SHAPE;
	}
	@Override
	public boolean hasSidedTransparency(@NotNull BlockState state)
	{
		return true;
	}
	@Override
	public @NotNull PistonBehavior phGetPistonBehavior(@NotNull BlockState state)
	{
		return PistonBehavior.DESTROY;
	}
	@Nullable
	@Override
	public BlockState getPlacementState(ItemPlacementContext context)
	{
		return getDefaultState().with(WATERLOGGED, context.getWorld().getFluidState(context.getBlockPos()).getRegistryEntry() == Fluids.WATER);
	}
	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(WATERLOGGED);
	}
	@Override
	public @NotNull FluidState getFluidState(BlockState state)
	{
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}
	@Override
	public @NotNull BlockState getStateForNeighborUpdate(BlockState stateIn, @NotNull Direction facing, @NotNull BlockState facingState, @NotNull WorldAccess levelIn, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos)
	{
		if (stateIn.get(WATERLOGGED))
		{
			levelIn.scheduleFluidTick(currentPos, Fluids.WATER, Fluids.WATER.getTickRate(levelIn));
		}
		
		return super.getStateForNeighborUpdate(stateIn, facing, facingState, levelIn, currentPos, facingPos);
	}
}
