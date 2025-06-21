package net.splatcraft.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin
{
	@Inject(method = "decorateName", at = @At("HEAD"))
	public void decorateName(PlayerInfo playerInfo, MutableComponent component, CallbackInfoReturnable<Component> cir)
	{
		if (SplatcraftConfig.get("splatcraft.coloredPlayerNames"))
			component.setStyle(component.getStyle().withColor(ColorUtils.getPlayerColor(playerInfo.getProfile().getId(), ClientUtils.getClient().level).getColor()));
	}
}
