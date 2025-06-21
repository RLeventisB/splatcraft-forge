package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.splatcraft.client.handlers.PlayerMovementHandler;
import net.splatcraft.client.handlers.RendererHandler;
import net.splatcraft.client.layer.InkTankFeature;
import net.splatcraft.handlers.SquidFormHandler;
import net.splatcraft.handlers.WeaponHandler;
import net.splatcraft.registries.SplatcraftEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

public class PlayerMixins
{
	@Mixin(Player.class)
	public static class PlayerMixin
	{
		@ModifyReturnValue(method = "createAttributes", at = @At("RETURN"))
		private static AttributeSupplier.Builder createAttributes(AttributeSupplier.Builder original)
		{
			return SplatcraftEntities.injectPlayerAttributes(original);
		}
	}
	@Mixin(ServerPlayer.class)
	public static class ServerPlayerMixinFabric
	{
		@Inject(method = "setGameMode", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
		public void splatcraft$onChangeGamemode(GameType gameMode, CallbackInfoReturnable<Boolean> cir)
		{
			SquidFormHandler.onGameModeSwitch((ServerPlayer) (Object) this, gameMode);
		}
	}
	@Mixin(LocalPlayer.class)
	public static class LocalPlayerMixinFabric
	{
		@Shadow
		public Input input;
		@Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/tutorial/Tutorial;onInput(Lnet/minecraft/client/player/Input;)V"))
		public void splatcraft$callInputUpdate(CallbackInfo ci)
		{
			PlayerMovementHandler.onInputUpdate((LocalPlayer) (Object) this, input);
		}
	}
	@Mixin(PlayerRenderer.class)
	public static class PlayerRendererMixin
	{
		@Inject(method = "<init>", at = @At("RETURN"))
		public void splatcraft$addLayers(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci)
		{
			PlayerRenderer renderer = (PlayerRenderer) (Object) this;
			renderer.addLayer(new InkTankFeature<>(renderer, ctx.getModelSet()));
		}
		// it isnt possible to add a wrapoperation to the base render call because it has a generic parameter :(
		@Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"), cancellable = true)
		public void splatcraft$overridePlayerRender(AbstractClientPlayer entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci)
		{
			if (RendererHandler.playerRender(entity, entityYaw, partialTicks, poseStack, buffer, packedLight))
			{
				ci.cancel();
			}
		}
	}
	@Mixin(HumanoidModel.class)
	public static class WeaponUsageModelMixin
	{
		@WrapOperation(method = "setupAnim*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isUsingItem()Z"))
		public boolean splatcraft$addWeaponUsageAsUsingItem(LivingEntity instance, Operation<Boolean> original)
		{
			Optional<InteractionHand> hand = WeaponHandler.getUsingWeaponHand(instance);
			if (hand.isPresent())
				return true;
			return original.call(instance);
		}
		@WrapOperation(method = "setupAnim*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getUsedItemHand()Lnet/minecraft/world/InteractionHand;"))
		public InteractionHand splatcraft$retrieveUsedWeaponHand(LivingEntity instance, Operation<InteractionHand> original)
		{
			Optional<InteractionHand> hand = WeaponHandler.getUsingWeaponHand(instance);
			return hand.orElseGet(() -> original.call(instance));
		}
	}
}
