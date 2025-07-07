package net.splatcraft.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.structs.BlockInkedResult;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.Nullable;

public interface IColoredBlock
{
	boolean canClimb();
	boolean canSwim();
	boolean canDamage();
	default boolean isInverted(Level world, BlockPos pos)
	{
		return (world.getBlockEntity(pos) instanceof InkColorTileEntity colorTileEntity) && colorTileEntity.isInverted();
	}
	default void setInverted(Level world, BlockPos pos, boolean inverted)
	{
		if (world.getBlockEntity(pos) instanceof InkColorTileEntity colorTileEntity)
			colorTileEntity.setInverted(inverted);
	}
	default @Nullable InkColor getColor(LevelReader world, BlockPos pos)
	{
		return (world.getBlockEntity(pos) instanceof InkColorTileEntity colorTileEntity) ? colorTileEntity.getInkColor() : InkColor.INVALID;
	}
	default boolean canRemoteColorChange(Level world, BlockPos pos, InkColor color, InkColor newColor)
	{
		return color != newColor;
	}
	boolean remoteColorChange(Level world, BlockPos pos, InkColor newColor);
	boolean remoteInkClear(Level world, BlockPos pos);
	default boolean setColor(Level world, BlockPos pos, InkColor color)
	{
		return false;
	}
	default BlockInkedResult inkBlock(Level world, BlockPos pos, InkColor color, float damage, InkBlockUtils.InkType inkType)
	{
		return BlockInkedResult.PASS;
	}
}
