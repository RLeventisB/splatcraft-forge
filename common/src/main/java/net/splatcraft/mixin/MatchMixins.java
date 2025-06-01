package net.splatcraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Camera;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.data.PlaySession;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.util.ClientUtils;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class MatchMixins
{
	@Mixin(LivingEntity.class)
	public static class LivingEntityMixin
	{
		@Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
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
		@Inject(method = "startUsingItem", at = @At("HEAD"), cancellable = true)
		public void splatcraft$preventItemUsageWhenDead(CallbackInfo ci)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			EntityInfoCapability.getOptional(entity).ifPresent(info ->
			{
				if (info.getMatchState(entity).movementDisabled)
				{
					ci.cancel();
				}
			});
		}
		@Inject(method = "updatingUsingItem", at = @At("HEAD"), cancellable = true)
		public void splatcraft$preventItemUsageAgainWhenDead(CallbackInfo ci)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			EntityInfoCapability.getOptional(entity).ifPresent(info ->
			{
				if (info.getMatchState(entity).movementDisabled)
				{
					entity.stopUsingItem();
					ci.cancel();
				}
			});
		}
		@Inject(method = "updateUsingItem", at = @At("HEAD"), cancellable = true)
		public void splatcraft$preventItemUsageAgainAgainWhenDead(CallbackInfo ci)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			EntityInfoCapability.getOptional(entity).ifPresent(info ->
			{
				if (info.getMatchState(entity).movementDisabled)
				{
					entity.stopUsingItem();
					ci.cancel();
				}
			});
		}
	}
	@Mixin(AbstractClientPlayer.class)
	public static class AbstractClientPlayerMixin
	{
		@Inject(method = "isSpectator", at = @At("HEAD"), cancellable = true)
		public void splatcraft$mimicSpectatorModeWhenDead(CallbackInfoReturnable<Boolean> cir)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			EntityInfoCapability.getOptional(entity).ifPresent(info ->
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
			LivingEntity entity = (LivingEntity) (Object) this;
			EntityInfoCapability.getOptional(entity).ifPresent(info ->
			{
				if (info.isPlaying() && info.isMatchRespawning())
				{
					cir.setReturnValue(false);
				}
			});
		}
	}
	@Mixin(Player.class)
	public static class PlayerMixin
	{
		@Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
		public void splatcraft$inhibitMovementWhenDead(CallbackInfo ci)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			EntityInfoCapability.getOptional(entity).ifPresent(info ->
			{
				if (info.isPlaying() && info.isMatchRespawning())
				{
					ci.cancel();
				}
			});
		}
		@Inject(method = "blockActionRestricted", at = @At("HEAD"), cancellable = true)
		public void splatcraft$prohibitBlockBreakingWhenDead(Level world, BlockPos pos, GameType gameMode, CallbackInfoReturnable<Boolean> cir)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			EntityInfoCapability.getOptional(entity).ifPresent(info ->
			{
				if (info.getMatchState(entity).movementDisabled)
				{
					cir.setReturnValue(true);
				}
			});
		}
	}
	@Mixin(LocalPlayer.class)
	public static class ClientPlayerMixinCommon
	{
		@Inject(method = "shouldShowDeathScreen", at = @At("HEAD"), cancellable = true)
		public void splatcraft$preventDeathScreenOnMatch(CallbackInfoReturnable<Boolean> cir)
		{
			EntityInfoCapability.getOptional((LocalPlayer) (Object) this).ifPresent(info ->
			{
				if (info.isPlaying())
				{
					cir.setReturnValue(false);
				}
			});
		}
		@Inject(method = "respawn", at = @At("HEAD"), cancellable = true)
		public void splatcraft$preventRespawnOnMatch(CallbackInfo ci)
		{
			EntityInfoCapability.getOptional((LocalPlayer) (Object) this).ifPresent(info ->
			{
				if (info.isPlaying())
				{
					ci.cancel();
				}
			});
		}
		@Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
		public void splatcraft$WHYARETHERETWOIMeanpreventDeathTimerOnMatch(CallbackInfo ci)
		{
			EntityInfoCapability.getOptional((LocalPlayer) (Object) this).ifPresent(info ->
			{
				if (info.isPlaying())
				{
					ci.cancel();
				}
			});
		}
	}
	@Mixin(MultiPlayerGameMode.class)
	public static class ClientInteractionManagerMixin
	{
		@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
		public void splatcraft$prohibitBlockInteractionWhenOnMatch(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir)
		{
			EntityInfoCapability.getOptional(player).ifPresent(info ->
			{
				if (info.getMatchState(player).movementDisabled)
				{
					cir.setReturnValue(InteractionResult.FAIL);
				}
			});
		}
	}
	@Mixin(ItemInHandRenderer.class)
	public static class HeldItemRendererMixin
	{
		@Inject(method = "renderItem", at = @At(value = "HEAD"), cancellable = true)
		public void splatcraft$cancelPlayerRenderIfDead(LivingEntity entity, ItemStack stack, ItemDisplayContext renderMode, boolean leftHanded, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci)
		{
			EntityInfoCapability.getOptional(entity).ifPresent(info ->
			{
				if (info.getMatchState(entity).modifiesCamera)
				{
					ci.cancel();
				}
			});
		}
	}
	@Mixin(PlayerRenderer.class)
	public static class PlayerRendererMixin
	{
		@Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "HEAD"), cancellable = true)
		public void splatcraft$cancelPlayerRenderIfDead(AbstractClientPlayer player, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci)
		{
			EntityInfoCapability.getOptional(player).ifPresent(info ->
			{
				if (info.isPlaying() && info.isMatchRespawning())
				{
					ci.cancel();
				}
			});
		}
		@Inject(method = "renderHand", at = @At(value = "HEAD"), cancellable = true)
		public void splatcraft$cancelHeldItemRenderIfDead(PoseStack matrices, MultiBufferSource vertexConsumers, int light, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, CallbackInfo ci)
		{
			EntityInfoCapability.getOptional(player).ifPresent(info ->
			{
				if (info.getMatchState(player).modifiesCamera)
				{
					ci.cancel();
				}
			});
		}
	}
	@Mixin(Camera.class)
	public static abstract class MatchCameraMixin
	{
		@Shadow
		private boolean initialized;
		@Shadow
		private BlockGetter level;
		@Shadow
		private float partialTickTime;
		@Shadow
		private boolean detached;
		@Shadow
		private Entity entity;
		@Shadow
		protected abstract void setPosition(double x, double y, double z);
		@Shadow
		protected abstract void setRotation(float yaw, float pitch);
		@Shadow
		protected abstract float getMaxZoom(float f);
		@Inject(method = "setup", at = @At(value = "HEAD"), cancellable = true)
		public void splatcraft$doCameraIntroPos(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci)
		{
			if (focusedEntity instanceof Player player)
			{
				EntityInfoCapability.getOptional(player).ifPresent(info ->
				{
					SaveInfo saveInfo = SaveInfoCapability.get();
					PlaySession session = saveInfo.playSessions().get(info.getPlayingStageId());
					Stage stage = saveInfo.stages().get(info.getPlayingStageId());
					Instant now = Instant.now();
					if (session != null)
					{
						float secondsBeforeStart = now.until(session.getMatchStartInstant(), ChronoUnit.MILLIS) / 1000f;
						ClientUtils.MatchCameraPositions cameraPositions = ClientUtils.getMatchIntroData(stage);
						if (secondsBeforeStart > 0)
						{
							float secondsAfterInit = PlaySession.INTRO_DURATION.getSeconds() - secondsBeforeStart;
							if (secondsAfterInit < 5)
							{
								ClientUtils.CameraPosition finalPos = ClientUtils.CameraPosition.lerp(
									cameraPositions.floorStart(), cameraPositions.birdsEye().withYaw(14.0f),
									1 - 1 / (1 + 3 * secondsAfterInit), Mth.sqrt(secondsAfterInit)
								);

								finalPos.applyTransformations(this::setPosition, this::setRotation);

								splatcraft$doCancel(ci, area, focusedEntity, tickDelta);
								return;
							}
							if (secondsAfterInit < 12)
							{
								int i = (int) ((secondsAfterInit - 5f) / 7f * cameraPositions.spawnPads().size());
								ClientUtils.CameraPosition lookData = cameraPositions.spawnPads().get(i);
								lookData.applyTransformations(this::setPosition, this::setRotation);

								splatcraft$doCancel(ci, area, focusedEntity, tickDelta);
								return;
							}
						}
						else
						{
							float secondsAfterEnd = session.getMatchEndInstant().until(now, ChronoUnit.MILLIS) / 1000f;
							if (secondsAfterEnd > 0)
							{
								float delta = Math.min(1f, secondsAfterEnd / Math.min(PlaySession.END_DURATION.getSeconds(), 2));
								ClientUtils.CameraPosition finalPos = ClientUtils.CameraPosition.lerp(
									ClientUtils.CameraPosition.from(player), cameraPositions.birdsEye(),
									// Mth.catmullrom(delta, -1, 0, 1, 0)
									(delta + delta * delta - delta * delta * delta));
								finalPos.applyTransformations(this::setPosition, this::setRotation);

								splatcraft$doCancel(ci, area, focusedEntity, tickDelta);
								return;
							}
						}
						Pair<UUID, Vector3f> killCamData = ClientUtils.killCamData;
						if (killCamData != null && info.isMatchRespawning() && info.getMatchRespawnTimeLeft() > 60)
						{
							Player killerPlayer = focusedEntity.level().getPlayerByUUID(ClientUtils.killCamData.getFirst());
							if (killerPlayer != null)
							{
								Vector3f killCamDirection = killCamData.getSecond();
								float delta = 1f - Math.min(info.getMatchRespawnTimeLeft() - 55, 0) / 5f;
								Vec3 camStartPos = focusedEntity.position();
								Vec3 camEndPos = killerPlayer.position().subtract(killCamDirection.x, killCamDirection.y, killCamDirection.z);
								Vec3 camPos = camStartPos.lerp(camEndPos, delta);

								float horizontalLength = killCamDirection.x * killCamDirection.x + killCamDirection.z * killCamDirection.z;
								float pitch = (float) (Mth.atan2(killCamDirection.y, horizontalLength) * Mth.RAD_TO_DEG);
								float yaw = (float) (Mth.atan2(killCamDirection.x, killCamDirection.z) * Mth.RAD_TO_DEG);

								setPosition(camPos.x, camPos.y, camPos.z);
								setRotation(Mth.lerp(delta, focusedEntity.getViewYRot(tickDelta), yaw), Mth.lerp(delta, focusedEntity.getViewXRot(tickDelta), pitch));

								splatcraft$doCancel(ci, area, focusedEntity, tickDelta);
							}
						}
					}
				});
			}
		}
		// could have made the inject be after these instructions! unfortunatelly neoforge changes the method so it is separate between loaders
		// :(
		@Unique
		private void splatcraft$doCancel(CallbackInfo ci, BlockGetter area, Entity focusedEntity, float tickDelta)
		{
			initialized = true;
			this.level = area;
			this.entity = focusedEntity;
			detached = true;
			partialTickTime = tickDelta;
			ci.cancel();
		}
	}
}
