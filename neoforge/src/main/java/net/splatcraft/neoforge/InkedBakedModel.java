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
import net.splatcraft.data.capabilities.worldink.ChunkInk;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.util.InkBlockUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record InkedBakedModel(BakedModel original, World world, BlockPos blockPos) implements BakedModel
{
	@Override
	public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, @NotNull Random random, @NotNull ModelData data, @Nullable RenderLayer renderType)
	{
		return getModel(face).getQuads(state, face, random, data, renderType);
	}
	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random)
	{
		return getModel(face).getQuads(state, face, random);
	}
	public BakedModel getModel(Direction face)
	{
		if (world == null || blockPos == null || face == null)
			return original;
		
		ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock(world, blockPos);
		if (ink != null)
		{
			ChunkInk.InkEntry inkEntry = ink.get(face.getId());
			if (inkEntry != null)
			{
				return ChunkInkHandler.Render.getInkedBakedModel();
			}
		}
		return original;
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
