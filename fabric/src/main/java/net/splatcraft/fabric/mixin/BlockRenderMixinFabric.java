package net.splatcraft.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.BlockBufferAllocatorStorage;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.render.chunk.SectionBuilder;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BasicBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.worldink.ChunkInk;
import net.splatcraft.data.capabilities.worldink.ChunkInkCapability;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.mixin.accessors.ChunkRegionAccessor;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.RelativeBlockPos;
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
	@Mixin(SectionBuilder.class)
	public static class ChunkRenderDispatcherMixinFabric
	{
		@Unique
		private static BlockPos splatcraft$blockPos;
		@Unique
		private static World splatcraft$world;
		@Unique
		private static boolean splatcraft$overrideRender;
		@WrapOperation(method = "build", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/chunk/ChunkRendererRegion;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
		public BlockState getBlockState(ChunkRendererRegion instance, BlockPos pos, Operation<BlockState> original)
		{
			return splatcraft$overrideRender ? SplatcraftBlocks.inkedBlock.get().getDefaultState() : original.call(instance, pos);
		}
		@Inject(method = "build", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/chunk/ChunkRendererRegion;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
		public void splatcraft$getBlockData(ChunkSectionPos sectionPos, ChunkRendererRegion renderRegion, VertexSorter vertexSorter, BlockBufferAllocatorStorage allocatorStorage, CallbackInfoReturnable<SectionBuilder.RenderData> cir, @Local(ordinal = 2) BlockPos blockPos3)
		{
			splatcraft$world = ((ChunkRegionAccessor) renderRegion).getWorld();
			splatcraft$blockPos = blockPos3;
			splatcraft$overrideRender = InkBlockUtils.isInkedAny(splatcraft$world, splatcraft$blockPos) && splatcraft$world.getBlockState(splatcraft$blockPos).isIn(SplatcraftTags.Blocks.RENDER_AS_CUBE);
		}
		@WrapOperation(method = "build", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/RenderLayers;getBlockLayer(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/render/RenderLayer;"))
		public RenderLayer getRenderLayer(BlockState state, Operation<RenderLayer> original)
		{
			ChunkInk chunkInk = ChunkInkCapability.getOrCreate(splatcraft$world, splatcraft$blockPos);
			RenderLayer originalLayer = original.call(state);
			
			if (chunkInk != null && chunkInk.isntEmpty() && chunkInk.isInkedAny(RelativeBlockPos.fromAbsolute(splatcraft$blockPos)))
			{
				return RenderLayer.getTranslucent();
			}
			return originalLayer;
		}
		@Mixin(BasicBakedModel.class)
		public static class BakedModelQuadModifierMixinFabric
		{
			@Shadow
			@Final
			protected List<BakedQuad> quads;
			@Shadow
			@Final
			protected Map<Direction, List<BakedQuad>> faceQuads;
			@Inject(method = "getQuads", at = @At(value = "HEAD"), cancellable = true)
			public void splatcraft$modifyQuads(BlockState state, Direction face, Random random, CallbackInfoReturnable<List<BakedQuad>> cir)
			{
				List<BakedQuad> originalList = face == null ? quads : faceQuads.get(face);
				if (splatcraft$world == null || splatcraft$blockPos == null)
				{
					return;
				}
				
				ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock(splatcraft$world, splatcraft$blockPos);
				if (ink != null && ink.isInkedAny() && ink.isInked(face.getId()))
				{
					ChunkInk.InkEntry inkEntry = ink.get(face.getId());
					if (inkEntry != null)
					{
						splatcraft$world = null;
						splatcraft$blockPos = null;
						ArrayList<BakedQuad> modifiedList = new ArrayList<>();
						for (BakedQuad quad : originalList)
						{
							modifiedList.add(new BakedQuad(Arrays.copyOf(quad.getVertexData(), quad.getVertexData().length),
								0, quad.getFace(), ChunkInkHandler.Render.getInkedBlockSprite(), quad.hasShade()));
						}
						cir.setReturnValue(modifiedList);
					}
				}
			}
		}
	}
}
