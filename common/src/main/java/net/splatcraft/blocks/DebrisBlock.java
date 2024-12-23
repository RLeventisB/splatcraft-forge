package net.splatcraft.blocks;

import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.splatcraft.dummys.ISplatcraftForgeBlockDummy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class DebrisBlock extends Block implements ISplatcraftForgeBlockDummy
{
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	public static final DirectionProperty DIRECTION = Properties.HORIZONTAL_FACING;
	private static final HashMap<Direction, VoxelShape> SHAPES = new HashMap<>()
	{{
		put(Direction.WEST, createCuboidShape(2.4, 0, 0, 15.2, 8, 16));
		put(Direction.NORTH, createCuboidShape(0, 0, 2.4, 16, 8, 15.2));
		put(Direction.EAST, createCuboidShape(0.8, 0, 0, 13.6, 8, 16));
		put(Direction.SOUTH, createCuboidShape(0, 0, 0.8, 16, 8, 13.6));
	}};
	public DebrisBlock(MapColor color)
	{
		super(AbstractBlock.Settings.create().mapColor(color).requiresTool().strength(5.0F, 6.0F).sounds(BlockSoundGroup.METAL).luminance(
			(state) -> 1
		));
		setDefaultState(getStateManager().getDefaultState().with(WATERLOGGED, false).with(DIRECTION, Direction.NORTH));
	}
	@Override
	public VoxelShape getOutlineShape(@NotNull BlockState state, @NotNull BlockView levelIn, @NotNull BlockPos pos, @NotNull ShapeContext context)
	{
		return SHAPES.get(state.get(DIRECTION));
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
		return getDefaultState().with(DIRECTION, context.getHorizontalPlayerFacing())
			.with(WATERLOGGED, context.getWorld().getFluidState(context.getBlockPos()).getRegistryEntry() == Fluids.WATER);
	}
	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(WATERLOGGED, DIRECTION);
	}
	@Override
	public @NotNull FluidState getFluidState(BlockState state)
	{
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}
	@Override
	public @NotNull BlockState getStateForNeighborUpdate(BlockState stateIn, @NotNull Direction facing, @NotNull BlockState facingState, @NotNull WorldAccess world, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos)
	{
		if (stateIn.get(WATERLOGGED))
		{
			world.scheduleFluidTick(currentPos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}
		
		return super.getStateForNeighborUpdate(stateIn, facing, facingState, world, currentPos, facingPos);
	}
}
