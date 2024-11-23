package net.splatcraft.forge.mixin.accessors;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererFovAccessor
{
    @Invoker
    double invokeGetFov(Camera pActiveRenderInfo, float pPartialTicks, boolean pUseFOVSetting);
}
