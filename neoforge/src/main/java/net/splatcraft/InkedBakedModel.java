package net.splatcraft;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.util.InkBlockUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record InkedBakedModel(BakedModel original, @NotNull ChunkInk.BlockEntry inkEntry) implements BakedModel
{
	public static BakedModel tryCreateFor(BakedModel original, Level level, BlockPos pos)
	{
		ChunkInk.BlockEntry inkBlock = InkBlockUtils.getInkBlock(level, pos);
		if (inkBlock == null)
			return original;
		return new InkedBakedModel(original, inkBlock);
	}
	@Override
	public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, @NotNull RandomSource random, @NotNull ModelData data, @Nullable RenderType renderType)
	{
		List<BakedQuad> quads = original.getQuads(state, face, random, data, renderType);
		return processQuads(quads, face);
	}
	@Override
	public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, @NotNull RandomSource random)
	{
		List<BakedQuad> quads = original.getQuads(state, face, random);
		return processQuads(quads, face);
	}
	private List<BakedQuad> processQuads(List<BakedQuad> quads, Direction face)
	{
		if (face == null)
		{
			List<BakedQuad> inkedQuads = new ArrayList<>(quads.size());
			for (BakedQuad quad : quads)
			{
				if (quad.getDirection() == null)
					continue;
				
				ChunkInk.InkEntry entry = inkEntry.get(quad.getDirection().get3DDataValue());
				if (entry != null)
				{
					inkedQuads.addAll(withSetData(quad, entry));
				}
				else
				{
					inkedQuads.add(quad);
				}
			}
			return inkedQuads;
		}
		else
		{
			ChunkInk.InkEntry inkData = getEntry(face);
			if (inkData != null)
				return withSetData(quads, inkData);
		}
		return quads;
	}
	private List<BakedQuad> withSetData(BakedQuad quad, ChunkInk.InkEntry data)
	{
		return switch (data.type())
		{
			case NORMAL -> List.of(InkedBakedQuad.createQuad(quad, data.color(), true, false));
			case GLOWING -> List.of(
				InkedBakedQuad.createQuad(quad, data.color(), true, false),
				InkedBakedQuad.createQuad(quad, data.color(), true, true)
			);
			case CLEAR -> List.of(InkedBakedQuad.createQuad(quad, data.color(), false, false));
		};
	}
	private List<BakedQuad> withSetData(List<BakedQuad> quads, ChunkInk.InkEntry data)
	{
		List<BakedQuad> inkedQuads = new ArrayList<>(quads.size());
		switch (data.type())
		{
			case NORMAL:
				for (BakedQuad quad : quads)
				{
					inkedQuads.add(InkedBakedQuad.createQuad(quad, data.color(), true, false));
				}
				
				break;
			
			case GLOWING:
				for (BakedQuad quad : quads)
				{
					inkedQuads.add(InkedBakedQuad.createQuad(quad, data.color(), true, false));
					inkedQuads.add(InkedBakedQuad.createQuad(quad, data.color(), true, true));
				}
				
				break;
			
			case CLEAR:
				for (BakedQuad quad : quads)
				{
					inkedQuads.add(InkedBakedQuad.createQuad(quad, data.color(), false, false));
				}
				break;
		}
		return inkedQuads;
	}
	public ChunkInk.InkEntry getEntry(Direction face)
	{
		if (face == null)
			return null;
		
		return inkEntry.get(face.get3DDataValue());
	}
	@Override
	public boolean useAmbientOcclusion()
	{
		return original.useAmbientOcclusion();
	}
	@Override
	public boolean isGui3d()
	{
		return original.isGui3d();
	}
	@Override
	public boolean usesBlockLight()
	{
		return original.usesBlockLight();
	}
	@Override
	public boolean isCustomRenderer()
	{
		return true;
	}
	@Override
	public @NotNull TextureAtlasSprite getParticleIcon()
	{
		return original.getParticleIcon();
	}
	@Override
	public @NotNull ItemTransforms getTransforms()
	{
		return original.getTransforms();
	}
	@Override
	public @NotNull ItemOverrides getOverrides()
	{
		return original.getOverrides();
	}
}
