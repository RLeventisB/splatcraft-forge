package net.splatcraft.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.render.chunk.SectionBuilder;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.worldink.ChunkInk;
import net.splatcraft.data.capabilities.worldink.ChunkInkCapability;
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
		public ChunkRenderTypeSet splatcraft$fixRenderLayer(BakedModel instance, BlockState state, Random random, net.neoforged.neoforge.client.model.data.ModelData modelData, Operation<ChunkRenderTypeSet> original, @Local(ordinal = 2) BlockPos blockpos, @Local(argsOnly = true) ChunkRendererRegion arg2)
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
		@WrapOperation(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/BlockRenderManager;getModel(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/render/model/BakedModel;"))
		public BakedModel splatcraft$appendInkModel(BlockRenderManager instance, BlockState state, Operation<BakedModel> original, @Local(ordinal = 2) BlockPos blockpos, @Local(argsOnly = true) ChunkRendererRegion arg2)
		{
			InkedBakedModel model = new InkedBakedModel(original.call(instance, state), ((ChunkRegionAccessor) arg2).getWorld(), blockpos);
//			splatcraft$world = null;
//			splatcraft$blockPos = null;
			return model;
		}
	}
}