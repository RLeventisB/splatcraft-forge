package net.splatcraft.dummys;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface ISplatcraftForgeBlockDummy
{
	private Block self()
	{
		return (Block) this;
	}
	default boolean phOnDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player, boolean willHarvest, FluidState fluid)
	{
		if (world.isClientSide())
		{
			// On the client, vanilla calls Level#setBlock, per MultiPlayerGameMode#destroyBlock
			return world.setBlock(pos, fluid.createLegacyBlock(), 11);
		}
		else
		{
			// On the server, vanilla calls Level#removeBlock, per ServerPlayerGameMode#destroyBlock
			return world.removeBlock(pos, false);
		}
	}
	@Nullable
	default PushReaction phGetPistonBehavior(@NotNull BlockState state)
	{
		return null;
	}
	default boolean phShouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side)
	{
		return state.isRedstoneConductor(level, pos);
	}
	@Nullable
	default Integer phGetBeaconColorMultiplier(BlockState state, LevelReader level, BlockPos pos, BlockPos beaconPos)
	{
		if (self() instanceof BeaconBeamBlock self)
			return self.getColor().getTextureDiffuseColor();
		return null;
	}
	default Optional<Vec3> phGetRespawnPosition(BlockState state, EntityType<?> type, LevelReader world, BlockPos pos, float orientation)
	{
		return Optional.empty();
	}
	default float phGetExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion)
	{
		return self().getExplosionResistance();
	}
	default boolean phAddLandingEffects(BlockState state1, ServerLevel levelserver, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles)
	{
		return false;
	}
	default boolean phAddHitEffects(BlockState state, Level levelObj, HitResult target, ParticleEngine manager)
	{
		return false;
	}
	default boolean phAddRunningEffects(BlockState state, Level world, BlockPos pos, Entity entity)
	{
		return false;
	}
	default boolean phCanHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player)
	{
		return true;
	}
	default boolean phCollisionExtendsVertically(BlockState state, BlockGetter level, BlockPos pos, Entity collidingEntity)
	{
		return state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS) || self() instanceof FenceGateBlock;
	}
	default boolean phCanConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction)
	{
		if (state.is(Blocks.REDSTONE_WIRE))
		{
			return true;
		}
		else if (state.is(Blocks.REPEATER))
		{
			Direction facing = state.getValue(RepeaterBlock.FACING);
			return facing == direction || facing.getOpposite() == direction;
		}
		else if (state.is(Blocks.OBSERVER))
		{
			return direction == state.getValue(ObserverBlock.FACING);
		}
		else
		{
			return state.isSignalSource() && direction != null;
		}
	}
	default ItemStack phGetCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player)
	{
		return self().getCloneItemStack(level, pos, state);
	}
}
