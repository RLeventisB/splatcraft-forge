package net.splatcraft.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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

public class InkwellBlock extends Block implements IColoredBlock, SimpleWaterloggedBlock, EntityBlock, ISplatcraftForgeBlockDummy
{
	public static final HashMap<Item, ColoredBlockItem> inkCoatingRecipes = new HashMap<>();
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final SoundType SOUND_TYPE = new SoundType(1.0F, 1.0F, SoundEvents.STONE_BREAK, SoundEvents.SLIME_BLOCK_STEP, SoundEvents.GLASS_PLACE, SoundEvents.GLASS_HIT, SoundEvents.SLIME_BLOCK_FALL);
	private static final VoxelShape SHAPE = Shapes.or(
		box(0, 0, 0, 16, 12, 16),
		box(1, 12, 1, 14, 13, 14),
		box(0, 13, 0, 16, 16, 16)
	);
	public InkwellBlock()
	{
		super(BlockBehaviour.Properties.of().isRedstoneConductor((state, getter, pos) -> false).instrument(NoteBlockInstrument.HAT).strength(0.35f).sound(SOUND_TYPE));
		registerDefaultState(getStateDefinition().any().setValue(WATERLOGGED, false));

		SplatcraftBlocks.inkColoredBlocks.add(this);
	}
	private static void tick(Level world, BlockPos pos, BlockState state, InkColorTileEntity t)
	{
		AABB bb = new AABB(t.getBlockPos().above());

		for (ItemEntity entity : world.getEntitiesOfClass(ItemEntity.class, bb, entity -> inkCoatingRecipes.containsKey(entity.getItem().getItem())))
		{
			ItemStack stack = entity.getItem();
			entity.setItem(ColorUtils.withColorLocked(ColorUtils.withInkColor(new ItemStack(inkCoatingRecipes.get(stack.getItem()), stack.getCount()), t.getInkColor()), true));
		}
	}
	@Override
	public Integer phGetBeaconColorMultiplier(BlockState state, LevelReader level, BlockPos pos, BlockPos beaconPos)
	{
		return getColor(level, pos).getColor();
	}
	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		return defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).holder() == Fluids.WATER);
	}
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(WATERLOGGED);
	}
	@Override
	public @NotNull FluidState getFluidState(BlockState state)
	{
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}
	@Override
	protected @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor world, @NotNull BlockPos pos, @NotNull BlockPos neighborPos)
	{
		if (state.getValue(WATERLOGGED))
			world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));

		return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
	}
	@Override
	protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context)
	{
		return SHAPE;
	}
	@Override
	public @NotNull PushReaction phGetPistonBehavior(@NotNull BlockState state)
	{
		return PushReaction.DESTROY;
	}
	@Override
	public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader reader, @NotNull BlockPos pos, @NotNull BlockState state)
	{
		ItemStack stack = super.getCloneItemStack(reader, pos, state);

		if (reader.getBlockEntity(pos) instanceof InkColorTileEntity colorTileEntity)
			ColorUtils.withColorLocked(ColorUtils.withInkColor(stack, ColorUtils.getInkColor(colorTileEntity)), true);

		return stack;
	}
	@Override
	public boolean isPathfindable(@NotNull BlockState p_60475_, @NotNull PathComputationType p_60478_)
	{
		return false;
	}
	@Override
	public boolean isPossibleToRespawnInThis(@NotNull BlockState pState)
	{
		return true;
	}
	@Override
	public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity entity, ItemStack stack)
	{
		if (stack.has(SplatcraftComponents.ITEM_COLOR_DATA) && world.getBlockEntity(pos) instanceof InkColorTileEntity)
		{
			ColorUtils.withInkColor(world.getBlockEntity(pos), ColorUtils.getEffectiveColor(stack));
		}
		super.setPlacedBy(world, pos, state, entity, stack);
	}
	@Override
	public boolean phShouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side)
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
	public InkColor getColor(LevelReader world, BlockPos pos)
	{
		if (world.getBlockEntity(pos) instanceof InkColorTileEntity tileEntity)
		{
			return tileEntity.getInkColor();
		}
		return InkColor.INVALID;
	}
	@Override
	public boolean remoteColorChange(Level world, BlockPos pos, InkColor newColor)
	{
		BlockState state = world.getBlockState(pos);
		BlockEntity tileEntity = world.getBlockEntity(pos);
		if (tileEntity instanceof InkColorTileEntity colorTileEntity && colorTileEntity.getInkColor() != newColor)
		{
			colorTileEntity.setColor(newColor);
			world.sendBlockUpdated(pos, state, state, 3);
			state.updateNeighbourShapes(world, pos, 3);
			return true;
		}
		return false;
	}
	@Override
	public boolean remoteInkClear(Level world, BlockPos pos)
	{
		return false;
	}
	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
	{
		return SplatcraftTileEntities.colorTileEntity.get().create(pos, state);
	}
	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType)
	{
		return world.isClientSide() ? null : (tickLevel, pos, tickState, te) -> tick(tickLevel, pos, tickState, (InkColorTileEntity) te);
	}
}
