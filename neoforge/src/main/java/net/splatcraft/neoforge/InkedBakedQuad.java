package net.splatcraft.neoforge;

import net.minecraft.client.render.model.BakedQuad;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkColor;

public class InkedBakedQuad extends BakedQuad
{
	public InkColor color;
	public InkBlockUtils.InkType type;
	public InkedBakedQuad(BakedQuad original, InkColor color, InkBlockUtils.InkType type, boolean isGlowyQuad)
	{
		super(original.getVertexData(), isGlowyQuad ? -1 : 0, original.getFace(), original.getSprite(), original.hasShade(), original.hasAmbientOcclusion());
		this.color = color;
		this.type = type;
	}
}
