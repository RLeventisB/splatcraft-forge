package net.splatcraft.blocks;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.*;
import net.splatcraft.dummys.ISplatcraftForgeBlockDummy;
import net.splatcraft.items.ColoredBlockItem;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class InkwellBlock extends Block implements IColoredBlock, Waterloggable, BlockEntityProvider, ISplatcraftForgeBlockDummy
{
	public static final HashMap<Item, ColoredBlockItem> inkCoatingRecipes = new HashMap<>();
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	public static final BlockSoundGroup SOUND_TYPE = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_STONE_BREAK, SoundEvents.BLOCK_SLIME_BLOCK_STEP, SoundEvents.BLOCK_GLASS_PLACE, SoundEvents.BLOCK_GLASS_HIT, SoundEvents.BLOCK_SLIME_BLOCK_FALL);
	private static final VoxelShape SHAPE = VoxelShapes.union(
		createCuboidShape(0, 0, 0, 16, 12, 16),
		createCuboidShape(1, 12, 1, 14, 13, 14),
		createCuboidShape(0, 13, 0, 16, 16, 16)
	);
	public InkwellBlock()
	{
		super(AbstractBlock.Settings.create().solidBlock((state, getter, pos) -> false).instrument(NoteBlockInstrument.HAT).strength(0.35f).sounds(SOUND_TYPE));
		setDefaultState(getStateManager().getDefaultState().with(WATERLOGGED, false));
		
		SplatcraftBlocks.inkColoredBlocks.add(this);
	}
	private static void tick(World world, BlockPos pos, BlockState state, InkColorTileEntity t)
	{
		Box bb = new Box(t.getPos().up());
		
		for (ItemEntity entity : world.getEntitiesByClass(ItemEntity.class, bb, entity -> inkCoatingRecipes.containsKey(entity.getStack().getItem())))
		{
			ItemStack stack = entity.getStack();
			entity.setStack(ColorUtils.withColorLocked(ColorUtils.withInkColor(new ItemStack(inkCoatingRecipes.get(stack.getItem()), stack.getCount()), t.getInkColor()), true));
		}
	}
	@Override
	public Integer phGetBeaconColorMultiplier(BlockState state, WorldView level, BlockPos pos, BlockPos beaconPos)
	{
		return getColor(level, pos).getColor();
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
		builder.add(WATERLOGGED);
	}
	@Override
	public @NotNull FluidState getFluidState(BlockState state)
	{
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}
	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos)
	{
		if (state.get(WATERLOGGED))
			world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		
		return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
	}
	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		return SHAPE;
	}
	@Override
	public @NotNull PistonBehavior phGetPistonBehavior(@NotNull BlockState state)
	{
		return PistonBehavior.DESTROY;
	}
	@Override
	public @NotNull ItemStack getPickStack(WorldView reader, BlockPos pos, BlockState state)
	{
		ItemStack stack = super.getPickStack(reader, pos, state);
		
		if (reader.getBlockEntity(pos) instanceof InkColorTileEntity colorTileEntity)
			ColorUtils.withColorLocked(ColorUtils.withInkColor(stack, ColorUtils.getInkColor(colorTileEntity)), true);
		
		return stack;
	}
	@Override
	public boolean canPathfindThrough(@NotNull BlockState p_60475_, @NotNull NavigationType p_60478_)
	{
		return false;
	}
	@Override
	public boolean canMobSpawnInside(@NotNull BlockState pState)
	{
		return true;
	}
	@Override
	public void onPlaced(@NotNull World world, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity entity, ItemStack stack)
	{
		if (stack.contains(SplatcraftComponents.ITEM_COLOR_DATA) && world.getBlockEntity(pos) instanceof InkColorTileEntity)
		{
			ColorUtils.withInkColor(world.getBlockEntity(pos), ColorUtils.getInkColor(stack));
		}
		super.onPlaced(world, pos, state, entity, stack);
	}
	@Override
	public boolean phShouldCheckWeakPower(BlockState state, RedstoneView level, BlockPos pos, Direction side)
	{
		return true;
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
		BlockEntity tileEntity = world.getBlockEntity(pos);
		if (tileEntity instanceof InkColorTileEntity colorTileEntity && colorTileEntity.getInkColor() != newColor)
		{
			colorTileEntity.setColor(newColor);
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
	@Nullable
	@Override
	public BlockEntity createBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
	{
		return SplatcraftTileEntities.colorTileEntity.get().instantiate(pos, state);
	}
	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType)
	{
		return world.isClient() ? null : (tickLevel, pos, tickState, te) -> tick(tickLevel, pos, tickState, (InkColorTileEntity) te);
	}
}
