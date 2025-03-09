package net.splatcraft.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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
	public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
	{
		return SplatcraftTileEntities.colorBarrierTileEntity.get().create(pos, state);
	}
	@Override
	public boolean setColor(Level world, BlockPos pos, InkColor color)
	{
		BlockState state = world.getBlockState(pos);
		if (world.getBlockEntity(pos) instanceof ColoredBarrierTileEntity te)
		{
			te.setColor(color);
			world.sendBlockUpdated(pos, state, state, 3);
			state.updateNeighbourShapes(world, pos, 3);
			return true;
		}
		return false;
	}
	@Override
	public InkColor getColor(LevelReader world, BlockPos pos)
	{
		if (world.getBlockEntity(pos) instanceof ColoredBarrierTileEntity te)
			return te.getColor();
		return InkColor.INVALID;
	}
	@Override
	public boolean isInverted(Level world, BlockPos pos)
	{
		return (world.getBlockEntity(pos) instanceof ColoredBarrierTileEntity te) && te.isInverted();
	}
	@Override
	public void setInverted(Level world, BlockPos pos, boolean inverted)
	{
		if (world.getBlockEntity(pos) instanceof ColoredBarrierTileEntity te)
			te.setInverted(inverted);
	}
	@Override
	public ItemStack phGetCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player)
	{
		return ColorUtils.withColorLocked(ColorUtils.withInkColor(super.phGetCloneItemStack(state, target, level, pos, player), getColor(level, pos)), true);
	}
	@Override
	public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter levelIn, @NotNull BlockPos pos, @NotNull CollisionContext context)
	{
		if (!(context instanceof EntityCollisionContext entityContext))
			return super.getCollisionShape(state, levelIn, pos, context);
		
		if (ColorUtils.getEntityColor(entityContext.getEntity()).isValid())
			return !canAllowThrough(pos, entityContext.getEntity()) ? super.getCollisionShape(state, levelIn, pos, context) : Shapes.empty();
		return entityContext.getEntity() == null || blocksColor ? super.getCollisionShape(state, levelIn, pos, context) : Shapes.empty();
	}
	public boolean canAllowThrough(BlockPos pos, Entity entity)
	{
		return blocksColor != ColorUtils.colorEquals(entity, entity.level().getBlockEntity(pos));
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
		return IColoredBlock.super.canRemoteColorChange(world, pos, color, newColor);
	}
	@Override
	public boolean remoteColorChange(Level world, BlockPos pos, InkColor newColor)
	{
		return setColor(world, pos, newColor);
	}
	@Override
	public boolean remoteInkClear(Level world, BlockPos pos)
	{
		return false;
	}
}