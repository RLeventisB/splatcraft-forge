package net.splatcraft.client.renderer;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.layer.SquidBumperColorLayer;
import net.splatcraft.client.models.SquidBumperModel;
import net.splatcraft.entities.SquidBumperEntity;
import org.jetbrains.annotations.NotNull;

public class SquidBumperRenderer extends LivingEntityRenderer<SquidBumperEntity, SquidBumperModel> implements FeatureRendererContext<SquidBumperEntity, SquidBumperModel>
{
    private static final Identifier TEXTURE = Splatcraft.identifierOf("textures/entity/squid_bumper_overlay.png");

    public SquidBumperRenderer(EntityRendererFactory.Context context)
    {
        super(context, new SquidBumperModel(context.getPart(SquidBumperModel.LAYER_LOCATION)), 0.5f);
        addFeature(new SquidBumperColorLayer(this, context.getModelLoader()));
        //addLayer(new SquidBumperOverlayLayer(this, context.getModelSet()));
    }

    @Override
    protected boolean hasLabel(SquidBumperEntity entity)
    {
        return !entity.hasCustomName() && !(entity.getInkHealth() >= 20) || super.hasLabel(entity) && (entity.hasCustomName() || entity == dispatcher.targetedEntity);
    }

    @Override
    protected void renderLabelIfPresent(SquidBumperEntity entity, @NotNull Text text, @NotNull MatrixStack matrices, @NotNull VertexConsumerProvider bufferIn, int packedLightIn, float delta)
    {
        if (entity.hasCustomName())
        {
            super.renderLabelIfPresent(entity, text, matrices, bufferIn, packedLightIn, delta);
        }
        else
        {
            float health = 20 - entity.getInkHealth();
            super.renderLabelIfPresent(entity, Text.literal((health >= 20 ? Formatting.DARK_RED : "") + String.format("%.1f", health)), matrices, bufferIn, packedLightIn, delta);
        }
    }

    @Override
    protected void setupTransforms(SquidBumperEntity entityLiving, @NotNull MatrixStack matrices, float ageInTicks, float rotationYaw, float partialTicks, float scale)
    {
        //PoseStackIn.rotate(Vector3f.POSITIVE_Y.rotationDegrees(180.0F - rotationYaw));
        float punchTime = (float) (entityLiving.getWorld().getTime() - entityLiving.punchCooldown) + partialTicks;
        float hurtTime = (float) (entityLiving.getWorld().getTime() - entityLiving.hurtCooldown) + partialTicks;

        if (punchTime < 5.0F)
        {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.sin(punchTime / 1.5F * (float) Math.PI) * 3.0F));
        }
        if (hurtTime < 5.0F)
        {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(hurtTime / 1.5F * (float) Math.PI) * 3.0F));
        }
    }

    @Override
    public @NotNull Identifier getTexture(@NotNull SquidBumperEntity entity)
    {
        return TEXTURE;
    }
}
