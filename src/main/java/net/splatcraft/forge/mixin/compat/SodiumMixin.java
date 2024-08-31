package net.splatcraft.forge.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.jellysquid.mods.sodium.client.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.blender.ColorSampler;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadWinding;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.forge.data.SplatcraftTags;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInk;
import net.splatcraft.forge.handlers.ChunkInkHandler;
import net.splatcraft.forge.registries.SplatcraftBlocks;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.InkBlockUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Arrays;

public class SodiumMixin
{
	@Mixin(BlockRenderer.class)
	public static class BlockRendererMixin
	{
		@WrapOperation(method = "renderQuadList", remap = false, at = @At(value = "INVOKE",
			target = "Lme/jellysquid/mods/sodium/client/render/pipeline/BlockRenderer;renderQuad(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lme/jellysquid/mods/sodium/client/render/chunk/format/ModelVertexSink;Lme/jellysquid/mods/sodium/client/model/IndexBufferBuilder;Lnet/minecraft/world/phys/Vec3;Lme/jellysquid/mods/sodium/client/model/quad/blender/ColorSampler;Lnet/minecraft/client/renderer/block/model/BakedQuad;Lme/jellysquid/mods/sodium/client/model/light/data/QuadLightData;Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;)V"))
		public void wrapRenderQuadList(BlockRenderer instance, BlockAndTintGetter world, BlockState state, BlockPos
			pos, BlockPos origin, ModelVertexSink vertices, IndexBufferBuilder indices, Vec3
			                               blockOffset, ColorSampler<BlockState> colorSampler, BakedQuad bakedQuad, QuadLightData
			                               light, ChunkModelBuilder model, Operation<Void> original)
		{
            if (world instanceof WorldSlice worldSlice)
            {
                ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock(((WorldSliceAccessor) worldSlice).getWorld(), pos);
                if (ink != null)
                {
                    int index = bakedQuad.getDirection().get3DDataValue();
                    if (ink.isInkedAny())
                    {
                        if (ink.isInked(bakedQuad.getDirection().get3DDataValue()))
                        {
                            float[] rgb = ColorUtils.hexToRGB(ink.color(index));
                            int color = ColorABGR.pack(rgb[0], rgb[1], rgb[2]);

                            splatcraft$renderInkQuad(color, ink.type(index) == InkBlockUtils.InkType.CLEAR ? null : ChunkInkHandler.Render.getInkedBlockSprite(), ink.type(index) == InkBlockUtils.InkType.GLOWING,
                                    origin, vertices, indices, blockOffset, bakedQuad, light, model);

                            if (ink.type(index) == InkBlockUtils.InkType.GLOWING)
                                splatcraft$renderInkQuad(ColorABGR.pack(1f, 1f, 1f), ChunkInkHandler.Render.getGlitterSprite(), true,
                                        origin, vertices, indices, blockOffset, bakedQuad, light, model);

                            if (ink.inmutable)
                                splatcraft$renderInkQuad(ColorABGR.pack(1f, 1f, 1f), ChunkInkHandler.Render.getPermanentInkSprite(), false,
                                        origin, vertices, indices, blockOffset, bakedQuad, light, model);

                            return;
                        }
                    }
					else
					{
						if (ink.inmutable)
							splatcraft$renderInkQuad(ColorABGR.pack(1f, 1f, 1f), ChunkInkHandler.Render.getPermanentInkSprite(), false,
								origin, vertices, indices, blockOffset, bakedQuad, light, model);
					}
				}
			}

            original.call(instance, world, state, pos, origin, vertices, indices, blockOffset, colorSampler, bakedQuad, light, model);
        }

        @Unique
        private void splatcraft$renderInkQuad(int packedColor, TextureAtlasSprite sprite, boolean emissive, BlockPos
                origin, ModelVertexSink vertices, IndexBufferBuilder indices, Vec3 blockOffset, BakedQuad
                                                      bakedQuad, QuadLightData light, ChunkModelBuilder model)
        {
            ModelQuadView src = (ModelQuadView) bakedQuad;
            ModelQuadOrientation orientation = ModelQuadOrientation.orientByBrightness(light.br);

            int vertexStart = vertices.getVertexCount();

            for (int i = 0; i < 4; ++i)
            {
                int j = orientation.getVertexIndex(i);
                float x = src.getX(j) + (float) blockOffset.x();
                float y = src.getY(j) + (float) blockOffset.y();
                float z = src.getZ(j) + (float) blockOffset.z();
                int color = ColorABGR.mul(packedColor, Math.max(light.br[j], Math.min(1, light.br[j] + (emissive ? 0.5f : 0))));
                float u = src.getTexU(j);
                float v = src.getTexV(j);

                if (sprite != null)
                {
                    Direction.Axis axis = bakedQuad.getDirection().getAxis();
                    u = sprite.getU0() + (axis.equals(Direction.Axis.X) ? z : x) * (sprite.getU1() - sprite.getU0());
                    v = sprite.getV0() + (axis.equals(Direction.Axis.Y) ? z : y) * (sprite.getV1() - sprite.getV0());
                }

                int lm = emissive ? LightTexture.pack(15, 15) : light.lm[j];
                vertices.writeVertex(origin, x, y, z, color, u, v, lm, model.getChunkId());
            }

            indices.add(vertexStart, ModelQuadWinding.CLOCKWISE);
            if (sprite == null)
                sprite = src.getSprite();
            if (sprite != null)
            {
                model.addSprite(sprite);
            }
        }
    }

    @Mixin(ChunkRenderRebuildTask.class)
    public static class ChunkRebuildMixin
    {
        @Unique
        private BlockPos splatcraft$blockPos;
        @Unique
        private Level splatcraft$level;

        @WrapOperation(method = "performBuild", remap = false, at = @At(value = "INVOKE",
                target = "Lme/jellysquid/mods/sodium/client/world/WorldSlice;getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;"))
        public BlockState getBlockState(WorldSlice instance, int x, int y, int z, Operation<BlockState> original)
        {
            splatcraft$level = ((WorldSliceAccessor) instance).getWorld();
            splatcraft$blockPos = new BlockPos(x, y, z);
            return InkBlockUtils.isInkedAny(splatcraft$level, splatcraft$blockPos) && splatcraft$level.getBlockState(splatcraft$blockPos).is(SplatcraftTags.Blocks.RENDER_AS_CUBE) ?
                    SplatcraftBlocks.inkedBlock.get().defaultBlockState() : original.call(instance, x, y, z);
        }

        @WrapOperation(method = "performBuild", remap = false, at = @At(value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/ItemBlockRenderTypes;canRenderInLayer(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/RenderType;)Z"))
        public boolean canRenderInLayer(BlockState state, RenderType type, Operation<Boolean> original)
        {
            if (InkBlockUtils.isInkedAny(splatcraft$level, splatcraft$blockPos))
            {
                ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock(splatcraft$level, splatcraft$blockPos);

                if (Arrays.stream(ink.entries).anyMatch(v -> v.type() == InkBlockUtils.InkType.GLOWING || v.type() == InkBlockUtils.InkType.CLEAR))
                    return type == RenderType.translucent();
                return type == RenderType.solid();
            }
            return original.call(state, type);
        }
    }

    @Mixin(WorldSlice.class)
    public interface WorldSliceAccessor
    {
        @Accessor("world")
        Level getWorld();
    }
}
