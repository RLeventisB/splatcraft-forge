package net.splatcraft.blocks;

import net.minecraft.block.*;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;

public class InkedCarpetBlock extends InkStainedBlock
{
    protected static final VoxelShape SHAPE = createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public InkedCarpetBlock(String name)
    {
        super(AbstractBlock.Settings.create().solidBlock((state, getter, pos) -> false).mapColor(MapColor.WHITE_GRAY).burnable().strength(0.1F).sounds(BlockSoundGroup.WOOL));
    }

    @Override
    public VoxelShape getOutlineShape(@NotNull BlockState st, @NotNull BlockView level, @NotNull BlockPos pos, @NotNull ShapeContext context)
    {
        return SHAPE;
    }

    public @NotNull BlockState getStateForNeighborUpdate(BlockState state, @NotNull Direction direction, @NotNull BlockState facingState, @NotNull WorldAccess level, @NotNull BlockPos pos, @NotNull BlockPos facingPos)
    {
        return !state.canPlaceAt(level, pos) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(state, direction, facingState, level, pos, facingPos);
    }

    public boolean isValidPosition(BlockState p_196260_1_, WorldView p_196260_2_, BlockPos p_196260_3_)
    {
        return !p_196260_2_.isAir(p_196260_3_.down());
    }
}
