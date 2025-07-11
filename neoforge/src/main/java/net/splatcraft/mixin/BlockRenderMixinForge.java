package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.splatcraft.InkedBakedModel;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.data.capabilities.chunkink.ChunkInkCapability;
import net.splatcraft.mixin.accessors.ChunkRegionAccessor;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.structs.RelativeBlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

public class BlockRenderMixinForge
{
	// note: these things are THREADED!!!! so a thread can modify some of the fields while the other one is rendering and do some bad things.,,.
	@Mixin(SectionCompiler.class)
	public static class ChunkRenderDispatcherMixinForge
	{
		@WrapOperation(method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/BakedModel;getRenderTypes(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;)Lnet/neoforged/neoforge/client/ChunkRenderTypeSet;"))
		public ChunkRenderTypeSet splatcraft$fixRenderLayer(BakedModel instance, BlockState state, RandomSource random, ModelData modelData, Operation<ChunkRenderTypeSet> original, @Local(ordinal = 2) BlockPos blockpos, @Local(argsOnly = true) RenderChunkRegion arg2)
		{
			Level world = ((ChunkRegionAccessor) arg2).getLevel();
			ChunkRenderTypeSet renderType = original.call(instance, state, random, modelData);
			if (!ChunkInkCapability.has(world, blockpos))
				return renderType;
			
			ChunkInk chunkInk = ChunkInkCapability.getOrCreate(world, blockpos);
			if (chunkInk.isntEmpty() && chunkInk.isInkedAny(RelativeBlockPos.fromAbsolute(blockpos)))
				return ChunkRenderTypeSet.union(renderType, ChunkRenderTypeSet.of(RenderType.translucent()));
			
			return renderType;
		}
		@WrapOperation(method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
		public BlockState splatcraft$renderAsCubeTagOverride(RenderChunkRegion instance, BlockPos pos, Operation<BlockState> original, @Local(ordinal = 2) BlockPos blockpos, @Local(argsOnly = true) RenderChunkRegion arg2)
		{
			BlockState originalState = original.call(instance, pos);
			return originalState.is(SplatcraftTags.Blocks.RENDER_AS_CUBE) && InkBlockUtils.isInkedAny(((ChunkRegionAccessor) arg2).getLevel(), blockpos) ? SplatcraftBlocks.inkedBlock.value().defaultBlockState() : originalState;
		}
	}
	@Mixin(BlockRenderDispatcher.class)
	public static class BlockRenderManagerMixin
	{
		@WrapOperation(method = "renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V"))
		public void splatcraft$addBakedModel(ModelBlockRenderer instance, BlockAndTintGetter blockRenderView, BakedModel model, BlockState state, BlockPos pos, PoseStack matrixStack, VertexConsumer vertexConsumer, boolean b, RandomSource random, long l, int i, ModelData modelData, RenderType renderLayer, Operation<Void> original)
		{
			Level level = ((ChunkRegionAccessor) blockRenderView).getLevel();
			original.call(instance, blockRenderView, InkedBakedModel.tryCreateFor(model, level, pos), state, pos, matrixStack, vertexConsumer, b, random, l, i, modelData, renderLayer);
		}
	}
}