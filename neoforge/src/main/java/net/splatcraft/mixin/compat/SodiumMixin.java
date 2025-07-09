package net.splatcraft.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.InkedBakedModel;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.util.InkBlockUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

public class SodiumMixin
{
	@Mixin(ChunkBuilderMeshingTask.class)
	public abstract static class ChunkMeshBuilderMixin
	{
		@ModifyArg(method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;renderModel(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V"))
		public BakedModel splatcraft$addInkedModel(BakedModel model, @Local BlockRenderCache cache, @Local(ordinal = 0) BlockPos.MutableBlockPos pos)
		{
			LevelSlice levelSlice = cache.getWorldSlice();
			ClientLevel level = ((LevelSliceAccessor) (Object) levelSlice).getLevel();
			
			model = InkedBakedModel.tryCreateFor(model, level, pos);
			
			return model;
		}
		@WrapOperation(method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
			at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;"))
		public BlockState splatcraft$renderAsCubeTagOverride(LevelSlice instance, int blockX, int blockY, int blockZ, Operation<BlockState> original, @Local(ordinal = 0) BlockPos.MutableBlockPos pos)
		{
			BlockState originalState = original.call(instance, blockX, blockY, blockZ);
			ClientLevel level = ((LevelSliceAccessor) (Object) instance).getLevel();
			
			return originalState.is(SplatcraftTags.Blocks.RENDER_AS_CUBE) && InkBlockUtils.isInkedAny(level, pos) ? SplatcraftBlocks.inkedBlock.value().defaultBlockState() : originalState;
		}
	}
	@Mixin(LevelSlice.class)
	public interface LevelSliceAccessor
	{
		@Accessor
		ClientLevel getLevel();
	}
	@Mixin(value = AbstractBlockRenderContext.class, remap = false)
	public abstract static class AbstractBlockThingMixin
	{
		@Shadow
		protected RenderType type;
		@Unique
		public RenderType splatcraft$oldRenderType;
		@Inject(method = "bufferDefaultModel", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"))
		public void splatcraft$modifyRenderType(BakedModel model, BlockState state, CallbackInfo ci)
		{
			// simly doing a wrapoperation with the transparent material wont work because their class is
			// from a fabric jar :(
			splatcraft$oldRenderType = type;
			type = RenderType.translucent();
		}
		@Inject(method = "bufferDefaultModel", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/caffeinemc/mods/sodium/client/render/frapi/mesh/MutableQuadViewImpl;fromVanilla(Lnet/minecraft/client/renderer/block/model/BakedQuad;Lnet/fabricmc/fabric/api/renderer/v1/material/RenderMaterial;Lnet/minecraft/core/Direction;)Lnet/caffeinemc/mods/sodium/client/render/frapi/mesh/MutableQuadViewImpl;"))
		public void splatcraft$restoreRenderType(BakedModel model, BlockState state, CallbackInfo ci)
		{
			if (splatcraft$oldRenderType != null)
				type = splatcraft$oldRenderType;
			splatcraft$oldRenderType = null;
		}
	/*
			@Inject(method = "bufferDefaultModel", at = @At(value = "HEAD"))
			public void splatcraft$getInkData(BakedModel model, BlockState state, CallbackInfo ci,
											  @Share(value = "blockEntry", namespace = Splatcraft.MODID) LocalRef<Optional<ChunkInk.BlockEntry>> blockEntry
			)
			{
				blockEntry.set(Optional.empty());
				
				if (level instanceof Level actualLevel)
				{
					model = new InkedBakedModel(model, actualLevel, pos);
				}
				if (level instanceof LevelSlice slice)
				{
					model = new InkedBakedModel(model, ((LevelSliceAccessor) (Object) slice).getLevel(), pos);
				}
			}
			@WrapOperation(method = "bufferDefaultModel", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/services/PlatformModelAccess;getQuads(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;Lnet/minecraft/client/renderer/RenderType;Lnet/caffeinemc/mods/sodium/client/services/SodiumModelData;)Ljava/util/List;"))
			public List<BakedQuad> splatcraft$modifyQuadList(PlatformModelAccess instance, BlockAndTintGetter blockAndTintGetter, BlockPos blockPos, BakedModel bakedModel, BlockState blockState, Direction direction, RandomSource randomSource, RenderType renderType, SodiumModelData sodiumModelData, Operation<List<BakedQuad>> original,
															 @Local Direction cullFace, @Local RandomSource random,
															 @Share(value = "blockEntry", namespace = Splatcraft.MODID) LocalRef<Optional<ChunkInk.BlockEntry>> blockEntry,
															 @Share(value = "inkEntry", namespace = Splatcraft.MODID) LocalRef<Optional<ChunkInk.InkEntry>> sharedInkEntry
			
			)
			{
				List<BakedQuad> originalList = original.call(instance, blockAndTintGetter, blockPos, bakedModel, blockState, direction, randomSource, renderType, sodiumModelData);
				sharedInkEntry.set(Optional.empty());
				if (cullFace == null)
				{
					return originalList;
				}
				
				Optional<ChunkInk.InkEntry> inkEntry = blockEntry.get().map(v -> v.get(cullFace.get3DDataValue()));
				sharedInkEntry.set(inkEntry);
				
				List<BakedQuad> quads = new ArrayList<>();
				if (inkEntry.isPresent() && inkEntry.get().type() != InkBlockUtils.InkType.CLEAR)
				{
					quads.addAll(ChunkInkHandler.Render.getInkedBlockQuad(cullFace, random));
					if (inkEntry.get().type() == InkBlockUtils.InkType.GLOWING)
					{
						quads.add(ChunkInkHandler.Render.getGlitterQuad()[cullFace.get3DDataValue()]);
					}
				}
				else
				{
					quads.addAll(originalList);
				}
				// lmfao
				if (blockEntry.get().isPresent() && blockEntry.get().get().immutable)
				{
					quads.add(ChunkInkHandler.Render.getPermaInkQuads()[cullFace.get3DDataValue()]);
				}
				return quads;
			}
			@Shadow
			protected BlockAndTintGetter level;
			@Shadow
			protected BlockPos pos;
	*/
	}
	@Mixin(value = BlockRenderer.class, remap = false)
	public abstract static class BlockRendererMixin
	{
		@WrapOperation(method = "renderModel", remap = false, at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/model/color/ColorProviderRegistry;getColorProvider(Lnet/minecraft/world/level/block/Block;)Lnet/caffeinemc/mods/sodium/client/model/color/ColorProvider;"))
		public ColorProvider<BlockState> splatcraft$modifyColorProvider(ColorProviderRegistry instance, Block block, Operation<ColorProvider<BlockState>> original,
		                                                                @Local(argsOnly = true) BakedModel bakedModel
		)
		{
			ColorProvider<BlockState> provider = original.call(instance, block);
			if (bakedModel instanceof InkedBakedModel inkedModel)
			{
				return splatcraft$getColorProvider(inkedModel.inkEntry().getActiveFlag(), provider);
			}
			return provider;
		}
		@Unique
		private static ColorProvider<BlockState> splatcraft$getColorProvider(byte activeFlag, ColorProvider<BlockState> provider)
		{
			return (slice, pos, scratchPos, state, quad, output) ->
			{
				int bit = 1 << quad.getLightFace().get3DDataValue();
				if ((activeFlag & bit) != 0)
				{
					Arrays.fill(output, quad.getColorIndex());
					return;
				}
				provider.getColors(slice, pos, scratchPos, state, quad, output);
			};
		}
	}
}
