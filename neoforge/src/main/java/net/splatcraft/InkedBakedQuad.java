package net.splatcraft;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.util.structs.InkColor;

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
				int uIndex = i * 8 + 4;
				int vIndex = i * 8 + 5;
				
				float originalU = original.getSprite().getUOffset(Float.intBitsToFloat(quadData[uIndex]));
				float originalV = original.getSprite().getVOffset(Float.intBitsToFloat(quadData[vIndex]));
				newData[uIndex] = Float.floatToRawIntBits(sprite.getU(originalU));
				newData[vIndex] = Float.floatToRawIntBits(sprite.getV(originalV));
			}
			return new InkedBakedQuad(original, newData, color, sprite, isGlowyQuad);
		}
		else
			return new InkedBakedQuad(original, quadData, color, sprite, isGlowyQuad);
	}
}
