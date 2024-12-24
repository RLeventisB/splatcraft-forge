package net.splatcraft.fabric.mixin;

import net.minecraft.server.network.ChunkDataSender;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;
import net.splatcraft.handlers.ChunkInkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkDataSender.class)
public class ServerChunkDataMixin
{
	@Inject(method = "sendChunkData", at = @At("TAIL"))
	private static void splatcraft$onChunkDataSent(ServerPlayNetworkHandler handler, ServerWorld world, WorldChunk chunk, CallbackInfo ci)
	{
		ChunkInkHandler.sendChunkData(handler, world, chunk);
	}
}
