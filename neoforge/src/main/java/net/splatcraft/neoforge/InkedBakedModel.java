package net.splatcraft.neoforge;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.splatcraft.data.capabilities.worldink.ChunkInk;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.util.InkBlockUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record InkedBakedModel(BakedModel original, World world, BlockPos blockPos) implements BakedModel
{
	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random)
	{
		List<BakedQuad> originalList = original.getQuads(state, face, random);
		if (world == null || blockPos == null)
			return originalList;
		
		ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock(world, blockPos);
		if (ink != null && ink.isInkedAny() && ink.isInked(face.getId()))
		{
			ChunkInk.InkEntry inkEntry = ink.get(face.getId());
			if (inkEntry != null)
			{
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
