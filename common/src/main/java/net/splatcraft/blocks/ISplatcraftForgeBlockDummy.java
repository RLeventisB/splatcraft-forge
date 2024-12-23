package net.splatcraft.blocks;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface ISplatcraftForgeBlockDummy
{
	private BlockEntity self()
	{
		return (BlockEntity) this;
	}
	default boolean onDestroyedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, FluidState fluid)
	{
		return true;
	}
	default @NotNull PistonBehavior getPistonBehavior(@NotNull BlockState state)
	{
		return PistonBehavior.NORMAL;
	}
	// todo: link these to forge
	default boolean shouldCheckWeakPower(RedstoneView level, BlockPos pos, Direction side)
	{
		return true;
	}
	default float[] getBeaconColorMultiplier(BlockState state, WorldAccess level, BlockPos pos, BlockPos beaconPos)
	{
		return new float[0];
	}
	default Optional<Vec3d> getRespawnPosition(BlockState state, EntityType<?> type, WorldView world, BlockPos pos, float orientation, @Nullable LivingEntity entity)
	{
		return Optional.empty();
	}
	default float getExplosionResistance(BlockState state, BlockView level, BlockPos pos, Explosion explosion)
	{
		return 0f;
	}
	default boolean addLandingEffects(BlockState state1, ServerWorld levelserver, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles)
	{
		return true;
	}
	default boolean addRunningEffects(BlockState state, World world, BlockPos pos, Entity entity)
	{
		return true;
	}
	default boolean canHarvestBlock(BlockState state, BlockView level, BlockPos pos, PlayerEntity player)
	{
		return true;
	}
	default boolean collisionExtendsVertically(BlockState state, BlockView level, BlockPos pos, Entity collidingEntity)
	{
		return false;
	}
	default ItemStack getPickStack(WorldView level, BlockPos pos, PlayerEntity player, boolean includeData, BlockState state)
	{
		return state.getBlock().getPickStack(level, pos, state); // idk man..
	}
	/**
	 * Whether redstone dust should visually connect to this block on a given side
	 * <p>
	 * The default implementation is identical to
	 * {@code RedStoneWireBlock#shouldConnectTo(BlockState, Direction)}
	 *
	 * <p>
	 * {@link RedstoneWireBlock} updates its visual connection when
	 * {@link BlockState#getStateForNeighborUpdate(Direction, BlockState, WorldAccess, BlockPos, BlockPos)}
	 * is called, this callback is used during the evaluation of its new shape.
	 *
	 * @param state     The current state
	 * @param level     The level
	 * @param pos       The block position in level
	 * @param direction The coming direction of the redstone dust connection (with respect to the block at pos)
	 * @return True if redstone dust should visually connect on the side passed
	 * <p>
	 * If the return value is evaluated based on level and pos (e.g. from BlockEntity), then the implementation of
	 * this block should notify its neighbors to update their shapes when necessary. Consider using
	 * {@link BlockState#updateNeighbors(WorldAccess, BlockPos, int, int)} or
	 * {@link BlockState#getStateForNeighborUpdate(Direction, BlockState, WorldAccess, BlockPos, BlockPos)}}.
	 * <p>
	 * Example:
	 * <p>
	 * 1. {@code yourBlockState.updateNeighbourShapes(level, yourBlockPos, UPDATE_ALL);}
	 * <p>
	 * 2. {@code neighborState.updateShape(fromDirection, stateOfYourBlock, level, neighborBlockPos, yourBlockPos)},
	 * where {@code fromDirection} is defined from the neighbor block's point of view.
	 */
	default boolean canConnectRedstone(BlockState state, BlockView level, BlockPos pos, @Nullable Direction direction)
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
	default void onDataPacket(ClientConnection net, BlockEntityUpdateS2CPacket pkt, RegistryWrapper.WrapperLookup lookupProvider)
	{
		NbtCompound compoundtag = pkt.getNbt();
		if (!compoundtag.isEmpty())
		{
			self().read(compoundtag, lookupProvider);
		}
	}
	default void handleUpdateTag(NbtCompound tag, RegistryWrapper.WrapperLookup lookupProvider)
	{
		self().read(tag, lookupProvider);
	}
}
