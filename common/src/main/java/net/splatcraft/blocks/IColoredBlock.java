package net.splatcraft.blocks;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.BlockInkedResult;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.Nullable;

public interface IColoredBlock
{
    boolean canClimb();

    boolean canSwim();

    boolean canDamage();

    default boolean isInverted(World world, BlockPos pos)
    {
        return (world.getBlockEntity(pos) instanceof InkColorTileEntity colorTileEntity) && colorTileEntity.isInverted();
    }

    default void setInverted(World world, BlockPos pos, boolean inverted)
    {
        if (world.getBlockEntity(pos) instanceof InkColorTileEntity colorTileEntity)
            colorTileEntity.setInverted(inverted);
    }

    default @Nullable InkColor getColor(WorldView world, BlockPos pos)
    {
        return (world.getBlockEntity(pos) instanceof InkColorTileEntity colorTileEntity) ? colorTileEntity.getInkColor() : null;
    }

    default boolean canRemoteColorChange(World world, BlockPos pos, InkColor color, InkColor newColor)
    {
        return color != newColor;
    }

    boolean remoteColorChange(World world, BlockPos pos, InkColor newColor);

    boolean remoteInkClear(World world, BlockPos pos);

    default boolean setColor(World world, BlockPos pos, InkColor color)
    {
        return false;
    }

    default BlockInkedResult inkBlock(World world, BlockPos pos, InkColor color, float damage, InkBlockUtils.InkType inkType)
    {
        return BlockInkedResult.PASS;
    }
}
