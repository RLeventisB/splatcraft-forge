package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.splatcraft.client.handlers.PlayerSlotHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class CooldownSlotMixin
{
	@Mixin(Minecraft.class)
	public static class MinecraftPart
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
	@Mixin(MouseHandler.class)
	public static class MouseHandlerPart
	{
		@Shadow
		@Final
		private Minecraft minecraft;
		@WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;swapPaint(D)V"))
		public void splatcraft$onHotbarSlotScroll(Inventory instance, double direction, Operation<Void> original)
		{
			int slot = PlayerSlotHandler.slotToAssign(minecraft.player);
			int currentSlot = minecraft.player.getInventory().selected;
			int nextSlot = (int) Mth.positiveModulo(currentSlot + direction, 9);
			if (slot != -1)
			{
				PlayerSlotHandler.enqueueSlot(nextSlot);
				minecraft.player.getInventory().selected = slot;
				return;
			}
			original.call(instance, direction);
		}
	}
}
