package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
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
		@Inject(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V", at = @At("HEAD"), cancellable = true)
		public void splatcraft$tweakItemRender(ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BakedModel model, CallbackInfo ci)
		{
			if (stack.getItem() instanceof SubWeaponItem subWeaponItem)
			{
				RendererHandler.renderSubWeapon(stack, subWeaponItem, matrices, vertexConsumers, light, MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true), leftHanded);
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
	@Mixin(HeldItemRenderer.class)
	public static class HeldItemRendererMixin
	{
		@WrapOperation(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
		public void splatcraft$overrideHeldItemRendering(HeldItemRenderer instance, AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Operation<Void> original)
		{
			if (RendererHandler.renderHand(tickDelta, hand, matrices))
			{
				original.call(instance, player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
			}
		}
	}
}
