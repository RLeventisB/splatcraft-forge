package net.splatcraft.client.renderer.tileentity;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.splatcraft.Splatcraft;
import net.splatcraft.blocks.ColoredBarrierBlock;
import net.splatcraft.blocks.IColoredBlock;
import net.splatcraft.blocks.StageBarrierBlock;
import net.splatcraft.tileentities.StageBarrierTileEntity;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

public class StageBarrierTileEntityRenderer implements BlockEntityRenderer<StageBarrierTileEntity>
{
    protected static final RenderPhase.Transparency TRANSLUCENT_TRANSPARENCY = new RenderPhase.Transparency("translucent_transparency", () ->
    {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    }, () ->
    {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });
    public static final RenderLayer BARRIER_RENDER = RenderLayer.of("splatcraft:stage_barriers", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 262144, false, true, RenderLayer.MultiPhaseParameters.builder()
        .lightmap(new RenderPhase.Lightmap(true)).texture(new RenderPhase.Texture(Splatcraft.identifierOf("textures/block/allowed_color_barrier_fancy.png"), false, true))
        .transparency(TRANSLUCENT_TRANSPARENCY).build(true));

    public StageBarrierTileEntityRenderer(BlockEntityRendererFactory.Context context)
    {
    }

    private static boolean shouldRenderSide(StageBarrierTileEntity te, Direction side)
    {
        BlockPos relativePos = te.getPos().offset(side);
        BlockState relativeState = te.getWorld().getBlockState(relativePos);

        if (!ClientUtils.shouldRenderSide(te, side)) return false;

        if (relativeState.getBlock() instanceof ColoredBarrierBlock block && te.getWorld().getBlockState(te.getPos()).getBlock() instanceof ColoredBarrierBlock coloredBlock)
            return block.canAllowThrough(relativePos, ClientUtils.getClientPlayer()) !=
                coloredBlock.canAllowThrough(te.getPos(), ClientUtils.getClientPlayer());

        return !(relativeState.getBlock() instanceof StageBarrierBlock);
    }

    private void addVertex(VertexConsumer builder, MatrixStack matrixStack, float x, float y, float z, float textureX, float textureY, float r, float g, float b, float a)
    {
        builder.vertex(matrixStack.peek().getPositionMatrix(), x, y, z)
            .color(r, g, b, a)
            .texture(textureX, textureY)
            .light(0, 240)
            .normal(1, 0, 0)
        ;
    }

    @Override
    public void render(StageBarrierTileEntity tileEntity, float partialTicks, @NotNull MatrixStack matrixStack, @NotNull VertexConsumerProvider buffer, int combinedLight, int combinedOverlay)
    {
        float activeTime = tileEntity.getActiveTime();
        Block block = tileEntity.getCachedState().getBlock();

        if (activeTime <= 0 || !(block instanceof StageBarrierBlock))
        {
            return;
        }
        Identifier registeredBlock = Registries.BLOCK.getId(block);

        Identifier textureLoc = Splatcraft.identifierOf("block/" + registeredBlock.getPath() + ((MinecraftClient.getInstance().options.getGraphicsMode().getValue().getId()) > 0 ? "_fancy" : ""));

        Sprite sprite = MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).apply(textureLoc);
        VertexConsumer builder = buffer.getBuffer(MinecraftClient.isFabulousGraphicsOrBetter() ? RenderLayer.getTranslucentMovingBlock() : RenderLayer.getTranslucent());

        float alpha = activeTime / tileEntity.getMaxActiveTime();
        float[] rgb = new float[]{1, 1, 1};
        if (tileEntity.getCachedState().getBlock() instanceof IColoredBlock)
        {
            InkColor color = ColorUtils.getColorLockedIfConfig(ColorUtils.getInkColorOrInverted(tileEntity.getWorld(), tileEntity.getPos()));
            rgb = color.getRGB();
        }

        if (shouldRenderSide(tileEntity, Direction.NORTH))
        {
            addVertex(builder, matrixStack, 0, 1, 0, sprite.getMinU(), sprite.getMaxV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 1, 1, 0, sprite.getMaxU(), sprite.getMaxV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 1, 0, 0, sprite.getMaxU(), sprite.getMinV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 0, 0, 0, sprite.getMinU(), sprite.getMinV(), rgb[0], rgb[1], rgb[2], alpha);
        }

        if (shouldRenderSide(tileEntity, Direction.SOUTH))
        {
            addVertex(builder, matrixStack, 0, 0, 1, sprite.getMinU(), sprite.getMinV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 1, 0, 1, sprite.getMaxU(), sprite.getMinV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 1, 1, 1, sprite.getMaxU(), sprite.getMaxV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 0, 1, 1, sprite.getMinU(), sprite.getMaxV(), rgb[0], rgb[1], rgb[2], alpha);
        }

        if (shouldRenderSide(tileEntity, Direction.WEST))
        {
            addVertex(builder, matrixStack, 0, 0, 0, sprite.getMinU(), sprite.getMinV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 0, 0, 1, sprite.getMinU(), sprite.getMaxV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 0, 1, 1, sprite.getMaxU(), sprite.getMaxV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 0, 1, 0, sprite.getMaxU(), sprite.getMinV(), rgb[0], rgb[1], rgb[2], alpha);
        }

        if (shouldRenderSide(tileEntity, Direction.EAST))
        {
            addVertex(builder, matrixStack, 1, 0, 0, sprite.getMinU(), sprite.getMinV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 1, 1, 0, sprite.getMaxU(), sprite.getMinV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 1, 1, 1, sprite.getMaxU(), sprite.getMaxV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 1, 0, 1, sprite.getMinU(), sprite.getMaxV(), rgb[0], rgb[1], rgb[2], alpha);
        }

        if (shouldRenderSide(tileEntity, Direction.DOWN))
        {
            addVertex(builder, matrixStack, 0, 0, 0, sprite.getMinU(), sprite.getMinV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 1, 0, 0, sprite.getMaxU(), sprite.getMinV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 1, 0, 1, sprite.getMaxU(), sprite.getMaxV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 0, 0, 1, sprite.getMinU(), sprite.getMaxV(), rgb[0], rgb[1], rgb[2], alpha);
        }

        if (shouldRenderSide(tileEntity, Direction.UP))
        {
            addVertex(builder, matrixStack, 0, 1, 1, sprite.getMinU(), sprite.getMaxV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 1, 1, 1, sprite.getMaxU(), sprite.getMaxV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 1, 1, 0, sprite.getMaxU(), sprite.getMinV(), rgb[0], rgb[1], rgb[2], alpha);
            addVertex(builder, matrixStack, 0, 1, 0, sprite.getMinU(), sprite.getMinV(), rgb[0], rgb[1], rgb[2], alpha);
        }
    }
}
