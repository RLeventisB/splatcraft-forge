package net.splatcraft.forge.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;

public class GrateBlock extends Block implements SimpleWaterloggedBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final Properties PROPERTIES = Properties.of(Material.METAL).noOcclusion().requiresCorrectToolForDrops().strength(4.0f).sound(SoundType.METAL);

    protected static final HashMap<Direction, VoxelShape> AABBS = new HashMap<>()
    {{
        put(Direction.NORTH, Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D));
        put(Direction.SOUTH, Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D));
        put(Direction.WEST, Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D));
        put(Direction.EAST, Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D));
        put(Direction.DOWN, Block.box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D));
       put(Direction.UP, Block.box(0.0D, 13.0D, 0.0D, 16.0D, 16.0D, 16.0D));
    }};

    public GrateBlock()
    {
        super(PROPERTIES);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.DOWN).setValue(WATERLOGGED, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter levelIn, BlockPos pos, CollisionContext context)
    {
        return AABBS.get(state.getValue(FACING));
    }

    public boolean isPathfindable(BlockState state, BlockGetter levelIn, BlockPos pos, PathComputationType type) {
        return type == PathComputationType.WATER && levelIn.getFluidState(pos).is(FluidTags.WATER);
    }


    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        BlockState blockstate = this.defaultBlockState();
        FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
        Direction direction = context.getClickedFace();

        if(context.getPlayer() != null && context.getPlayer().isShiftKeyDown())
            blockstate = blockstate.setValue(FACING, direction.getOpposite());
        else if (!context.replacingClickedOnBlock() && direction.getAxis().isHorizontal())
            blockstate = blockstate.setValue(FACING, context.getClickLocation().y - (double) context.getClickedPos().getY() > 0.5D ? Direction.UP : Direction.DOWN);
        else
            blockstate = blockstate.setValue(FACING, direction == Direction.UP ? Direction.DOWN : Direction.UP);

        return blockstate.setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState state)
    {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /**
     * Update the provided state given the provided neighbor facing and neighbor state, returning a new state.
     * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately
     * returns its solidified counterpart.
     * Note that this method should ideally consider only the specific face passed in.
     */
    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor levelIn, BlockPos currentPos, BlockPos facingPos)
    {
        if (stateIn.getValue(WATERLOGGED))
        {
            levelIn.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(levelIn));
        }

        return super.updateShape(stateIn, facing, facingState, levelIn, currentPos, facingPos);
    }

}
