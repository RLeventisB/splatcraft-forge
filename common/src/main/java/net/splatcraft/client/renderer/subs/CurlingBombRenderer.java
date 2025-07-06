package net.splatcraft.client.renderer.subs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.subs.CurlingBombModel;
import net.splatcraft.entities.subs.CurlingBombEntity;
import net.splatcraft.items.weapons.settings.SubWeaponRecords;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.util.Mth.*;

public class CurlingBombRenderer extends SubWeaponRenderer<CurlingBombEntity, CurlingBombModel>
{
	private static final ResourceLocation TEXTURE = Splatcraft.identifierOf("textures/item/weapons/sub/curling_bomb.png");
	private static final ResourceLocation INK_TEXTURE = Splatcraft.identifierOf("textures/item/weapons/sub/curling_bomb_ink.png");
	private static final ResourceLocation OVERLAY_TEXTURE = Splatcraft.identifierOf("textures/item/weapons/sub/curling_bomb_overlay.png");
	private final CurlingBombModel MODEL;
	public CurlingBombRenderer(EntityRendererProvider.Context context)
	{
		super(context);
		MODEL = new CurlingBombModel(context.bakeLayer(CurlingBombModel.LAYER_LOCATION));
	}
	@Override
	public void render(CurlingBombEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack matrixStackIn, @NotNull MultiBufferSource bufferIn, int packedLightIn)
	{
		matrixStackIn.pushPose();
		
		if (!entity.isItem)
		{
			matrixStackIn.mulPose(Axis.YP.rotationDegrees(lerp(partialTicks, entity.yRotO, entity.getYRot()) - 180.0F));
			
			float f = entity.getFlashIntensity(partialTicks);
			float f1 = 1.0F + sin(f * 100.0F) * f * 0.01F;
			f = clamp(f, 0.0F, 1.0F);
			f *= f * f;
			float f2 = (1.0F + f * 0.4F) * f1;
			float f3 = (1.0F + f * 0.1F) / f1;
			matrixStackIn.scale(f2, -f3, f2);
		}
		
		matrixStackIn.translate(0, -1.5, 0);
		super.render(entity, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
		matrixStackIn.popPose();
	}
	@Override
	protected float getOverlayProgress(CurlingBombEntity livingEntityIn, float partialTicks)
	{
		float f = livingEntityIn.getFlashIntensity(partialTicks);
		return (int) (f * 10.0F) % 2 == 0 ? 0.0F : clamp(f, 0.5F, 1.0F);
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
	public @Nullable ResourceLocation getOverlayTexture(CurlingBombEntity entity)
	{
		return OVERLAY_TEXTURE;
	}
	@Override
	public int getOverlayColor(CurlingBombEntity entity, float partialTicks)
	{
		SubWeaponSettings<SubWeaponRecords.CurlingBombDataRecord> settings = entity.getSettings();
		float v = Math.clamp(1 - (entity.fuseTime - partialTicks) / (settings.subDataRecord.warningFrame()), 0, 1);
		byte r = (byte) (255 * v);
		byte g = (byte) (255 * (1 - v));
		byte b = (byte) (255 * v);
		return r << 16 | g << 8 | b;
	}
}
