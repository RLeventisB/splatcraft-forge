package net.splatcraft.blocks;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.BlockInkedResult;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SplatSwitchBlock extends Block implements IColoredBlock, Waterloggable, BlockEntityProvider, ISplatcraftForgeBlockDummy
{
    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    private static final VoxelShape[] SHAPES = new VoxelShape[]
        {
            createCuboidShape(1, 14, 1, 15, 16, 15),
            createCuboidShape(1, 0, 1, 15, 2, 15),
            createCuboidShape(1, 1, 14, 15, 15, 16),
            createCuboidShape(1, 1, 0, 15, 15, 2),
            createCuboidShape(14, 1, 1, 16, 15, 15),
            createCuboidShape(0, 1, 1, 2, 15, 15)
        };

    public SplatSwitchBlock()
    {
        super(Settings.create().mapColor(MapColor.IRON_GRAY).requiresTool().strength(5.0F).sounds(BlockSoundGroup.METAL).nonOpaque());
        setDefaultState(getDefaultState().with(FACING, Direction.UP).with(POWERED, false));

        SplatcraftBlocks.inkColoredBlocks.add(this);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, @NotNull BlockView level, @NotNull BlockPos pos, @NotNull ShapeContext context)
    {
        return SHAPES[state.get(FACING).ordinal()];
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> containter)
    {
        containter.add(FACING, POWERED, WATERLOGGED);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockView level, BlockPos pos, @Nullable Direction side)
    {
        return true;
    }

    @Override
    public boolean emitsRedstonePower(@NotNull BlockState state)
    {
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, @NotNull BlockView level, @NotNull BlockPos pos, @NotNull Direction face)
    {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, @NotNull BlockView level, @NotNull BlockPos pos, @NotNull Direction face)
    {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public BlockState getPlacementState(@NotNull ItemPlacementContext context)
    {
        BlockState state = super.getPlacementState(context).with(FACING, context.getSide());
        return state.with(WATERLOGGED, context.getWorld().getFluidState(context.getBlockPos()).getRegistryEntry() == Fluids.WATER);
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state)
    {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public @NotNull BlockState getStateForNeighborUpdate(@NotNull BlockState stateIn, @NotNull Direction facing, @NotNull BlockState facingState, @NotNull WorldAccess levelIn, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos)
    {
        if (InkedBlock.isTouchingLiquid(levelIn, currentPos) && levelIn instanceof World world)
        {
            stateIn = stateIn.with(POWERED, false);
            world.setBlockState(currentPos, stateIn, 3);
            playSound(levelIn, currentPos, stateIn);
            updateNeighbors(stateIn, world, currentPos);
            return stateIn;
        }
        return super.getStateForNeighborUpdate(stateIn, facing, facingState, levelIn, currentPos, facingPos);
    }

    @Override
    public void onStateReplaced(BlockState state, @NotNull World world, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving)
    {
        if (state.get(POWERED))
            updateNeighbors(state, world, pos);
        super.onStateReplaced(state, world, pos, newState, isMoving);
    }

    private void updateNeighbors(BlockState state, World world, BlockPos pos)
    {
        world.updateNeighborsAlways(pos, this);
        world.updateNeighborsAlways(pos.offset(state.get(FACING).getOpposite()), this);
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

    @Override
    public boolean remoteColorChange(World world, BlockPos pos, InkColor newColor)
    {
        return false;
    }

    @Override
    public InkColor getColor(WorldView world, BlockPos pos)
    {
        BlockState state = world.getBlockState(pos);
        return state.get(POWERED) && world.getBlockEntity(pos) instanceof InkColorTileEntity tileEntity ? tileEntity.getInkColor() : InkColor.INVALID;
    }

    @Override
    public BlockInkedResult inkBlock(World world, BlockPos pos, InkColor color, float damage, InkBlockUtils.InkType inkType)
    {
        if (!(world.getBlockState(pos).getBlock().equals(this)) || !(world.getBlockEntity(pos) instanceof InkColorTileEntity te))
            return BlockInkedResult.FAIL;

        BlockState state = world.getBlockState(pos);
        InkColor switchColor = te.getInkColor();

        te.setColor(color);
        world.setBlockState(pos, state.with(POWERED, true), 3);
        playSound(world, pos, state);
        updateNeighbors(state, world, pos);
        return color != switchColor ? BlockInkedResult.SUCCESS : BlockInkedResult.ALREADY_INKED;
    }

    @Override
    public boolean remoteInkClear(World world, BlockPos pos)
    {
        BlockState state = world.getBlockState(pos);
        if (state.get(POWERED))
        {
            world.setBlockState(pos, state.with(POWERED, false), 3);
            playSound(world, pos, state);
            return true;
        }
        return false;
    }

    private void playSound(WorldAccess level, BlockPos currentPos, BlockState stateIn)
    {
        level.playSound(null, currentPos, stateIn.get(POWERED) ? SplatcraftSounds.splatSwitchPoweredOn : SplatcraftSounds.splatSwitchPoweredOff, SoundCategory.BLOCKS, 1f, 1f);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return SplatcraftTileEntities.colorTileEntity.get().instantiate(pos, state);
    }
}
