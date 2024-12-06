package net.splatcraft.forge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.splatcraft.forge.data.SplatcraftTags;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInk;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInkCapability;
import net.splatcraft.forge.handlers.ChunkInkHandler;
import net.splatcraft.forge.registries.SplatcraftBlocks;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.RelativeBlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;

//TODO use RenderLevelStageEvent to render ink over blocks instead of overriding block rendering with mixins,
// this may have been a bad idea for compatibility
@OnlyIn(Dist.CLIENT)
public class BlockRenderMixin
{
    /*@Mixin(ModelBlockRenderer.class)
    public static class Renderer
    {
        @Inject(method = "putQuadData", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFF[IIZ)V"))
        public void getBlockPosFromQuad(BlockAndTintGetter level, BlockState pState, BlockPos blockPos, VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float pBrightness0, float pBrightness1, float pBrightness2, float pBrightness3, int pLightmap0, int pLightmap1, int pLightmap2, int pLightmap3, int pPackedOverlay, CallbackInfo ci)
        {
            if (level instanceof RenderChunkRegion region && ChunkInkHandler.Render.splatcraft$renderInkedBlock(region, blockPos, consumer, pose, quad, new float[]{pBrightness0, pBrightness1, pBrightness2, pBrightness3}, new int[]{pLightmap0, pLightmap1, pLightmap2, pLightmap3}, pPackedOverlay, true))
                ci.cancel();
        }
    }*/

    @Mixin(RenderChunkRegion.class)
    public interface ChunkRegionAccessor
    {
        @Accessor("level")
        Level getLevel();
    }

    @Mixin(ChunkRenderDispatcher.RenderChunk.RebuildTask.class)
    public static class ChunkRenderDispatcherMixin
    {
        @Unique
        private static BlockPos splatcraft$blockPos;
        @Unique
        private static Level splatcraft$level;
        @Unique
        private static boolean splatcraft$overrideRender;

        @Inject(method = "compile", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
        public void splatcraft$getBlockData(float pX, float pY, float pZ, ChunkBufferBuilderPack pChunkBufferBuilderPack, CallbackInfoReturnable<ChunkRenderDispatcher.RenderChunk.RebuildTask.CompileResults> cir, ChunkRenderDispatcher.RenderChunk.RebuildTask.CompileResults chunkrenderdispatcher$renderchunk$rebuildtask$compileresults, int i, BlockPos blockpos, BlockPos blockpos1, VisGraph visgraph, RenderChunkRegion renderchunkregion, PoseStack posestack, Set<?> set, RandomSource randomsource, BlockRenderDispatcher blockrenderdispatcher, Iterator<?> var15, BlockPos blockpos2)
        {
            splatcraft$level = ((ChunkRegionAccessor) renderchunkregion).getLevel();
            splatcraft$blockPos = blockpos2;
            splatcraft$overrideRender = InkBlockUtils.isInkedAny(splatcraft$level, splatcraft$blockPos) && splatcraft$level.getBlockState(splatcraft$blockPos).is(SplatcraftTags.Blocks.RENDER_AS_CUBE);
        }

        @WrapOperation(method = "compile", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/BakedModel;getRenderTypes(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Lnet/minecraftforge/client/model/data/ModelData;)Lnet/minecraftforge/client/ChunkRenderTypeSet;"))
        public ChunkRenderTypeSet getRenderLayer(BakedModel instance, BlockState state, RandomSource randomSource, ModelData modelData, Operation<ChunkRenderTypeSet> original)
        {
            ChunkInk chunkInk = ChunkInkCapability.getOrNull(splatcraft$level, splatcraft$blockPos);
            if (chunkInk != null && chunkInk.isntEmpty() && chunkInk.isInkedAny(RelativeBlockPos.fromAbsolute(splatcraft$blockPos)))
                return ChunkRenderTypeSet.union(original.call(instance, state, randomSource, modelData), ChunkRenderTypeSet.of(RenderType.translucent()));
            return original.call(instance, state, randomSource, modelData);
        }

        @WrapOperation(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
        public BlockState getBlockState(RenderChunkRegion region, BlockPos pos, Operation<BlockState> original)
        {
            return splatcraft$overrideRender ? SplatcraftBlocks.inkedBlock.get().defaultBlockState() : original.call(region, pos);
        }

        @Mixin(IForgeBakedModel.class)
        public static class BakedModelQuadModifierMixin
        {
            @WrapOperation(method = "getQuads", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/BakedModel;getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;)Ljava/util/List;"))
            public List<BakedQuad> splatcraft$modifyQuads(BakedModel instance, BlockState state, Direction direction, RandomSource randomSource, Operation<List<BakedQuad>> original)
            {
                List<BakedQuad> originalList = original.call(instance, state, direction, randomSource);
                if (splatcraft$level == null || splatcraft$blockPos == null)
                    return originalList;

                ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock(splatcraft$level, splatcraft$blockPos);
                if (ink != null && ink.isInkedAny() && ink.isInked(direction.get3DDataValue()))
                {
                    ChunkInk.InkEntry inkEntry = ink.get(direction.get3DDataValue());
                    if (inkEntry != null)
                    {
                        splatcraft$level = null;
                        splatcraft$blockPos = null;
                        ArrayList<BakedQuad> modifiedList = new ArrayList<>();
                        for (BakedQuad quad : originalList)
                        {
                            modifiedList.add(new BakedQuad(Arrays.copyOf(quad.getVertices(), quad.getVertices().length),
                                0, quad.getDirection(), ChunkInkHandler.Render.getInkedBlockSprite(), quad.isShade()));
                        }
                        return modifiedList;
                    }
                }
                return originalList;
            }
        }
    }
}
