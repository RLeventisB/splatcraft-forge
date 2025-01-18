package net.splatcraft.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.util.PlayerCooldown;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class WeaponHotkeyMixin
{
	@Mixin(ClientPlayerInteractionManager.class)
	public static abstract class PlayerControllerMix
	{
		@Inject(method = "stopUsingItem", at = @At("HEAD"), cancellable = true)
		private void splatcraft$releaseUsingItem(PlayerEntity player, CallbackInfo callbackInfo)
		{
			if (
				SplatcraftKeyHandler.isSubWeaponHotkeyDown() && player.getActiveHand() == Hand.OFF_HAND ||
					PlayerCooldown.hasCooldownAnd(player, PlayerCooldown::preventStopUsing) ||
					((player.getActiveItem().getItem() instanceof WeaponBaseItem<?> weaponItem && weaponItem.preventStopUsingWeapon(player.getWorld(), player)))
			)
				callbackInfo.cancel();
		}
	}
	@Mixin(MinecraftClient.class)
	public static abstract class MinecraftInstance
	{
		@Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
		private void splatcraft$startUseItem(CallbackInfo ci)
		{
			if (SplatcraftKeyHandler.isSubWeaponHotkeyDown())
			{
				SplatcraftKeyHandler.startUsingItemInHand(Hand.OFF_HAND);
				ci.cancel();
			}
		}
	}
}