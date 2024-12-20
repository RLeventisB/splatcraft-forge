package net.splatcraft.blocks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.StageBarrierTileEntity;
import net.splatcraft.util.ClientUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StageBarrierBlock extends Block implements BlockEntityProvider
{
    public static final VoxelShape COLLISION = createCuboidShape(0.0, 0.01, 0.0, 16, 15.99, 16);
    public final boolean damagesPlayer;
    //todo: oops! all forge extensions
    /*@Override
    public boolean addLandingEffects(BlockState state1, ServerWorld levelserver, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles)
    {
        return true;
    }*.

    /* ???
    @Override
    public boolean addHitEffects(BlockState state, Level levelObj, HitResult target, ParticleManager manager)
    {
        return true;
    }

    @Override
    public boolean addRunningEffects(BlockState state, World world, BlockPos pos, Entity entity)
    {
        return true;
    }
*/

    public StageBarrierBlock(boolean damagesPlayer)
    {
        super(AbstractBlock.Settings.create().pistonBehavior(PistonBehavior.BLOCK).strength(-1.0F, 3600000.8F).nonOpaque());
        this.damagesPlayer = damagesPlayer;
    }

    @Override
    public VoxelShape getOutlineShape(@NotNull BlockState state, @NotNull BlockView levelIn, @NotNull BlockPos pos, @NotNull ShapeContext context)
    {
        if (ClientUtils.getClientPlayer().isCreative() || !(levelIn.getBlockEntity(pos) instanceof StageBarrierTileEntity te))
        {
            return VoxelShapes.fullCube();
        }

        return te.getActiveTime() > 5 ? super.getOutlineShape(state, levelIn, pos, context) : VoxelShapes.empty();
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockView levelIn, @NotNull BlockPos pos, @NotNull ShapeContext context)
    {
        return COLLISION;
    }

    @Override
    public @NotNull BlockRenderType getRenderType(@NotNull BlockState state)
    {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean isTransparent(@NotNull BlockState state, @NotNull BlockView reader, @NotNull BlockPos pos)
    {
        return true;
    }

    @Environment(EnvType.CLIENT)
    public float getAmbientOcclusionLightLevel(@NotNull BlockState p_220080_1_, @NotNull BlockView p_220080_2_, @NotNull BlockPos p_220080_3_)
    {
        return 1.0F;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return SplatcraftTileEntities.stageBarrierTileEntity.get().instantiate(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull World world, @NotNull BlockState state, @NotNull BlockEntityType<T> type)
    {
        return (level, pos, state1, blockEntity) -> ((StageBarrierTileEntity) blockEntity).tick();
    }
}
