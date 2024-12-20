package net.splatcraft.blocks;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.splatcraft.registries.SplatcraftBlocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TarpBlock extends Block implements Waterloggable
{
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public static final BooleanProperty UP = Properties.UP;
    public static final BooleanProperty DOWN = Properties.DOWN;
    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty WEST = Properties.WEST;
    public static final Map<Direction, BooleanProperty> FACING_TO_PROPERTY_MAP = ConnectingBlock.FACING_PROPERTIES.entrySet().stream().collect(Util.toMap());
    private static final VoxelShape UP_AABB = createCuboidShape(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape DOWN_AABB = createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
    private static final VoxelShape EAST_AABB = createCuboidShape(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D);
    private static final VoxelShape WEST_AABB = createCuboidShape(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SOUTH_AABB = createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
    private static final VoxelShape NORTH_AABB = createCuboidShape(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);
    private final Map<BlockState, VoxelShape> stateToShapeMap;

    public TarpBlock()
    {
        this(AbstractBlock.Settings.create().mapColor(MapColor.WHITE_GRAY).burnable().solidBlock(SplatcraftBlocks::noRedstoneConduct));
    }

    public TarpBlock(Settings properties)
    {
        super(properties);
        setDefaultState(getDefaultState().with(WATERLOGGED, Boolean.FALSE).with(DOWN, Boolean.TRUE).with(UP, Boolean.FALSE).with(NORTH, Boolean.FALSE).with(EAST, Boolean.FALSE).with(SOUTH, Boolean.FALSE).with(WEST, Boolean.FALSE));
        stateToShapeMap = ImmutableMap.copyOf(getStateManager().getStates().stream().collect(Collectors.toMap(Function.identity(), TarpBlock::getShapeForState)));
    }

    private static VoxelShape getShapeForState(BlockState state)
    {
        VoxelShape voxelshape = VoxelShapes.empty();

        if (state.get(UP))
            voxelshape = UP_AABB;

        if (state.get(DOWN))
            voxelshape = VoxelShapes.union(voxelshape, DOWN_AABB);

        if (state.get(NORTH))
            voxelshape = VoxelShapes.union(voxelshape, SOUTH_AABB);

        if (state.get(SOUTH))
            voxelshape = VoxelShapes.union(voxelshape, NORTH_AABB);

        if (state.get(EAST))
            voxelshape = VoxelShapes.union(voxelshape, WEST_AABB);

        if (state.get(WEST))
            voxelshape = VoxelShapes.union(voxelshape, EAST_AABB);

        return voxelshape;
    }

    public VoxelShape getOutlineShape(@NotNull BlockState state, @NotNull BlockView level, @NotNull BlockPos pos, @NotNull ShapeContext context)
    {
        return stateToShapeMap.get(state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
    {
        builder.add(UP, DOWN, NORTH, SOUTH, WEST, EAST, WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context)
    {
        BlockState state = context.getWorld().getBlockState(context.getBlockPos()).isOf(this) ? context.getWorld().getBlockState(context.getBlockPos()) :
            super.getPlacementState(context).with(DOWN, false).with(WATERLOGGED, context.getWorld().getFluidState(context.getBlockPos()).getRegistryEntry() == Fluids.WATER);

        state = state.with(FACING_TO_PROPERTY_MAP.get(context.getSide().getOpposite()), true);

        for (Direction direction : Direction.values())
            if (state.get(FACING_TO_PROPERTY_MAP.get(direction)))
                return state;

        return state.with(DOWN, true);
    }

    @Override
    public boolean canReplace(@NotNull BlockState state, @NotNull ItemPlacementContext context)
    {
        for (Direction direction : Direction.values())
            if (state.get(FACING_TO_PROPERTY_MAP.get(direction)))
                return context.getStack().getItem().equals(asItem()) && !state.get(FACING_TO_PROPERTY_MAP.get(context.getSide().getOpposite()));
        return true;
    }

    @Override
    public void afterBreak(@NotNull World world, PlayerEntity player, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable BlockEntity te, @NotNull ItemStack stack)
    {
        player.incrementStat(Stats.MINED.getOrCreateStat(this));
        player.addExhaustion(0.005F);

        for (Direction dir : Direction.values())
            if (state.get(FACING_TO_PROPERTY_MAP.get(dir)))
                dropStacks(state, world, pos, te, player, stack);
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state)
    {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    public static class Seethrough extends TarpBlock
    {
        public Seethrough()
        {
            super(AbstractBlock.Settings.create().solidBlock(SplatcraftBlocks::noRedstoneConduct).instrument(NoteBlockInstrument.HAT).nonOpaque());
        }

        public @NotNull VoxelShape getCameraCollisionShape(@NotNull BlockState p_48735_, @NotNull BlockView p_48736_, @NotNull BlockPos p_48737_, @NotNull ShapeContext p_48738_)
        {
            return VoxelShapes.empty();
        }

        @Override
        public float getAmbientOcclusionLightLevel(@NotNull BlockState p_48731_, @NotNull BlockView p_48732_, @NotNull BlockPos p_48733_)
        {
            return 1.0F;
        }

        @Override
        public boolean isTransparent(@NotNull BlockState p_48740_, @NotNull BlockView p_48741_, @NotNull BlockPos p_48742_)
        {
            return true;
        }

        @Override
        public boolean hasSidedTransparency(@NotNull BlockState p_60576_)
        {
            return true;
        }
    }
}