package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.client.handlers.RendererHandler;
import net.splatcraft.items.weapons.subs.SubWeaponItem;
import net.splatcraft.registries.SplatcraftItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class ItemRenderingMixins
{
	@Mixin(ItemRenderer.class)
	public static class ItemRendererMixin
	{
		@Inject(method = "render", at = @At("HEAD"), cancellable = true)
		public void splatcraft$tweakItemRender(ItemStack stack, ItemDisplayContext renderMode, boolean leftHanded, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, BakedModel model, CallbackInfo ci)
		{
			if (stack.getItem() instanceof SubWeaponItem subWeaponItem)
			{
				RendererHandler.renderSubWeapon(stack, subWeaponItem, matrices, vertexConsumers, light, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), leftHanded);
				ci.cancel();
			}
			if (stack.getItem().equals(SplatcraftItems.powerEgg.get()))
			{
//	 todo: does this ever change anything???
// old code felt sloppy and unnecessary idk why (but ill leave a note here in case something breaks when rendering power eggs, idk how but ok)

//				Identifier key = SplatcraftItems.powerEgg.getId();
//				model = MinecraftClient.getInstance().getItemRenderer().getModels().getModelManager().getModel(ModelIdentifier.ofInventoryVariant(key));
			}
		}
	}
	@Mixin(ItemInHandRenderer.class)
	public static class HeldItemRendererMixin
	{
		@WrapOperation(method = "renderHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
		public void splatcraft$overrideHeldItemRendering(ItemInHandRenderer instance, AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, MultiBufferSource vertexConsumers, int light, Operation<Void> original)
		{
			if (RendererHandler.renderHand(tickDelta, hand, matrices))
			{
				original.call(instance, player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
			}
		}
	}
}
