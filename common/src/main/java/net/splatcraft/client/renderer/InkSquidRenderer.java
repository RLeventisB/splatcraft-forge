package net.splatcraft.client.renderer;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.layer.InkSquidColorLayer;
import net.splatcraft.client.models.InkSquidModel;
import net.splatcraft.entities.InkSquidEntity;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class InkSquidRenderer extends LivingEntityRenderer<LivingEntity, InkSquidModel> implements FeatureRendererContext<LivingEntity, InkSquidModel>
{
    private static final Identifier TEXTURE = Splatcraft.identifierOf("textures/entity/ink_squid_overlay.png");
    private static EntityRendererFactory.Context squidFormContext;

    public InkSquidRenderer(EntityRendererFactory.Context context)
    {
        super(context, new InkSquidModel(context.getPart(InkSquidModel.LAYER_LOCATION)), 0.5f);
        addFeature(new InkSquidColorLayer(this, context.getModelLoader()));

        if (squidFormContext == null)
            squidFormContext = context;
    }

    public static EntityRendererFactory.Context getContext()
    {
        return squidFormContext;
    }

    private static void addVertexPair(VertexConsumer p_174308_, Matrix4f p_174309_, float p_174310_, float p_174311_, float p_174312_, int p_174313_, int p_174314_, int p_174315_, int p_174316_, float p_174317_, float p_174318_, float p_174319_, float p_174320_, int p_174321_, boolean p_174322_)
    {
        float f = (float) p_174321_ / 24.0F;
        int i = (int) MathHelper.lerp(f, (float) p_174313_, (float) p_174314_);
        int j = (int) MathHelper.lerp(f, (float) p_174315_, (float) p_174316_);
        int k = LightmapTextureManager.pack(i, j);
        float f1 = p_174321_ % 2 == (p_174322_ ? 1 : 0) ? 0.7F : 1.0F;
        float f2 = 0.5F * f1;
        float f3 = 0.4F * f1;
        float f4 = 0.3F * f1;
        float f5 = p_174310_ * f;
        float f6 = p_174311_ > 0.0F ? p_174311_ * f * f : p_174311_ - p_174311_ * (1.0F - f) * (1.0F - f);
        float f7 = p_174312_ * f;
        p_174308_.vertex(p_174309_, f5 - p_174319_, f6 + p_174318_, f7 + p_174320_).color(f2, f3, f4, 1.0F).light(k);
        p_174308_.vertex(p_174309_, f5 + p_174319_, f6 + p_174317_ - p_174318_, f7 - p_174320_).color(f2, f3, f4, 1.0F).light(k);
    }

    @Override
    protected boolean isVisible(@NotNull LivingEntity entity)
    {
        return super.isVisible(entity) && (entity.shouldRenderName() || entity.hasCustomName() && entity == dispatcher.targetedEntity);
    }

    @Override
    public void render(@NotNull LivingEntity entity, float p_115309_, float partialTicks, @NotNull MatrixStack poseStack, @NotNull VertexConsumerProvider bufferSource, int p_115313_)
    {
        super.render(entity, p_115309_, partialTicks, poseStack, bufferSource, p_115313_);

        if (entity instanceof InkSquidEntity squid)
            renderLeash(squid, partialTicks, poseStack, bufferSource);
    }

    @Override
    public @NotNull Identifier getTexture(@NotNull LivingEntity entity)
    {
        return TEXTURE;
    }

    private void renderLeash(InkSquidEntity squid, float partialTicks, MatrixStack matrixStack, VertexConsumerProvider bufferSource)
    {
        Entity holder = squid.getLeashHolder();

        if (holder == null)
            return;

        matrixStack.push();
        Vec3d vec3 = holder.getLeashPos(partialTicks);
        float d0 = (MathHelper.lerp(partialTicks, squid.bodyYaw, squid.prevBodyYaw) * MathHelper.RADIANS_PER_DEGREE) + MathHelper.HALF_PI;
        Vec3d vec31 = squid.getLeashOffset(partialTicks);
        double d1 = Math.cos(d0) * vec31.z + Math.sin(d0) * vec31.x;
        double d2 = Math.sin(d0) * vec31.z - Math.cos(d0) * vec31.x;
        double d3 = MathHelper.lerp(partialTicks, squid.prevX, squid.getX()) + d1;
        double d4 = MathHelper.lerp(partialTicks, squid.prevY, squid.getY()) + vec31.y;
        double d5 = MathHelper.lerp(partialTicks, squid.prevZ, squid.getZ()) + d2;
        matrixStack.translate(d1, vec31.y, d2);
        float f = (float) (vec3.x - d3);
        float f1 = (float) (vec3.y - d4);
        float f2 = (float) (vec3.z - d5);
        VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderLayer.getLeash());
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        float f4 = (float) (Math.sqrt(f * f + f2 * f2) * 0.025F / 2.0F);
        float f5 = f2 * f4;
        float f6 = f * f4;
        BlockPos blockpos = CommonUtils.createBlockPos(squid.getCameraPosVec(partialTicks));
        BlockPos blockpos1 = CommonUtils.createBlockPos(holder.getCameraPosVec(partialTicks));
        int i = getBlockLight(squid, blockpos);
        int j = getHolderBlockLightLevel(holder, blockpos1);
        int k = squid.getWorld().getLightLevel(LightType.SKY, blockpos);
        int l = squid.getWorld().getLightLevel(LightType.SKY, blockpos1);

        for (int i1 = 0; i1 <= 24; ++i1)
        {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025F, 0.025F, f5, f6, i1, false);
        }

        for (int j1 = 24; j1 >= 0; --j1)
        {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025F, 0.0F, f5, f6, j1, true);
        }

        matrixStack.pop();
    }

    protected int getHolderBlockLightLevel(Entity p_114496_, BlockPos p_114497_)
    {
        return p_114496_.isOnFire() ? 15 : p_114496_.getWorld().getLightLevel(LightType.BLOCK, p_114497_);
    }
}
