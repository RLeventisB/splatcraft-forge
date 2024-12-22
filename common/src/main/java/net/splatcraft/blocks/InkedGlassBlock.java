package net.splatcraft.blocks;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.TranslucentBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkedGlassBlock extends TranslucentBlock implements IColoredBlock, BlockEntityProvider
{
    public InkedGlassBlock(String name)
    {
        super(AbstractBlock.Settings.create().instrument(NoteBlockInstrument.HAT).strength(0.3F).sounds(BlockSoundGroup.GLASS).nonOpaque()
            .allowsSpawning((state, level, pos, entity) -> false)
            .solidBlock((state, level, pos) -> false)
            .suffocates((state, level, pos) -> false)
            .blockVision((state, level, pos) -> false));
        SplatcraftBlocks.inkColoredBlocks.add(this);
    }

    @Override
    public boolean hasSidedTransparency(@NotNull BlockState p_220074_1_)
    {
        return true;
    }

    // todo: me haed hurs
    /*@Override
    public float[] getBeaconColorMultiplier(BlockState state, world level, BlockPos pos, BlockPos beaconPos)
    {
        return ColorUtils.hexToRGB(getColor((Level) level, pos));
    }*/

    //  why ARE THERE TWOOO???????????????? IT'S 4 AM I CANT UNDERSTAND THIS
    /*@Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state)
    {
        return ColorUtils.setColorLocked(ColorUtils.setInkColor(super.getPickStack(world, pos, state), getColor((World) world, pos)), true);
    }*/

    @Override
    public void onPlaced(@NotNull World world, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity entity, ItemStack stack)
    {
        if (stack.contains(SplatcraftComponents.ITEM_COLOR_DATA) && world.getBlockEntity(pos) instanceof InkColorTileEntity)
        {
            ColorUtils.setInkColor(world.getBlockEntity(pos), ColorUtils.getInkColor(stack));
        }
        super.onPlaced(world, pos, state, entity, stack);
    }

    @Override
    public @NotNull ItemStack getPickStack(@NotNull WorldView reader, @NotNull BlockPos pos, @NotNull BlockState state)
    {
        ItemStack stack = super.getPickStack(reader, pos, state);

        if (reader.getBlockEntity(pos) instanceof InkColorTileEntity)
            ColorUtils.setColorLocked(ColorUtils.setInkColor(stack, ColorUtils.getInkColor(reader.getBlockEntity(pos))), true);

        return stack;
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
    public InkColor getColor(WorldView world, BlockPos pos)
    {
        if (world.getBlockEntity(pos) instanceof InkColorTileEntity blockEntity)
        {
            return blockEntity.getInkColor();
        }
        return InkColor.INVALID;
    }

    @Override
    public boolean remoteColorChange(World world, BlockPos pos, InkColor newColor)
    {
        BlockState state = world.getBlockState(pos);
        if (world.getBlockEntity(pos) instanceof InkColorTileEntity blockEntity && blockEntity.getInkColor() != newColor)
        {
            blockEntity.setColor(newColor);
            world.updateListeners(pos, state, state, 2);
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
}