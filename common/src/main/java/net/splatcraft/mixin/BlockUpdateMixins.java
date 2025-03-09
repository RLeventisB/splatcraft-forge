package net.splatcraft.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.splatcraft.handlers.ChunkInkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

public class BlockUpdateMixins
{
	@Mixin(ServerLevel.class)
	public static class ServerWorldMixin
	{
		@Inject(method = "updateNeighborsAt", at = @At("TAIL"))
		public void splatcraft$updateInk(BlockPos pos, Block block, CallbackInfo ci)
		{
			ChunkInkHandler.onBlockUpdate((ServerLevel) (Object) this, pos, Direction.stream().toList());
		}
		@Inject(method = "updateNeighborsAtExceptFromFacing", at = @At("TAIL"))
		public void splatcraft$updateInk(BlockPos pos, Block block, Direction direction, CallbackInfo ci)
		{
			List<Direction> values = Direction.stream().filter(v -> v != direction).toList();
			ChunkInkHandler.onBlockUpdate((ServerLevel) (Object) this, pos, values);
		}
	}
	@Mixin(Level.class)
	public static class WorldMixin
	{
		@Inject(method = "updateNeighborsAt", at = @At("TAIL"))
		public void splatcraft$updateInk(BlockPos pos, Block block, CallbackInfo ci)
		{
			ChunkInkHandler.onBlockUpdate((Level) (Object) this, pos, Direction.stream().toList());
		}
	}
}
