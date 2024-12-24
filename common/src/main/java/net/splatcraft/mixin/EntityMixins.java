package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.splatcraft.client.handlers.RendererHandler;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.handlers.SplatcraftCommonHandler;
import net.splatcraft.handlers.SquidFormHandler;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.PlayerCooldown;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

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
			if (!(entity instanceof PlayerEntity player) || !EntityInfoCapability.hasCapability(player))
			{
				return;
			}
			
			if (InkBlockUtils.canSquidHide(player) && EntityInfoCapability.get(player).isSquid())
				cir.setReturnValue(true);
		}
		@Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
		public void setSprinting(boolean sprinting, CallbackInfo ci)
		{
			Entity entity = (Entity) (Object) this;
			if (!(entity instanceof PlayerEntity player) || !EntityInfoCapability.hasCapability(player))
			{
				return;
			}
			if (sprinting && PlayerCooldown.hasPlayerCooldown(player))
			{
				player.setSprinting(false);
				ci.cancel();
			}
		}
		@WrapOperation(method = "spawnSprintingParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getRenderType()Lnet/minecraft/block/BlockRenderType;"))
		public BlockRenderType addRunningEffects(BlockState instance, Operation<BlockRenderType> original)
		{
			Entity entity = ((Entity) (Object) this);
			World world = entity.getWorld();
			BlockPos pos = entity.getLandingPos();
			if (InkBlockUtils.isInked(world, pos, Direction.UP))
			{
				ColorUtils.addInkSplashParticle(world, InkBlockUtils.getInkBlock(world, pos).color(Direction.UP.getId()), entity.getX() + world.getRandom().nextFloat() * entity.getWidth() - entity.getWidth() * 0.5,
					entity.getBodyY(world.getRandom().nextFloat() * 0.3f), entity.getZ() + world.getRandom().nextFloat() * entity.getWidth() - entity.getWidth() * 0.5, 0.3f + world.random.nextFloat() * 0.4f);
				return BlockRenderType.MODEL;
			}
			
			return original.call(instance);
		}
		@Inject(method = "playStepSound", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "HEAD"))
		public void getBlockPos(BlockPos pos, BlockState state, CallbackInfo ci)
		{
			splatcraft$stepBlockPos = pos;
		}
		@WrapOperation(method = "playStepSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V"))
		public void getRunningSound(Entity instance, SoundEvent sound, float volume, float pitch, Operation<Void> original)
		{
			Entity entity = (Entity) (Object) this;
			World world = entity.getWorld();
			if (InkBlockUtils.isInked(world, splatcraft$stepBlockPos, Direction.UP))
			{
				BlockSoundGroup soundGroup = entity instanceof LivingEntity player && EntityInfoCapability.isSquid(player) && InkBlockUtils.canSquidSwim(player) ?
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
		// todo: there is a revive method that only players use! maybe handle it in the future if another mod uses it (not like making mods is suitable for compatibility though)
		@Inject(method = "setRemoved", at = @At("TAIL"))
		public void splatcraft$onEntityRemoval(Entity.RemovalReason reason, CallbackInfo ci)
		{
			Entity entity = (Entity) (Object) this;
			if (entity instanceof LivingEntity livingEntity)
			{
				if (InkOverlayCapability.hasCapability(livingEntity))
				{
					InkOverlayCapability.remove(livingEntity);
				}
				if (EntityInfoCapability.hasCapability(livingEntity))
				{
					EntityInfoCapability.remove(livingEntity);
				}
			}
		}
	}
	@Mixin(LivingEntity.class)
	public static class LivingEntityMixin
	{
		@Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;applyDamage(Lnet/minecraft/entity/damage/DamageSource;F)V"))
		public void splatcraft$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir)
		{
			SplatcraftCommonHandler.onPlayerAboutToDie((LivingEntity) (Object) this, amount);
		}
		@Inject(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getVelocity()Lnet/minecraft/util/math/Vec3d;"))
		public void onJump(CallbackInfo ci)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			if (entity instanceof PlayerEntity player)
			{
				for (var item : player.getInventory().main)
				{
					splatcraft$processItemForJumpRng(item, entity);
				}
				for (var item : player.getInventory().offHand)
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
				splatcraft$processItemForJumpRng(entity.getMainHandStack(), entity);
				splatcraft$processItemForJumpRng(entity.getOffHandStack(), entity);
			}
			
			SplatcraftCommonHandler.onPlayerJump(entity);
			SquidFormHandler.modifyJumpSpeed(entity);
		}
		@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
		public void splatcraft$writeSplatcraftData(NbtCompound nbt, CallbackInfo ci)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			NbtCompound splatcraftData = new NbtCompound();
			
			if (InkOverlayCapability.hasCapability(entity))
			{
				NbtCompound data = new NbtCompound();
				InkOverlayCapability.serialize(entity, data);
				splatcraftData.put("ink_overlay", data);
			}
			
			if (EntityInfoCapability.hasCapability(entity))
			{
				NbtCompound data = new NbtCompound();
				EntityInfoCapability.serialize(entity, data);
				splatcraftData.put("entity_info", data);
			}
			nbt.put("splatcraft_data", splatcraftData);
		}
		@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
		public void splatcraft$readSplatcraftData(NbtCompound nbt, CallbackInfo ci)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			if (nbt.contains("splatcraft_data"))
			{
				NbtCompound splatcraftData = nbt.getCompound("splatcraft_data");
				if (splatcraftData.contains("ink_overlay"))
				{
					InkOverlayCapability.deserialize(entity, splatcraftData.getCompound("ink_overlay"));
				}
				if (splatcraftData.contains("entity_info"))
				{
					EntityInfoCapability.deserialize(entity, splatcraftData.getCompound("entity_info"));
				}
			}
			else if (nbt.contains("ForgeCaps"))
			{
				NbtCompound forgeCaps = nbt.getCompound("ForgeCaps");
				if (forgeCaps.contains("splatcraft:ink_overlay"))
				{
					InkOverlayCapability.deserialize(entity, forgeCaps.getCompound("splatcraft:ink_overlay"));
				}
			}
		}
		@Inject(method = "damage", at = @At("HEAD"))
		public void splatcraft$failsafeDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir)
		{
			SquidFormHandler.onLivingHurt((LivingEntity) (Object) this, source, cir);
		}
		@Inject(method = "handleFallDamage", at = @At("HEAD"))
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
		@ModifyReturnValue(method = "getAttackDistanceScalingFactor", at = @At("RETURN"))
		public double splatcraft$modifyVisibility(double original)
		{
			SquidFormHandler.modifyVisibility((LivingEntity) (Object) this, original);
			return original;
		}
		@WrapOperation(method = "fall", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;spawnParticles(Lnet/minecraft/particle/ParticleEffect;DDDIDDDD)I"))
		public int addLandingEffects(ServerWorld instance, ParticleEffect j, double v, double clientboundlevelparticlespacket, double i, int particle, double x, double y, double z, double count, Operation<Integer> original)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			BlockPos pos = entity.getLandingPos();
			
			if (InkBlockUtils.isInked(entity.getWorld(), pos, Direction.UP))
			{
				ColorUtils.addInkSplashParticle(entity.getWorld(), InkBlockUtils.getInkBlock(entity.getWorld(), pos).color(Direction.UP.getId()), entity.getX(), entity.getBodyY(entity.getWorld().getRandom().nextFloat() * 0.3f), entity.getZ(), (float) (Math.sqrt(i) * 0.3f));
				return 0;
			}
			return original.call(instance, j, v, clientboundlevelparticlespacket, i, particle, x, y, z, count);
		}
		@WrapOperation(method = "playBlockFallSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V"))
		public void splatcraft$getFallSound(LivingEntity instance, SoundEvent soundEvent, float volume, float pitch, Operation<Void> original)
		{
			LivingEntity entity = (LivingEntity) (Object) this;
			if (InkBlockUtils.isInked(entity.getWorld(), entity.getLandingPos(), Direction.UP))
			{
				original.call(instance, SplatcraftSounds.SOUND_TYPE_INK.getFallSound(), volume, pitch);
				return;
			}
			original.call(instance, soundEvent, volume, pitch);
		}
	}
	@Mixin(EntityRenderer.class)
	public static class EntityRendererMixin
	{
		@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderer;renderLabelIfPresent(Lnet/minecraft/entity/Entity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V"))
		public void splatcraft$colorLabel(EntityRenderer instance, Entity entity, Text text, MatrixStack matrixStack, VertexConsumerProvider consumerProvider, int light, float tickDelta, Operation<Void> original)
		{
			original.call(instance, entity, RendererHandler.modifyNameplate(entity, text), matrixStack, consumerProvider, light, tickDelta);
		}
	}
}