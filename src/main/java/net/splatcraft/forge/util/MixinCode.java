package net.splatcraft.forge.util;

// this class only exists so hot swapping mixin code is possible lol
// also for the stack trace ig uess

// edit after one day: this class WASNT used at all LOL
public class MixinCode
{
    /*@Unique
    public static void splacraft$drawInkedFace(ChunkModelBuilder builder, Material material, BakedQuadView quad, int[] colors, QuadLightData light, ModelQuadOrientation orientation, ChunkVertexEncoder.Vertex[] vertices, ChunkMeshBufferBuilder vertexBuffer, ChunkInk.BlockEntry ink)
    {
        if (ink != null)
        {
            int index = quad.getLightFace().get3DDataValue();
            if (ink.isInked(index))
            {
                if (ink.type(index) == InkBlockUtils.InkType.GLOWING)
                    splatcraft$drawFace(builder, material, quad, light, orientation, vertices.clone(), colors, 0xFFFFFFFF, true, ChunkInkHandler.Render.getGlitterSprite(), vertexBuffer);

                if (ink.inmutable)
                    splatcraft$drawFace(builder, material, quad, light, orientation, vertices.clone(), colors, 0xFFFFFFFF, false, ChunkInkHandler.Render.getPermanentInkSprite(), vertexBuffer);

                if (quad instanceof BakedQuad bakedQuad)
//                if (false)
                {
                    SodiumMixin.SpriteAccessor mixin = (SodiumMixin.SpriteAccessor) bakedQuad;
                    TextureAtlasSprite sprite = ChunkInkHandler.Render.getInkedBlockSprite();
                    mixin.setSprite(sprite);
//                    mixin.setVertices(ChunkInkHandler.Render.getInkedBlockQuad(quad.getLightFace()).getVertices().clone());

                    float[] rgbs = ColorUtils.hexToRGB(ink.color(index));
                    int color = ColorABGR.pack(rgbs[0], rgbs[1], rgbs[2]);

                    for (int dstIndex = 0; dstIndex < 4; dstIndex++)
                    {
                        int srcIndex = orientation.getVertexIndex(dstIndex);
                        ChunkVertexEncoder.Vertex out = vertices[dstIndex];

                        int rgb = colors != null ? colors[srcIndex] : quad.getColor(srcIndex);
                        rgb = ColorMixer.mul(rgb, color);
                        out.color = ColorABGR.withAlpha(rgb, light.br[srcIndex]);

                        out.u = quad.getTexU(srcIndex);
                        out.v = quad.getTexV(srcIndex);

                        out.light = ModelQuadUtil.mergeBakedLight(quad.getLight(srcIndex), light.lm[srcIndex]);
                    }

//                    vertexBuffer.push(vertices, material);
                }

//                    ci.cancel(); // bye mod compatibility though :( NVM I DID THE ACCESSOR THING (though the vectors will be calculated twice :()
            }
            else
            {
                if (ink.inmutable)
                    splatcraft$drawFace(builder, material, quad, light, orientation, vertices, colors, 0xFFFFFFFF, false, ChunkInkHandler.Render.getPermanentInkSprite(), vertexBuffer);
            }
        }
    }

    @Unique
    public static void splatcraft$drawFace(ChunkModelBuilder builder, Material material, BakedQuadView quad, QuadLightData light, ModelQuadOrientation orientation, ChunkVertexEncoder.Vertex[] vertices, int[] originalColors, int color, boolean emissive, TextureAtlasSprite sprite, ChunkMeshBufferBuilder vertexBuffer)
    {
        for (int dstIndex = 0; dstIndex < 4; dstIndex++)
        {
            int srcIndex = orientation.getVertexIndex(dstIndex);
            ChunkVertexEncoder.Vertex out = vertices[dstIndex];

            int rgb = originalColors != null ? originalColors[srcIndex] : quad.getColor(srcIndex);
            rgb = ColorMixer.mul(rgb, color);
            out.color = ColorABGR.withAlpha(rgb, light.br[srcIndex] + (emissive ? 0.5f : 0));
            if (sprite != null)
            {
                Direction.Axis axis = quad.getLightFace().getAxis();
                out.u = sprite.getU0() + (axis.equals(Direction.Axis.X) ? out.z : out.x) * (sprite.getU1() - sprite.getU0());
                out.v = sprite.getV0() + (axis.equals(Direction.Axis.Y) ? out.z : out.y) * (sprite.getV1() - sprite.getV0());
            }
            else
            {
                out.u = quad.getTexU(srcIndex);
                out.v = quad.getTexV(srcIndex);
            }
            out.light = emissive ? LightTexture.pack(15, 15) : ModelQuadUtil.mergeBakedLight(quad.getLight(srcIndex), light.lm[srcIndex]);
        }

        vertexBuffer.push(vertices.clone(), material);
        builder.addSprite(sprite);
    }*/
}
