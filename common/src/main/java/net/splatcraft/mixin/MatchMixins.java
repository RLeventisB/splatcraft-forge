package net.splatcraft.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class MatchMixins
{
	@Mixin(LivingEntity.class)
	public static class LivingEntityMixin
	{
		@Inject(method = "updatePostDeath", at = @At("HEAD"), cancellable = true)
		public void splatcraft$preventDeathTimerOnMatch(CallbackInfo ci)
		{
			EntityInfoCapability.getOptional((LivingEntity) (Object) this).ifPresent(info ->
			{
				if (info.isPlaying())
				{
					ci.cancel();
				}
			});
		}
		@Inject(method = "setCurrentHand", at = @At("HEAD"), cancellable = true)
		public void splatcraft$preventItemUsageWhenDead(CallbackInfo ci)
		{
			EntityInfoCapability.getOptional((LivingEntity) (Object) this).ifPresent(info ->
			{
				if (info.isPlaying() && info.isMatchRespawning())
				{
					ci.cancel();
				}
			});
		}
		@Inject(method = "tickActiveItemStack", at = @At("HEAD"), cancellable = true)
		public void splatcraft$preventItemUsageAgainWhenDead(CallbackInfo ci)
		{
			EntityInfoCapability.getOptional((LivingEntity) (Object) this).ifPresent(info ->
			{
				if (info.isPlaying() && info.isMatchRespawning())
				{
					ci.cancel();
				}
			});
		}
	}
	@Mixin(AbstractClientPlayerEntity.class)
	public static class AbstractClientPlayerMixin
	{
		@Inject(method = "isSpectator", at = @At("HEAD"), cancellable = true)
		public void splatcraft$mimicSpectatorModeWhenDead(CallbackInfoReturnable<Boolean> cir)
		{
			EntityInfoCapability.getOptional((LivingEntity) (Object) this).ifPresent(info ->
			{
				if (info.isPlaying() && info.isMatchRespawning())
				{
					cir.setReturnValue(true);
				}
			});
		}
		@Inject(method = "isCreative", at = @At("HEAD"), cancellable = true)
		public void splatcraft$mimicSpectatorModeWhenDeadTwo(CallbackInfoReturnable<Boolean> cir)
		{
			EntityInfoCapability.getOptional((LivingEntity) (Object) this).ifPresent(info ->
			{
				if (info.isPlaying() && info.isMatchRespawning())
				{
					cir.setReturnValue(false);
				}
			});
		}
	}
	@Mixin(PlayerEntity.class)
	public static class PlayerMixin
	{
		@Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
		public void splatcraft$inhibitMovementWhenDead(CallbackInfo ci)
		{
			EntityInfoCapability.getOptional((LivingEntity) (Object) this).ifPresent(info ->
			{
				if (info.isPlaying() && info.isMatchRespawning())
				{
					ci.cancel();
				}
			});
		}
		@Inject(method = "isBlockBreakingRestricted", at = @At("HEAD"), cancellable = true)
		public void splatcraft$prohibitBlockBreakingWhenDead(World world, BlockPos pos, GameMode gameMode, CallbackInfoReturnable<Boolean> cir)
		{
			EntityInfoCapability.getOptional((LivingEntity) (Object) this).ifPresent(info ->
			{
				if (info.isPlaying() && info.isMatchRespawning())
				{
					cir.setReturnValue(true);
				}
			});
		}
	}
	@Mixin(ClientPlayerEntity.class)
	public static class ClientPlayerMixinCommon
	{
		@Inject(method = "showsDeathScreen", at = @At("HEAD"), cancellable = true)
		public void splatcraft$preventDeathScreenOnMatch(CallbackInfoReturnable<Boolean> cir)
		{
			EntityInfoCapability.getOptional((ClientPlayerEntity) (Object) this).ifPresent(info ->
			{
				if (info.isPlaying())
				{
					cir.setReturnValue(false);
				}
			});
		}
		@Inject(method = "requestRespawn", at = @At("HEAD"), cancellable = true)
		public void splatcraft$preventRespawnOnMatch(CallbackInfo ci)
		{
			EntityInfoCapability.getOptional((ClientPlayerEntity) (Object) this).ifPresent(info ->
			{
				if (info.isPlaying())
				{
					ci.cancel();
				}
			});
		}
		@Inject(method = "updatePostDeath", at = @At("HEAD"), cancellable = true)
		public void splatcraft$WHYARETHERETWOIMeanpreventDeathTimerOnMatch(CallbackInfo ci)
		{
			EntityInfoCapability.getOptional((ClientPlayerEntity) (Object) this).ifPresent(info ->
			{
				if (info.isPlaying())
				{
					ci.cancel();
				}
			});
		}
	}
	@Mixin(ClientPlayerInteractionManager.class)
	public static class ClientInteractionManagerMixin
	{
		@Shadow
		@Final
		private MinecraftClient client;
		@Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
		public void splatcraft$prohibitBlockInteractionWhenOnMatch(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir)
		{
			EntityInfoCapability.getOptional(player).ifPresent(info ->
			{
				if (info.isPlaying() && info.isMatchRespawning())
				{
					cir.setReturnValue(ActionResult.FAIL);
				}
			});
		}
	}
	@Mixin(HeldItemRenderer.class)
	public static class HeldItemRendererMixin
	{
		@Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "HEAD"), cancellable = true)
		public void splatcraft$cancelPlayerRenderIfDead(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci)
		{
			EntityInfoCapability.getOptional(entity).ifPresent(info ->
			{
				if (info.isPlaying() && info.isMatchRespawning())
				{
					ci.cancel();
				}
			});
		}
	}
	@Mixin(PlayerEntityRenderer.class)
	public static class PlayerRendererMixin
	{
		@Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "HEAD"), cancellable = true)
		public void splatcraft$cancelPlayerRenderIfDead(AbstractClientPlayerEntity player, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci)
		{
			EntityInfoCapability.getOptional(player).ifPresent(info ->
			{
				if (info.isPlaying() && info.isMatchRespawning())
				{
					ci.cancel();
				}
			});
		}
		@Inject(method = "renderArm", at = @At(value = "HEAD"), cancellable = true)
		public void splatcraft$cancelHeldItemRenderIfDead(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve, CallbackInfo ci)
		{
			EntityInfoCapability.getOptional(player).ifPresent(info ->
			{
				if (info.isPlaying() && info.isMatchRespawning())
				{
					ci.cancel();
				}
			});
		}
	}
}
