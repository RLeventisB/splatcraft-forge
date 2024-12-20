package net.splatcraft.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.splatcraft.data.capabilities.worldink.ChunkInkCapability;
import net.splatcraft.handlers.ChunkInkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkLoadingManager.class)
public class ChunkSerializerMixin
{
    @Inject(method = "track(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/world/chunk/WorldChunk;)V", at = @At("TAIL"))
    private static void splatcraft$trackChunk(ServerPlayerEntity player, WorldChunk chunk, CallbackInfo ci)
    {
        ChunkInkHandler.onChunkWatch(player, player.getWorld(), chunk);
    }

    @Inject(method = "untrack", at = @At("TAIL"))
    private static void splatcraft$untrackChunk(ServerPlayerEntity player, ChunkPos pos, CallbackInfo ci)
    {
        ChunkInkCapability.unloadChunkData(player.getWorld(), pos);
    }
}
