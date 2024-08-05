package net.splatcraft.forge.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.forge.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.forge.entities.SquidBumperEntity;
import net.splatcraft.forge.util.ColorUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class InkOverlayLayer<E extends LivingEntity, M extends EntityModel<E>> extends RenderLayer<E, M>
{
	private final List<RenderType> BUFFERS = Arrays.asList(
		RenderType.entitySmoothCutout(new ResourceLocation(Splatcraft.MODID, "textures/entity/ink_overlay_" + 0 + ".png")),
		RenderType.entitySmoothCutout(new ResourceLocation(Splatcraft.MODID, "textures/entity/ink_overlay_" + 1 + ".png")),
		RenderType.entitySmoothCutout(new ResourceLocation(Splatcraft.MODID, "textures/entity/ink_overlay_" + 2 + ".png")),
		RenderType.entitySmoothCutout(new ResourceLocation(Splatcraft.MODID, "textures/entity/ink_overlay_" + 3 + ".png")),
		RenderType.entitySmoothCutout(new ResourceLocation(Splatcraft.MODID, "textures/entity/ink_overlay_" + 4 + ".png"))
	);
	public InkOverlayLayer(RenderLayerParent<E, M> parent)
	{
		super(parent);
	}
	@Override
	public void render(@NotNull PoseStack matrixStack, @NotNull MultiBufferSource bufferIn, int packedLightIn, @NotNull E entity, float v, float v1, float v2, float v3, float v4, float v5)
	{
		int overlay = -1;
		float[] rgb = ColorUtils.hexToRGB(ColorUtils.DEFAULT);
		
		if (InkOverlayCapability.hasCapability(entity))
		{
			InkOverlayInfo info = InkOverlayCapability.get(entity);
			rgb = ColorUtils.hexToRGB(info.getColor());
			overlay = (int) (Math.min(info.getAmount() / (entity instanceof SquidBumperEntity ? SquidBumperEntity.maxInkHealth : entity.getMaxHealth()) * 4, 4) - 1);
		}
		
		if (overlay <= -1)
		{
			return;
		}
		
		//alex mob coming in clutch
		// hell yeah
		VertexConsumer ivertexbuilder = bufferIn.getBuffer(BUFFERS.get(overlay));
		this.getParentModel().renderToBuffer(matrixStack, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, rgb[0], rgb[1], rgb[2], 1.0F);
	}
}
