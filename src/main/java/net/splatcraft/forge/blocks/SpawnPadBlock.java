package net.splatcraft.forge.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.forge.entities.SpawnShieldEntity;
import net.splatcraft.forge.registries.SplatcraftBlocks;
import net.splatcraft.forge.registries.SplatcraftTileEntities;
import net.splatcraft.forge.tileentities.InkColorTileEntity;
import net.splatcraft.forge.tileentities.SpawnPadTileEntity;
import net.splatcraft.forge.util.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("deprecation")
public class SpawnPadBlock extends Block implements IColoredBlock, SimpleWaterloggedBlock, EntityBlock
{
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final DirectionProperty DIRECTION = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 6.1, 16);
    private Aux auxBlock;

    public SpawnPadBlock()
    {
        super(Properties.of().mapColor(MapColor.METAL).strength(2.0f).requiresCorrectToolForDrops());
        this.registerDefaultState(this.getStateDefinition().any().setValue(WATERLOGGED, false).setValue(DIRECTION, Direction.NORTH));

        SplatcraftBlocks.inkColoredBlocks.add(this);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return SplatcraftTileEntities.spawnPadTileEntity.get().create(pos, state);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter levelIn, @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        return SHAPE;
    }

    @Override
    public Optional<Vec3> getRespawnPosition(BlockState state, EntityType<?> type, LevelReader levelReader, BlockPos pos, float orientation, @Nullable LivingEntity entity)
    {
        if (entity != null && !ColorUtils.colorEquals(entity, levelReader.getBlockEntity(pos)))
            return Optional.empty();

        Vec3 vec = DismountHelper.findSafeDismountLocation(type, levelReader, pos, false);

        return vec == null ? Optional.empty() : Optional.of(vec);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player)
    {
        return ColorUtils.setColorLocked(ColorUtils.setInkColor(super.getCloneItemStack(state, target, level, pos, player), getColor((Level) level, pos)), true);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context)
    {
        for (Direction dir : Direction.values())
        {
            if (dir.get2DDataValue() < 0)
                continue;

            for (int i = 0; i <= 1; i++)
                if (!context.getLevel().getBlockState(context.getClickedPos().relative(dir).relative(dir.getCounterClockWise(), i)).canBeReplaced(context))
                    return null;
        }
        return defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER).setValue(DIRECTION, context.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(WATERLOGGED).add(DIRECTION);
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state)
    {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public @NotNull BlockState updateShape(BlockState stateIn, @NotNull Direction facing, @NotNull BlockState facingState, @NotNull LevelAccessor levelIn, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos)
    {
        if (stateIn.getValue(WATERLOGGED))
        {
            levelIn.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(levelIn));
        }

        return super.updateShape(stateIn, facing, facingState, levelIn, currentPos, facingPos);
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction(@NotNull BlockState state)
    {
        return PushReaction.BLOCK;
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockGetter reader, @NotNull BlockPos pos, @NotNull BlockState state)
    {
        ItemStack stack = new ItemStack(this);

        if (reader.getBlockEntity(pos) instanceof InkColorTileEntity tileEntity)
            ColorUtils.setColorLocked(ColorUtils.setInkColor(stack, ColorUtils.getInkColor(tileEntity)), true);

        return stack;
    }

    @Override
    public boolean isPathfindable(@NotNull BlockState p_196266_1_, @NotNull BlockGetter p_196266_2_, @NotNull BlockPos p_196266_3_, @NotNull PathComputationType p_196266_4_)
    {
        return false;
    }

    @Override
    public boolean isPossibleToRespawnInThis(@NotNull BlockState state)
    {
        return true;
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity entity, ItemStack stack)
    {
        if (stack.getTag() != null && level.getBlockEntity(pos) instanceof SpawnPadTileEntity spawnPadTile)
        {
            ColorUtils.setInkColor(level.getBlockEntity(pos), ColorUtils.getInkColor(stack));

            SpawnShieldEntity shield = new SpawnShieldEntity(level, pos, ColorUtils.getInkColorOrInverted(stack));
            spawnPadTile.setSpawnShield(shield);

            level.addFreshEntity(shield);
        }

        for (Direction dir : Direction.values())
        {
            if (dir.get2DDataValue() < 0)
                continue;

            for (int i = 0; i <= 1; i++)
            {
                BlockPos auxPos = pos.relative(dir).relative(dir.getCounterClockWise(), i);
                level.setBlock(auxPos, auxBlock.defaultBlockState()
                    .setValue(WATERLOGGED, level.getFluidState(auxPos).getType() == Fluids.WATER)
                    .setValue(DIRECTION, dir)
                    .setValue(Aux.IS_CORNER, i == 1), 3);
            }
        }
        level.blockUpdated(pos, Blocks.AIR);
        state.updateNeighbourShapes(level, pos, 3);

        super.setPlacedBy(level, pos, state, entity, stack);
    }

    @Override
    public void playerWillDestroy(@NotNull Level p_176208_1_, @NotNull BlockPos p_176208_2_, @NotNull BlockState p_176208_3_, @NotNull Player p_176208_4_)
    {
        super.playerWillDestroy(p_176208_1_, p_176208_2_, p_176208_3_, p_176208_4_);
    }

    @Override
    public boolean canClimb()
    {
        return false;
    }

    @Override
    public boolean canSwim()
    {
        return true;
    }

    @Override
    public boolean canDamage()
    {
        return false;
    }

    @Override
    public int getColor(Level level, BlockPos pos)
    {
        if (level.getBlockEntity(pos) instanceof InkColorTileEntity tileEntity)
        {
            return tileEntity.getColor();
        }
        return -1;
    }

    @Override
    public boolean remoteColorChange(Level level, BlockPos pos, int newColor)
    {
        BlockState state = level.getBlockState(pos);
        if (level.getBlockEntity(pos) instanceof SpawnPadTileEntity spawnPad && spawnPad.getColor() != newColor)
        {
            spawnPad.setColor(newColor);
            SpawnShieldEntity shield = spawnPad.getSpawnShield();
            if (shield != null)
                shield.setColor(ColorUtils.getInkColorOrInverted(level, pos));
            level.sendBlockUpdated(pos, state, state, 3);
            state.updateNeighbourShapes(level, pos, 3);
            return true;
        }
        return false;
    }

    @Override
    public boolean remoteInkClear(Level level, BlockPos pos)
    {
        return false;
    }

    public static class Aux extends Block implements IColoredBlock, SimpleWaterloggedBlock
    {
        public static final BooleanProperty IS_CORNER = BooleanProperty.create("corner");
        private static final VoxelShape[] SHAPES = new VoxelShape[8];
        final SpawnPadBlock parent;

        public Aux(SpawnPadBlock parent)
        {
            super(parent.properties);
            this.parent = parent;

            parent.auxBlock = this;

            this.registerDefaultState(this.getStateDefinition().any().setValue(WATERLOGGED, false).setValue(DIRECTION, Direction.NORTH).setValue(IS_CORNER, false));
        }

        @Override
        public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter reader, @NotNull BlockPos pos, @NotNull CollisionContext context)
        {
            int i = state.getValue(DIRECTION).get2DDataValue() * 2 + (state.getValue(IS_CORNER) ? 1 : 0);

            if (i < 0)
                return Shapes.empty();

            if (SHAPES[i] == null)
                SHAPES[i] = Shapes.or(
                    BarrierBarBlock.modifyShapeForDirection(state.getValue(DIRECTION), Block.box(state.getValue(IS_CORNER) ? 8 : 0, 0, 8, 16, 6, 16)),
                    BarrierBarBlock.modifyShapeForDirection(state.getValue(DIRECTION).getOpposite(), Block.box(0, 6, 6, state.getValue(IS_CORNER) ? 7 : 16, 7, 7)),
                    BarrierBarBlock.modifyShapeForDirection(state.getValue(DIRECTION), Block.box(state.getValue(IS_CORNER) ? 10 : 0, 0, 10, 16, 6.1, 16)),
                    state.getValue(IS_CORNER) ? BarrierBarBlock.modifyShapeForDirection(state.getValue(DIRECTION), Block.box(9, 6, 10, 10, 7, 16)) : Shapes.empty());

            return SHAPES[i];
        }

        public BlockPos getParentPos(BlockState state, BlockPos pos)
        {
            return pos.relative(state.getValue(DIRECTION).getOpposite()).relative(state.getValue(DIRECTION).getClockWise(), state.getValue(IS_CORNER) ? 1 : 0);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context)
        {
            return defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
        {
            builder.add(WATERLOGGED).add(DIRECTION).add(IS_CORNER);
        }

        @Override
        public @NotNull FluidState getFluidState(BlockState state)
        {
            return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
        }

        @Override
        public @NotNull BlockState updateShape(@NotNull BlockState stateIn, @NotNull Direction facing, @NotNull BlockState facingState, LevelAccessor levelIn, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos)
        {
            if (levelIn.getBlockState(getParentPos(stateIn, currentPos)).getBlock() != parent)
                return Blocks.AIR.defaultBlockState();

            if (stateIn.getValue(WATERLOGGED))
            {
                levelIn.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(levelIn));
            }

            return super.updateShape(stateIn, facing, facingState, levelIn, currentPos, facingPos);
        }

        @Override
        public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid)
        {
            BlockPos parentPos = getParentPos(state, pos);
            if (level.getBlockState(parentPos).getBlock() == parent)
                level.destroyBlock(parentPos, willHarvest);
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }

        @Override
        public @NotNull PushReaction getPistonPushReaction(@NotNull BlockState state)
        {
            return PushReaction.BLOCK;
        }

        @Override
        public boolean canClimb()
        {
            return false;
        }

        @Override
        public boolean canSwim()
        {
            return true;
        }

        @Override
        public boolean canDamage()
        {
            return false;
        }

        @Override
        public boolean remoteColorChange(Level level, BlockPos pos, int newColor)
        {
            return parent.remoteColorChange(level, getParentPos(level.getBlockState(pos), pos), newColor);
        }

        @Override
        public boolean remoteInkClear(Level level, BlockPos pos)
        {
            return false;
        }

        @Override
        public boolean isPathfindable(@NotNull BlockState p_196266_1_, @NotNull BlockGetter p_196266_2_, @NotNull BlockPos p_196266_3_, @NotNull PathComputationType p_196266_4_)
        {
            return false;
        }

        @Override
        public Optional<Vec3> getRespawnPosition(BlockState state, EntityType<?> type, LevelReader world, BlockPos pos, float orientation, @Nullable LivingEntity entity)
        {
            BlockPos parentPos = getParentPos(state, pos);
            return parent.getRespawnPosition(world.getBlockState(parentPos), type, world, parentPos, orientation, entity);
        }

        @Override
        public @NotNull ItemStack getCloneItemStack(@NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull BlockState state)
        {
            BlockPos parentPos = getParentPos(state, pos);
            return parent.getCloneItemStack(level, parentPos, level.getBlockState(parentPos));
        }

        @Override
        public @NotNull RenderShape getRenderShape(@NotNull BlockState state)
        {
            return RenderShape.INVISIBLE;
        }
    }
}
