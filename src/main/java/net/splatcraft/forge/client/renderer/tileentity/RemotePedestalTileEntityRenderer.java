package net.splatcraft.forge.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.forge.tileentities.RemotePedestalTileEntity;
import org.jetbrains.annotations.NotNull;

public class RemotePedestalTileEntityRenderer implements BlockEntityRenderer<RemotePedestalTileEntity>
{
	public RemotePedestalTileEntityRenderer()
	{
	
	}
	@Override
	public void render(RemotePedestalTileEntity remotePedestalTileEntity, float partialTicks, @NotNull PoseStack matrixStack, @NotNull MultiBufferSource buffer, int combinedLight, int combinedOverlay)
	{
		ItemStack stack = remotePedestalTileEntity.getItem(0);
		
		if (!stack.isEmpty())
		{
			matrixStack.pushPose();
			matrixStack.translate(0.5F, 1F, 0.5F);
			//matrixStack.rotate(Vector3f.YP.rotation(f);
			Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, combinedLight, combinedOverlay, matrixStack, buffer, remotePedestalTileEntity.getLevel(), (int) remotePedestalTileEntity.getBlockPos().asLong());
			matrixStack.popPose();
		}
	}
}
