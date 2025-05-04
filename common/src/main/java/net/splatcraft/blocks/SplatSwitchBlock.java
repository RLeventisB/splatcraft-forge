package net.splatcraft.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.dummys.ISplatcraftForgeBlockDummy;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.BlockInkedResult;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SplatSwitchBlock extends Block implements IColoredBlock, SimpleWaterloggedBlock, EntityBlock, ISplatcraftForgeBlockDummy
{
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	private static final VoxelShape[] SHAPES = new VoxelShape[]
		{
			box(1, 14, 1, 15, 16, 15),
			box(1, 0, 1, 15, 2, 15),
			box(1, 1, 14, 15, 15, 16),
			box(1, 1, 0, 15, 15, 2),
			box(14, 1, 1, 16, 15, 15),
			box(0, 1, 1, 2, 15, 15)
		};
	public SplatSwitchBlock()
	{
		super(Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0F).sound(SoundType.METAL).noOcclusion());
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP).setValue(POWERED, false));

		SplatcraftBlocks.inkColoredBlocks.add(this);
	}
	@Override
	public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context)
	{
		return SHAPES[state.getValue(FACING).ordinal()];
	}
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> containter)
	{
		containter.add(FACING, POWERED, WATERLOGGED);
	}
	@Override
	public boolean phCanConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction side)
	{
		return true;
	}
	@Override
	public boolean isSignalSource(@NotNull BlockState state)
	{
		return true;
	}
	@Override
	public int getSignal(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull Direction face)
	{
		return state.getValue(POWERED) ? 15 : 0;
	}
	@Override
	public int getDirectSignal(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull Direction face)
	{
		return state.getValue(POWERED) ? 15 : 0;
	}
	@Override
	public BlockState getStateForPlacement(@NotNull BlockPlaceContext context)
	{
		BlockState state = super.getStateForPlacement(context).setValue(FACING, context.getClickedFace());
		return state.setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).holder() == Fluids.WATER);
	}
	@Override
	public @NotNull FluidState getFluidState(BlockState state)
	{
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}
	@Override
	public @NotNull BlockState updateShape(@NotNull BlockState stateIn, @NotNull Direction facing, @NotNull BlockState facingState, @NotNull LevelAccessor levelIn, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos)
	{
		if (InkedBlock.isTouchingLiquid(levelIn, currentPos) && levelIn instanceof Level world)
		{
			stateIn = stateIn.setValue(POWERED, false);
			world.setBlock(currentPos, stateIn, 3);
			playSound(levelIn, currentPos, stateIn);
			updateNeighbors(stateIn, world, currentPos);
			return stateIn;
		}
		return super.updateShape(stateIn, facing, facingState, levelIn, currentPos, facingPos);
	}
	@Override
	public void onRemove(BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving)
	{
		if (state.getValue(POWERED))
			updateNeighbors(state, world, pos);
		super.onRemove(state, world, pos, newState, isMoving);
	}
	private void updateNeighbors(BlockState state, Level world, BlockPos pos)
	{
		world.updateNeighborsAt(pos, this);
		world.updateNeighborsAt(pos.relative(state.getValue(FACING).getOpposite()), this);
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
	public boolean remoteColorChange(Level world, BlockPos pos, InkColor newColor)
	{
		return false;
	}
	@Override
	public InkColor getColor(LevelReader world, BlockPos pos)
	{
		BlockState state = world.getBlockState(pos);
		return state.getValue(POWERED) && world.getBlockEntity(pos) instanceof InkColorTileEntity tileEntity ? tileEntity.getInkColor() : InkColor.INVALID;
	}
	@Override
	public BlockInkedResult inkBlock(Level world, BlockPos pos, InkColor color, float damage, InkBlockUtils.InkType inkType)
	{
		if (!(world.getBlockState(pos).getBlock().equals(this)) || !(world.getBlockEntity(pos) instanceof InkColorTileEntity te))
			return BlockInkedResult.FAIL;

		BlockState state = world.getBlockState(pos);
		InkColor switchColor = te.getInkColor();

		te.setColor(color);
		world.setBlock(pos, state.setValue(POWERED, true), 3);
		playSound(world, pos, state);
		updateNeighbors(state, world, pos);
		return color != switchColor ? BlockInkedResult.SUCCESS : BlockInkedResult.ALREADY_INKED;
	}
	@Override
	public boolean remoteInkClear(Level world, BlockPos pos)
	{
		BlockState state = world.getBlockState(pos);
		if (state.getValue(POWERED))
		{
			world.setBlock(pos, state.setValue(POWERED, false), 3);
			playSound(world, pos, state);
			return true;
		}
		return false;
	}
	private void playSound(LevelAccessor level, BlockPos currentPos, BlockState stateIn)
	{
		level.playSound(null, currentPos, stateIn.getValue(POWERED) ? SplatcraftSounds.splatSwitchPoweredOn : SplatcraftSounds.splatSwitchPoweredOff, SoundSource.BLOCKS, 1f, 1f);
	}
	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
	{
		return SplatcraftTileEntities.colorTileEntity.get().create(pos, state);
	}
}
