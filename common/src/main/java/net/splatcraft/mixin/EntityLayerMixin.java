package net.splatcraft.mixin;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.ResourceManager;
import net.splatcraft.registries.SplatcraftEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public class EntityLayerMixin
{
    @Shadow
    private Map<EntityType<?>, EntityRenderer<?>> renderers = ImmutableMap.of();
    @Shadow
    private Map<SkinTextures.Model, EntityRenderer<? extends PlayerEntity>> modelRenderers = Map.of();

    @Inject(method = "reload", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "RETURN"))
    public void a(ResourceManager manager, CallbackInfo ci, EntityRendererFactory.Context context)
    {
        SplatcraftEntities.addRenderLayers(renderers, modelRenderers, context);
    }
}
