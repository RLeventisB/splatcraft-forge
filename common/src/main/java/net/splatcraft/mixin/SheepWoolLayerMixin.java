package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.SheepWoolFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.util.InkColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(SheepWoolFeatureRenderer.class)
public abstract class SheepWoolLayerMixin
{
    @WrapOperation(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/passive/SheepEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/feature/SheepWoolFeatureRenderer;render(Lnet/minecraft/client/render/entity/model/EntityModel;Lnet/minecraft/client/render/entity/model/EntityModel;Lnet/minecraft/util/Identifier;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFFI)V"))
    public void render(EntityModel instance, EntityModel entityModel, Identifier identifier, MatrixStack matrixStack, VertexConsumerProvider consumerProvider, int light, LivingEntity entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch, float tickDelta, int color, Operation<Void> original)
    {
        if (InkOverlayCapability.hasCapability(entity))
        {
            InkColor inkColor = InkOverlayCapability.get(entity).getWoolColor();

            if (inkColor.isValid())
                color = inkColor.getColorWithAlpha(255);
        }

        original.call(instance, entityModel, identifier, matrixStack, consumerProvider, light, entity, limbAngle, limbDistance, age, headYaw, headPitch, tickDelta, color);
    }
}
