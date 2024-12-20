package net.splatcraft.blocks;

import net.minecraft.block.*;
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
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class GrateBlock extends Block implements Waterloggable
{
    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public static final Settings PROPERTIES = Settings.create().mapColor(MapColor.IRON_GRAY).nonOpaque().requiresTool().strength(4.0f).sounds(BlockSoundGroup.METAL);
    protected static final HashMap<Direction, VoxelShape> AABBS = new HashMap<>()
    {{
        put(Direction.NORTH, createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D));
        put(Direction.SOUTH, createCuboidShape(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D));
        put(Direction.WEST, createCuboidShape(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D));
        put(Direction.EAST, createCuboidShape(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D));
        put(Direction.DOWN, createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D));
        put(Direction.UP, createCuboidShape(0.0D, 13.0D, 0.0D, 16.0D, 16.0D, 16.0D));
    }};

    public GrateBlock()
    {
        super(PROPERTIES);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.DOWN).with(WATERLOGGED, false));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, @NotNull BlockView levelIn, @NotNull BlockPos pos, @NotNull ShapeContext context)
    {
        return AABBS.get(state.get(FACING));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context)
    {
        BlockState blockstate = getDefaultState();
        FluidState fluidstate = context.getWorld().getFluidState(context.getBlockPos());
        Direction direction = context.getSide();

        if (context.getPlayer() != null && context.getPlayer().isSneaking())
            blockstate = blockstate.with(FACING, direction.getOpposite());
        else if (!context.canReplaceExisting() && direction.getAxis().isHorizontal())
            blockstate = blockstate.with(FACING, context.getHitPos().y - (double) context.getBlockPos().getY() > 0.5D ? Direction.UP : Direction.DOWN);
        else
            blockstate = blockstate.with(FACING, direction == Direction.UP ? Direction.DOWN : Direction.UP);

        return blockstate.with(WATERLOGGED, fluidstate.getRegistryEntry() == Fluids.WATER);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state)
    {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    /**
     * Update the provided state given the provided neighbor facing and neighbor state, returning a new state.
     * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately
     * returns its solidified counterpart.
     * Note that this method should ideally consider only the specific face passed in.
     */
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
