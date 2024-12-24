package net.splatcraft.neoforge.mixin;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.splatcraft.data.capabilities.worldink.ChunkInkCapability;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkLoadingManager.class)
public class ServerChunkLoadManagerMixin
{
	@Shadow
	@Final
	ServerWorld world;
	@Inject(method = "method_60440", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/WorldChunk;setLoadedToWorld(Z)V"))
	private void splatcraft$removeChunk(ChunkHolder chunkHolder, long pos, CallbackInfo ci)
	{
		ChunkInkCapability.unloadChunkData(world, new ChunkPos(pos));
	}
}
