package net.splatcraft.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;
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

public class CanvasBlock extends Block implements IColoredBlock, EntityBlock, ISplatcraftForgeBlockDummy
{
	public static final BooleanProperty INKED = BooleanProperty.create("inked");
	public CanvasBlock(String name)
	{
		super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).ignitedByLava().strength(0.8f).sound(SoundType.WOOL));
		SplatcraftBlocks.inkColoredBlocks.add(this);
		registerDefaultState(defaultBlockState().setValue(INKED, false));
	}
	@Nullable
	@Override
	public BlockState getStateForPlacement(@NotNull BlockPlaceContext context)
	{
		return super.getStateForPlacement(context).setValue(INKED, ColorUtils.getEffectiveColor(context.getItemInHand()).isValid());
	}
	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
	{
		InkColorTileEntity te = SplatcraftTileEntities.colorTileEntity.get().create(pos, state);
		if (te != null)
			te.setColor(InkColor.INVALID);
		return te;
	}
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(INKED);
	}
	@Override
	public @NotNull BlockState updateShape(@NotNull BlockState stateIn, @NotNull Direction facing, @NotNull BlockState facingState, @NotNull LevelAccessor world, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos)
	{
		InkColor color = getColor(world, currentPos);
		
		if (InkedBlock.isTouchingLiquid(world, currentPos))
		{
			if (world.getBlockEntity(currentPos) instanceof InkColorTileEntity tileEntity)
				tileEntity.setColor(InkColor.INVALID);
		}
		
		return super.updateShape(stateIn, facing, facingState, world, currentPos, facingPos).setValue(INKED, color.isValid());
	}
	@Override
	public BlockInkedResult inkBlock(Level world, BlockPos pos, InkColor color, float damage, InkBlockUtils.InkType inkType)
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
			world.setBlock(pos, state.setValue(INKED, true), 2);
			world.sendBlockUpdated(pos, state, state.setValue(INKED, true), 2);
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
	public boolean remoteColorChange(Level world, BlockPos pos, InkColor newColor)
	{
		return setColor(world, pos, newColor);
	}
	@Override
	public boolean setColor(Level world, BlockPos pos, InkColor newColor)
	{
		BlockEntity tileEntity = world.getBlockEntity(pos);
		if (tileEntity instanceof InkColorTileEntity colorTile && colorTile.getInkColor() != newColor)
		{
			colorTile.setColor(newColor);
			
			BlockState state = world.getBlockState(pos);
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
	@Override
	public ItemStack phGetCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player)
	{
		ItemStack stack = ISplatcraftForgeBlockDummy.super.phGetCloneItemStack(state, target, level, pos, player);
		if (state.getValue(INKED))
			return ColorUtils.withColorLocked(ColorUtils.withInkColor(stack, getColor(level, pos)), true);
		return stack;
	}
}
