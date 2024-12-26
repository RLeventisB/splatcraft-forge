package net.splatcraft.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.splatcraft.dummys.ISplatcraftForgeBlockDummy;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.ColoredBarrierTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ColoredBarrierBlock extends StageBarrierBlock implements IColoredBlock, ISplatcraftForgeBlockDummy
{
	public final boolean blocksColor;
	public ColoredBarrierBlock(boolean blocksColor)
	{
		super(false);
		this.blocksColor = blocksColor;
	}
	@Override
	public @Nullable BlockEntity createBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
	{
		return SplatcraftTileEntities.colorBarrierTileEntity.get().instantiate(pos, state);
	}
	@Override
	public boolean setColor(World world, BlockPos pos, InkColor color)
	{
		BlockState state = world.getBlockState(pos);
		if (world.getBlockEntity(pos) instanceof ColoredBarrierTileEntity te)
		{
			te.setColor(color);
			world.updateListeners(pos, state, state, 3);
			state.updateNeighbors(world, pos, 3);
			return true;
		}
		return false;
	}
	@Override
	public InkColor getColor(WorldView world, BlockPos pos)
	{
		if (world.getBlockEntity(pos) instanceof ColoredBarrierTileEntity te)
			return te.getColor();
		return InkColor.INVALID;
	}
	@Override
	public boolean isInverted(World world, BlockPos pos)
	{
		return (world.getBlockEntity(pos) instanceof ColoredBarrierTileEntity te) && te.isInverted();
	}
	@Override
	public void setInverted(World world, BlockPos pos, boolean inverted)
	{
		if (world.getBlockEntity(pos) instanceof ColoredBarrierTileEntity te)
			te.setInverted(inverted);
	}
	@Override
	public ItemStack phGetCloneItemStack(BlockState state, HitResult target, WorldView level, BlockPos pos, PlayerEntity player)
	{
		return ColorUtils.withColorLocked(ColorUtils.withInkColor(super.phGetCloneItemStack(state, target, level, pos, player), getColor(level, pos)), true);
	}
	@Override
	public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockView levelIn, @NotNull BlockPos pos, @NotNull ShapeContext context)
	{
		if (!(context instanceof EntityShapeContext entityContext))
			return super.getCollisionShape(state, levelIn, pos, context);
		
		if (ColorUtils.getEntityColor(entityContext.getEntity()).isValid())
			return !canAllowThrough(pos, entityContext.getEntity()) ? super.getCollisionShape(state, levelIn, pos, context) : VoxelShapes.empty();
		return entityContext.getEntity() == null || blocksColor ? super.getCollisionShape(state, levelIn, pos, context) : VoxelShapes.empty();
	}
	public boolean canAllowThrough(BlockPos pos, Entity entity)
	{
		return blocksColor != ColorUtils.colorEquals(entity, entity.getWorld().getBlockEntity(pos));
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
	public boolean canRemoteColorChange(World world, BlockPos pos, InkColor color, InkColor newColor)
	{
		return IColoredBlock.super.canRemoteColorChange(world, pos, color, newColor);
	}
	@Override
	public boolean remoteColorChange(World world, BlockPos pos, InkColor newColor)
	{
		return setColor(world, pos, newColor);
	}
	@Override
	public boolean remoteInkClear(World world, BlockPos pos)
	{
		return false;
	}
}