package net.splatcraft.forge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.splatcraft.forge.data.SplatcraftTags;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInk;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInkCapability;
import net.splatcraft.forge.handlers.ChunkInkHandler;
import net.splatcraft.forge.registries.SplatcraftBlocks;
import net.splatcraft.forge.util.InkBlockUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Set;

//TODO use RenderLevelStageEvent to render ink over blocks instead of overriding block rendering with mixins,
// this may have been a bad idea for compatibility
@OnlyIn(Dist.CLIENT)
public class BlockRenderMixin
{
	@Mixin(ModelBlockRenderer.class)
	public static class Renderer
	{
		@Inject(method = "putQuadData", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
			target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFF[IIZ)V"))
		public void getBlockPosFromQuad(BlockAndTintGetter level, BlockState pState, BlockPos blockPos, VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float pBrightness0, float pBrightness1, float pBrightness2, float pBrightness3, int pLightmap0, int pLightmap1, int pLightmap2, int pLightmap3, int pPackedOverlay, CallbackInfo ci)
		{
			if (level instanceof RenderChunkRegion region && ChunkInkHandler.Render.splatcraft$renderInkedBlock(region, blockPos, consumer, pose, quad, new float[] {pBrightness0, pBrightness1, pBrightness2, pBrightness3}, new int[] {pLightmap0, pLightmap1, pLightmap2, pLightmap3}, pPackedOverlay, true))
				ci.cancel();
		}
	}
	@Mixin(ChunkRenderDispatcher.RenderChunk.RebuildTask.class)
	public static class ChunkRenderDispatcherMixin
	{
		@Unique
		private BlockPos splatcraft$blockPos;
		@Unique
		private Level splatcraft$level;
		@Unique
		private static boolean splatcraft$renderAsCube;
		@Inject(method = "compile", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
		public void getBlockData(float pX, float pY, float pZ, ChunkBufferBuilderPack pChunkBufferBuilderPack, CallbackInfoReturnable<ChunkRenderDispatcher.RenderChunk.RebuildTask.CompileResults> cir, ChunkRenderDispatcher.RenderChunk.RebuildTask.CompileResults chunkrenderdispatcher$renderchunk$rebuildtask$compileresults, int i, BlockPos blockpos, BlockPos blockpos1, VisGraph visgraph, RenderChunkRegion renderchunkregion, PoseStack posestack, Set set, RandomSource randomsource, BlockRenderDispatcher blockrenderdispatcher, Iterator var15, BlockPos blockpos2)
		{
			splatcraft$level = ((ChunkRegionAccessor) renderchunkregion).getLevel();
			splatcraft$blockPos = blockpos2;
			splatcraft$renderAsCube = InkBlockUtils.isInkedAny(splatcraft$level, splatcraft$blockPos) && splatcraft$level.getBlockState(splatcraft$blockPos).is(SplatcraftTags.Blocks.RENDER_AS_CUBE);
		}
		@WrapOperation(method = "compile", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/BakedModel;getRenderTypes(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Lnet/minecraftforge/client/model/data/ModelData;)Lnet/minecraftforge/client/ChunkRenderTypeSet;"))
		public ChunkRenderTypeSet getRenderLayer(BakedModel instance, BlockState state, RandomSource randomSource, ModelData modelData, Operation<ChunkRenderTypeSet> original)
		{
			ChunkInk chunkInk = ChunkInkCapability.getOrNull(splatcraft$level, splatcraft$blockPos);
			if (chunkInk.isntEmpty())
			{
				if (chunkInk.isInkedAny(splatcraft$blockPos))
				{
					return ChunkRenderTypeSet.union(original.call(instance, state, randomSource, modelData), ChunkRenderTypeSet.of(RenderType.translucent()));
				}
			}
			return original.call(instance, state, randomSource, modelData);
		}
		@WrapOperation(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
		public BlockState getBlockState(RenderChunkRegion region, BlockPos pos, Operation<BlockState> original)
		{
			return splatcraft$renderAsCube ? SplatcraftBlocks.inkedBlock.get().defaultBlockState() : original.call(region, pos);
		}
	}
	@Mixin(RenderChunkRegion.class)
	public interface ChunkRegionAccessor
	{
		@Accessor("level")
		Level getLevel();
	}
}
