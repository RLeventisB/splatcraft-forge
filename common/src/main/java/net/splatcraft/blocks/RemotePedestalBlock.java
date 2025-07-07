package net.splatcraft.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.dummys.ISplatcraftForgeBlockDummy;
import net.splatcraft.items.IColoredItem;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.RemotePedestalTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemotePedestalBlock extends Block implements IColoredBlock, EntityBlock, ISplatcraftForgeBlockDummy
{
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	private static final VoxelShape SHAPE = Shapes.or(
		box(3, 0, 3, 13, 2, 13),
		box(4, 2, 4, 12, 3, 12),
		box(5, 3, 5, 11, 11, 11),
		box(4, 11, 4, 12, 13, 12)
	);
	public RemotePedestalBlock()
	{
		super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).requiresCorrectToolForDrops());
		SplatcraftBlocks.inkColoredBlocks.add(this);
		registerDefaultState(defaultBlockState().setValue(POWERED, false));
	}
	@Override
	public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter levelIn, @NotNull BlockPos pos, @NotNull CollisionContext context)
	{
		return SHAPE;
	}
	@Override
	public ItemStack phGetCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player)
	{
		return ColorUtils.withColorLocked(ColorUtils.withInkColor(ISplatcraftForgeBlockDummy.super.phGetCloneItemStack(state, target, level, pos, player), getColor(level, pos)), true);
	}
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> container)
	{
		container.add(POWERED);
	}
	@Override
	public @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level world, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult rayTrace)
	{
		if (world.getBlockEntity(pos) instanceof RemotePedestalTileEntity te)
		{
			if (te.isEmpty() && player.getItemInHand(player.getUsedItemHand()).is(SplatcraftTags.Items.REMOTES))
			{
				te.setItem(0, player.getItemInHand(player.getUsedItemHand()).copy());
				player.getItemInHand(player.getUsedItemHand()).setCount(0);
				return InteractionResult.sidedSuccess(world.isClientSide);
			}
			else if (!te.isEmpty())
			{
				ItemStack remote = te.removeItemNoUpdate(0);
				if (!player.addItem(remote))
					CommonUtils.spawnItem(world, pos.above(), remote);
				return InteractionResult.sidedSuccess(world.isClientSide);
			}
		}

		return super.useWithoutItem(state, world, pos, player, rayTrace);
	}
	@Override
	public void neighborChanged(BlockState state, Level levelIn, @NotNull BlockPos pos, @NotNull Block blockIn, @NotNull BlockPos fromPos, boolean isMoving)
	{
		boolean isPowered = levelIn.hasNeighborSignal(pos);

		if (isPowered != state.getValue(POWERED))
		{
			if (isPowered && levelIn.getBlockEntity(pos) instanceof RemotePedestalTileEntity tileEntity)
			{
				tileEntity.onPowered();
			}

			levelIn.setBlock(pos, state.setValue(POWERED, isPowered), 3);
			updateColor(levelIn, pos, pos.below());
		}
	}
	@Override
	public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity entity, @NotNull ItemStack stack)
	{
		super.setPlacedBy(world, pos, state, entity, stack);
		updateColor(world, pos, pos.below());
	}
	@Override
	public @NotNull BlockState updateShape(@NotNull BlockState stateIn, Direction facing, @NotNull BlockState facingState, @NotNull LevelAccessor levelIn, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos)
	{
		if (facing.equals(Direction.DOWN) && levelIn instanceof Level world)
			updateColor(world, currentPos, facingPos);
		return stateIn;
	}
	public void updateColor(Level levelIn, BlockPos currentPos, BlockPos facingPos)
	{
		if (levelIn.getBlockState(facingPos).getBlock() instanceof InkwellBlock)
			setColor(levelIn, currentPos, ColorUtils.getEffectiveColor(levelIn, facingPos));
	}
	@Override
	public boolean hasAnalogOutputSignal(@NotNull BlockState p_149740_1_)
	{
		return true;
	}
	@Override
	public int getAnalogOutputSignal(BlockState state, @NotNull Level world, @NotNull BlockPos pos)
	{
		if (state.getValue(POWERED) && world.getBlockEntity(pos) instanceof RemotePedestalTileEntity te)
			return te.getSignal();

		return 0;
	}
	@Override
	public boolean phCanConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction side)
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
		return false;
	}
	@Override
	public boolean canDamage()
	{
		return false;
	}
	@Override
	public boolean canRemoteColorChange(Level world, BlockPos pos, InkColor color, InkColor newColor)
	{
		RemotePedestalTileEntity te = (RemotePedestalTileEntity) world.getBlockEntity(pos);
		if (!te.isEmpty() && te.getItem(0).getItem() instanceof IColoredItem)
			return ColorUtils.getInkColor(te.getItem(0)) != newColor;
		return false;
	}
	@Override
	public boolean remoteColorChange(Level world, BlockPos pos, InkColor newColor)
	{
		if (world.getBlockEntity(pos) instanceof RemotePedestalTileEntity te)
		{
			if (!te.isEmpty() && te.getItem(0).getItem() instanceof IColoredItem)
			{
				ItemStack stack = te.getItem(0);
				ColorUtils.withColorLocked(stack, true);
				if (ColorUtils.getInkColor(stack) != newColor)
				{
					ColorUtils.withInkColor(stack, newColor);
					world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
					return true;
				}
			}
		}

		return false;
	}
	@Override
	public boolean setColor(Level world, BlockPos pos, InkColor color)
	{
		if (!(world.getBlockEntity(pos) instanceof RemotePedestalTileEntity te))
			return false;
		te.setColor(color);
		world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
		return true;
	}
	@Override
	public boolean remoteInkClear(Level world, BlockPos pos)
	{
		return false;
	}
	@Override
	public void onRemove(BlockState state, @NotNull Level world, @NotNull BlockPos pos, BlockState newState, boolean isMoving)
	{
		if (!state.is(newState.getBlock()))
		{
			if (world.getBlockEntity(pos) instanceof RemotePedestalTileEntity)
			{
				Containers.dropContentsOnDestroy(state, newState, world, pos);
				world.blockUpdated(pos, this);
			}

			super.onRemove(state, world, pos, newState, isMoving);
		}
	}
	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
	{
		return SplatcraftTileEntities.remotePedestalTileEntity.get().create(pos, state);
	}
}
