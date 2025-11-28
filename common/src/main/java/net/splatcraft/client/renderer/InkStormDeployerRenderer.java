package net.splatcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.client.handlers.RendererHandler;
import net.splatcraft.entities.InkStormDeployerEntity;
import org.jetbrains.annotations.NotNull;

public class InkStormDeployerRenderer extends EntityRenderer<InkStormDeployerEntity>
{
	public InkStormDeployerRenderer(EntityRendererProvider.Context context)
	{
		super(context);
	}
	@Override
	public void render(@NotNull InkStormDeployerEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack matrixStack, @NotNull MultiBufferSource buffer, int packedLight)
	{
	}
	@Override
	public boolean shouldRender(@NotNull InkStormDeployerEntity entity, @NotNull Frustum frustum, double x, double y, double z)
	{
		return !entity.isRising();
	}
	@Override
	public @NotNull ResourceLocation getTextureLocation(@NotNull InkStormDeployerEntity entity)
	{
		return RendererHandler.MAGIC_PIXEL;
	}
}