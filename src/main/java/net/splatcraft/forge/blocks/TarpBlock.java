package net.splatcraft.forge.blocks;

import com.google.common.collect.ImmutableMap;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import net.splatcraft.forge.registries.SplatcraftBlocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class TarpBlock extends Block implements SimpleWaterloggedBlock
{
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final Map<Direction, BooleanProperty> FACING_TO_PROPERTY_MAP = PipeBlock.PROPERTY_BY_DIRECTION.entrySet().stream().collect(Util.toMap());
    private static final VoxelShape UP_AABB = Block.box(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape DOWN_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
    private static final VoxelShape EAST_AABB = Block.box(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D);
    private static final VoxelShape WEST_AABB = Block.box(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
    private static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);
    private final Map<BlockState, VoxelShape> stateToShapeMap;

    public TarpBlock()
    {
        this(Properties.of().mapColor(MapColor.WOOL).ignitedByLava().isRedstoneConductor(SplatcraftBlocks::noRedstoneConduct));
    }

    public TarpBlock(Properties properties)
    {
        super(properties);
        this.registerDefaultState(defaultBlockState().setValue(WATERLOGGED, Boolean.FALSE).setValue(DOWN, Boolean.TRUE).setValue(UP, Boolean.FALSE).setValue(NORTH, Boolean.FALSE).setValue(EAST, Boolean.FALSE).setValue(SOUTH, Boolean.FALSE).setValue(WEST, Boolean.FALSE));
        this.stateToShapeMap = ImmutableMap.copyOf(this.getStateDefinition().getPossibleStates().stream().collect(Collectors.toMap(Function.identity(), TarpBlock::getShapeForState)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(UP, DOWN, NORTH, SOUTH, WEST, EAST, WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context)
    {
        BlockState superState = super.getStateForPlacement(context);
        Level level = context.getLevel();

        BlockState state;
        if (level.getBlockState(context.getClickedPos()).is(this))
        {
            state = level.getBlockState(context.getClickedPos());
        }
        else
        {
            if (superState == null) {
                return null;
            }
            state = superState.setValue(DOWN, false).setValue(WATERLOGGED, level.getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
        }

        state = state.setValue(FACING_TO_PROPERTY_MAP.get(context.getClickedFace().getOpposite()), true);

        for (Direction direction : Direction.values())
            if (state.getValue(FACING_TO_PROPERTY_MAP.get(direction)))
                return state;

        return state.setValue(DOWN, true);
    }

    @Override
    public boolean canBeReplaced(@NotNull BlockState state, @NotNull BlockPlaceContext context)
    {
        for (Direction direction : Direction.values())
            if (state.getValue(FACING_TO_PROPERTY_MAP.get(direction)))
                return context.getItemInHand().getItem().equals(asItem()) && !state.getValue(FACING_TO_PROPERTY_MAP.get(context.getClickedFace().getOpposite()));
        return true;
    }

    @Override
    public void playerDestroy(@NotNull Level levelIn, Player player, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable BlockEntity te, @NotNull ItemStack stack)
    {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);

        for (Direction dir : Direction.values())
            if (state.getValue(FACING_TO_PROPERTY_MAP.get(dir)))
                dropResources(state, levelIn, pos, te, player, stack);
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state)
    {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        return stateToShapeMap.get(state);
    }

    private static VoxelShape getShapeForState(BlockState state)
    {
        VoxelShape voxelshape = Shapes.empty();

        if (state.getValue(UP))
            voxelshape = UP_AABB;

        if (state.getValue(DOWN))
            voxelshape = Shapes.or(voxelshape, DOWN_AABB);

        if (state.getValue(NORTH))
            voxelshape = Shapes.or(voxelshape, SOUTH_AABB);

        if (state.getValue(SOUTH))
            voxelshape = Shapes.or(voxelshape, NORTH_AABB);

        if (state.getValue(EAST))
            voxelshape = Shapes.or(voxelshape, WEST_AABB);

        if (state.getValue(WEST))
            voxelshape = Shapes.or(voxelshape, EAST_AABB);

        return voxelshape;
    }

    @SuppressWarnings("deprecation")
    public static class Seethrough extends TarpBlock
    {
        public Seethrough()
        {
            super(Properties.of().isRedstoneConductor(SplatcraftBlocks::noRedstoneConduct).instrument(NoteBlockInstrument.HAT).noOcclusion());
        }

        public @NotNull VoxelShape getVisualShape(@NotNull BlockState p_48735_, @NotNull BlockGetter p_48736_, @NotNull BlockPos p_48737_, @NotNull CollisionContext p_48738_)
        {
            return Shapes.empty();
        }

        public float getShadeBrightness(@NotNull BlockState p_48731_, @NotNull BlockGetter p_48732_, @NotNull BlockPos p_48733_)
        {
            return 1.0F;
        }

        public boolean propagatesSkylightDown(@NotNull BlockState p_48740_, @NotNull BlockGetter p_48741_, @NotNull BlockPos p_48742_)
        {
            return true;
        }

        @Override
        public boolean useShapeForLightOcclusion(@NotNull BlockState p_60576_)
        {
            return true;
        }
    }
}