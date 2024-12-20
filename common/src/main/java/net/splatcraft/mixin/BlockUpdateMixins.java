package net.splatcraft.mixin;

import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.splatcraft.handlers.ChunkInkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

public class BlockUpdateMixins
{
    @Mixin(ServerWorld.class)
    public static class ServerWorldMixin
    {
        @Inject(method = "updateNeighborsAlways", at = @At("TAIL"))
        public void splatcraft$updateInk(BlockPos pos, Block block, CallbackInfo ci)
        {
            ChunkInkHandler.onBlockUpdate((ServerWorld) (Object) this, pos, Direction.stream().toList());
        }

        @Inject(method = "updateNeighborsExcept", at = @At("TAIL"))
        public void splatcraft$updateInk(BlockPos pos, Block block, Direction direction, CallbackInfo ci)
        {
            List<Direction> values = Direction.stream().toList();
            values.remove(direction);
            ChunkInkHandler.onBlockUpdate((ServerWorld) (Object) this, pos, values);
        }
    }

    @Mixin(World.class)
    public static class WorldMixin
    {
        @Inject(method = "updateNeighborsAlways", at = @At("TAIL"))
        public void splatcraft$updateInk(BlockPos pos, Block block, CallbackInfo ci)
        {
            ChunkInkHandler.onBlockUpdate((World) (Object) this, pos, Direction.stream().toList());
        }
    }
}
