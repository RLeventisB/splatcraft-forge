package net.splatcraft.client.renderer.subs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.subs.TorpedoModel;
import net.splatcraft.entities.subs.TorpedoEntity;
import org.jetbrains.annotations.NotNull;

public class TorpedoRenderer extends SubWeaponRenderer<TorpedoEntity, TorpedoModel>
{
	private static final ResourceLocation TEXTURE = Splatcraft.identifierOf("textures/item/weapons/sub/torpedo.png");
	private static final ResourceLocation OVERLAY_TEXTURE = Splatcraft.identifierOf("textures/item/weapons/sub/torpedo_ink.png");
	private final TorpedoModel MODEL;
	public TorpedoRenderer(EntityRendererProvider.Context context)
	{
		super(context);
		MODEL = new TorpedoModel(context.bakeLayer(TorpedoModel.LAYER_LOCATION));
	}
	@Override
	public void render(TorpedoEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack matrices, @NotNull MultiBufferSource bufferIn, int packedLightIn)
	{
		matrices.pushPose();
		if (!entity.isItem)
		{
			matrices.translate(0, entity.getBbWidth() / 2, 0);
			
			int counter = entity.getCounter();
			if (entity.isLockedIn() && counter > 0)
			{
				Vec3 sineMovement = entity.getForward().scale(Mth.square(Math.max(0, (counter - 5f) / 4f)) * Mth.sin(entity.tickCount));
				matrices.translate(sineMovement.z, sineMovement.y, sineMovement.x);
			}
			
			matrices.mulPose(Axis.YP.rotationDegrees(entity.getViewYRot(partialTicks) - 180.0F));
			matrices.mulPose(Axis.XP.rotationDegrees(entity.getViewXRot(partialTicks)));
			matrices.scale(1, -1, 1);
		}
		else
		{
			matrices.translate(0, -entity.getBbWidth() / 4, 0);
			matrices.scale(0.7f, 0.7f, 0.7f);
		}
		super.render(entity, entityYaw, partialTicks, matrices, bufferIn, packedLightIn);
		matrices.popPose();
	}
	@Override
	public @NotNull ResourceLocation getTextureLocation(@NotNull TorpedoEntity entity)
	{
		return TEXTURE;
	}
	@Override
	public TorpedoModel getModel()
	{
		return MODEL;
	}
	@Override
	public ResourceLocation getInkTextureLocation(TorpedoEntity entity)
	{
		return OVERLAY_TEXTURE;
	}
}
