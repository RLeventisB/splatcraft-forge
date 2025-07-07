package net.splatcraft.client.layer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.entities.SquidBumperEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.List;

public class InkOverlayLayer<E extends LivingEntity, M extends EntityModel<E>> extends RenderLayer<E, M>
{
	private final List<RenderType> BUFFERS = Arrays.asList(
		RenderType.entitySmoothCutout(Splatcraft.identifierOf("textures/entity/ink_overlay_" + 0 + ".png")),
		RenderType.entitySmoothCutout(Splatcraft.identifierOf("textures/entity/ink_overlay_" + 1 + ".png")),
		RenderType.entitySmoothCutout(Splatcraft.identifierOf("textures/entity/ink_overlay_" + 2 + ".png")),
		RenderType.entitySmoothCutout(Splatcraft.identifierOf("textures/entity/ink_overlay_" + 3 + ".png")),
		RenderType.entitySmoothCutout(Splatcraft.identifierOf("textures/entity/ink_overlay_" + 4 + ".png"))
	);
	public InkOverlayLayer(RenderLayerParent<E, M> parent)
	{
		super(parent);
	}
	@Override
	public void render(@NotNull PoseStack matrixStack, @NotNull MultiBufferSource bufferIn, int packedLightIn, @NotNull E entity, float v, float v1, float v2, float v3, float v4, float v5)
	{
		InkOverlayInfo info = InkOverlayCapability.get(entity);
		InkColor color = ColorUtils.getColorLockedIfConfig(info.getColor());
		int overlay = (int) (Math.min(info.getAmount() / (entity instanceof SquidBumperEntity ? SquidBumperEntity.maxInkHealth : entity.getMaxHealth()) * 4, 4) - 1);
		
		if (overlay <= -1)
		{
			return;
		}
		
		//alex mob coming in clutch
		// hell yeah
		VertexConsumer ivertexbuilder = bufferIn.getBuffer(BUFFERS.get(overlay));
		getParentModel().renderToBuffer(matrixStack, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, color.getColorWithAlpha(255));
	}
}
