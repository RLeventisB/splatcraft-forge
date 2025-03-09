package net.splatcraft.mixin;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.registries.SplatcraftEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public class EntityLayerMixin
{
	@Shadow
	private Map<EntityType<?>, EntityRenderer<?>> renderers = ImmutableMap.of();
	@Shadow
	private Map<PlayerSkin.Model, EntityRenderer<? extends Player>> playerRenderers = Map.of();
	@Inject(method = "onResourceManagerReload", at = @At(value = "RETURN"))
	public void a(ResourceManager manager, CallbackInfo ci, @Local EntityRendererProvider.Context context)
	{
		SplatcraftEntities.addRenderLayers(renderers, playerRenderers, context);
	}
}
