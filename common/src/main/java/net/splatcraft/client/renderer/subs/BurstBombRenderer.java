package net.splatcraft.client.renderer.subs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.subs.BurstBombModel;
import net.splatcraft.entities.subs.BurstBombEntity;
import org.jetbrains.annotations.NotNull;

public class BurstBombRenderer extends SubWeaponRenderer<BurstBombEntity, BurstBombModel>
{
	private static final ResourceLocation TEXTURE = Splatcraft.identifierOf("textures/item/weapons/sub/burst_bomb.png");
	private static final ResourceLocation OVERLAY_TEXTURE = Splatcraft.identifierOf("textures/item/weapons/sub/burst_bomb_ink.png");
	private final BurstBombModel MODEL;
	public BurstBombRenderer(EntityRendererProvider.Context context)
	{
		super(context);
		MODEL = new BurstBombModel(context.bakeLayer(BurstBombModel.LAYER_LOCATION));
	}
	@Override
	public void render(BurstBombEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack matrices, @NotNull MultiBufferSource bufferIn, int packedLightIn)
	{
		matrices.pushPose();
		if (!entity.isItem)
		{
			//PoseStackIn.translate(0.0D, 0.2/*0.15000000596046448D*/, 0.0D);
			matrices.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 180.0F));
			matrices.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot()) + 90F));
			matrices.scale(1, -1, 1);
		}
		super.render(entity, entityYaw, partialTicks, matrices, bufferIn, packedLightIn);
		matrices.popPose();
	}
	@Override
	public @NotNull ResourceLocation getTextureLocation(@NotNull BurstBombEntity entity)
	{
		return TEXTURE;
	}
	@Override
	public BurstBombModel getModel()
	{
		return MODEL;
	}
	@Override
	public ResourceLocation getInkTextureLocation(BurstBombEntity entity)
	{
		return OVERLAY_TEXTURE;
	}
}
