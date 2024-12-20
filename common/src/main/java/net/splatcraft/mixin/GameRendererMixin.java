package net.splatcraft.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.InkBlockUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin
{
    /**
     * Disables view bobbing if configured.
     */
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void onBobView(MatrixStack matrices, float f, CallbackInfo ci)
    {
        if (ClientUtils.getClientPlayer() != null)
        {
            if (EntityInfoCapability.isSquid(ClientUtils.getClientPlayer()))
            {
                SplatcraftConfig.PreventBobView value = SplatcraftConfig.get("splatcraft.preventBobView");
                if (value.equals(SplatcraftConfig.PreventBobView.ALWAYS) || value.equals(SplatcraftConfig.PreventBobView.SUBMERGED) && EntityInfoCapability.isSquid(ClientUtils.getClientPlayer()) && InkBlockUtils.canSquidHide(ClientUtils.getClientPlayer()))
                    ci.cancel();
            }
        }
    }
}