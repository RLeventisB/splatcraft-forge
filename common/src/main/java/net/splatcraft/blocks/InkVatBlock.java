package net.splatcraft.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.InkVatTileEntity;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkVatBlock extends BaseEntityBlock implements IColoredBlock
{
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final MapCodec<InkVatBlock> CODEC = simpleCodec(InkVatBlock::new);
	public InkVatBlock(Properties setting)
	{
		super(setting);
		SplatcraftBlocks.inkColoredBlocks.add(this);

		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false).setValue(POWERED, false));
	}
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(FACING, ACTIVE, POWERED);
	}
	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}
	@Override
	public @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, Level levelIn, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand handIn, @NotNull BlockHitResult hit)
	{
		if (levelIn.isClientSide)
		{
			return ItemInteractionResult.SUCCESS;
		}

		if (levelIn.getBlockEntity(pos) instanceof InkVatTileEntity inkVatTile && player instanceof ServerPlayer serverPlayer)
		{
			serverPlayer.openMenu(inkVatTile);
			return ItemInteractionResult.SUCCESS;
		}

		return ItemInteractionResult.FAIL;
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
	public boolean remoteInkClear(Level world, BlockPos pos)
	{
		return false;
	}
	@Override
	public InkColor getColor(LevelReader world, BlockPos pos)
	{
		if (world.getBlockEntity(pos) instanceof InkVatTileEntity tileEntity)
		{
			return tileEntity.getColor();
		}
		return InkColor.INVALID;
	}
	@Override
	public boolean setColor(Level world, BlockPos pos, InkColor color)
	{
		if (!(world.getBlockEntity(pos) instanceof InkVatTileEntity tileEntity))
		{
			return false;
		}
		tileEntity.setColor(color);
		world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
		return true;
	}
	@Override
	protected @NotNull MapCodec<? extends BaseEntityBlock> codec()
	{
		return null;
	}
	@Override
	public @NotNull RenderShape getRenderShape(@NotNull BlockState state)
	{
		return RenderShape.MODEL;
	}
	@Override
	public @NotNull BlockState rotate(BlockState state, Rotation rot)
	{
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}
	@Override
	public @NotNull BlockState mirror(BlockState state, Mirror mirrorIn)
	{
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}
	@Override
	public void setPlacedBy(@NotNull Level levelIn, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, ItemStack stack)
	{
		if (stack.has(DataComponents.CUSTOM_NAME))
		{
			BlockEntity tileentity = levelIn.getBlockEntity(pos);
			if (tileentity instanceof InkVatTileEntity inkVatTile)
			{
				inkVatTile.setComponents(DataComponentMap.builder().set(DataComponents.CUSTOM_NAME, stack.get(DataComponents.CUSTOM_NAME)).build());
			}
		}
	}
	@Override
	public void onRemove(BlockState state, @NotNull Level world, @NotNull BlockPos pos, BlockState newState, boolean isMoving)
	{
		if (!state.is(newState.getBlock()))
		{
			BlockEntity tileentity = world.getBlockEntity(pos);
			if (tileentity instanceof InkVatTileEntity)
			{
				Containers.dropContentsOnDestroy(state, newState, world, pos);
				world.updateNeighbourForOutputSignal(pos, this);
			}

			super.onRemove(state, world, pos, newState, isMoving);
		}
	}
	@Override
	public boolean hasAnalogOutputSignal(@NotNull BlockState state)
	{
		return true;
	}
	@Override
	public int getAnalogOutputSignal(@NotNull BlockState blockState, Level world, @NotNull BlockPos pos)
	{
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(world.getBlockEntity(pos));
	}
	@Override
	public void neighborChanged(BlockState state, Level world, @NotNull BlockPos pos, @NotNull Block blockIn, @NotNull BlockPos fromPos, boolean isMoving)
	{
		boolean isPowered = world.hasNeighborSignal(pos);
		if (isPowered != state.getValue(POWERED))
		{
			if (isPowered && world.getBlockEntity(pos) instanceof InkVatTileEntity tileEntity)
			{
				tileEntity.onRedstonePulse();
			}

			world.setBlock(pos, state.setValue(POWERED, isPowered), 3);
		}
	}
	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
	{
		return SplatcraftTileEntities.inkVatTileEntity.get().create(pos, state);
	}
	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level world, @NotNull BlockState state, @NotNull BlockEntityType<T> type)
	{
		return createTickerHelper(type, SplatcraftTileEntities.inkVatTileEntity.get(), InkVatTileEntity::tick);
	}
}
