package net.splatcraft.client.renderer.subs;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.models.subs.BurstBombModel;
import net.splatcraft.entities.subs.BurstBombEntity;
import org.jetbrains.annotations.NotNull;

public class BurstBombRenderer extends SubWeaponRenderer<BurstBombEntity, BurstBombModel>
{
    private static final Identifier TEXTURE = Splatcraft.identifierOf("textures/item/weapons/sub/burst_bomb.png");
    private static final Identifier OVERLAY_TEXTURE = Splatcraft.identifierOf("textures/item/weapons/sub/burst_bomb_ink.png");
    private final BurstBombModel MODEL;

    public BurstBombRenderer(EntityRendererFactory.Context context)
    {
        super(context);
        MODEL = new BurstBombModel(context.getPart(BurstBombModel.LAYER_LOCATION));
    }

    @Override
    public void render(BurstBombEntity entityIn, float entityYaw, float partialTicks, @NotNull MatrixStack matrices, @NotNull VertexConsumerProvider bufferIn, int packedLightIn)
    {

        matrices.push();
        if (!entityIn.isItem)
        {
            //PoseStackIn.translate(0.0D, 0.2/*0.15000000596046448D*/, 0.0D);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevYaw, entityIn.getYaw()) - 180.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevPitch, entityIn.getPitch()) + 90F));
            matrices.scale(1, -1, 1);
        }
        super.render(entityIn, entityYaw, partialTicks, matrices, bufferIn, packedLightIn);
        matrices.pop();
    }

    @Override
    public @NotNull Identifier getTexture(@NotNull BurstBombEntity entity)
    {
        return TEXTURE;
    }

    @Override
    public BurstBombModel getModel()
    {
        return MODEL;
    }

    @Override
    public Identifier getInkTextureLocation(BurstBombEntity entity)
    {
        return OVERLAY_TEXTURE;
    }
}
