package net.splatcraft.mixin;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.splatcraft.dummys.ISplatcraftForgeBlockDummy;
import net.splatcraft.dummys.ISplatcraftForgeBlockEntityDummy;
import net.splatcraft.dummys.ISplatcraftForgeItemDummy;
import net.splatcraft.platform.IExtraDataOnAddEntity;
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
	@Mixin(IExtraDataOnAddEntity.class)
	public interface EntityExtraDataMixin extends IEntityWithComplexSpawn
	{
		@Override
		default void readSpawnData(@NotNull RegistryFriendlyByteBuf buf)
		{
			((IExtraDataOnAddEntity) this).readExtraData(buf);
		}
		@Override
		default void writeSpawnData(@NotNull RegistryFriendlyByteBuf buf)
		{
			((IExtraDataOnAddEntity) this).writeExtraData(buf);
		}
	}
	@Mixin(ISplatcraftForgeBlockDummy.class)
	public interface ClientBlockMixin extends IClientBlockExtensions
	{
		@Override
		default boolean addHitEffects(@NotNull BlockState state, @NotNull Level level, @NotNull HitResult target, @NotNull ParticleEngine manager)
		{
			return ((ISplatcraftForgeBlockDummy) this).phAddHitEffects(state, level, target, manager);
		}
	}
	@Mixin(ISplatcraftForgeBlockDummy.class)
	public interface BlockMixin extends IBlockExtension
	{
		@Override
		default boolean canHarvestBlock(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull Player player)
		{
			return ((ISplatcraftForgeBlockDummy) this).phCanHarvestBlock(state, level, pos, player);
		}
		@Override
		default boolean onDestroyedByPlayer(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, boolean willHarvest, @NotNull FluidState fluid)
		{
			return ((ISplatcraftForgeBlockDummy) this).phOnDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
		}
		@Override
		default @NotNull Optional<ServerPlayer.RespawnPosAngle> getRespawnPosition(@NotNull BlockState state, @NotNull EntityType<?> type, @NotNull LevelReader levelReader, @NotNull BlockPos pos, float orientation)
		{
			Optional<Vec3> respawnPosition = ((ISplatcraftForgeBlockDummy) this).phGetRespawnPosition(state, type, levelReader, pos, orientation);
			return respawnPosition.map(vec3d -> new ServerPlayer.RespawnPosAngle(vec3d, 0));
		}
		default float getExplosionResistance(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull Explosion explosion)
		{
			return ((ISplatcraftForgeBlockDummy) this).phGetExplosionResistance(state, level, pos, explosion);
		}
		@Override
		default @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult target, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player)
		{
			return ((ISplatcraftForgeBlockDummy) this).phGetCloneItemStack(state, target, level, pos, player);
		}
		@Override
		default boolean addLandingEffects(@NotNull BlockState state1, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state2, @NotNull LivingEntity entity, int numberOfParticles)
		{
			return ((ISplatcraftForgeBlockDummy) this).phAddLandingEffects(state1, level, pos, state2, entity, numberOfParticles);
		}
		@Override
		default boolean addRunningEffects(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity)
		{
			return ((ISplatcraftForgeBlockDummy) this).phAddRunningEffects(state, level, pos, entity);
		}
		@Override
		default boolean shouldCheckWeakPower(@NotNull BlockState state, @NotNull SignalGetter level, @NotNull BlockPos pos, @NotNull Direction side)
		{
			return ((ISplatcraftForgeBlockDummy) this).phShouldCheckWeakPower(state, level, pos, side);
		}
		@Override
		@Nullable
		default Integer getBeaconColorMultiplier(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockPos beaconPos)
		{
			return ((ISplatcraftForgeBlockDummy) this).phGetBeaconColorMultiplier(state, level, pos, beaconPos);
		}
		@Override
		default boolean collisionExtendsVertically(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull Entity collidingEntity)
		{
			return ((ISplatcraftForgeBlockDummy) this).phCollisionExtendsVertically(state, level, pos, collidingEntity);
		}
		@Override
		default boolean canConnectRedstone(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @Nullable Direction direction)
		{
			return ((ISplatcraftForgeBlockDummy) this).phCanConnectRedstone(state, level, pos, direction);
		}
		@Override
		default @Nullable PushReaction getPistonPushReaction(@NotNull BlockState state)
		{
			return ((ISplatcraftForgeBlockDummy) this).phGetPistonBehavior(state);
		}
	}
	@Mixin(ISplatcraftForgeBlockEntityDummy.class)
	public interface BlockEntityMixin extends IBlockEntityExtension
	{
		@Override
		default void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider lookupProvider)
		{
			((ISplatcraftForgeBlockEntityDummy) this).phHandleUpdateTag(tag, lookupProvider);
		}
		@Override
		default void onDataPacket(@NotNull Connection net, @NotNull ClientboundBlockEntityDataPacket pkt, HolderLookup.@NotNull Provider lookupProvider)
		{
			((ISplatcraftForgeBlockEntityDummy) this).phOnDataPacket(net, pkt, lookupProvider);
		}
	}
}
