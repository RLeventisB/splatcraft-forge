package net.splatcraft.client.renderer.subs;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.subs.SuctionBombModel;
import net.splatcraft.entities.subs.SuctionBombEntity;
import org.jetbrains.annotations.NotNull;

public class SuctionBombRenderer extends SubWeaponRenderer<SuctionBombEntity, SuctionBombModel>
{
	private static final Identifier TEXTURE = Splatcraft.identifierOf("textures/item/weapons/sub/suction_bomb.png");
	private static final Identifier OVERLAY_TEXTURE = Splatcraft.identifierOf("textures/item/weapons/sub/suction_bomb_ink.png");
	private final SuctionBombModel MODEL;
	public SuctionBombRenderer(EntityRendererFactory.Context context)
	{
		super(context);
		MODEL = new SuctionBombModel(context.getPart(SuctionBombModel.LAYER_LOCATION));
	}
	@Override
	public void render(SuctionBombEntity entityIn, float entityYaw, float partialTicks, @NotNull MatrixStack matrixStack, @NotNull VertexConsumerProvider bufferIn, int packedLightIn)
	{
		matrixStack.push();
		if (!entityIn.isItem)
		{
			matrixStack.translate(0, 0.25f, 0);
			matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entityIn.getYaw(partialTicks) - 180.0F));
			matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entityIn.getPitch(partialTicks) + 90));
//			matrixStack.translate(0, 0, 0);
			matrixStack.scale(1, -1, 1);
			
			float f = entityIn.getFlashIntensity(partialTicks);
			float f1 = 1.0F + MathHelper.sin(f * 100.0F) * f * 0.01F;
			f = MathHelper.clamp(f, 0.0F, 1.0F);
			f = f * f;
			f = f * f;
			float f2 = (1.0F + f * 0.4F) * f1;
			float f3 = (1.0F + f * 0.1F) / f1;
			matrixStack.scale(f2, f3, f2);
		}
		
		super.render(entityIn, entityYaw, partialTicks, matrixStack, bufferIn, packedLightIn);
		matrixStack.pop();
	}
	@Override
	protected int getBlockLight(SuctionBombEntity entity, BlockPos pos)
	{
		return super.getBlockLight(entity, pos);
	}
	protected float getOverlayProgress(SuctionBombEntity livingEntityIn, float partialTicks)
	{
		float f = livingEntityIn.getFlashIntensity(partialTicks);
		return (int) (f * 10.0F) % 2 == 0 ? 0.0F : MathHelper.clamp(f, 0.5F, 1.0F);
	}
	@Override
	public @NotNull Identifier getTexture(@NotNull SuctionBombEntity entity)
	{
		return TEXTURE;
	}
	@Override
	public SuctionBombModel getModel()
	{
		return MODEL;
	}
	@Override
	public Identifier getInkTextureLocation(SuctionBombEntity entity)
	{
		return OVERLAY_TEXTURE;
	}
}
