package net.splatcraft.forge.client.renderer.subs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.client.models.subs.CurlingBombModel;
import net.splatcraft.forge.entities.subs.CurlingBombEntity;
import net.splatcraft.forge.items.weapons.settings.SubWeaponSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.util.Mth.*;

public class CurlingBombRenderer extends SubWeaponRenderer<CurlingBombEntity, CurlingBombModel>
{
	private final CurlingBombModel MODEL;
	private static final ResourceLocation TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/weapons/sub/curling_bomb.png");
	private static final ResourceLocation INK_TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/weapons/sub/curling_bomb_ink.png");
	private static final ResourceLocation OVERLAY_TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/weapons/sub/curling_bomb_overlay.png");

	public CurlingBombRenderer(EntityRendererProvider.Context context)
	{
		super(context);
		MODEL = new CurlingBombModel(context.bakeLayer(CurlingBombModel.LAYER_LOCATION));
	}

	@Override
	public void render(CurlingBombEntity entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn, @NotNull MultiBufferSource bufferIn, int packedLightIn) {

		matrixStackIn.pushPose();

		if(!entityIn.isItem)
		{
			matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(lerp(partialTicks, entityIn.yRotO, entityIn.getYRot()) - 180.0F));
			//matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.xRotO, entityIn.xRot)+90F));
			//matrixStackIn.scale(1, -1, 1);

			float f = entityIn.getFlashIntensity(partialTicks);
			float f1 = 1.0F + sin(f * 100.0F) * f * 0.01F;
			f = clamp(f, 0.0F, 1.0F);
			f = f * f;
			f = f * f;
			float f2 = (1.0F + f * 0.4F) * f1;
			float f3 = (1.0F + f * 0.1F) / f1;
			matrixStackIn.scale(f2, -f3, f2);
		}


		matrixStackIn.translate(0, -1.5, 0);
		super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
		matrixStackIn.popPose();
	}

	@Override
	protected float getOverlayProgress(CurlingBombEntity livingEntityIn, float partialTicks) {
		float f = livingEntityIn.getFlashIntensity(partialTicks);
		return (int)(f * 10.0F) % 2 == 0 ? 0.0F : clamp(f, 0.5F, 1.0F);
	}

	@Override
	public @NotNull ResourceLocation getTextureLocation(@NotNull CurlingBombEntity entity)
	{
		return TEXTURE;
	}

	@Override
	public CurlingBombModel getModel()
	{
		return MODEL;
	}

	@Override
	public ResourceLocation getInkTextureLocation(CurlingBombEntity entity)
	{
		return INK_TEXTURE;
	}

	@Override
	public @Nullable ResourceLocation getOverlayTextureLocation(CurlingBombEntity entity) {
		return OVERLAY_TEXTURE;
	}

	@Override
	public float[] getOverlayColor(CurlingBombEntity entity, float partialTicks)
	{
		SubWeaponSettings settings = entity.getSettings();
		float v = clamp((lerp(partialTicks, entity.prevFuseTime, entity.fuseTime)) / (settings.fuseTime - settings.cookTime), 0, 1);
		return new float[] {v, 1-v, 0};
	}
}
