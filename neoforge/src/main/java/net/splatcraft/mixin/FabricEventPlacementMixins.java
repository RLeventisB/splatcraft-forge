package net.splatcraft.mixin;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.platform.NeoForgePlatformHelper;
import net.splatcraft.platform.event.LifecycleEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class FabricEventPlacementMixins
{
	@OnlyIn(Dist.CLIENT)
	@Mixin(Minecraft.class)
	public static class ClientMixins
	{
		@Inject(method = "run", at = @At(value = "HEAD"))
		private void onStart(CallbackInfo ci)
		{
			Minecraft client = (Minecraft) (Object) this;
			NeoForgePlatformHelper.INSTANCE.invokeConsumerEvent(LifecycleEvents.ClientStarted.class, client);
		}
		@Inject(method = "destroy", at = @At(value = "HEAD", remap = false))
		private void onStopping(CallbackInfo ci)
		{
			Minecraft client = (Minecraft) (Object) this;
			NeoForgePlatformHelper.INSTANCE.invokeConsumerEvent(LifecycleEvents.ClientStopped.class, client);
		}
	}
}
