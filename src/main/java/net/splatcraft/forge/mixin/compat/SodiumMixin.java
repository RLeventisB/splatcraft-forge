package net.splatcraft.forge.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
import me.jellysquid.mods.sodium.client.model.light.LightMode;
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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInk;
import net.splatcraft.forge.handlers.ChunkInkHandler;
import net.splatcraft.forge.util.InkBlockUtils;
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
    @Mixin(BlockRenderer.class)
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

        //        @Inject(method = "renderModel", remap = false, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;getGeometry(Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;Lnet/minecraft/core/Direction;)Ljava/util/List;"))
        @Inject(method = "renderModel", remap = false, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;getGeometry(Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;Lnet/minecraft/core/Direction;)Ljava/util/List;", ordinal = 0))
        public void splatcraft$getInkData(BlockRenderContext ctx, ChunkBuildBuffers buffers, CallbackInfo ci, Material material, ChunkModelBuilder meshBuilder, ColorProvider colorizer, LightMode mode, LightPipeline lighter, Vec3 renderOffset, Direction[] var9, int var10, int var11, Direction face)
        {
            ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock(ctx.world().world, ctx.pos());
            ChunkInk.InkEntry faceEntry = ink != null ? ink.get(face.get3DDataValue()) : null;
            splatcraft$inkEntry = ink;
            splatcraft$hasInkEntry = ink != null;
            splatcraft$faceEntry = faceEntry;
            splatcraft$isFaceInked = faceEntry != null;
        }

        @WrapOperation(method = "getGeometry", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/BakedModel;getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;Lnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)Ljava/util/List;"))
        public List<BakedQuad> splatcraft$modifyQuadList(BakedModel instance, BlockState blockState, Direction face, RandomSource random, ModelData modelData, RenderType renderType, Operation<List<BakedQuad>> original)
        {
            List<BakedQuad> quads = new ArrayList<>();
            if (splatcraft$isFaceInked && splatcraft$faceEntry.type() != InkBlockUtils.InkType.CLEAR)
            {
                quads.addAll(ChunkInkHandler.Render.getInkedBlockQuad(face, random, renderType));
                if (splatcraft$faceEntry.type() == InkBlockUtils.InkType.GLOWING)
                {
                    quads.add(ChunkInkHandler.Render.getGlitterQuad()[face.get3DDataValue()]);
                }
            }
            else
            {
                quads.addAll(original.call(instance, blockState, face, random, modelData, renderType));
            }
            if (splatcraft$hasInkEntry && splatcraft$inkEntry.inmutable)
            {
                quads.add(ChunkInkHandler.Render.getPermaInkQuads()[face == null ? 0 : face.get3DDataValue()]);
            }
            return quads;
        }

        @WrapOperation(method = "renderModel", remap = false, at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;renderQuadList(Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;Lme/jellysquid/mods/sodium/client/model/light/LightPipeline;Lme/jellysquid/mods/sodium/client/model/color/ColorProvider;Lnet/minecraft/world/phys/Vec3;Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;Ljava/util/List;Lnet/minecraft/core/Direction;)V"))
        public void splatcraft$modifyColorProvider(BlockRenderer instance, BlockRenderContext context, Material quad, LightPipeline lightData, ColorProvider<BlockState> vertexColors, Vec3 sprite, ChunkModelBuilder i, List<BakedQuad> quadsSize, Direction direction, Operation<Void> original)
        {
            if (splatcraft$isFaceInked)
            {
                original.call(instance, context, quad, lightData, ChunkInkHandler.Render.getSplatcraftColorProvider(), sprite, i, quadsSize, direction);
                return;
            }
            original.call(instance, context, quad, lightData, vertexColors, sprite, i, quadsSize, direction);
        }

        /*@Inject(method = "renderQuadList", remap = false, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;writeGeometry(Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;Lnet/minecraft/world/phys/Vec3;Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;Lme/jellysquid/mods/sodium/client/model/quad/BakedQuadView;[ILme/jellysquid/mods/sodium/client/model/light/data/QuadLightData;)V"))
        public void splatcraft$getInkData(BlockRenderContext context, Material material, LightPipeline lighter, ColorProvider<BlockState> colorizer, Vec3 offset, ChunkModelBuilder builder, List<BakedQuad> quads, Direction cullFace, CallbackInfo ci, int index, int quadCount, BakedQuadView quad, QuadLightData light, int[] colors)
        {
            ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock(context.world().world, context.pos());
            ModelQuadOrientation orientation = this.useReorienting ? ModelQuadOrientation.orientByBrightness(light.br, light.lm) : ModelQuadOrientation.NORMAL;

            if(ink.isInkedAny())
                MixinCode.splacraft$drawInkedFace(builder, material, quad, colors, light, orientation, vertices.clone(), vertexBuffer, ink);
        }*/

        /*@WrapOperation(method = "renderQuadList", at = @At(value = "INVOKE", target =
        "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;writeGeometry(Lme/jellysquid/mods
        /sodium/client/render/chunk/compile/pipeline/BlockRenderContext;Lme/jellysquid/mods/sodium/client/render/chunk/compile
        /buffers/ChunkModelBuilder;Lnet/minecraft/world/phys/Vec3;Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material
        /Material;Lme/jellysquid/mods/sodium/client/model/quad/BakedQuadView;[ILme/jellysquid/mods/sodium/client/model/light/data
        /QuadLightData;)V"))

        holy siht this is a long method name
        public void splatcraft$wrapRenderQuadList(BlockRenderer instance,
                                                  BlockRenderContext context,
                                                  ChunkModelBuilder builder,
                                                  Vec3 offset,
                                                  Material material,
                                                  BakedQuadView quad,
                                                  int[] vertexColors,
                                                  QuadLightData lightData,
                                                  Operation<Void> original)
        {
            ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock(context.world().world, context.pos());
            if (ink != null)
            {
                int index = quad.getLightFace().get3DDataValue();
                if (ink.isInkedAny())
                {
                    if (ink.isInked(quad.getLightFace().get3DDataValue()))
                    {
                        float[] rgbs = ColorUtils.hexToRGB(ink.color(index));
                        int color = ColorABGR.pack(rgbs[0], rgbs[1], rgbs[2]);

                        splatcraft$renderInkQuad(instance, color, ink.type(index) == InkBlockUtils.InkType.CLEAR ? null : ChunkInkHandler.Render.getInkedBlockSprite(), ink.type(index) == InkBlockUtils.InkType.GLOWING,
                                context, builder, offset, material, quad, vertexColors, lightData);

                        if (ink.type(index) == InkBlockUtils.InkType.GLOWING)
                            splatcraft$renderInkQuad(instance, 0xFFFFFFFF, ChunkInkHandler.Render.getGlitterSprite(), true,
                                    context, builder, offset, material, quad, vertexColors, lightData);

                        if (ink.inmutable)
                            splatcraft$renderInkQuad(instance, 0xFFFFFFFF, ChunkInkHandler.Render.getPermanentInkSprite(), false,
                                    context, builder, offset, material, quad, vertexColors, lightData);

                        return;
                    }
                }
                else
                {
                    if (ink.inmutable)
                        splatcraft$renderInkQuad(instance, 0xFFFFFFFF, ChunkInkHandler.Render.getPermanentInkSprite(), false,
                                context, builder, offset, material, quad, vertexColors, lightData);
                }
            }

            original.call(instance, context, builder, offset, material, quad, vertexColors, lightData);
        }

        @Unique
        private void splatcraft$renderInkQuad(BlockRenderer instance, int packedColor, TextureAtlasSprite sprite, boolean emissive, BlockRenderContext ctx, ChunkModelBuilder builder, Vec3 offset, Material material, BakedQuadView quad, int[] colors, QuadLightData light)
        {
            ModelQuadOrientation orientation = ModelQuadOrientation.orientByBrightness(light.br, light.lm);
            ChunkVertexEncoder.Vertex[] vertices = ((VerticesAccessor) instance).getVertices();
            ModelQuadFacing normalFace = quad.getNormalFace();

            for (int dstIndex = 0; dstIndex < 4; ++dstIndex)
            {
                int srcIndex = orientation.getVertexIndex(dstIndex);
                ChunkVertexEncoder.Vertex out = vertices[dstIndex];
                out.x = ctx.origin().x() + quad.getX(srcIndex) + (float) offset.x();
                out.y = ctx.origin().y() + quad.getY(srcIndex) + (float) offset.y();
                out.z = ctx.origin().z() + quad.getZ(srcIndex) + (float) offset.z();
                int rgb = colors != null ? colors[srcIndex] : quad.getColor(srcIndex);
                rgb = ColorMixer.mul(rgb, packedColor);
                out.color = ColorABGR.withAlpha(rgb, light.br[srcIndex] + (emissive ? 0.5f : 0));
                if (sprite != null)
                {
                    Direction.Axis axis = quad.getLightFace().getAxis();
                    out.u = sprite.getU0() + (axis.equals(Direction.Axis.X) ? out.z : out.x) * (sprite.getU1() - sprite.getU0());
                    out.v = sprite.getV0() + (axis.equals(Direction.Axis.Y) ? out.z : out.y) * (sprite.getV1() - sprite.getV0());
                }
                else
                {
                    out.u = quad.getTexU(srcIndex);
                    out.v = quad.getTexV(srcIndex);
                }
                if (quad instanceof BakedQuad bakedQuad)
                {
                    SpriteAccessor mixin = (SpriteAccessor) bakedQuad;
                    mixin.setSprite(sprite);
                }
                out.light = emissive ? LightTexture.pack(15, 15) : light.lm[srcIndex];
            }

            ChunkMeshBufferBuilder vertexBuffer = builder.getVertexBuffer(normalFace);
            vertexBuffer.push(vertices, material);
        }*/
    }

    @Mixin(ChunkBuilderMeshingTask.class)
    public static class ChunkRebuildMixin
    {
        @Unique
        private BlockPos splatcraft$blockPos;
        @Unique
        private Level splatcraft$level;

        @Inject(method = "execute(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lme/jellysquid/mods/sodium/client/util/task/CancellationToken;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;", remap = false, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/BakedModel;getModelData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraftforge/client/model/data/ModelData;)Lnet/minecraftforge/client/model/data/ModelData;"))
        public void getBlockState(ChunkBuildContext buildContext, CancellationToken cancellationToken, CallbackInfoReturnable<ChunkBuildOutput> cir, BuiltSectionInfo.Builder renderData, VisGraph occluder, ChunkBuildBuffers buffers, BlockRenderCache cache, WorldSlice slice, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockPos.MutableBlockPos blockPos, BlockPos.MutableBlockPos modelOffset, BlockRenderContext context, int y, int z, int x, BlockState blockState, BakedModel model)
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
                return ChunkRenderTypeSet.union(original.call(instance, state, randomSource, modelData), ChunkRenderTypeSet.of(RenderType.translucent()));
            return original.call(instance, state, randomSource, modelData);
        }
    }

   /* @Mixin(BakedQuad.class)
    public interface SpriteAccessor
    {
        @Mutable
        @Final
        @Accessor("sprite")
        void setSprite(TextureAtlasSprite sprite);

        @Mutable
        @Final
        @Accessor("vertices")
        void setVertices(int[] vertices);
    }*/
}
