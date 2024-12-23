package net.splatcraft.blocks;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.splatcraft.dummys.ISplatcraftForgeBlockDummy;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.BlockInkedResult;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CanvasBlock extends Block implements IColoredBlock, BlockEntityProvider, ISplatcraftForgeBlockDummy
{
	public static final BooleanProperty INKED = BooleanProperty.of("inked");
	public CanvasBlock(String name)
	{
		super(AbstractBlock.Settings.create().mapColor(MapColor.WHITE_GRAY).burnable().strength(0.8f).sounds(BlockSoundGroup.WOOL));
		SplatcraftBlocks.inkColoredBlocks.add(this);
		setDefaultState(getDefaultState().with(INKED, false));
	}
	@Nullable
	@Override
	public BlockState getPlacementState(@NotNull ItemPlacementContext context)
	{
		return super.getPlacementState(context).with(INKED, ColorUtils.getInkColor(context.getStack()).isValid());
	}
	@Nullable
	@Override
	public BlockEntity createBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
	{
		InkColorTileEntity te = SplatcraftTileEntities.colorTileEntity.get().instantiate(pos, state);
		if (te != null)
			te.setColor(InkColor.INVALID);
		return te;
	}
	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(INKED);
	}
	@Override
	public @NotNull BlockState getStateForNeighborUpdate(@NotNull BlockState stateIn, @NotNull Direction facing, @NotNull BlockState facingState, @NotNull WorldAccess world, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos)
	{
		InkColor color = getColor(world, currentPos);
		
		if (InkedBlock.isTouchingLiquid(world, currentPos))
		{
			if (world.getBlockEntity(currentPos) instanceof InkColorTileEntity tileEntity)
				tileEntity.setColor(InkColor.INVALID);
		}
		
		return super.getStateForNeighborUpdate(stateIn, facing, facingState, world, currentPos, facingPos).with(INKED, color.isValid());
	}
	@Override
	public BlockInkedResult inkBlock(World world, BlockPos pos, InkColor color, float damage, InkBlockUtils.InkType inkType)
	{
		if (InkedBlock.isTouchingLiquid(world, pos))
			return BlockInkedResult.FAIL;
		
		if (color == getColor(world, pos))
			return BlockInkedResult.ALREADY_INKED;
		
		BlockEntity tileEntity = world.getBlockEntity(pos);
		if (tileEntity instanceof InkColorTileEntity colorTileEntity)
		{
			BlockState state = world.getBlockState(pos);
			colorTileEntity.setColor(color);
			world.setBlockState(pos, state.with(INKED, true), 2);
			world.updateListeners(pos, state, state.with(INKED, true), 2);
			return BlockInkedResult.SUCCESS;
		}
		
		return BlockInkedResult.FAIL;
	}
	@Override
	public boolean canClimb()
	{
		return true;
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
		return setColor(world, pos, newColor);
	}
	@Override
	public boolean setColor(World world, BlockPos pos, InkColor newColor)
	{
		BlockEntity tileEntity = world.getBlockEntity(pos);
		if (tileEntity instanceof InkColorTileEntity colorTile && colorTile.getInkColor() != newColor)
		{
			colorTile.setColor(newColor);
			
			BlockState state = world.getBlockState(pos);
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
	@Override
	public ItemStack phGetCloneItemStack(BlockState state, HitResult target, WorldView level, BlockPos pos, PlayerEntity player)
	{
		ItemStack stack = ISplatcraftForgeBlockDummy.super.phGetCloneItemStack(state, target, level, pos, player);
		if (state.get(INKED))
			return ColorUtils.withColorLocked(ColorUtils.withInkColor(stack, getColor(level, pos)), true);
		return stack;
	}
}
