package net.splatcraft.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.BlockBufferAllocatorStorage;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.render.chunk.SectionBuilder;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.extensions.IBakedModelExtension;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.worldink.ChunkInk;
import net.splatcraft.data.capabilities.worldink.ChunkInkCapability;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.mixin.accessors.ChunkRegionAccessor;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.RelativeBlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockRenderMixinForge
{
	@Mixin(SectionBuilder.class)
	public static class ChunkRenderDispatcherMixinForge
	{
		@Unique
		private static BlockPos splatcraft$blockPos;
		@Unique
		private static World splatcraft$world;
		@Unique
		private static boolean splatcraft$overrideRender;
		@Unique
		private static BlockPos getSplatcraft$blockPos()
		{
			return splatcraft$blockPos;
		}
		@Unique
		private static void setSplatcraft$blockPos(BlockPos splatcraft$blockPos)
		{
			ChunkRenderDispatcherMixinForge.splatcraft$blockPos = splatcraft$blockPos;
		}
		@Unique
		private static World getSplatcraft$world()
		{
			return splatcraft$world;
		}
		@Unique
		private static void setSplatcraft$world(World splatcraft$world)
		{
			ChunkRenderDispatcherMixinForge.splatcraft$world = splatcraft$world;
		}
		@Inject(method = "compile", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/chunk/ChunkRendererRegion;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
		public void splatcraft$getBlockData(ChunkSectionPos pos, ChunkRendererRegion region, VertexSorter arg3, BlockBufferAllocatorStorage arg4, List<AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers, CallbackInfoReturnable<SectionBuilder.RenderData> cir, @Local(ordinal = 2) BlockPos blockpos2)
		{
			splatcraft$world = ((ChunkRegionAccessor) region).getWorld();
			splatcraft$blockPos = blockpos2;
			splatcraft$overrideRender = InkBlockUtils.isInkedAny(splatcraft$world, splatcraft$blockPos) && splatcraft$world.getBlockState(splatcraft$blockPos).isIn(SplatcraftTags.Blocks.RENDER_AS_CUBE);
		}
		@WrapOperation(method = "compile", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/model/BakedModel;getRenderTypes(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/random/Random;Lnet/neoforged/neoforge/client/model/data/ModelData;)Lnet/neoforged/neoforge/client/ChunkRenderTypeSet;"))
		public ChunkRenderTypeSet getRenderLayer(BakedModel instance, BlockState state, Random random, net.neoforged.neoforge.client.model.data.ModelData modelData, Operation<ChunkRenderTypeSet> original)
		{
			ChunkRenderTypeSet renderType = original.call(instance, state, random, modelData);
			if (!ChunkInkCapability.has(splatcraft$world, splatcraft$blockPos))
				return renderType;
			ChunkInk chunkInk = ChunkInkCapability.get(splatcraft$world, splatcraft$blockPos);
			if (chunkInk.isntEmpty() && chunkInk.isInkedAny(RelativeBlockPos.fromAbsolute(splatcraft$blockPos)))
				return ChunkRenderTypeSet.union(renderType, ChunkRenderTypeSet.of(RenderLayer.getTranslucent()));
			return renderType;
		}
		@WrapOperation(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/chunk/ChunkRendererRegion;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
		public BlockState getBlockState(ChunkRendererRegion instance, BlockPos pos, Operation<BlockState> original)
		{
			return splatcraft$overrideRender ? SplatcraftBlocks.inkedBlock.get().getDefaultState() : original.call(instance, pos);
		}
		@Mixin(IBakedModelExtension.class)
		public interface BakedModelQuadModifierMixin
		{
			@SuppressWarnings("UnnecessarilyQualifiedStaticUsage")
			@WrapOperation(method = "getQuads", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/model/BakedModel;getQuads(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;Lnet/minecraft/util/math/random/Random;)Ljava/util/List;"))
			default List<BakedQuad> splatcraft$modifyQuads(BakedModel instance, BlockState state, Direction direction, Random random, Operation<List<BakedQuad>> original)
			{
				List<BakedQuad> originalList = original.call(instance, state, direction, random);
				if (BlockRenderMixinForge.ChunkRenderDispatcherMixinForge.getSplatcraft$world() == null || BlockRenderMixinForge.ChunkRenderDispatcherMixinForge.getSplatcraft$blockPos() == null)
					return originalList;
				
				ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock(BlockRenderMixinForge.ChunkRenderDispatcherMixinForge.getSplatcraft$world(), BlockRenderMixinForge.ChunkRenderDispatcherMixinForge.getSplatcraft$blockPos());
				if (ink != null && ink.isInkedAny() && ink.isInked(direction.getId()))
				{
					ChunkInk.InkEntry inkEntry = ink.get(direction.getId());
					if (inkEntry != null)
					{
						BlockRenderMixinForge.ChunkRenderDispatcherMixinForge.setSplatcraft$world(null);
						BlockRenderMixinForge.ChunkRenderDispatcherMixinForge.setSplatcraft$blockPos(null);
						ArrayList<BakedQuad> modifiedList = new ArrayList<>();
						for (BakedQuad quad : originalList)
						{
							modifiedList.add(new BakedQuad(Arrays.copyOf(quad.getVertexData(), quad.getVertexData().length),
								0, quad.getFace(), ChunkInkHandler.Render.getInkedBlockSprite(), quad.hasShade()));
						}
						return modifiedList;
					}
				}
				return originalList;
			}
		}
	}
}