package net.splatcraft.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.splatcraft.data.PlaySession;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
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
			LivingEntity entity = (LivingEntity) (Object) this;
			EntityInfoCapability.getOptional(entity).ifPresent(info ->
			{
				if (CommonUtils.isEntityMatchImmobile(entity, info))
				{
					ci.cancel();
				}
			});
		}
		@Inject(method = "tickActiveItemStack", at = @At("HEAD"), cancellable = true)
		public void splatcraft$preventItemUsageAgainWhenDead(CallbackInfo ci)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			EntityInfoCapability.getOptional(entity).ifPresent(info ->
			{
				if (CommonUtils.isEntityMatchImmobile(entity, info))
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
			LivingEntity entity = (LivingEntity) (Object) this;
			EntityInfoCapability.getOptional(entity).ifPresent(info ->
			{
				if (CommonUtils.isEntityMatchImmobile(entity, info))
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
		@Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
		public void splatcraft$prohibitBlockInteractionWhenOnMatch(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir)
		{
			EntityInfoCapability.getOptional(player).ifPresent(info ->
			{
				if (CommonUtils.isEntityMatchImmobile(player, info))
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
				PlaySession.getPlaySession(entity).ifPresent(session ->
				{
					if (Instant.now().until(session.getMatchStartInstant(), ChronoUnit.SECONDS) > 2)
						ci.cancel();
				});
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
	@Mixin(Camera.class)
	public static abstract class MatchCameraMixin
	{
		@Shadow
		private boolean ready;
		@Shadow
		private BlockView area;
		@Shadow
		private float lastTickDelta;
		@Shadow
		private boolean thirdPerson;
		@Shadow
		private Entity focusedEntity;
		@Shadow
		protected abstract void setPos(double x, double y, double z);
		@Shadow
		protected abstract void setRotation(float yaw, float pitch);
		@Shadow
		protected abstract float clipToSpace(float f);
		@Inject(method = "update", at = @At(value = "HEAD"), cancellable = true)
		public void splatcraft$doCameraIntroPos(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci)
		{
			if (focusedEntity instanceof PlayerEntity player)
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
						if (secondsBeforeStart > 0)
						{
							float secondsAfterInit = PlaySession.INTRO_DURATION.getSeconds() - secondsBeforeStart;
							Pair<Vec3d, Vec2f>[] cameraPositions = ClientUtils.getMatchIntroData(stage);
							if (secondsAfterInit < 5)
							{
								Vec3d matchCenterPos = cameraPositions[0].getFirst();
								matchCenterPos = matchCenterPos.add(0, 5 + 20 * (1 - 1 / (1 + secondsAfterInit)), 0);
								
								setPos(matchCenterPos.x, matchCenterPos.y, matchCenterPos.z);
								setRotation(MathHelper.sqrt(secondsAfterInit * (90 / MathHelper.sqrt(5))), 90);
								
								splatcraft$doCancel(ci, area, focusedEntity, tickDelta);
								return;
							}
							if (secondsAfterInit < 12)
							{
								int i = 1 + (int) (((secondsAfterInit - 5f) / 7f) * (cameraPositions.length - 1));
								Pair<Vec3d, Vec2f> lookData = cameraPositions[i];
								setPos(lookData.getFirst().x, lookData.getFirst().y, lookData.getFirst().z);
								setRotation(lookData.getSecond().y, lookData.getSecond().x);
								
								splatcraft$doCancel(ci, area, focusedEntity, tickDelta);
								return;
							}
						}
						Pair<UUID, Vector3f> killCamData = ClientUtils.killCamData;
						if (killCamData != null && info.isMatchRespawning() && info.getMatchRespawnTimeLeft() > 60)
						{
							PlayerEntity killerPlayer = focusedEntity.getWorld().getPlayerByUuid(ClientUtils.killCamData.getFirst());
							if (killerPlayer != null)
							{
								Vector3f killCamDirection = killCamData.getSecond();
								float delta = 1f - Math.min(info.getMatchRespawnTimeLeft() - 55, 0) / 5f;
								Vec3d camStartPos = focusedEntity.getPos();
								Vec3d camEndPos = killerPlayer.getPos().subtract(killCamDirection.x, killCamDirection.y, killCamDirection.z);
								Vec3d camPos = camStartPos.lerp(camEndPos, delta);
								
								float horizontalLength = killCamDirection.x * killCamDirection.x + killCamDirection.z * killCamDirection.z;
								float pitch = (float) (MathHelper.atan2(killCamDirection.y, horizontalLength) * MathHelper.DEGREES_PER_RADIAN);
								float yaw = (float) (MathHelper.atan2(killCamDirection.x, killCamDirection.z) * MathHelper.DEGREES_PER_RADIAN);
								
								setPos(camPos.x, camPos.y, camPos.z);
								setRotation(MathHelper.lerp(delta, focusedEntity.getYaw(tickDelta), yaw), MathHelper.lerp(delta, focusedEntity.getPitch(tickDelta), pitch));
								
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
		private void splatcraft$doCancel(CallbackInfo ci, BlockView area, Entity focusedEntity, float tickDelta)
		{
			ready = true;
			this.area = area;
			this.focusedEntity = focusedEntity;
			thirdPerson = true;
			lastTickDelta = tickDelta;
			ci.cancel();
		}
	}
}
