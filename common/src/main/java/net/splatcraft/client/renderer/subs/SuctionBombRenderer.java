package net.splatcraft.client.renderer.subs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.subs.SuctionBombModel;
import net.splatcraft.entities.subs.SuctionBombEntity;
import org.jetbrains.annotations.NotNull;

public class SuctionBombRenderer extends SubWeaponRenderer<SuctionBombEntity, SuctionBombModel>
{
	private static final ResourceLocation TEXTURE = Splatcraft.identifierOf("textures/item/weapons/sub/suction_bomb.png");
	private static final ResourceLocation OVERLAY_TEXTURE = Splatcraft.identifierOf("textures/item/weapons/sub/suction_bomb_ink.png");
	private final SuctionBombModel MODEL;
	public SuctionBombRenderer(EntityRendererProvider.Context context)
	{
		super(context);
		MODEL = new SuctionBombModel(context.bakeLayer(SuctionBombModel.LAYER_LOCATION));
	}
	@Override
	public void render(SuctionBombEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack matrixStack, @NotNull MultiBufferSource bufferIn, int packedLightIn)
	{
		matrixStack.pushPose();
		if (!entity.isItem)
		{
			matrixStack.translate(0, entity.getBbHeight() / 2, 0);
			matrixStack.mulPose(Axis.YP.rotationDegrees(entity.getViewYRot(partialTicks) - 180.0F));
			matrixStack.mulPose(Axis.XP.rotationDegrees(entity.getViewXRot(partialTicks) + 90));
			matrixStack.scale(1, -1, 1);
			
			float f = entity.getFlashIntensity(partialTicks);
			float f1 = 1.0F + Mth.sin(f * 100.0F) * f * 0.01F;
			f = Mth.clamp(f, 0.0F, 1.0F);
			f = f * f;
			f = f * f;
			float f2 = (1.0F + f * 0.4F) * f1;
			float f3 = (1.0F + f * 0.1F) / f1;
			matrixStack.scale(f2, f3, f2);
		}
		
		super.render(entity, entityYaw, partialTicks, matrixStack, bufferIn, packedLightIn);
		matrixStack.popPose();
	}
	@Override
	protected int getBlockLightLevel(@NotNull SuctionBombEntity entity, @NotNull BlockPos pos)
	{
		return super.getBlockLightLevel(entity, pos);
	}
	protected float getOverlayProgress(SuctionBombEntity livingEntityIn, float partialTicks)
	{
		float f = livingEntityIn.getFlashIntensity(partialTicks);
		return (int) (f * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(f, 0.5F, 1.0F);
	}
	@Override
	public @NotNull ResourceLocation getTextureLocation(@NotNull SuctionBombEntity entity)
	{
		return TEXTURE;
	}
	@Override
	public SuctionBombModel getModel()
	{
		return MODEL;
	}
	@Override
	public ResourceLocation getInkTextureLocation(SuctionBombEntity entity)
	{
		return OVERLAY_TEXTURE;
	}
}
