package net.splatcraft.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.render.chunk.SectionBuilder;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.data.capabilities.chunkink.ChunkInkCapability;
import net.splatcraft.mixin.accessors.ChunkRegionAccessor;
import net.splatcraft.neoforge.InkedBakedModel;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.RelativeBlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

public class BlockRenderMixinForge
{
	// note: these things are THREADED!!!! so a thread can modify some of the fields while the other one is rendering and do some bad things.,,.
	@Mixin(SectionBuilder.class)
	public static class ChunkRenderDispatcherMixinForge
	{
		@WrapOperation(method = "compile", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/model/BakedModel;getRenderTypes(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/random/Random;Lnet/neoforged/neoforge/client/model/data/ModelData;)Lnet/neoforged/neoforge/client/ChunkRenderTypeSet;"))
		public ChunkRenderTypeSet splatcraft$fixRenderLayer(BakedModel instance, BlockState state, Random random, ModelData modelData, Operation<ChunkRenderTypeSet> original, @Local(ordinal = 2) BlockPos blockpos, @Local(argsOnly = true) ChunkRendererRegion arg2)
		{
			World world = ((ChunkRegionAccessor) arg2).getWorld();
			ChunkRenderTypeSet renderType = original.call(instance, state, random, modelData);
			if (!ChunkInkCapability.has(world, blockpos))
				return renderType;
			ChunkInk chunkInk = ChunkInkCapability.get(world, blockpos);
			if (chunkInk.isntEmpty() && chunkInk.isInkedAny(RelativeBlockPos.fromAbsolute(blockpos)))
				return ChunkRenderTypeSet.union(renderType, ChunkRenderTypeSet.of(RenderLayer.getTranslucent()));
			return renderType;
		}
		@WrapOperation(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/chunk/ChunkRendererRegion;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
		public BlockState getBlockState(ChunkRendererRegion instance, BlockPos pos, Operation<BlockState> original, @Local(ordinal = 2) BlockPos blockpos, @Local(argsOnly = true) ChunkRendererRegion arg2)
		{
			BlockState originalState = original.call(instance, pos);
			return originalState.isIn(SplatcraftTags.Blocks.RENDER_AS_CUBE) && InkBlockUtils.isInkedAny(((ChunkRegionAccessor) arg2).getWorld(), blockpos) ? SplatcraftBlocks.inkedBlock.get().getDefaultState() : originalState;
		}
	}
	@Mixin(BlockRenderManager.class)
	public static class BlockRenderManagerMixin
	{
		@WrapOperation(method = "renderBatched", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/BlockModelRenderer;tesselateBlock(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/render/RenderLayer;)V"))
		public void splatcraft$addBakedModel(BlockModelRenderer instance, BlockRenderView blockRenderView, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrixStack, VertexConsumer vertexConsumer, boolean b, Random random, long l, int i, ModelData modelData, RenderLayer renderLayer, Operation<Void> original)
		{
			original.call(instance, blockRenderView, new InkedBakedModel(model, ((ChunkRegionAccessor) blockRenderView).getWorld(), pos), state, pos, matrixStack, vertexConsumer, b, random, l, i, modelData, renderLayer);
		}
	}
}