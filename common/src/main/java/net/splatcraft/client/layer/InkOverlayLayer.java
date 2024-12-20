package net.splatcraft.client.layer;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.entities.SquidBumperEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class InkOverlayLayer<E extends LivingEntity, M extends EntityModel<E>> extends FeatureRenderer<E, M>
{
    private final List<RenderLayer> BUFFERS = Arrays.asList(
        RenderLayer.getEntitySmoothCutout(Splatcraft.identifierOf("textures/entity/ink_overlay_" + 0 + ".png")),
        RenderLayer.getEntitySmoothCutout(Splatcraft.identifierOf("textures/entity/ink_overlay_" + 1 + ".png")),
        RenderLayer.getEntitySmoothCutout(Splatcraft.identifierOf("textures/entity/ink_overlay_" + 2 + ".png")),
        RenderLayer.getEntitySmoothCutout(Splatcraft.identifierOf("textures/entity/ink_overlay_" + 3 + ".png")),
        RenderLayer.getEntitySmoothCutout(Splatcraft.identifierOf("textures/entity/ink_overlay_" + 4 + ".png"))
    );

    public InkOverlayLayer(FeatureRendererContext<E, M> parent)
    {
        super(parent);
    }

    @Override
    public void render(@NotNull MatrixStack matrixStack, @NotNull VertexConsumerProvider bufferIn, int packedLightIn, @NotNull E entity, float v, float v1, float v2, float v3, float v4, float v5)
    {
        int overlay = -1;
        InkColor color = ColorUtils.getDefaultColor();

        if (InkOverlayCapability.hasCapability(entity))
        {
            InkOverlayInfo info = InkOverlayCapability.get(entity);
            color = ColorUtils.getColorLockedIfConfig(info.getColor());
            overlay = (int) (Math.min(info.getAmount() / (entity instanceof SquidBumperEntity ? SquidBumperEntity.maxInkHealth : entity.getMaxHealth()) * 4, 4) - 1);
        }

        if (overlay <= -1)
        {
            return;
        }

        //alex mob coming in clutch
        // hell yeah
        VertexConsumer ivertexbuilder = bufferIn.getBuffer(BUFFERS.get(overlay));
        getContextModel().render(matrixStack, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, color.getColorWithAlpha(255));
    }
}
