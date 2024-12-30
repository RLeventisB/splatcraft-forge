package net.splatcraft.neoforge;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.handlers.ChunkInkHandler;
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
		Pair<BakedModel, ChunkInk.InkEntry> modelData = getModel(face);
		if (modelData.getRight() == null)
			return modelData.getLeft().getQuads(state, face, random, data, renderType);
		
		return withSetData(modelData.getLeft().getQuads(state, face, random, data, renderType), modelData.getRight(), face);
	}
	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random)
	{
		Pair<BakedModel, ChunkInk.InkEntry> modelData = getModel(face);
		if (modelData.getRight() == null)
			return modelData.getLeft().getQuads(state, face, random);
		return withSetData(modelData.getLeft().getQuads(state, face, random), modelData.getRight(), face);
	}
	private List<BakedQuad> withSetData(List<BakedQuad> quads, ChunkInk.InkEntry data, @Nullable Direction face)
	{
		List<BakedQuad> inkedQuads = new ArrayList<>(quads.size());
		boolean hasGlowing = data.type() == InkBlockUtils.InkType.GLOWING;
		for (BakedQuad quad : quads)
		{
			inkedQuads.add(new InkedBakedQuad(quad, data.color(), data.type(), false));
		}
		if (hasGlowing && face != null)
		{
			inkedQuads.add(new InkedBakedQuad(ChunkInkHandler.Render.getGlitterQuad()[face.getId()], data.color(), data.type(), true));
		}
		return inkedQuads;
	}
	public Pair<BakedModel, ChunkInk.InkEntry> getModel(Direction face)
	{
		if (world == null || blockPos == null || face == null)
			return new Pair<>(original, null);
		
		ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock(world, blockPos);
		if (ink != null)
		{
			ChunkInk.InkEntry inkEntry = ink.get(face.getId());
			if (inkEntry != null)
			{
				if (inkEntry.type() == InkBlockUtils.InkType.CLEAR)
					return new Pair<>(original, inkEntry);
				return new Pair<>(ChunkInkHandler.Render.getInkedBakedModel(), inkEntry);
			}
		}
		return new Pair<>(original, null);
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
