package net.splatcraft.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.util.ClientUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerTabOverlayMixin
{
	@Inject(method = "applyGameModeFormatting", at = @At("HEAD"))
	public void decorateName(PlayerListEntry playerInfo, MutableText component, CallbackInfoReturnable<Text> cir)
	{
		if (SplatcraftConfig.get("coloredPlayerNames"))
			component.setStyle(component.getStyle().withColor(ClientUtils.getClientPlayerColor(playerInfo.getProfile().getId()).getColor()));
	}
}
