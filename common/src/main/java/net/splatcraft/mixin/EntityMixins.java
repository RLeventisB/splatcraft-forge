package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.client.handlers.RendererHandler;
import net.splatcraft.handlers.SplatcraftCommonHandler;
import net.splatcraft.handlers.SquidFormHandler;
import net.splatcraft.items.weapons.DualieItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.platform.Components;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkDamageUtils;
import net.splatcraft.util.action.EntityAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

public class EntityMixins
{
	@Mixin(Entity.class)
	public static class EntityMixin
	{
		@Unique
		private BlockPos splatcraft$stepBlockPos;
		@Inject(method = "isInvisible", at = @At("TAIL"), cancellable = true)
		public void splatcraft$modifyVisibility(CallbackInfoReturnable<Boolean> cir)
		{
			Entity entity = (Entity) (Object) this;
			if (!(entity instanceof LivingEntity living) || !Components.ENTITY_INFO.has(living))
			{
				return;
			}
			
			if (InkBlockUtils.canSquidHide(living) && CommonUtils.isSquid(living))
				cir.setReturnValue(true);
		}
		@Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
		public void setSprinting(boolean sprinting, CallbackInfo ci)
		{
			Entity entity = (Entity) (Object) this;
			if (!(entity instanceof Player player) || !Components.ENTITY_INFO.has(player))
			{
				return;
			}
			if (sprinting && EntityAction.hasEntityAction(player))
			{
				player.setSprinting(false);
				ci.cancel();
			}
		}
		@WrapOperation(method = "spawnSprintParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;"))
		public RenderShape addRunningEffects(BlockState instance, Operation<RenderShape> original)
		{
			Entity entity = ((Entity) (Object) this);
			Level world = entity.level();
			BlockPos pos = entity.getOnPosLegacy();
			if (InkBlockUtils.isInked(world, pos, Direction.UP))
			{
				ColorUtils.addInkSplashParticle(world, InkBlockUtils.getInkBlock(world, pos).color(Direction.UP.get3DDataValue()), entity.getX() + world.getRandom().nextFloat() * entity.getBbWidth() - entity.getBbWidth() * 0.5,
					entity.getY(world.getRandom().nextFloat() * 0.3f), entity.getZ() + world.getRandom().nextFloat() * entity.getBbWidth() - entity.getBbWidth() * 0.5, 0.3f + world.random.nextFloat() * 0.4f);
				return RenderShape.MODEL;
			}
			
			return original.call(instance);
		}
		@Inject(method = "playStepSound", at = @At(value = "HEAD"))
		public void getBlockPos(BlockPos pos, BlockState state, CallbackInfo ci)
		{
			splatcraft$stepBlockPos = pos;
		}
		@WrapOperation(method = "playStepSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
		public void getRunningSound(Entity instance, SoundEvent sound, float volume, float pitch, Operation<Void> original)
		{
			Entity entity = (Entity) (Object) this;
			Level world = entity.level();
			if (InkBlockUtils.isInked(world, splatcraft$stepBlockPos, Direction.UP))
			{
				SoundType soundGroup = entity instanceof LivingEntity living && CommonUtils.isSquid(living) && InkBlockUtils.canSquidSwim(living) ?
					SplatcraftSounds.SOUND_TYPE_SWIMMING : SplatcraftSounds.SOUND_TYPE_INK;
				original.call(instance, soundGroup.getFallSound(), volume, pitch);
				return;
			}
			original.call(instance, sound, volume, pitch);
		}
		@Inject(method = "tick", at = @At(value = "TAIL"))
		public void splatcraft$afterTick(CallbackInfo ci)
		{
			Entity entity = (Entity) (Object) this;
			SquidFormHandler.doSquidRotation(entity);
			SplatcraftCommonHandler.onLivingTick(entity);
		}
		@Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true)
		public void splatcraft$cancelMovementIfRoll(float speed, Vec3 movementInput, CallbackInfo ci)
		{
			Entity entity = (Entity) (Object) this;
			if (entity instanceof LivingEntity living)
			{
				Optional<EntityAction> action = EntityAction.getEntityActionOptional(living);
				if (action.isPresent() && action.get() instanceof DualieItem.DodgeRollAction dodgeRoll && !dodgeRoll.canMove())
					ci.cancel();
			}
		}
	}
	@Mixin(LivingEntity.class)
	public static class LivingEntityMixin
	{
		@Inject(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
		public void splatcraft$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir)
		{
			SplatcraftCommonHandler.onPlayerAboutToDie((LivingEntity) (Object) this, amount);
		}
		@Inject(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSprinting()Z"))
		public void onJump(CallbackInfo ci)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			if (entity instanceof Player player)
			{
				for (var item : player.getInventory().items)
				{
					splatcraft$processItemForJumpRng(item, entity);
				}
				for (var item : player.getInventory().offhand)
				{
					splatcraft$processItemForJumpRng(item, entity);
				}
				for (var item : player.getInventory().armor)
				{
					splatcraft$processItemForJumpRng(item, entity);
				}
			}
			else
			{
				splatcraft$processItemForJumpRng(entity.getMainHandItem(), entity);
				splatcraft$processItemForJumpRng(entity.getOffhandItem(), entity);
			}
			
			SplatcraftCommonHandler.onEntityJump(entity);
			SquidFormHandler.modifyJumpSpeed(entity);
		}
		@Inject(method = "hurt", at = @At("HEAD"))
		public void splatcraft$failsafeDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir)
		{
			SquidFormHandler.onLivingHurt((LivingEntity) (Object) this, source, cir);
		}
		// shhhhh you play like 20 times per second
		@WrapOperation(method = "handleDamageEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)Lnet/minecraft/sounds/SoundEvent;"))
		public SoundEvent splatcraft$silenceHurtDamage(LivingEntity instance, DamageSource damageSource, Operation<SoundEvent> original)
		{
			if (damageSource.is(SplatcraftDamageTypes.ENEMY_INK) || damageSource instanceof InkDamageUtils.InkDamageSource inkSource && inkSource.doSound)
				return null;
			return original.call(instance, damageSource);
		}
		@Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
		public void splatcraft$handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir)
		{
			SquidFormHandler.cancelDamageIfSquid((LivingEntity) (Object) this, fallDistance, cir);
		}
		@Unique
		public void splatcraft$processItemForJumpRng(ItemStack stack, LivingEntity entity)
		{
			if (stack.getItem() instanceof WeaponBaseItem<?> weaponItem)
			{
				CommonRecords.ShotDeviationDataRecord deviationData = weaponItem.getSettings(stack).getShotDeviationData(stack, entity);
				
				ShotDeviationHelper.registerJumpForShotDeviation(stack, deviationData);
			}
		}
		@ModifyReturnValue(method = "getVisibilityPercent", at = @At("RETURN"))
		public double splatcraft$modifyVisibility(double original)
		{
			SquidFormHandler.modifyVisibility((LivingEntity) (Object) this, original);
			return original;
		}
		@WrapOperation(method = "checkFallDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
		public int addLandingEffects(ServerLevel instance, ParticleOptions j, double v, double clientboundlevelparticlespacket, double i, int particle, double x, double y, double z, double count, Operation<Integer> original)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			BlockPos pos = entity.getOnPosLegacy();
			
			if (InkBlockUtils.isInked(entity.level(), pos, Direction.UP))
			{
				ColorUtils.addInkSplashParticle(entity.level(), InkBlockUtils.getInkBlock(entity.level(), pos).color(Direction.UP.get3DDataValue()), entity.getX(), entity.getY(entity.level().getRandom().nextFloat() * 0.3f), entity.getZ(), (float) (Math.sqrt(i) * 0.3f));
				return 0;
			}
			return original.call(instance, j, v, clientboundlevelparticlespacket, i, particle, x, y, z, count);
		}
		@WrapOperation(method = "playBlockFallSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
		public void splatcraft$getFallSound(LivingEntity instance, SoundEvent soundEvent, float volume, float pitch, Operation<Void> original)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			if (InkBlockUtils.isInked(entity.level(), entity.getOnPosLegacy(), Direction.UP))
			{
				original.call(instance, SplatcraftSounds.SOUND_TYPE_INK.getFallSound(), volume, pitch);
				return;
			}
			original.call(instance, soundEvent, volume, pitch);
		}
		@WrapOperation(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getJumpPower()F"))
		public float splatcraft$cancelJumpIfRolling(LivingEntity instance, Operation<Float> original)
		{
			if (instance.isUsingItem() && instance.getUseItem().getItem() instanceof DualieItem && (instance.xxa != 0 || instance.zza != 0) || EntityAction.getSpecificEntityActionIf(instance, action -> !action.canMove(), DualieItem.DodgeRollAction.class).isPresent())
				return 0;
			return original.call(instance);
		}
	}
	@Mixin(EntityRenderer.class)
	public static class EntityRendererMixin
	{
		@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V"))
		public void splatcraft$colorLabel(EntityRenderer instance, Entity entity, Component text, PoseStack matrixStack, MultiBufferSource consumerProvider, int light, float tickDelta, Operation<Void> original)
		{
			original.call(instance, entity, RendererHandler.modifyNameplate(entity, text), matrixStack, consumerProvider, light, tickDelta);
		}
		@Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
		public <T extends Entity> void splatcraft$hideEnemyTeamNametag(T entity, CallbackInfoReturnable<Boolean> cir)
		{
			if (!(entity instanceof LivingEntity living))
				return;
			
			RendererHandler.processHidingEntity(cir, living);
		}
	}
}