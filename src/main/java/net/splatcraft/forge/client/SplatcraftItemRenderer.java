package net.splatcraft.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.forge.client.handlers.RendererHandler;
import org.jetbrains.annotations.NotNull;

public class SplatcraftItemRenderer extends BlockEntityWithoutLevelRenderer
{
    public static final SplatcraftItemRenderer INSTANCE = new SplatcraftItemRenderer();

    public SplatcraftItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(@NotNull ItemStack pStack, @NotNull ItemDisplayContext pDisplayContext, @NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay)
    {
        if (!RendererHandler.renderSubWeapon(pStack, pPoseStack, pBuffer, pPackedLight, pPackedOverlay))
        {
            super.renderByItem(pStack, pDisplayContext, pPoseStack, pBuffer, pPackedLight, pPackedOverlay);
        }
    }
}