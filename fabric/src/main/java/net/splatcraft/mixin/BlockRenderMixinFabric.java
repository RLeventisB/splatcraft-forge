package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.data.capabilities.chunkink.ChunkInkCapability;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.mixin.accessors.ChunkRegionAccessor;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.structs.RelativeBlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BlockRenderMixinFabric
{
	@Pseudo
	@Mixin(SectionCompiler.class)
	public static class ChunkRenderDispatcherMixinFabric
	{
		@Unique
		private static BlockPos splatcraft$blockPos;
		@Unique
		private static Level splatcraft$world;
		@Unique
		private static boolean splatcraft$overrideRender;
		@WrapOperation(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
		public BlockState getBlockState(RenderChunkRegion instance, BlockPos pos, Operation<BlockState> original)
		{
			return splatcraft$overrideRender ? SplatcraftBlocks.inkedBlock.value().defaultBlockState() : original.call(instance, pos);
		}
		@Inject(method = "compile", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
		public void splatcraft$getBlockData(SectionPos sectionPos, RenderChunkRegion renderRegion, VertexSorting vertexSorter, SectionBufferBuilderPack allocatorStorage, CallbackInfoReturnable<SectionCompiler.Results> cir, @Local(ordinal = 2) BlockPos blockPos3)
		{
			splatcraft$world = ((ChunkRegionAccessor) renderRegion).getLevel();
			splatcraft$blockPos = blockPos3;
			splatcraft$overrideRender = InkBlockUtils.isInkedAny(splatcraft$world, splatcraft$blockPos) && splatcraft$world.getBlockState(splatcraft$blockPos).is(SplatcraftTags.Blocks.RENDER_AS_CUBE);
		}
		@WrapOperation(method = "compile", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ItemBlockRenderTypes;getChunkRenderType(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/RenderType;"))
		public RenderType getRenderLayer(BlockState state, Operation<RenderType> original)
		{
			RenderType originalLayer = original.call(state);
			if (!ChunkInkCapability.has(splatcraft$world, splatcraft$blockPos))
				return originalLayer;
			ChunkInk chunkInk = ChunkInkCapability.get(splatcraft$world, splatcraft$blockPos);
			
			if (chunkInk != null && chunkInk.isntEmpty() && chunkInk.isInkedAny(RelativeBlockPos.fromAbsolute(splatcraft$blockPos)))
			{
				return RenderType.translucent();
			}
			return originalLayer;
		}
		@Mixin(SimpleBakedModel.class)
		public static class BakedModelQuadModifierMixinFabric
		{
			@Shadow
			@Final
			protected List<BakedQuad> unculledFaces;
			@Shadow
			@Final
			protected Map<Direction, List<BakedQuad>> culledFaces;
			@Inject(method = "getQuads", at = @At(value = "HEAD"), cancellable = true)
			public void splatcraft$modifyQuads(BlockState state, Direction face, RandomSource random, CallbackInfoReturnable<List<BakedQuad>> cir)
			{
				List<BakedQuad> originalList = face == null ? unculledFaces : culledFaces.get(face);
				if (splatcraft$world == null || splatcraft$blockPos == null)
				{
					return;
				}
				
				ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock(splatcraft$world, splatcraft$blockPos);
				if (ink != null && ink.isInkedAny() && ink.isInked(face.get3DDataValue()))
				{
					ChunkInk.InkEntry inkEntry = ink.get(face.get3DDataValue());
					if (inkEntry != null)
					{
						splatcraft$world = null;
						splatcraft$blockPos = null;
						ArrayList<BakedQuad> modifiedList = new ArrayList<>();
						for (BakedQuad quad : originalList)
						{
							modifiedList.add(new BakedQuad(Arrays.copyOf(quad.getVertices(), quad.getVertices().length),
								0, quad.getDirection(), ChunkInkHandler.Render.getInkedBlockSprite(), quad.isShade()));
						}
						cir.setReturnValue(modifiedList);
					}
				}
			}
		}
	}
}
