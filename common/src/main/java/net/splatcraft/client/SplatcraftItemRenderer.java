package net.splatcraft.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.splatcraft.client.handlers.RendererHandler;
import org.jetbrains.annotations.NotNull;

public class SplatcraftItemRenderer extends BuiltinModelItemRenderer
{
    public static final SplatcraftItemRenderer INSTANCE = new SplatcraftItemRenderer();

    public SplatcraftItemRenderer()
    {
        super(MinecraftClient.getInstance().getBlockEntityRenderDispatcher(), MinecraftClient.getInstance().getEntityModelLoader());
    }

    @Override
    public void render(@NotNull ItemStack stack, @NotNull ModelTransformationMode modelMode, @NotNull MatrixStack poseStack, @NotNull VertexConsumerProvider bufferSource, int packedLight, int packedOverlay)
    {
        if (!RendererHandler.renderSubWeapon(stack, poseStack, bufferSource, packedLight, MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true)))
            super.render(stack, modelMode, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
