package net.splatcraft.neoforge;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.util.InkBlockUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record InkedBakedModel(BakedModel original, World world, BlockPos blockPos) implements BakedModel
{
	@Override
	public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, @NotNull Random random, @NotNull ModelData data, @Nullable RenderLayer renderType)
	{
		List<BakedQuad> quads = original.getQuads(state, face, random, data, renderType);
		return processQuads(quads, face);
	}
	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random)
	{
		List<BakedQuad> quads = original.getQuads(state, face, random);
		return processQuads(quads, face);
	}
	private List<BakedQuad> processQuads(List<BakedQuad> quads, Direction face)
	{
		if (face == null)
		{
			if (world != null && blockPos != null)
			{
				ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock(world, blockPos);
				if (ink != null)
				{
					List<BakedQuad> inkedQuads = new ArrayList<>(quads.size());
					for (BakedQuad quad : quads)
					{
						if (quad.getFace() == null)
							continue;
						
						ChunkInk.InkEntry entry = ink.get(quad.getFace().getId());
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
			}
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
		if (world == null || blockPos == null || face == null)
			return null;
		
		ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock(world, blockPos);
		if (ink != null)
		{
			return ink.get(face.getId());
		}
		return null;
	}
	@Override
	public boolean useAmbientOcclusion()
	{
		return original.useAmbientOcclusion();
	}
	@Override
	public boolean hasDepth()
	{
		return original.hasDepth();
	}
	@Override
	public boolean isSideLit()
	{
		return original.isSideLit();
	}
	@Override
	public boolean isBuiltin()
	{
		return true;
	}
	@Override
	public Sprite getParticleSprite()
	{
		return original.getParticleSprite();
	}
	@Override
	public ModelTransformation getTransformation()
	{
		return original.getTransformation();
	}
	@Override
	public ModelOverrideList getOverrides()
	{
		return original.getOverrides();
	}
	@Override
	public String toString()
	{
		return "InkedBakedModel[" +
			"original=" + original + ", " +
			"world=" + world + ", " +
			"blockPos=" + blockPos + ']';
	}
}
