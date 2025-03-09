package net.splatcraft.blocks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.dummys.ISplatcraftForgeBlockDummy;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.StageBarrierTileEntity;
import net.splatcraft.util.ClientUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StageBarrierBlock extends Block implements EntityBlock, ISplatcraftForgeBlockDummy
{
	public static final VoxelShape COLLISION = box(0.0, 0.01, 0.0, 16, 15.99, 16);
	public final boolean damagesPlayer;
	public StageBarrierBlock(boolean damagesPlayer)
	{
		super(BlockBehaviour.Properties.of().pushReaction(PushReaction.BLOCK).strength(-1.0F, 3600000.8F).noOcclusion());
		this.damagesPlayer = damagesPlayer;
	}
	@Override
	public boolean phAddLandingEffects(BlockState state1, ServerLevel levelserver, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles)
	{
		return true;
	}
	@Override
	public boolean phAddHitEffects(BlockState state, Level levelObj, HitResult target, ParticleEngine manager)
	{
		return true;
	}
	@Override
	public boolean phAddRunningEffects(BlockState state, Level world, BlockPos pos, Entity entity)
	{
		return true;
	}
	@Override
	public VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter levelIn, @NotNull BlockPos pos, @NotNull CollisionContext context)
	{
		if (ClientUtils.getClientPlayer().isCreative() || !(levelIn.getBlockEntity(pos) instanceof StageBarrierTileEntity te))
		{
			return Shapes.block();
		}
		
		return te.getActiveTime() > 5 ? super.getShape(state, levelIn, pos, context) : Shapes.empty();
	}
	@Override
	public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter levelIn, @NotNull BlockPos pos, @NotNull CollisionContext context)
	{
		return COLLISION;
	}
	@Override
	public @NotNull RenderShape getRenderShape(@NotNull BlockState state)
	{
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}
	@Override
	public boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter reader, @NotNull BlockPos pos)
	{
		return true;
	}
	@Environment(EnvType.CLIENT)
	public float getShadeBrightness(@NotNull BlockState p_220080_1_, @NotNull BlockGetter p_220080_2_, @NotNull BlockPos p_220080_3_)
	{
		return 1.0F;
	}
	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
	{
		return SplatcraftTileEntities.stageBarrierTileEntity.get().create(pos, state);
	}
	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level world, @NotNull BlockState state, @NotNull BlockEntityType<T> type)
	{
		return (level, pos, state1, blockEntity) -> ((StageBarrierTileEntity) blockEntity).tick();
	}
}
