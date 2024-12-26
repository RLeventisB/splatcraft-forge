package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.splatcraft.client.handlers.RendererHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class CooldownSlotMixin
{
	@Shadow
	@Nullable
	public ClientPlayerEntity player;
	// these sometimes dont work!!
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;render(Lnet/minecraft/client/render/RenderTickCounter;Z)V"))
	public void splatcraft$onRenderStart(boolean tick, CallbackInfo ci)
	{
		RendererHandler.onRenderTick((MinecraftClient) (Object) this);
	}
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;render(Lnet/minecraft/client/render/RenderTickCounter;Z)V", shift = At.Shift.AFTER))
	public void splatcraft$onRenderEnd(boolean tick, CallbackInfo ci)
	{
		RendererHandler.onRenderTick((MinecraftClient) (Object) this);
	}
	@WrapOperation(method = "handleInputEvents", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I"))
	public void splatcraft$onHotbarSlotChange(PlayerInventory instance, int value, Operation<Void> original)
	{
		int slot = RendererHandler.slotToAssign(player);
		if (slot != -1)
		{
			original.call(instance, slot);
		}
	}
}
