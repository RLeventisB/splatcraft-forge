package net.splatcraft.blocks;

import net.minecraft.block.*;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GrateRampBlock extends Block implements Waterloggable
{
    public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    private static final VoxelShape START = createCuboidShape(0, 0, 0, 3, 3, 16);
    private static final VoxelShape END = createCuboidShape(13, 13, 0, 16, 16, 16);
    private static final VoxelShape SEGMENT = createCuboidShape(1, 2, 0, 4, 5, 16);
    public static final VoxelShape[] SHAPES = makeVoxelShape();

    public GrateRampBlock()
    {
        super(GrateBlock.PROPERTIES);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(WATERLOGGED, false));
    }

    private static VoxelShape[] makeVoxelShape()
    {
        VoxelShape[] shapes = new VoxelShape[8];

        for (int i = 0; i < 6; i++)
        {
            shapes[i] = SEGMENT.offset(.125 * i, .125 * i, 0);
        }

        shapes[6] = START;
        shapes[7] = END;

        return createVoxelShapes(shapes);
    }

    protected static VoxelShape modifyShapeForDirection(Direction facing, VoxelShape shape)
    {
        Box bb = shape.getBoundingBox();

        return switch (facing)
        {
            case SOUTH -> VoxelShapes.cuboid(new Box(1 - bb.maxZ, bb.minY, bb.minX, 1 - bb.minZ, bb.maxY, bb.maxX));
            case EAST ->
                VoxelShapes.cuboid(new Box(1 - bb.maxX, bb.minY, 1 - bb.maxZ, 1 - bb.minX, bb.maxY, 1 - bb.minZ));
            case WEST -> VoxelShapes.cuboid(new Box(bb.minZ, bb.minY, 1 - bb.maxX, bb.maxZ, bb.maxY, 1 - bb.minX));
            default -> shape;
        };
    }

    protected static VoxelShape[] createVoxelShapes(VoxelShape... shapes)
    {
        VoxelShape[] result = new VoxelShape[4];

        for (int i = 0; i < 4; i++)
        {
            result[i] = VoxelShapes.empty();
            for (VoxelShape shape : shapes)
            {
                result[i] = VoxelShapes.union(result[i], modifyShapeForDirection(Direction.fromHorizontal(i), shape));
            }
        }

        return result;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, @NotNull BlockView levelIn, @NotNull BlockPos pos, @NotNull ShapeContext context)
    {
        return SHAPES[state.get(FACING).ordinal() - 2];
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context)
    {
        BlockPos blockPos = context.getBlockPos();
        Direction direction = context.getSide();
        FluidState fluidstate = context.getWorld().getFluidState(blockPos);
        boolean flip = direction != Direction.DOWN && (direction == Direction.UP || !(context.getHitPos().y - (double) blockPos.getY() <= 0.5D));
        return getDefaultState().with(FACING, flip ? context.getHorizontalPlayerFacing().getOpposite() : context.getHorizontalPlayerFacing()).with(WATERLOGGED, fluidstate.getRegistryEntry() == Fluids.WATER);
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state)
    {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPathfindThrough(@NotNull BlockState state, @NotNull NavigationType type)
    {
        return false;
    }
}
