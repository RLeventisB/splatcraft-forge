package net.splatcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Camera;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
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
import net.splatcraft.client.handlers.RendererHandler;
import net.splatcraft.client.renderer.SplatcraftRenderTypes;
import net.splatcraft.data.PlaySession;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.SaveInfoCapability;
import net.splatcraft.data.capabilities.structs.PlayerInfo;
import net.splatcraft.data.capabilities.structs.SaveInfo;
import net.splatcraft.platform.Components;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.specials.StingRayAction;
import net.splatcraft.util.structs.InkColor;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

public class MatchMixins
{
	@Mixin(LivingEntity.class)
	public static class LivingEntityMixin
	{
		@Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
		public void splatcraft$preventDeathTimerOnMatch(CallbackInfo ci)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			if (entity instanceof Player player)
				Components.PLAYER_INFO.getOptional(player).ifPresent(info ->
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
			if (entity instanceof Player player)
				Components.PLAYER_INFO.getOptional(player).ifPresent(info ->
				{
					if (info.calculateMatchState(entity).movementDisabled)
					{
						ci.cancel();
					}
				});
		}
		@Inject(method = "updatingUsingItem", at = @At("HEAD"), cancellable = true)
		public void splatcraft$preventItemUsageAgainWhenDead(CallbackInfo ci)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			if (entity instanceof Player player)
				Components.PLAYER_INFO.getOptional(player).ifPresent(info ->
				{
					if (info.calculateMatchState(entity).movementDisabled)
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
			if (entity instanceof Player player)
				Components.PLAYER_INFO.getOptional(player).ifPresent(info ->
				{
					if (info.calculateMatchState(entity).movementDisabled)
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
			if (entity instanceof Player player)
				Components.PLAYER_INFO.getOptional(player).ifPresent(info ->
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
			if (entity instanceof Player player)
				Components.PLAYER_INFO.getOptional(player).ifPresent(info ->
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
			if (entity instanceof Player player)
				Components.PLAYER_INFO.getOptional(player).ifPresent(info ->
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
			if (entity instanceof Player player)
				Components.PLAYER_INFO.getOptional(player).ifPresent(info ->
				{
					if (info.calculateMatchState(entity).movementDisabled)
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
			LivingEntity entity = (LivingEntity) (Object) this;
			if (entity instanceof Player player)
				Components.PLAYER_INFO.getOptional(player).ifPresent(info ->
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
			LivingEntity entity = (LivingEntity) (Object) this;
			if (entity instanceof Player player)
				Components.PLAYER_INFO.getOptional(player).ifPresent(info ->
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
			LivingEntity entity = (LivingEntity) (Object) this;
			if (entity instanceof Player player)
				Components.PLAYER_INFO.getOptional(player).ifPresent(info ->
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
			Components.PLAYER_INFO.getOptional(player).ifPresent(info ->
			{
				if (info.calculateMatchState(player).movementDisabled)
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
			if (entity instanceof Player player)
				Components.PLAYER_INFO.getOptional(player).ifPresent(info ->
				{
					if (info.calculateMatchState(entity).modifiesCamera)
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
			Components.PLAYER_INFO.getOptional(player).ifPresent(info ->
			{
				if (info.isPlaying() && info.isMatchRespawning())
				{
					ci.cancel();
				}
			});
		}
		@Inject(method = "renderNameTag(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V", at = @At("HEAD"), cancellable = true)
		public void splatcraft$hideEnemyTeamNametag(AbstractClientPlayer entity, Component displayName, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTick, CallbackInfo ci)
		{
			LocalPlayer clientPlayer = ClientUtils.getClientPlayer();
			// if the client player, for some reason, renders their own name tag, or isnt playing, then do not cancel rendering
			if (entity == clientPlayer || !Components.PLAYER_INFO.hasAnd(clientPlayer, PlayerInfo::isPlaying))
				return;

			InkColor clientColor = ColorUtils.getEntityColor(clientPlayer);
			InkColor entityColor = ColorUtils.getEntityColor(entity);

			// if the player isnt in a match or has the same color, do not cancel rendering
			if (!Components.PLAYER_INFO.hasAnd(entity, PlayerInfo::isPlaying) || clientColor.equals(entityColor))
				return;

			// if the player killed the client player, do not cancel rendering
			if (ClientUtils.killCamData != null && entity.getUUID().equals(ClientUtils.killCamData.getFirst()))
				return;

			// if the player is dead (in a match), do not cancel rendering
			if (Components.PLAYER_INFO.hasAnd(entity, PlayerInfo::isMatchRespawning))
				return;

			ci.cancel();
		}
		@Inject(method = "renderHand", at = @At(value = "HEAD"), cancellable = true)
		public void splatcraft$cancelHeldItemRenderIfDead(PoseStack matrices, MultiBufferSource vertexConsumers, int light, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, CallbackInfo ci)
		{
			Components.PLAYER_INFO.getOptional(player).ifPresent(info ->
			{
				if (info.calculateMatchState(player).modifiesCamera)
				{
					ci.cancel();
				}
			});
		}
	}
	@Mixin(LivingEntityRenderer.class)
	public static abstract class LivingEntityRendererMixin
	{
		@Shadow
		@Final
		protected List<RenderLayer<LivingEntity, EntityModel<LivingEntity>>> layers;
		@Shadow
		protected EntityModel<LivingEntity> model;
		@Shadow
		public static int getOverlayCoords(LivingEntity livingEntity, float u)
		{
			return 0;
		}
		@Shadow
		protected abstract float getWhiteOverlayProgress(LivingEntity livingEntity, float partialTicks);
		@Inject(method = "shouldShowName*", at = @At("HEAD"), cancellable = true)
		public <T extends Entity> void splatcraft$hideEnemyTeamNametag(T entity, CallbackInfoReturnable<Boolean> cir)
		{
			if (!(entity instanceof LivingEntity living))
				return;

			RendererHandler.processHidingEntity(cir, living);
		}
		@SuppressWarnings("UnresolvedLocalCapture")
		@Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(value = "INVOKE",
				target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getRenderType(Lnet/minecraft/world/entity/LivingEntity;ZZZ)Lnet/minecraft/client/renderer/RenderType;"))
		public <T extends LivingEntity> void splatcraft$renderLayersAsSilhouette(
			T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci,
			@Local(index = 15, ordinal = 9) float f5,
			@Local(index = 14, ordinal = 8) float f4,
			@Local(index = 13, ordinal = 7) float f9,
			@Local(index = 10, ordinal = 4) float f2,
			@Local(index = 11, ordinal = 5) float f6
		)
		{
			LocalPlayer clientPlayer = ClientUtils.getClientPlayer();
			StingRayAction stingRayAction = EntityAction.getSpecificEntityAction(clientPlayer, StingRayAction.class);

			if (stingRayAction == null)
				return;

			if (ColorUtils.getEntityColor(clientPlayer).equals(ColorUtils.getEntityColor(entity)))
				return;

			float distanceProgress = entity.distanceTo(clientPlayer) / stingRayAction.getRevealRadius();
			float silhouetteStrength = Math.max(0, Math.max(0, 1f - (stingRayAction.getUsageTick() + partialTicks) / 20f) - (Math.max(0, distanceProgress - 1f) * 30f));

			if (silhouetteStrength == 0)
				return;

			EntityRenderer<T> renderer = (EntityRenderer<T>) (Object) this;
			int i = getOverlayCoords(entity, getWhiteOverlayProgress(entity, partialTicks));

			RenderType silhouetteRenderType = SplatcraftRenderTypes.entitySilhouette(renderer.getTextureLocation(entity));
			MultiBufferSource.BufferSource bufferSource = ClientUtils.getClient().renderBuffers().bufferSource();

			SplatcraftRenderTypes.WrappedSilhouetteMultiBufferSource silhouetteBufferSource = new SplatcraftRenderTypes.WrappedSilhouetteMultiBufferSource(bufferSource, silhouetteStrength);

			model.renderToBuffer(poseStack, bufferSource.getBuffer(silhouetteRenderType), packedLight, i,
				FastColor.ARGB32.color((int) (255 * silhouetteStrength), -1));

			if (!entity.isSpectator())
			{
				for (RenderLayer<LivingEntity, EntityModel<LivingEntity>> renderlayer : layers)
				{
					renderlayer.render(poseStack, silhouetteBufferSource, packedLight, entity, f5, f4, partialTicks, f9, f2, f6);
				}
			}

			bufferSource.endBatch(silhouetteRenderType);
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
		@Shadow
		protected abstract void move(float zoom, float dy, float dx);
		@Inject(method = "setup", at = @At(value = "HEAD"), cancellable = true)
		public void splatcraft$doCameraIntroPos(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci)
		{
			if (focusedEntity instanceof Player player)
			{
				Components.PLAYER_INFO.getOptional(player).ifPresent(info ->
				{
					SaveInfo saveInfo = SaveInfoCapability.get();
					PlaySession session = saveInfo.playSessions().get(info.getPlayingStageId());
					Stage stage = saveInfo.stages().get(info.getPlayingStageId());
					float nowSeconds = (player.level().getGameTime() + tickDelta) / 20f;
					if (session != null)
					{
						float secondsBeforeStart = session.getMatchStartTime() / 20f - nowSeconds;
						ClientUtils.MatchCameraPositions cameraPositions = ClientUtils.getMatchIntroData(stage);
						if (secondsBeforeStart > 0)
						{
							float secondsAfterInit = PlaySession.INTRO_DURATION / 20f - secondsBeforeStart;
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
								if (cameraPositions.spawnPads().isEmpty()) // youre not supposed to be empty, why are you empty when rejoining an match.
								{
									ClientUtils.matchStartCameraPosProvider.reset();
								}
								else
								{
									int i = (int) ((secondsAfterInit - 5f) / 7f * cameraPositions.spawnPads().size());
									ClientUtils.CameraPosition lookData = cameraPositions.spawnPads().get(i);
									lookData.applyTransformations(this::setPosition, this::setRotation);

									splatcraft$doCancel(ci, area, focusedEntity, tickDelta);
									return;
								}
							}
						}
						else
						{
							float secondsAfterEnd = nowSeconds - session.getMatchEndTime() / 20f;
							if (session.hasEnded() && secondsAfterEnd > 0)
							{
								float delta = Math.min(1f, secondsAfterEnd / Math.min(PlaySession.END_DURATION / 20f, 2));
								ClientUtils.CameraPosition finalPos = ClientUtils.CameraPosition.lerp(
									ClientUtils.CameraPosition.from(player), cameraPositions.birdsEye(),
									// Mth.catmullrom(delta, -1, 0, 1, 0)
									(delta + delta * delta - delta * delta * delta));
								finalPos.applyTransformations(this::setPosition, this::setRotation);

								splatcraft$doCancel(ci, area, focusedEntity, tickDelta);
								return;
							}
						}
						Pair<UUID, Vector2f> killCamData = ClientUtils.killCamData;
						if (killCamData != null && info.isMatchRespawning() && info.getMatchRespawnTimeLeft() <= 60)
						{
							Player killerPlayer = focusedEntity.level().getPlayerByUUID(killCamData.getFirst());
							if (killerPlayer != null)
							{
								float killProgress = 1f - Math.max(info.getMatchRespawnTimeLeft() - 55 - tickDelta, 0) / 5f;
								Vector2f killCamRotData = killCamData.getSecond();

								ClientUtils.CameraPosition killCam = new ClientUtils.CameraPosition(
									killerPlayer.getEyePosition(tickDelta),
									killCamRotData.x,
									killCamRotData.y);

								ClientUtils.CameraPosition finalPos = ClientUtils.CameraPosition.lerp(
									ClientUtils.CameraPosition.from(player), killCam,
									killProgress);
								finalPos.applyTransformations(this::setPosition, this::setRotation);

								move(-getMaxZoom(4.0F * killProgress), 0.0F, 0.0F);
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
			level = area;
			entity = focusedEntity;
			detached = true;
			partialTickTime = tickDelta;
			ci.cancel();
		}
	}
}
