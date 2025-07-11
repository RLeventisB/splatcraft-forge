package net.splatcraft.handlers;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.blocks.SpawnPadBlock;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.items.weapons.IChargeableWeapon;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.platform.Components;
import net.splatcraft.platform.Services;
import net.splatcraft.platform.event.EventResult;
import net.splatcraft.platform.event.InteractionEvents;
import net.splatcraft.platform.event.PlayerEvents;
import net.splatcraft.platform.event.TickEvents;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.registries.SplatcraftStats;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.EntityStoredCharge;
import net.splatcraft.util.InkBlockUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

public class SquidFormHandler
{
	public static void registerEvents()
	{
		Services.PLATFORM.registerListener(PlayerEvents.AttackEntity.class, SquidFormHandler::onPlayerAttackEntity);
		Services.PLATFORM.registerListener(InteractionEvents.ClientLeftClickAir.class, SquidFormHandler::onPlayerInteract);
		Services.PLATFORM.registerListener(InteractionEvents.ClientRightClickAir.class, SquidFormHandler::onPlayerInteract);
		Services.PLATFORM.registerListener(InteractionEvents.LeftClickBlock.class, SquidFormHandler::onPlayerInteract);
		Services.PLATFORM.registerListener(InteractionEvents.RightClickBlock.class, SquidFormHandler::onPlayerInteract);
		Services.PLATFORM.registerListener(InteractionEvents.RightClickItem.class, SquidFormHandler::onPlayerInteractItem);
		Services.PLATFORM.registerListener(InteractionEvents.InteractEntity.class, SquidFormHandler::onPlayerInteract);
		Services.PLATFORM.registerListener(TickEvents.PlayerAfter.class, SquidFormHandler::playerTick);
	}
	public static void onLivingHurt(LivingEntity entity, DamageSource source, CallbackInfoReturnable<Boolean> cir)
	{
		if (source.is(SplatcraftDamageTypes.ENEMY_INK) && entity.getHealth() <= 4)
			cir.cancel();
	}
	public static void playerTick(Player player)
	{
		if (InkBlockUtils.onEnemyInk(player))
		{
			if (player.tickCount % 20 == 0 && player.getHealth() > 4 && player.level().getDifficulty() != Difficulty.PEACEFUL)
				player.hurt(SplatcraftDamageTypes.of(player.level(), SplatcraftDamageTypes.ENEMY_INK), Math.min(player.getHealth() - 4, 2f));
			if (player.level().getRandom().nextFloat() < 0.5f)
			{
				ColorUtils.addStandingInkSplashParticle(player.level(), player, 1);
			}
		}
		
		if (SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.WATER_DAMAGE) && player.isUnderWater() && player.tickCount % 10 == 0 && !MobEffectUtil.hasWaterBreathing(player))
			player.hurt(SplatcraftDamageTypes.of(player.level(), SplatcraftDamageTypes.WATER), 8f);
		
		EntityInfo info = Components.ENTITY_INFO.getOrCreate(player);
		tickSquidState(player, info);
		
		if (info.isSquid())
		{
			if (!player.getAbilities().flying)
			{
				player.setSprinting(player.isUnderWater());
				player.walkDist = player.walkDistO;
			}
			
			player.setPose(Pose.SWIMMING);
			player.releaseUsingItem();
			
			player.awardStat(SplatcraftStats.SQUID_TIME);
			
			if (InkBlockUtils.canSquidHide(player))
			{
				if (player.getHealth() < player.getMaxHealth() && SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.INK_HEALING) && player.tickCount % 5 == 0 && !player.hasEffect(MobEffects.POISON) && !player.hasEffect(MobEffects.WITHER))
				{
					player.heal(0.5f);
					if (SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.INK_HEALING_CONSUMES_HUNGER))
						player.causeFoodExhaustion(0.25f);
					
					Components.INK_OVERLAY.getOrCreate(player).addAmount(-0.49f);
				}
				
				boolean crouch = player.isShiftKeyDown();
				if (!crouch && player.level().getRandom().nextFloat() <= 0.6f && (Math.abs(player.getX() - player.xo) > 0.14 || Math.abs(player.getY() - player.yo) > 0.07 || Math.abs(player.getZ() - player.zo) > 0.14))
				{
					ColorUtils.addInkSplashParticle(player.level(), player, 1.1f);
				}
			}
			if (!info.isDoingSquidSurge() && player.level().getRandom().nextFloat() <= info.getSquidSurgeState() / EntityInfo.MAX_SQUID_SURGE_CHARGE)
			{
				ColorUtils.addInkSplashParticle(player.level(), player, 0.9f);
			}
			
			Optional<BlockPos> posBelowOptional = InkBlockUtils.getBlockStandingOnPos(player);
			posBelowOptional.ifPresent(posBelow ->
			{
				Block blockBelow = player.level().getBlockState(posBelow).getBlock();
				if (blockBelow instanceof SpawnPadBlock.Aux aux)
				{
					BlockPos newPos = aux.getParentPos(player.level().getBlockState(posBelow), posBelow);
					if (player.level().getBlockState(newPos).getBlock() instanceof SpawnPadBlock)
					{
						posBelow = newPos;
						blockBelow = player.level().getBlockState(newPos).getBlock();
					}
				}
				
				if (blockBelow instanceof InkwellBlock || (SplatcraftGameRules.getLocalizedRule(player.level(), posBelow, SplatcraftGameRules.UNIVERSAL_INK) && blockBelow instanceof SpawnPadBlock))
				{
					ColorUtils.setPlayerColor(player, ColorUtils.getEffectiveColor(player.level(), posBelow));
				}
				
				if (blockBelow instanceof SpawnPadBlock)
				{
					InkColorTileEntity spawnPad = (InkColorTileEntity) player.level().getBlockEntity(posBelow);
					
					if (player instanceof ServerPlayer serverPlayer && ColorUtils.colorEquals(player, spawnPad))
					{
						serverPlayer.setRespawnPosition(player.level().dimension(), posBelow, player.level().getBlockState(posBelow).getValue(SpawnPadBlock.DIRECTION).toYRot(), false, true);
					}
				}
			});
		}
		if (Components.INK_OVERLAY.has(player))
		{
			Components.INK_OVERLAY.getOrCreate(player).addAmount(-0.01f);
		}
	}
	private static void tickSquidState(Player player, EntityInfo info)
	{
		SquidState state = info.getSquidState(); // this is more readable with enums though :(
		
		if (InkBlockUtils.canSquidHide(player) && info.isSquid())
		{
			if (state == SquidState.SUBMERGING)
				state = SquidState.SUBMERGED;
			else if (state != SquidState.SUBMERGED)
				state = SquidState.SUBMERGING;
		}
		else
		{
			if (state == SquidState.SURFACING)
				state = SquidState.SURFACED;
			else if (state != SquidState.SURFACED)
				state = SquidState.SURFACING;
		}
		
		if (state == SquidState.SUBMERGING)
		{
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.inkSubmerge, SoundSource.PLAYERS, 0.5F, ((player.level().getRandom().nextFloat() - player.level().getRandom().nextFloat()) * 0.2F + 1.0F) * 0.95F);
			
			if (player.level() instanceof ServerLevel serverLevel)
			{
				for (int i = 0; i < 2; i++)
					ColorUtils.addInkSplashParticle(serverLevel, player, 1.4f);
			}
		}
		else if (state == SquidState.SURFACING)
		{
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.inkSurface, SoundSource.PLAYERS, 0.5F, ((player.level().getRandom().nextFloat() - player.level().getRandom().nextFloat()) * 0.2F + 1.0F) * 0.95F);
		}
		
		info.setSquidState(state);
	}
	public static void cancelDamageIfSquid(LivingEntity entity, float fallDistance, CallbackInfoReturnable<Boolean> cir)
	{
		if (CommonUtils.isSquid(entity))
		{
			if (InkBlockUtils.canSquidHide(entity))
			{
				if (entity instanceof ServerPlayer player)
					SplatcraftStats.FALL_INTO_INK_TRIGGER.value().trigger(player, fallDistance);
				cir.setReturnValue(false);
			}
		}
	}
	public static double modifyVisibility(LivingEntity entity, double original)
	{
		if (CommonUtils.isSquid(entity) && InkBlockUtils.canSquidHide(entity))
		{
			return (Math.abs(entity.getX() - entity.xo) > 0.14 || Math.abs(entity.getY() - entity.yo) > 0.07 || Math.abs(entity.getZ() - entity.zo) > 0.14 ? 0.7 : 0);
		}
		return original;
	}
	public static void onGameModeSwitch(Player player, GameType newGameMode)
	{
		if (newGameMode != GameType.SPECTATOR) return;
		player.releaseUsingItem();
		Components.ENTITY_INFO.getOrCreate(player).setIsSquid(false);
		SplatcraftPacketHandler.sendToTrackersAndSelf(new PlayerSetSquidS2CPacket(player.getUUID(), false), player);
	}
	public static EventResult onPlayerAttackEntity(Player player, Level level, Entity target, InteractionHand hand, @Nullable EntityHitResult result)
	{
		if (CommonUtils.isSquid(player))
			return EventResult.interruptFalse();
		return EventResult.pass();
	}
	public static EventResult onPlayerInteract(Player player, Object... params)
	{
		if (CommonUtils.isSquid(player))
			return EventResult.interruptFalse();
		return EventResult.pass();
	}
	public static EventResult onPlayerInteractItem(Player player, Object... params)
	{
		if (CommonUtils.isSquid(player))
			return EventResult.interruptFalse();
		return EventResult.pass();
	}
	public static void doSquidRotation(Entity entity)
	{
		if (!entity.level().isClientSide() || !(entity instanceof LivingEntity living))
			return;
		
		if (Components.INK_OVERLAY.has(living))
		{
			InkOverlayInfo info = Components.INK_OVERLAY.getOrCreate(living);
			Vec3 prev = WeaponHandler.getEntityPrevPos(living).oldOldPosition;
			
			info.setSquidPitch((float) (Math.abs(living.getY() - prev.y) * living.position().subtract(prev).normalize().y));
		}
	}
	public static void modifyJumpSpeed(LivingEntity entity)
	{
		if (CommonUtils.isSquid(entity) && InkBlockUtils.canSquidSwim(entity))
		{
			entity.setDeltaMovement(entity.getDeltaMovement().x, entity.getDeltaMovement().y * 1.1, entity.getDeltaMovement().z);
		}
	}
	public static void setSquid(LivingEntity entity, boolean newSquid)
	{
		setSquid(entity, Components.ENTITY_INFO.getOrCreate(entity), newSquid);
	}
	public static void setSquid(LivingEntity entity, EntityInfo info, boolean newSquid)
	{
		if (info.isSquid() == newSquid)
			return;
		
		info.setIsSquid(newSquid);
		if (!newSquid)
			info.flagSquidCancel();
		
		if (newSquid)
		{
			boolean didStoreCharge = false;
			for (InteractionHand hand : InteractionHand.values())
			{
				ItemStack stack = entity.getItemInHand(hand);
				if (stack.getItem() instanceof IChargeableWeapon chargeable)
				{
					if (!didStoreCharge && chargeable.canStore(stack))
					{
						EntityStoredCharge.storeCharge(entity, stack);
						didStoreCharge = true;
					}
					
					chargeable.setCharge(stack, 0f);
					WeaponBaseItem<?> weaponItem = (WeaponBaseItem<?>) stack.getItem();
					weaponItem.getResetShootingAction(stack, entity).ifPresent(SpecialHandler.ResetAction::run);
				}
			}
		}
		else
		{
			if (EntityStoredCharge.hasCharge(entity))
			{
				for (InteractionHand hand : InteractionHand.values())
				{
					ItemStack stack = entity.getItemInHand(hand);
					if (EntityStoredCharge.chargeMatches(entity, stack))
					{
						EntityStoredCharge.retrieveCharge(entity, stack);
						entity.startUsingItem(hand);
						break;
					}
				}
			}
		}
	}
	public enum SquidState implements StringRepresentable
	{
		SUBMERGED(0),
		SUBMERGING(1),
		SURFACING(2),
		SURFACED(3);
		public static final Codec<SquidState> CODEC = StringRepresentable.fromEnum(SquidState::values);
		public final byte state;
		SquidState(int state)
		{
			this.state = (byte) state;
		}
		@Override
		public @NotNull String getSerializedName()
		{
			return name();
		}
	}
}