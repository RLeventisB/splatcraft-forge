package net.splatcraft.neoforge;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.util.InkColor;

public class InkedBakedQuad extends BakedQuad
{
	private InkedBakedQuad(BakedQuad original, int[] vertexData, InkColor color, TextureAtlasSprite sprite, boolean isGlowyQuad)
	{
		super(vertexData, isGlowyQuad ? -1 : color.getColorWithAlpha(255), original.getDirection(), sprite, isGlowyQuad || original.isShade(), original.hasAmbientOcclusion());
	}
	public static InkedBakedQuad createQuad(BakedQuad original, InkColor color, boolean replaceSprite, boolean isGlowyQuad)
	{
		TextureAtlasSprite sprite;
		if (replaceSprite)
		{
			if (isGlowyQuad)
				sprite = ChunkInkHandler.Render.getGlitterSprite();
			else
				sprite = ChunkInkHandler.Render.getInkedBlockSprite();
		}
		else
			sprite = original.getSprite();
		int[] quadData = original.getVertices();
		if (replaceSprite)
		{
			int[] newData = new int[32];
			System.arraycopy(quadData, 0, newData, 0, 32);
			// quad data:
			// index 0, 1, 2: x, y, z respectively
			// index 3: literally only -1
			// index 4, 5: uv data
			for (int i = 0; i < 4; i++)
			{
				newData[i * 8 + 4] = Float.floatToRawIntBits(sprite.getU(ChunkInkHandler.Render.defaultUv.getU(i)));
				newData[i * 8 + 5] = Float.floatToRawIntBits(sprite.getV(ChunkInkHandler.Render.defaultUv.getV(i)));
			}
			return new InkedBakedQuad(original, newData, color, sprite, isGlowyQuad);
		}
		else
			return new InkedBakedQuad(original, quadData, color, sprite, isGlowyQuad);
	}
}
