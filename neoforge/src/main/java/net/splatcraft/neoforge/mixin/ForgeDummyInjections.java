package net.splatcraft.neoforge.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
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
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import net.splatcraft.dummys.ISplatcraftForgeBlockDummy;
import net.splatcraft.dummys.ISplatcraftForgeBlockEntityDummy;
import net.splatcraft.dummys.ISplatcraftForgeItemDummy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

public class ForgeDummyInjections
{
	// ps: do NOT. put the dummy method's name the same as the extension, or else an stackoverflow exception will occur!!! idk why!!!
	// oh yeah ph stands for placeholder
	@Mixin(ISplatcraftForgeItemDummy.class)
	public interface ItemMixin extends IItemExtension
	{
		@Override
		default boolean onEntityItemUpdate(@NotNull ItemStack stack, @NotNull ItemEntity entity)
		{
			return ((ISplatcraftForgeItemDummy) this).phOnEntityItemUpdate(stack, entity);
		}
		@Override
		default boolean isRepairable(@NotNull ItemStack stack)
		{
			return ((ISplatcraftForgeItemDummy) this).phIsRepairable(stack);
		}
		@Override
		default int getMaxStackSize(@NotNull ItemStack stack)
		{
			return ((ISplatcraftForgeItemDummy) this).phGetMaxStackSize(stack);
		}
		@Override
		default boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged)
		{
			return ((ISplatcraftForgeItemDummy) this).phShouldCauseReequipAnimation(oldStack, newStack, slotChanged);
		}
	}
	@Mixin(ISplatcraftForgeBlockDummy.class)
	public interface ClientBlockMixin extends IClientBlockExtensions
	{
		@Override
		default boolean addHitEffects(@NotNull BlockState state, @NotNull World level, @NotNull HitResult target, @NotNull ParticleManager manager)
		{
			return ((ISplatcraftForgeBlockDummy) this).phAddHitEffects(state, level, target, manager);
		}
	}
	@Mixin(ISplatcraftForgeBlockDummy.class)
	public interface BlockMixin extends IBlockExtension
	{
		@Override
		default boolean canHarvestBlock(@NotNull BlockState state, @NotNull BlockView level, @NotNull BlockPos pos, @NotNull PlayerEntity player)
		{
			return ((ISplatcraftForgeBlockDummy) this).phCanHarvestBlock(state, level, pos, player);
		}
		@Override
		default boolean onDestroyedByPlayer(@NotNull BlockState state, @NotNull World level, @NotNull BlockPos pos, @NotNull PlayerEntity player, boolean willHarvest, @NotNull FluidState fluid)
		{
			return ((ISplatcraftForgeBlockDummy) this).phOnDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
		}
		@Override
		default @NotNull Optional<ServerPlayerEntity.RespawnPos> getRespawnPosition(@NotNull BlockState state, @NotNull EntityType<?> type, @NotNull WorldView levelReader, @NotNull BlockPos pos, float orientation)
		{
			Optional<Vec3d> respawnPosition = ((ISplatcraftForgeBlockDummy) this).phGetRespawnPosition(state, type, levelReader, pos, orientation);
			return respawnPosition.map(vec3d -> new ServerPlayerEntity.RespawnPos(vec3d, 0));
		}
		default float getExplosionResistance(@NotNull BlockState state, @NotNull BlockView level, @NotNull BlockPos pos, @NotNull Explosion explosion)
		{
			return ((ISplatcraftForgeBlockDummy) this).phGetExplosionResistance(state, level, pos, explosion);
		}
		@Override
		default @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult target, @NotNull WorldView level, @NotNull BlockPos pos, @NotNull PlayerEntity player)
		{
			return ((ISplatcraftForgeBlockDummy) this).phGetCloneItemStack(state, target, level, pos, player);
		}
		@Override
		default boolean addLandingEffects(@NotNull BlockState state1, @NotNull ServerWorld level, @NotNull BlockPos pos, @NotNull BlockState state2, @NotNull LivingEntity entity, int numberOfParticles)
		{
			return ((ISplatcraftForgeBlockDummy) this).phAddLandingEffects(state1, level, pos, state2, entity, numberOfParticles);
		}
		@Override
		default boolean addRunningEffects(@NotNull BlockState state, @NotNull World level, @NotNull BlockPos pos, @NotNull Entity entity)
		{
			return ((ISplatcraftForgeBlockDummy) this).phAddRunningEffects(state, level, pos, entity);
		}
		@Override
		default boolean shouldCheckWeakPower(@NotNull BlockState state, @NotNull RedstoneView level, @NotNull BlockPos pos, @NotNull Direction side)
		{
			return ((ISplatcraftForgeBlockDummy) this).phShouldCheckWeakPower(state, level, pos, side);
		}
		@Override
		@Nullable
		default Integer getBeaconColorMultiplier(@NotNull BlockState state, @NotNull WorldView level, @NotNull BlockPos pos, @NotNull BlockPos beaconPos)
		{
			return ((ISplatcraftForgeBlockDummy) this).phGetBeaconColorMultiplier(state, level, pos, beaconPos);
		}
		@Override
		default boolean collisionExtendsVertically(@NotNull BlockState state, @NotNull BlockView level, @NotNull BlockPos pos, @NotNull Entity collidingEntity)
		{
			return ((ISplatcraftForgeBlockDummy) this).phCollisionExtendsVertically(state, level, pos, collidingEntity);
		}
		@Override
		default boolean canConnectRedstone(@NotNull BlockState state, @NotNull BlockView level, @NotNull BlockPos pos, @Nullable Direction direction)
		{
			return ((ISplatcraftForgeBlockDummy) this).phCanConnectRedstone(state, level, pos, direction);
		}
		@Override
		default @Nullable PistonBehavior getPistonPushReaction(@NotNull BlockState state)
		{
			return ((ISplatcraftForgeBlockDummy) this).phGetPistonBehavior(state);
		}
	}
	@Mixin(ISplatcraftForgeBlockEntityDummy.class)
	public interface BlockEntityMixin extends IBlockEntityExtension
	{
		@Override
		default void handleUpdateTag(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup lookupProvider)
		{
			((ISplatcraftForgeBlockEntityDummy) this).phHandleUpdateTag(tag, lookupProvider);
		}
		@Override
		default void onDataPacket(@NotNull ClientConnection net, @NotNull BlockEntityUpdateS2CPacket pkt, RegistryWrapper.@NotNull WrapperLookup lookupProvider)
		{
			((ISplatcraftForgeBlockEntityDummy) this).phOnDataPacket(net, pkt, lookupProvider);
		}
	}
}
