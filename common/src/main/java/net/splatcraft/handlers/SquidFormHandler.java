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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.blocks.SpawnPadBlock;
import net.splatcraft.data.capabilities.structs.InkOverlayData;
import net.splatcraft.data.capabilities.structs.SquidInfo;
import net.splatcraft.data.capabilities.structs.WeaponInfo;
import net.splatcraft.items.weapons.IChargeableWeapon;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.mixin.accessors.LivingEntityAccesor;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.network.s2c.UpdateInkOverlayPacket;
import net.splatcraft.platform.Components;
import net.splatcraft.platform.Services;
import net.splatcraft.platform.event.EventResult;
import net.splatcraft.platform.event.InteractionEvents;
import net.splatcraft.platform.event.PlayerEvents;
import net.splatcraft.platform.event.TickEvents;
import net.splatcraft.registries.*;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.EntityStoredCharge;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SquidFormHandler
{
	public static Map<LivingEntity, EnemyInkData> inkTouchData = new HashMap<>();
	public static void registerEvents()
	{
		Services.PLATFORM.registerListener(PlayerEvents.AttackEntity.class, SquidFormHandler::interruptIfSquid);
		Services.PLATFORM.registerListener(InteractionEvents.ClientLeftClickAir.class, SquidFormHandler::interruptIfSquid);
		Services.PLATFORM.registerListener(InteractionEvents.ClientRightClickAir.class, SquidFormHandler::interruptIfSquid);
		Services.PLATFORM.registerListener(InteractionEvents.LeftClickBlock.class, SquidFormHandler::interruptIfSquid);
		Services.PLATFORM.registerListener(InteractionEvents.RightClickBlock.class, SquidFormHandler::interruptIfSquid);
		Services.PLATFORM.registerListener(InteractionEvents.RightClickItem.class, SquidFormHandler::interruptIfSquid);
		Services.PLATFORM.registerListener(InteractionEvents.InteractEntity.class, SquidFormHandler::interruptIfSquid);
		Services.PLATFORM.registerListener(TickEvents.PlayerAfter.class, SquidFormHandler::playerTick);
	}
	public static void onLivingHurt(LivingEntity entity, DamageSource source, CallbackInfoReturnable<Boolean> cir)
	{
		if (source.is(SplatcraftDamageTypes.ENEMY_INK) && entity.getHealth() <= 4)
			cir.cancel();
	}
	public static void playerTick(Player player)
	{
		LivingEntityAccesor accesor = (LivingEntityAccesor) player;
		boolean alternativeInkHealth = SplatcraftGameRules.getLocalizedRule(player, SplatcraftGameRules.ALTERNATIVE_INK_HEALTH);
		if (InkBlockUtils.onEnemyInk(player))
		{
			doEnemyInkDamage(player, alternativeInkHealth);
		}
		else
		{
			if (alternativeInkHealth && !player.level().isClientSide())
				inkTouchData.remove(player);
		}

		if (player.tickCount % 10 == 0 &&
		    player.isUnderWater() &&
		    SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.WATER_DAMAGE) &&
		    !MobEffectUtil.hasWaterBreathing(player))
			player.hurt(SplatcraftDamageTypes.of(player.level(), SplatcraftDamageTypes.WATER), 8f);

		@NotNull SquidInfo info = Components.SQUID_INFO.getOrCreate(player);
		tickSquidState(player, info);

		boolean canSquidHide = InkBlockUtils.canSquidHide(player);
		if (alternativeInkHealth && !player.level().isClientSide())
		{
			doAlternativeHealing(player, accesor, canSquidHide, info);
		}

		if (info.isSquid())
		{
			if (!player.getAbilities().flying)
			{
				// this is for easier movement when underwater without sprinting, dont know what the walkDist is for tho
				player.setSprinting(player.isUnderWater());
				player.walkDist = player.walkDistO;
			}

			player.setPose(Pose.SWIMMING);
			player.releaseUsingItem();

			player.awardStat(SplatcraftStats.SQUID_TIME);

			if (canSquidHide)
			{
				if (!alternativeInkHealth)
				{
					if (player.getHealth() < player.getMaxHealth() &&
					    SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.INK_HEALING) &&
					    player.tickCount % 5 == 0 &&
					    !hasDamageOvertime(player))
					{
						player.heal(0.5f);
						if (SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.INK_HEALING_CONSUMES_HUNGER))
							player.causeFoodExhaustion(0.25f);

						Components.INK_OVERLAY.getOrCreate(player).addAmount(-0.49f);
					}
				}

				boolean crouch = player.isShiftKeyDown();
				if (!crouch &&
				    player.level().getRandom().nextFloat() <= 0.6f &&
				    (Math.abs(player.getX() - player.xo) > 0.14 ||
				     Math.abs(player.getY() - player.yo) > 0.07 ||
				     Math.abs(player.getZ() - player.zo) > 0.14))
				{
					ColorUtils.addInkSplashParticle(player.level(), player, 1.1f, true);
				}
			}

			// do surge particles when charging
			if (!info.isDoingSquidSurge() &&
			    player.level().getRandom().nextFloat() <= info.squidSurgeState() / SquidInfo.MAX_SQUID_SURGE_CHARGE)
			{
				ColorUtils.addInkSplashParticle(player.level(), player, 0.9f, true);
			}

			doActionsWithBlockBelow(player);
		}

		// remove ink overlay passively
		if (Components.INK_OVERLAY.has(player) && !alternativeInkHealth)
		{
			Components.INK_OVERLAY.get(player).addAmount(-0.01f);
		}
	}
	private static void doActionsWithBlockBelow(Player player)
	{
		Optional<BlockPos> posBelowOptional = InkBlockUtils.getBlockBelowPos(player);
		if (posBelowOptional.isEmpty())
			return;

		BlockPos posBelow = posBelowOptional.get();
		Block blockBelow = player.level().getBlockState(posBelow).getBlock();

		switch (blockBelow)
		{
			case
				SpawnPadBlock ignored when SplatcraftGameRules.getLocalizedRule(player.level(), posBelow, SplatcraftGameRules.UNIVERSAL_INK):
				ColorUtils.setPlayerColor(player, ColorUtils.getEffectiveColor(player.level(), posBelow));
				break;

			case InkwellBlock ignored:
				ColorUtils.setPlayerColor(player, ColorUtils.getEffectiveColor(player.level(), posBelow));
				break;

			case SpawnPadBlock.Aux aux:
				BlockPos newPos = aux.getParentPos(player.level().getBlockState(posBelow), posBelow);
				if (player.level().getBlockState(newPos).getBlock() instanceof SpawnPadBlock)
				{
					setSpawnPadSpawnpoint(player, newPos);
				}
				break;
			case SpawnPadBlock ignored:
				setSpawnPadSpawnpoint(player, posBelow);

			default:
				break;
		}
	}
	private static void setSpawnPadSpawnpoint(Player player, BlockPos posBelow)
	{
		InkColorTileEntity spawnPad = (InkColorTileEntity) player.level().getBlockEntity(posBelow);

		if (player instanceof ServerPlayer serverPlayer && ColorUtils.colorEquals(player, spawnPad))
		{
			serverPlayer.setRespawnPosition(player.level().dimension(), posBelow, player.level().getBlockState(posBelow).getValue(SpawnPadBlock.DIRECTION).toYRot(), false, true);
		}
	}
	private static void doEnemyInkDamage(Player player, boolean alternativeInkHealth)
	{
		if (player.level().getDifficulty() != Difficulty.PEACEFUL)
		{
			if (alternativeInkHealth)
			{
				if (!player.level().isClientSide())
				{
					doAlternativeEnemyInkDamage(player);
				}
			}
			else
			{
				if (player.tickCount % 20 == 0 && player.getHealth() > 4)
					player.hurt(SplatcraftDamageTypes.of(player.level(), SplatcraftDamageTypes.ENEMY_INK), Math.min(player.getHealth() - 4, 2f));
			}
		}
		if (player.level().getRandom().nextFloat() < 0.5f)
		{
			ColorUtils.addStandingInkSplashParticle(player.level(), player, 1);
		}
	}
	private static void doAlternativeHealing(Player player, LivingEntityAccesor accesor, boolean canSquidHide, @NotNull SquidInfo info)
	{
		if (player.level().getGameTime() - accesor.getLastDamageStamp() > 20 && !inkTouchData.containsKey(player))
		{
			if (player.getHealth() < player.getMaxHealth() &&
			    SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.INK_HEALING) &&
			    !hasDamageOvertime(player))
			{
				player.heal(canSquidHide && info.isSquid() ? 1f : 0.125f);
				if (SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.INK_HEALING_CONSUMES_HUNGER))
					player.causeFoodExhaustion(0.05f);

				InkOverlayData overlayInfo = Components.INK_OVERLAY.getOrCreate(player);
				overlayInfo.setAmount((player.getMaxHealth() - player.getHealth()));

				SplatcraftPacketHandler.sendToTrackersAndSelf(new UpdateInkOverlayPacket(player, overlayInfo), player);
			}
		}
	}
	private static void doAlternativeEnemyInkDamage(LivingEntity entity)
	{
		long gameTime = entity.level().getGameTime();
		EnemyInkData enemyInkData = inkTouchData.computeIfAbsent(entity, (no) -> new EnemyInkData(gameTime, 0f));

		long touchTimestamp = enemyInkData.damageTimestamp();
		float totalDamageDone = enemyInkData.totalDamage();
		float maxDamage = (float) entity.getAttributeValue(SplatcraftAttributes.maxEnemyInkDamage);

		if (gameTime - touchTimestamp >= entity.getAttributeValue(SplatcraftAttributes.enemyInkResistanceTime))
		{
			if (totalDamageDone < maxDamage)
			{
				float health = entity.getHealth();
				float damage = 0.18f;

				if (health - damage <= 1)
					damage = health - 1;

				if (damage <= 0) // failsafe for "negative" damage because sometimes this happens and idk why
					WeaponHandler.accumulateAltEnemyInkDamage(entity, InkColor.INVALID, 0, true);
				else
					WeaponHandler.accumulateAltEnemyInkDamageWithLimit(entity, InkColor.INVALID, damage, totalDamageDone, maxDamage);

				inkTouchData.put(entity, enemyInkData.withTotalDamage(totalDamageDone + damage));
			}
			else
			{
				// add "phantom damage" so the player doesn't regenerate as soon as they leave the ink
				WeaponHandler.accumulateAltEnemyInkDamage(entity, InkColor.INVALID, 0, true);
			}
		}
	}
	private static boolean hasDamageOvertime(LivingEntity entity)
	{
		return entity.hasEffect(MobEffects.POISON) || entity.hasEffect(MobEffects.WITHER);
	}
	private static void tickSquidState(LivingEntity entity, SquidInfo info)
	{
		SquidState state = info.squidState(); // this is more readable with enums though :(

		if (InkBlockUtils.canSquidHide(entity) && info.isSquid())
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
			entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.inkSubmerge, SoundSource.PLAYERS, 0.5F, ((entity.level().getRandom().nextFloat() - entity.level().getRandom().nextFloat()) * 0.2F + 1.0F) * 0.95F);

			if (entity.level() instanceof ServerLevel)
			{
				for (int i = 0; i < 2; i++)
					ColorUtils.addInkSplashParticle(entity.level(), entity, 1.4f, true);
			}
		}
		else if (state == SquidState.SURFACING)
		{
			entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.inkSurface, SoundSource.PLAYERS, 0.5F, ((entity.level().getRandom().nextFloat() - entity.level().getRandom().nextFloat()) * 0.2F + 1.0F) * 0.95F);
		}

		SquidState finalState = state;
		Components.SQUID_INFO.update(entity, v -> v.withSquidState(finalState));
	}
	public static void cancelDamageIfSquid(LivingEntity entity, float fallDistance, CallbackInfoReturnable<Boolean> cir)
	{
		if (!CommonUtils.isSquid(entity))
		{
			return;
		}
		if (!InkBlockUtils.canSquidHide(entity))
		{
			return;
		}

		if (entity instanceof ServerPlayer player)
			SplatcraftStats.FALL_INTO_INK_TRIGGER.value().trigger(player, fallDistance);
		cir.setReturnValue(false);
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
		Components.SQUID_INFO.updateOrCreate(player, info -> info.setSquid(false));
		SplatcraftPacketHandler.sendToTrackersAndSelf(new PlayerSetSquidS2CPacket(player.getUUID(), false), player);
	}
	private static @NotNull EventResult interruptIfSquid(Player player, Object... params)
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
			InkOverlayData info = Components.INK_OVERLAY.getOrCreate(living);
			Vec3 prev = WeaponHandler.getEntityPrevPos(living).oldOldPosition;

			info.setSquidPitch((float) (Math.abs(living.getY() - prev.y) * living.position().subtract(prev).normalize().y));
			
			Components.INK_OVERLAY.set(living, info);
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
		if (!Components.SQUID_INFO.hasChangedAfterUpdateOrCreate(entity, info -> info.setSquid(newSquid)))
			return;

		if (!newSquid)
			Components.WEAPON_INFO.updateOrCreate(entity, WeaponInfo::flagSquidCancel);

		if (newSquid)
		{
			boolean didStoreCharge = false;
			for (InteractionHand hand : InteractionHand.values())
			{
				ItemStack stack = entity.getItemInHand(hand);
				if (!(stack.getItem() instanceof IChargeableWeapon chargeable))
					continue;

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
		else
		{
			if (EntityStoredCharge.hasCharge(entity))
			{
				for (InteractionHand hand : InteractionHand.values())
				{
					ItemStack stack = entity.getItemInHand(hand);
					if (!EntityStoredCharge.chargeMatches(entity, stack))
						continue;

					EntityStoredCharge.retrieveCharge(entity, stack);
					entity.startUsingItem(hand);
					break;
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
	public record EnemyInkData(long damageTimestamp, float totalDamage)
	{
		public EnemyInkData withDamageTimestamp(long damageTimestamp)
		{
			return new EnemyInkData(damageTimestamp, totalDamage);
		}
		public EnemyInkData withTotalDamage(float totalDamage)
		{
			return new EnemyInkData(damageTimestamp, totalDamage);
		}
	}
}