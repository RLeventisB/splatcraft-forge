package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.input.Input;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import net.splatcraft.client.handlers.PlayerMovementHandler;
import net.splatcraft.client.handlers.RendererHandler;
import net.splatcraft.handlers.SquidFormHandler;
import net.splatcraft.registries.SplatcraftEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class PlayerMixins
{
	@Mixin(PlayerEntity.class)
	public static class PlayerMixin
	{
		@ModifyReturnValue(method = "createPlayerAttributes", at = @At("RETURN"))
		private static DefaultAttributeContainer.Builder createAttributes(DefaultAttributeContainer.Builder original)
		{
			return SplatcraftEntities.injectPlayerAttributes(original);
		}
	}
	@Mixin(ServerPlayerEntity.class)
	public static class ServerPlayerMixinFabric
	{
		@Inject(method = "changeGameMode", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"))
		public void splatcraft$onChangeGamemode(GameMode gameMode, CallbackInfoReturnable<Boolean> cir)
		{
			SquidFormHandler.onGameModeSwitch((ServerPlayerEntity) (Object) this, gameMode);
		}
	}
	@Mixin(ClientPlayerEntity.class)
	public static class LocalPlayerMixinFabric
	{
		@Shadow
		public Input input;
		@Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/tutorial/TutorialManager;onMovement(Lnet/minecraft/client/input/Input;)V"))
		public void splatcraft$callInputUpdate(CallbackInfo ci)
		{
			PlayerMovementHandler.onInputUpdate((ClientPlayerEntity) (Object) this, input);
		}
	}
	@Mixin(PlayerEntityRenderer.class)
	public static class PlayerRendererMixin
	{
		@Inject(method = "renderArm", at = @At(value = "HEAD"), cancellable = true)
		public void splatcraft$overrideArmRender(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve, CallbackInfo ci)
		{
			RendererHandler.renderArm((PlayerEntityRenderer) (Object) this, matrices, vertexConsumers, light, player, arm, sleeve);
			ci.cancel();
		}
		@WrapOperation(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
		public void splatcraft$overridePlayerRender(PlayerEntityRenderer instance, LivingEntity player, float f, float g, MatrixStack matrixStack, VertexConsumerProvider consumerProvider, int i, Operation<Void> original)
		{
			if (!RendererHandler.playerRender(instance, (AbstractClientPlayerEntity) player, f, g, matrixStack, consumerProvider, i))
			{
				original.call(instance, player, f, g, matrixStack, consumerProvider, i);
			}
		}
	}
}
