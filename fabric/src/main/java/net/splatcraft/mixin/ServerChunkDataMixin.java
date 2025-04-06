package net.splatcraft.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
import net.splatcraft.handlers.ChunkInkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerChunkSender.class)
public class ServerChunkDataMixin
{
	@Inject(method = "sendChunk", at = @At("TAIL"))
	private static void splatcraft$onChunkDataSent(ServerGamePacketListenerImpl handler, ServerLevel world, LevelChunk chunk, CallbackInfo ci)
	{
		ChunkInkHandler.sendChunkData(handler, world, chunk);
	}
}
