package net.splatcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.layer.SquidBumperColorLayer;
import net.splatcraft.client.models.SquidBumperModel;
import net.splatcraft.entities.SquidBumperEntity;
import org.jetbrains.annotations.NotNull;

public class SquidBumperRenderer extends LivingEntityRenderer<SquidBumperEntity, SquidBumperModel> implements RenderLayerParent<SquidBumperEntity, SquidBumperModel>
{
	private static final ResourceLocation TEXTURE = Splatcraft.identifierOf("textures/entity/squid_bumper_overlay.png");
	public SquidBumperRenderer(EntityRendererProvider.Context context)
	{
		super(context, new SquidBumperModel(context.bakeLayer(SquidBumperModel.LAYER_LOCATION)), 0.5f);
		addLayer(new SquidBumperColorLayer(this, context.getModelSet()));
		//addLayer(new SquidBumperOverlayLayer(this, context.getModelSet()));
	}
	@Override
	protected boolean shouldShowName(SquidBumperEntity entity)
	{
		return !entity.hasCustomName() && !(entity.getInkHealth() >= 20) || super.shouldShowName(entity) && (entity.hasCustomName() || entity == entityRenderDispatcher.crosshairPickEntity);
	}
	@Override
	protected void renderNameTag(SquidBumperEntity entity, @NotNull Component text, @NotNull PoseStack matrices, @NotNull MultiBufferSource bufferIn, int packedLightIn, float delta)
	{
		if (entity.hasCustomName())
		{
			super.renderNameTag(entity, text, matrices, bufferIn, packedLightIn, delta);
		}
		else
		{
			float health = 20 - entity.getInkHealth();
			super.renderNameTag(entity, Component.literal((health >= 20 ? ChatFormatting.DARK_RED : "") + String.format("%.1f", health)), matrices, bufferIn, packedLightIn, delta);
		}
	}
	@Override
	protected void setupRotations(SquidBumperEntity entityLiving, @NotNull PoseStack matrices, float ageInTicks, float rotationYaw, float partialTicks, float scale)
	{
		//PoseStackIn.rotate(Vector3f.POSITIVE_Y.rotationDegrees(180.0F - rotationYaw));
		float punchTime = (float) (entityLiving.level().getGameTime() - entityLiving.punchCooldown) + partialTicks;
		float hurtTime = (float) (entityLiving.level().getGameTime() - entityLiving.hurtCooldown) + partialTicks;
		
		if (punchTime < 5.0F)
		{
			matrices.mulPose(Axis.YP.rotationDegrees(Mth.sin(punchTime / 1.5F * (float) Math.PI) * 3.0F));
		}
		if (hurtTime < 5.0F)
		{
			matrices.mulPose(Axis.ZP.rotationDegrees(Mth.sin(hurtTime / 1.5F * (float) Math.PI) * 3.0F));
		}
	}
	@Override
	public @NotNull ResourceLocation getTextureLocation(@NotNull SquidBumperEntity entity)
	{
		return TEXTURE;
	}
}
