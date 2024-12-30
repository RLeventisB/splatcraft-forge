package net.splatcraft.blocks;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Dismounting;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.splatcraft.dummys.ISplatcraftForgeBlockDummy;
import net.splatcraft.entities.SpawnShieldEntity;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.tileentities.SpawnPadTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SpawnPadBlock extends Block implements IColoredBlock, Waterloggable, BlockEntityProvider, ISplatcraftForgeBlockDummy
{
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	public static final DirectionProperty DIRECTION = Properties.HORIZONTAL_FACING;
	private static final VoxelShape SHAPE = createCuboidShape(0, 0, 0, 16, 6.1, 16);
	private Aux auxBlock;
	public SpawnPadBlock()
	{
		super(AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY).strength(2.0f).requiresTool());
		setDefaultState(getStateManager().getDefaultState().with(WATERLOGGED, false).with(DIRECTION, Direction.NORTH));
		
		SplatcraftBlocks.inkColoredBlocks.add(this);
	}
	@Nullable
	@Override
	public BlockEntity createBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
	{
		return SplatcraftTileEntities.spawnPadTileEntity.get().instantiate(pos, state);
	}
	@Override
	public @NotNull VoxelShape getOutlineShape(@NotNull BlockState state, @NotNull BlockView levelIn, @NotNull BlockPos pos, @NotNull ShapeContext context)
	{
		return SHAPE;
	}
	@Override
	public Optional<Vec3d> phGetRespawnPosition(BlockState state, EntityType<?> type, WorldView WorldView, BlockPos pos, float orientation)
	{
		Vec3d vec = Dismounting.findRespawnPos(type, WorldView, pos, false);
		
		return vec == null ? Optional.empty() : Optional.of(vec);
	}
	@Override
	public ItemStack phGetCloneItemStack(BlockState state, HitResult target, WorldView level, BlockPos pos, PlayerEntity player)
	{
		return ColorUtils.withColorLocked(ColorUtils.withInkColor(ISplatcraftForgeBlockDummy.super.phGetCloneItemStack(state, target, level, pos, player), getColor(level, pos)), true);
	}
	@Nullable
	@Override
	public BlockState getPlacementState(@NotNull ItemPlacementContext context)
	{
		for (Direction dir : Direction.values())
		{
			if (dir.getHorizontal() < 0)
				continue;
			
			for (int i = 0; i <= 1; i++)
				if (!context.getWorld().getBlockState(context.getBlockPos().offset(dir).offset(dir.rotateYCounterclockwise(), i)).canReplace(context))
					return null;
		}
		return getDefaultState().with(WATERLOGGED, context.getWorld().getFluidState(context.getBlockPos()).getRegistryEntry() == Fluids.WATER).with(DIRECTION, context.getHorizontalPlayerFacing());
	}
	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(WATERLOGGED).add(DIRECTION);
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
	@Override
	public @NotNull PistonBehavior phGetPistonBehavior(@NotNull BlockState state)
	{
		return PistonBehavior.BLOCK;
	}
	@Override
	public @NotNull ItemStack getPickStack(@NotNull WorldView reader, @NotNull BlockPos pos, @NotNull BlockState state)
	{
		ItemStack stack = new ItemStack(this);
		
		if (reader.getBlockEntity(pos) instanceof InkColorTileEntity tileEntity)
			ColorUtils.withColorLocked(ColorUtils.withInkColor(stack, ColorUtils.getInkColor(tileEntity)), true);
		
		return stack;
	}
	@Override
	public boolean canPathfindThrough(@NotNull BlockState p_196266_1_, NavigationType type)
	{
		return false;
	}
	@Override
	public boolean canMobSpawnInside(@NotNull BlockState state)
	{
		return true;
	}
	@Override
	public void onPlaced(@NotNull World world, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity entity, ItemStack stack)
	{
		if (!world.isClient() && !stack.getComponents().isEmpty() && world.getBlockEntity(pos) instanceof SpawnPadTileEntity spawnPadTile)
		{
			ColorUtils.withInkColor(world.getBlockEntity(pos), ColorUtils.getEffectiveColor(stack, entity));
			
			SpawnShieldEntity shield = new SpawnShieldEntity(world, pos, ColorUtils.getEffectiveColor(stack));
			spawnPadTile.setSpawnShield(shield);
			
			world.spawnEntity(shield);
		}
		
		for (Direction dir : Direction.values())
		{
			if (dir.getHorizontal() < 0)
				continue;
			
			for (int i = 0; i <= 1; i++)
			{
				BlockPos auxPos = pos.offset(dir).offset(dir.rotateYCounterclockwise(), i);
				world.setBlockState(auxPos, auxBlock.getDefaultState()
					.with(WATERLOGGED, world.getFluidState(auxPos).getRegistryEntry() == Fluids.WATER)
					.with(DIRECTION, dir)
					.with(Aux.IS_CORNER, i == 1), 3);
			}
		}
		world.updateNeighbors(pos, Blocks.AIR);
		state.updateNeighbors(world, pos, 3);
		
		super.onPlaced(world, pos, state, entity, stack);
	}
	@Override
	public BlockState onBreak(@NotNull World p_176208_1_, @NotNull BlockPos p_176208_2_, @NotNull BlockState p_176208_3_, @NotNull PlayerEntity p_176208_4_)
	{
		return super.onBreak(p_176208_1_, p_176208_2_, p_176208_3_, p_176208_4_);
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
	public InkColor getColor(WorldView world, BlockPos pos)
	{
		if (world.getBlockEntity(pos) instanceof InkColorTileEntity tileEntity)
		{
			return tileEntity.getInkColor();
		}
		return InkColor.INVALID;
	}
	@Override
	public boolean remoteColorChange(World world, BlockPos pos, InkColor newColor)
	{
		BlockState state = world.getBlockState(pos);
		if (world.getBlockEntity(pos) instanceof SpawnPadTileEntity spawnPad && spawnPad.getInkColor() != newColor)
		{
			spawnPad.setColor(newColor);
			SpawnShieldEntity shield = spawnPad.getSpawnShield();
			if (shield != null)
				shield.setColor(ColorUtils.getEffectiveColor(world, pos));
			world.updateListeners(pos, state, state, 3);
			state.updateNeighbors(world, pos, 3);
			return true;
		}
		return false;
	}
	@Override
	public boolean remoteInkClear(World world, BlockPos pos)
	{
		return false;
	}
	public static class Aux extends Block implements IColoredBlock, Waterloggable, ISplatcraftForgeBlockDummy
	{
		public static final BooleanProperty IS_CORNER = BooleanProperty.of("corner");
		private static final VoxelShape[] SHAPES = new VoxelShape[8];
		final SpawnPadBlock parent;
		public Aux(SpawnPadBlock parent)
		{
			super(parent.settings);
			this.parent = parent;
			
			parent.auxBlock = this;
			
			setDefaultState(getStateManager().getDefaultState().with(WATERLOGGED, false).with(DIRECTION, Direction.NORTH).with(IS_CORNER, false));
		}
		@Override
		public VoxelShape getOutlineShape(BlockState state, @NotNull BlockView reader, @NotNull BlockPos pos, @NotNull ShapeContext context)
		{
			int i = state.get(DIRECTION).getHorizontal() * 2 + (state.get(IS_CORNER) ? 1 : 0);
			
			if (i < 0)
				return VoxelShapes.empty();
			
			if (SHAPES[i] == null)
				SHAPES[i] = VoxelShapes.union(
					BarrierBarBlock.modifyShapeForDirection(state.get(DIRECTION), createCuboidShape(state.get(IS_CORNER) ? 8 : 0, 0, 8, 16, 6, 16)),
					BarrierBarBlock.modifyShapeForDirection(state.get(DIRECTION).getOpposite(), createCuboidShape(0, 6, 6, state.get(IS_CORNER) ? 7 : 16, 7, 7)),
					BarrierBarBlock.modifyShapeForDirection(state.get(DIRECTION), createCuboidShape(state.get(IS_CORNER) ? 10 : 0, 0, 10, 16, 6.1, 16)),
					state.get(IS_CORNER) ? BarrierBarBlock.modifyShapeForDirection(state.get(DIRECTION), createCuboidShape(9, 6, 10, 10, 7, 16)) : VoxelShapes.empty());
			
			return SHAPES[i];
		}
		public BlockPos getParentPos(BlockState state, BlockPos pos)
		{
			return pos.offset(state.get(DIRECTION).getOpposite()).offset(state.get(DIRECTION).rotateYClockwise(), state.get(IS_CORNER) ? 1 : 0);
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
			builder.add(WATERLOGGED).add(DIRECTION).add(IS_CORNER);
		}
		@Override
		public @NotNull FluidState getFluidState(BlockState state)
		{
			return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
		}
		@Override
		public @NotNull BlockState getStateForNeighborUpdate(@NotNull BlockState stateIn, @NotNull Direction facing, @NotNull BlockState facingState, WorldAccess levelIn, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos)
		{
			if (levelIn.getBlockState(getParentPos(stateIn, currentPos)).getBlock() != parent)
				return Blocks.AIR.getDefaultState();
			
			if (stateIn.get(WATERLOGGED))
			{
				levelIn.scheduleFluidTick(currentPos, Fluids.WATER, Fluids.WATER.getTickRate(levelIn));
			}
			
			return super.getStateForNeighborUpdate(stateIn, facing, facingState, levelIn, currentPos, facingPos);
		}
		@Override
		public boolean phOnDestroyedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, FluidState fluid)
		{
			BlockPos parentPos = getParentPos(state, pos);
			if (world.getBlockState(parentPos).getBlock() == parent)
				world.removeBlock(parentPos, willHarvest);
			return ISplatcraftForgeBlockDummy.super.phOnDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
		}
		@Override
		public @NotNull PistonBehavior phGetPistonBehavior(@NotNull BlockState state)
		{
			return PistonBehavior.BLOCK;
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
		public boolean remoteColorChange(World world, BlockPos pos, InkColor newColor)
		{
			return parent.remoteColorChange(world, getParentPos(world.getBlockState(pos), pos), newColor);
		}
		@Override
		public boolean remoteInkClear(World world, BlockPos pos)
		{
			return false;
		}
		@Override
		public boolean canPathfindThrough(@NotNull BlockState p_196266_1_, @NotNull NavigationType p_196266_4_)
		{
			return false;
		}
		@Override
		public Optional<Vec3d> phGetRespawnPosition(BlockState state, EntityType<?> type, WorldView world, BlockPos pos, float orientation)
		{
			BlockPos parentPos = getParentPos(state, pos);
			return parent.phGetRespawnPosition(world.getBlockState(parentPos), type, world, parentPos, orientation);
		}
		@Override
		public @NotNull ItemStack getPickStack(@NotNull WorldView level, @NotNull BlockPos pos, @NotNull BlockState state)
		{
			BlockPos parentPos = getParentPos(state, pos);
			return parent.getPickStack(level, parentPos, level.getBlockState(parentPos));
		}
		@Override
		public @NotNull BlockRenderType getRenderType(@NotNull BlockState state)
		{
			return BlockRenderType.INVISIBLE;
		}
	}
}
