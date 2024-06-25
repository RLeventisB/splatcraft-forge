package net.splatcraft.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
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
	public void renderByItem(@NotNull ItemStack stack, ItemTransforms.@NotNull TransformType transformType, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay)
	{
		if(!RendererHandler.renderSubWeapon(stack, transformType, poseStack, bufferSource, packedLight, Minecraft.getInstance().getDeltaFrameTime()))
			super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
	}
}
