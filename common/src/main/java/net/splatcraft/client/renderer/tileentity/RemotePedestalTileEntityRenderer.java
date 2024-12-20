package net.splatcraft.client.renderer.tileentity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.splatcraft.tileentities.RemotePedestalTileEntity;
import org.jetbrains.annotations.NotNull;

public class RemotePedestalTileEntityRenderer implements BlockEntityRenderer<RemotePedestalTileEntity>
{
    public RemotePedestalTileEntityRenderer()
    {

    }

    @Override
    public void render(RemotePedestalTileEntity remotePedestalTileEntity, float partialTicks, @NotNull MatrixStack matrixStack, @NotNull VertexConsumerProvider buffer, int combinedLight, int combinedOverlay)
    {
        ItemStack stack = remotePedestalTileEntity.getStack(0);

        if (!stack.isEmpty())
        {
            matrixStack.push();
            matrixStack.translate(0.5F, 1F, 0.5F);
            //matrixStack.rotate(Vector3f.POSITIVE_Y.rotation(f);
            MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.GROUND, combinedLight, combinedOverlay, matrixStack, buffer, remotePedestalTileEntity.getWorld(), (int) remotePedestalTileEntity.getPos().asLong());
            matrixStack.pop();
        }
    }
}
