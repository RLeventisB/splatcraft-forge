package net.splatcraft.forge.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.forge.registries.SplatcraftBlocks;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public class InkedCarpetBlock extends InkStainedBlock
{
    public InkedCarpetBlock()
    {
        super(Properties.of().isRedstoneConductor(SplatcraftBlocks::noRedstoneConduct).mapColor(MapColor.WOOL).ignitedByLava().strength(0.1F).sound(SoundType.WOOL));
    }

    protected static final VoxelShape SHAPE = box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public @NotNull VoxelShape getShape(@NotNull BlockState st, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        return SHAPE;
    }

    public @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState facingState, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos facingPos)
    {
        return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, facingState, level, pos, facingPos);
    }
}