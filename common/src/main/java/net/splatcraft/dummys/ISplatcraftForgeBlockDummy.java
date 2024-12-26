package net.splatcraft.dummys;

import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface ISplatcraftForgeBlockDummy
{
	private Block self()
	{
		return (Block) this;
	}
	default boolean phOnDestroyedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, FluidState fluid)
	{
		return true;
	}
	default @NotNull PistonBehavior phGetPistonBehavior(@NotNull BlockState state)
	{
		return PistonBehavior.NORMAL;
	}
	// todo: link these to forge
	default boolean phShouldCheckWeakPower(BlockState state, RedstoneView level, BlockPos pos, Direction side)
	{
		return true;
	}
	default Integer phGetBeaconColorMultiplier(BlockState state, WorldView level, BlockPos pos, BlockPos beaconPos)
	{
		return null;
	}
	default Optional<Vec3d> phGetRespawnPosition(BlockState state, EntityType<?> type, WorldView world, BlockPos pos, float orientation)
	{
		return Optional.empty();
	}
	default float phGetExplosionResistance(BlockState state, BlockView level, BlockPos pos, Explosion explosion)
	{
		return 0f;
	}
	default boolean phAddLandingEffects(BlockState state1, ServerWorld levelserver, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles)
	{
		return false;
	}
	default boolean phAddHitEffects(BlockState state, World levelObj, HitResult target, ParticleManager manager)
	{
		return false;
	}
	default boolean phAddRunningEffects(BlockState state, World world, BlockPos pos, Entity entity)
	{
		return false;
	}
	default boolean phCanHarvestBlock(BlockState state, BlockView level, BlockPos pos, PlayerEntity player)
	{
		return true;
	}
	default boolean phCollisionExtendsVertically(BlockState state, BlockView level, BlockPos pos, Entity collidingEntity)
	{
		return false;
	}
	default boolean phCanConnectRedstone(BlockState state, BlockView level, BlockPos pos, @Nullable Direction direction)
	{
		if (state.isOf(Blocks.REDSTONE_WIRE))
		{
			return true;
		}
		else if (state.isOf(Blocks.REPEATER))
		{
			Direction facing = state.get(RepeaterBlock.FACING);
			return facing == direction || facing.getOpposite() == direction;
		}
		else if (state.isOf(Blocks.OBSERVER))
		{
			return direction == state.get(ObserverBlock.FACING);
		}
		else
		{
			return state.emitsRedstonePower() && direction != null;
		}
	}
	default ItemStack phGetCloneItemStack(BlockState state, HitResult target, WorldView level, BlockPos pos, PlayerEntity player)
	{
		return self().getPickStack(level, pos, state);
	}
}
