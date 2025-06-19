package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.splatcraft.client.handlers.PlayerSlotHandler;
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
}
