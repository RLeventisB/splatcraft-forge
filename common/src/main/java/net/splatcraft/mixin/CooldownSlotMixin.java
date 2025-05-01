package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.client.handlers.PlayerSlotHandler;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.util.action.EntityAction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class CooldownSlotMixin
{
	@Shadow
	@Nullable
	public LocalPlayer player;
	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V", shift = At.Shift.AFTER))
	public void splatcraft$onRenderEnd(boolean tick, CallbackInfo ci)
	{
		PlayerSlotHandler.reassignSlot((Minecraft) (Object) this);
	}
	@WrapOperation(method = "handleKeybinds", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Inventory;selected:I"))
	public void splatcraft$onHotbarSlotChange(Inventory instance, int value, Operation<Void> original)
	{
		int slot = PlayerSlotHandler.slotToAssign(player);
		if (slot != -1)
		{
			PlayerSlotHandler.enqueueSlot(value);
			value = slot;
		}
		original.call(instance, value);
	}
	@WrapOperation(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isItemEnabled(Lnet/minecraft/world/flag/FeatureFlagSet;)Z"))
	public boolean splatcraft$preventWeaponUsageIfProhibited(ItemStack instance, FeatureFlagSet enabledFlags, Operation<Boolean> original)
	{
		if (instance.getItem() instanceof WeaponBaseItem<?> && EntityAction.hasActionAnd(player, EntityAction::preventWeaponUse))
			return false;
		return original.call(instance, enabledFlags);
	}
}
