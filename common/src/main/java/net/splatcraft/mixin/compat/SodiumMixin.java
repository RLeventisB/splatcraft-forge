/*package net.splatcraft.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.util.task.CancellationToken;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.renderer.RenderLayer;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3d;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.splatcraft.data.capabilities.worldink.ChunkInk;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.util.InkBlockUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;

public class SodiumMixin
{
    @Mixin(value = BlockRenderer.class, remap = false)
    public abstract static class BlockRendererMixin
    {
        @Unique
        public boolean splatcraft$hasInkEntry;
        @Unique
        public boolean splatcraft$isFaceInked;
        @Unique
        public ChunkInk.BlockEntry splatcraft$inkEntry;
        @Unique
        public ChunkInk.InkEntry splatcraft$faceEntry;

        @Inject(method = "renderModel", remap = false, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "HEAD", target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;getGeometry(Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;Lnet/minecraft/core/Direction;)Ljava/util/List;", ordinal = 0))
        public void splatcraft$getInkData(BlockRenderContext ctx, ChunkBuildBuffers buffers, CallbackInfo ci)
        {
            ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock(ctx.world().world, ctx.pos());
            splatcraft$inkEntry = ink;
            splatcraft$hasInkEntry = ink != null;
        }

        @WrapOperation(method = "getGeometry", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/BakedModel;getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;Lnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderLayer;)Ljava/util/List;"))
        public List<BakedQuad> splatcraft$modifyQuadList(BakedModel instance, BlockState blockState, Direction face, RandomSource random, ModelData modelData, RenderLayer renderType, Operation<List<BakedQuad>> original)
        {
            ChunkInk.InkEntry faceEntry = splatcraft$inkEntry != null && face != null ? splatcraft$inkEntry.get(face.getId()) : null;
            splatcraft$faceEntry = faceEntry;
            splatcraft$isFaceInked = faceEntry != null;

            List<BakedQuad> quads = new ArrayList<>();
            if (splatcraft$isFaceInked && splatcraft$faceEntry.type() != InkBlockUtils.InkType.CLEAR)
            {
                quads.addAll(ChunkInkHandler.Render.getInkedBlockQuad(face, random, renderType));
                if (splatcraft$faceEntry.type() == InkBlockUtils.InkType.GLOWING)
                {
                    quads.add(ChunkInkHandler.Render.getGlitterQuad()[face.getId()]);
                }
            }
            else
            {
                quads.addAll(original.call(instance, blockState, face, random, modelData, renderType));
            }
            if (splatcraft$hasInkEntry && splatcraft$inkEntry.inmutable)
            {
                quads.add(ChunkInkHandler.Render.getPermaInkQuads()[face == null ? 0 : face.getId()]);
            }
            return quads;
        }

        @WrapOperation(method = "renderModel", remap = false, at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;renderQuadList(Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;Lme/jellysquid/mods/sodium/client/model/light/LightPipeline;Lme/jellysquid/mods/sodium/client/model/color/ColorProvider;Lnet/minecraft/world/phys/Vec3d;Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;Ljava/util/List;Lnet/minecraft/core/Direction;)V"))
        public void splatcraft$modifyColorProvider(BlockRenderer instance, BlockRenderContext context, Material quad, LightPipeline lightData, ColorProvider<BlockState> vertexColors, Vec3d sprite, ChunkModelBuilder i, List<BakedQuad> quadsSize, Direction direction, Operation<Void> original)
        {
            if (splatcraft$isFaceInked)
            {
                original.call(instance, context, quad, lightData, ChunkInkHandler.Render.getSplatcraftColorProvider(), sprite, i, quadsSize, direction);
                return;
            }
            original.call(instance, context, quad, lightData, vertexColors, sprite, i, quadsSize, direction);
        }
    }

    @Mixin(value = ChunkBuilderMeshingTask.class, remap = false)
    public static class ChunkRebuildMixin
    {
        @Unique
        private BlockPos splatcraft$blockPos;
        @Unique
        private Level splatcraft$level;

        @Inject(method = "execute(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lme/jellysquid/mods/sodium/client/util/task/CancellationToken;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;", locals = LocalCapture.CAPTURE_FAILHARD, remap = false, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/BakedModel;getModelData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraftforge/client/model/data/ModelData;)Lnet/minecraftforge/client/model/data/ModelData;"))
        public void getBlockState(ChunkBuildContext buildContext, CancellationToken cancellationToken, CallbackInfoReturnable<ChunkBuildOutput> cir, BuiltSectionInfo.Builder renderData, VisGraph occluder, ChunkBuildBuffers buffers, BlockRenderCache cache, WorldSlice slice, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockPos.Mutable blockPos, BlockPos.Mutable modelOffset, BlockRenderContext context, int y, int z, int x, BlockState blockState, BakedModel model)
        {
            splatcraft$level = slice.world;
            splatcraft$blockPos = blockPos;
        }

        @WrapOperation(method = "execute(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lme/jellysquid/mods/sodium/client/util/task/CancellationToken;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;", remap = false, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/BakedModel;getRenderTypes(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Lnet/minecraftforge/client/model/data/ModelData;)Lnet/minecraftforge/client/ChunkRenderTypeSet;"))
        public ChunkRenderTypeSet canRenderInLayer(BakedModel instance, BlockState state, RandomSource randomSource, ModelData modelData, Operation<ChunkRenderTypeSet> original)
        {
            ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock(splatcraft$level, splatcraft$blockPos);

            if (ink != null && ink.isInkedAny())
                return ChunkRenderTypeSet.union(original.call(instance, state, randomSource, modelData), ChunkRenderTypeSet.of(RenderLayer.translucent()));
            return original.call(instance, state, randomSource, modelData);
        }
    }
}
*/