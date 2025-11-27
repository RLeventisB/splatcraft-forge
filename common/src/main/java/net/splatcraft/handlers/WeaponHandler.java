package net.splatcraft.handlers;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.scores.Scoreboard;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.data.capabilities.structs.InkOverlayData;
import net.splatcraft.data.capabilities.structs.WeaponInfo;
import net.splatcraft.entities.IColoredEntity;
import net.splatcraft.entities.SquidBumperEntity;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.mixin.accessors.LivingEntityAccesor;
import net.splatcraft.mixin.accessors.ServerPlayerAccesor;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendEnemyInkDamagePacket;
import net.splatcraft.network.s2c.UpdateEntityActionOnlyPacket;
import net.splatcraft.network.s2c.UpdateInkOverlayPacket;
import net.splatcraft.platform.Components;
import net.splatcraft.platform.Services;
import net.splatcraft.platform.event.EntityEvents;
import net.splatcraft.platform.event.EventResult;
import net.splatcraft.platform.event.TickEvents;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.EntityStoredCharge;
import net.splatcraft.util.InkDamageUtils;
import net.splatcraft.util.action.ActionEndResult;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.structs.InkColor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class WeaponHandler
{
	private static final Map<LivingEntity, OldEntityTransformData> prevPosMap = new LinkedHashMap<>();
	private static final Map<LivingEntity, Vec3> lastGroundedPos = new LinkedHashMap<>();
	private static final Map<LivingEntity, Short> weaponUseTime = new LinkedHashMap<>();
	private static final Map<LivingEntity, Short> movedQuicklyDisable = new LinkedHashMap<>();
	public static Map<Entity, AccumulatedDamageData> accumulatedDamageData = new HashMap<>();
	public static void registerEvents()
	{
		Services.PLATFORM.registerListener(EntityEvents.LivingDeath.class, WeaponHandler::onDeath);
		
		Services.PLATFORM.registerListener(TickEvents.PlayerAfter.class, (player) ->
		{
			Optional<EntityAction> cooldown = EntityAction.getEntityActionOptional(player);
			
			boolean usagePreventedByCooldown = false;
			if (cooldown.isPresent())
			{
				if (cooldown.get().getItemSlot() instanceof EntitySlot.PlayerInventorySlot playerSlot)
					player.getInventory().selected = playerSlot.getSlotIndex();
				
				usagePreventedByCooldown = cooldown.get().preventWeaponUse();
				
				EntityAction previousAction = EntityAction.getEntityAction(player);
				if (previousAction == null)
					return;
				
				ActionEndResult endResult = tickEntityActions(player, previousAction);
				if (endResult.doSet())
					EntityAction.setEntityAction(player, endResult.resultingAction().orElse(null), true, endResult.resultingAction().isEmpty());
				if (endResult.doSync() && !player.level().isClientSide())
					SplatcraftPacketHandler.sendToTrackersAndSelf(UpdateEntityActionOnlyPacket.create(player), player);
			}
			
			if (usagePreventedByCooldown || !player.isUsingItem() || player.getUseItemRemainingTicks() <= 0 || CommonUtils.anyWeaponOnCooldown(player))
			{
				EntityStoredCharge.dischargeWeapon(player);
			}
		});
		
		Services.PLATFORM.registerListener(TickEvents.ServerLevelBefore.class, (level) -> level.getEntities().get(EntityTypeTest.forClass(LivingEntity.class), entity ->
		{
			tickPreviousPosMap(entity);
			return AbortableIterationConsumer.Continuation.CONTINUE;
		}));
		Services.PLATFORM.registerListener(TickEvents.ServerLevelAfter.class, (level) ->
		{
			level.getEntities().getAll().forEach(entity ->
			{
				boolean alternativeInkHealth = SplatcraftGameRules.getLocalizedRule(entity, SplatcraftGameRules.ALTERNATIVE_INK_HEALTH);
				if (alternativeInkHealth)
				{
					processAlternativeDamage(entity);
				}
			});
			level.getEntities().get(EntityTypeTest.forClass(LivingEntity.class), entity ->
			{
				if (Components.WEAPON_INFO.has(entity))
				{
					Components.WEAPON_INFO.update(entity, WeaponInfo::reduceSquidAnimationTick);
				}
				if (!Components.ENTITY_INFO.has(entity))
				{
					return AbortableIterationConsumer.Continuation.CONTINUE;
				}
				
				boolean alternativeInkHealth = SplatcraftGameRules.getLocalizedRule(entity, SplatcraftGameRules.ALTERNATIVE_INK_HEALTH);
				if (alternativeInkHealth)
				{
					processAlternativeDamage(entity);
				}
				
				if (entity.onGround() && (!(entity instanceof Player player) || Components.PLAYER_INFO.hasAnd(player, info -> !info.isMatchRespawning())))
					lastGroundedPos.put(entity, entity.position());
				
				return AbortableIterationConsumer.Continuation.CONTINUE;
			});
		});
		if (Services.PLATFORM.isClientSide())
			registerClientEvents();
	}
	private static void registerClientEvents()
	{
		Services.PLATFORM.registerListener(TickEvents.ClientLevelAfter.class, (level) -> level.getEntities().get(EntityTypeTest.forClass(LivingEntity.class), entity ->
		{
			Optional<InteractionHand> hand = getUsingWeaponHand(entity);
			if (hand.isPresent())
			{
				short useTime = weaponUseTime.computeIfAbsent(entity, v -> (short) 0);
				useTime++;
				weaponUseTime.put(entity, useTime);
			}
			else
			{
				weaponUseTime.remove(entity);
			}
			Short time = movedQuicklyDisable.get(entity);
			if (time != null)
			{
				time--;
				if (time > 0)
					movedQuicklyDisable.put(entity, time);
				else
					movedQuicklyDisable.remove(entity);
			}
			return AbortableIterationConsumer.Continuation.CONTINUE;
		}));
	}
	public static void doScoreboardLogicOnDeath(DamageSource dmgSource, LivingEntity target, InkColor color)
	{
		Scoreboard scoreboard = target.level().getScoreboard();
		if (ScoreboardHandler.hasColorCriterion(color))
		{
			scoreboard.forAllObjectives(ScoreboardHandler.getDeathsAsColor(color), target, score -> score.add(1));
		}
		if (dmgSource.getDirectEntity() instanceof LivingEntity source)
		{
			if (ScoreboardHandler.hasColorCriterion(color))
				scoreboard.forAllObjectives(ScoreboardHandler.getColorKills(color), target, score -> score.add(1));
			if (ScoreboardHandler.hasColorCriterion(ColorUtils.getEntityColor(source)))
				scoreboard.forAllObjectives(ScoreboardHandler.getKillsAsColor(ColorUtils.getEntityColor(source)), target, score -> score.add(1));
		}
	}
	private static ActionEndResult tickEntityActions(Player player, EntityAction action)
	{
		if (action == null)
			return null;
		
		ActionEndResult endResult = null;
		
		if (action.getTime() == action.getMaxTime())
			action.onStart(player);
		if (action.isCancellable(player) && CommonUtils.isSquid(player))
		{
			endResult = action.canEnd(player, EntityAction.EndType.CANCELLED);
			if (!endResult.tickAfter())
				return endResult;
		}
		endResult = ActionEndResult.merge(action.tick(player), endResult);
		player.setSprinting(false);
		
		if (endResult.tickAfter())
		{
			if (action.reversedTime())
			{
				if (action.getTime() >= action.getMaxTime())
				{
					endResult = ActionEndResult.merge(action.canEnd(player, EntityAction.EndType.TIME), endResult);
					if (!endResult.tickAfter())
						return endResult;
				}
				action.setTime(action.getTime() + 1);
			}
			else
			{
				if (action.getTime() <= 1)
				{
					endResult = ActionEndResult.merge(action.canEnd(player, EntityAction.EndType.TIME), endResult);
					if (!endResult.tickAfter())
						return endResult;
				}
				action.setTime(action.getTime() - 1);
			}
		}
		
		return endResult;
	}
	public static void tickPreviousPosMap(LivingEntity entity)
	{
		Vec3 oldOldPos = entity.getPosition(0);
		Vec2 oldOldRot = new Vec2(entity.xRotO, entity.yRotO);
		OldEntityTransformData oldData = prevPosMap.get(entity);
		if (oldData != null)
		{
			oldOldPos = oldData.oldPosition;
			oldOldRot = oldData.oldRot;
		}
		OldEntityTransformData posData = new OldEntityTransformData(entity.getPosition(0), oldOldPos, new Vec2(entity.xRotO, entity.yRotO), oldOldRot);
		prevPosMap.put(entity, posData);
	}
	public static Optional<Vec3> getEntityLastGroundedPos(LivingEntity entity)
	{
		return Optional.ofNullable(lastGroundedPos.get(entity));
	}
	public static void disableMovedQuickly(LivingEntity entity, float ticks)
	{
		movedQuicklyDisable.put(entity, (short) ticks);
	}
	public static void disableMovedQuickly(LivingEntity entity, short ticks)
	{
		movedQuicklyDisable.put(entity, ticks);
	}
	public static void resetLastGroundedPos(LivingEntity entity)
	{
		lastGroundedPos.remove(entity);
	}
	public static OldEntityTransformData getEntityPrevPos(LivingEntity entity)
	{
		return prevPosMap.containsKey(entity) ? prevPosMap.get(entity) : new OldEntityTransformData(
			entity.position(), entity.getPosition(0),
			new Vec2(entity.getXRot(), entity.getYRot()), new Vec2(entity.xRotO, entity.yRotO));
	}
	public static boolean canContinueShooting(LivingEntity living)
	{
		return living.isUsingItem() && !EntityAction.hasEntityActionAnd(living, EntityAction::preventWeaponUse) && !CommonUtils.isSquid(living);
	}
	public static Optional<InteractionHand> getUsingWeaponHand(LivingEntity entity)
	{
		return getWeaponHand(entity, (x, y) -> y.preventsChanging(x, entity));
	}
	public static Optional<InteractionHand> getWeaponHand(LivingEntity entity, Predicate<ItemStack> predicate)
	{
		for (var hand : InteractionHand.values())
		{
			ItemStack itemStack = entity.getItemInHand(hand);
			if (itemStack.getItem() instanceof WeaponBaseItem<?> weapon && predicate.test(itemStack))
			{
				return Optional.of(hand);
			}
		}
		return Optional.empty();
	}
	public static Optional<InteractionHand> getWeaponHand(LivingEntity entity, BiPredicate<ItemStack, WeaponBaseItem<?>> predicate)
	{
		for (var hand : InteractionHand.values())
		{
			ItemStack itemStack = entity.getItemInHand(hand);
			if (itemStack.getItem() instanceof WeaponBaseItem<?> weapon && predicate.test(itemStack, weapon))
			{
				return Optional.of(hand);
			}
		}
		return Optional.empty();
	}
	public static short getWeaponUseTime(LivingEntity entity)
	{
		return weaponUseTime.getOrDefault(entity, (short) -1);
	}
	public static Component getWeaponNameComponent(ResourceLocation weaponId)
	{
		if (I18n.exists("weaponRecipe." + weaponId)) // exception for weapons that dont have a set name (for example, nautilus (79 or 47), aerospray (MG, RG, or PG))
		{
			return Component.translatable("weaponRecipe." + weaponId);
		}
		return Component.translatable(weaponId.toLanguageKey("item"));
	}
	public static EventResult onDeath(LivingEntity entity, DamageSource dmgSource)
	{
		prevPosMap.remove(entity);
		weaponUseTime.remove(entity);
		lastGroundedPos.remove(entity);
		movedQuicklyDisable.remove(entity);
		return EventResult.pass();
	}
	public static boolean hasMovedQuicklyDisabled(LivingEntity entity)
	{
		return movedQuicklyDisable.containsKey(entity);
	}
	public static void forceLastGroundedPos(LivingEntity entity)
	{
		if (Services.PLATFORM.getServerInstance() == null)
			return;
		
		Vec3 floorPos = entity.position().add(0, 0.1, 0);
		
		BlockHitResult result = entity.level().clip(new ClipContext(floorPos, floorPos.subtract(0, -1, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.of(entity)));
		if (result.getType() != HitResult.Type.MISS)
			floorPos = result.getLocation();
		lastGroundedPos.put(entity, floorPos);
	}
	public static void processAlternativeDamage(Entity entity)
	{
		long gameTime = entity.level().getGameTime();
		AccumulatedDamageData damageData = accumulatedDamageData.get(entity);
		
		if (damageData == null)
			return;
		
		float accumulatedDamage = damageData.accumulatedDamage();
		
		if (accumulatedDamage < 0.5 && !damageData.forceDamage)
		{
			return;
		}
		
		DamageSource source = SplatcraftDamageTypes.of(entity.level(), damageData.damageKey() == null ? SplatcraftDamageTypes.ENEMY_INK : damageData.damageKey());
		boolean damage = true;
		if (entity instanceof IColoredEntity coloredEntity)
		{
			damage = coloredEntity.onEntityInked(source, accumulatedDamage, damageData.damagingColor);
		}
		if (damage && entity instanceof LivingEntity livingEntity && !(entity instanceof SquidBumperEntity))
		{
			float newHealth = livingEntity.getHealth() - accumulatedDamage;
			if (!livingEntity.isDeadOrDying() && newHealth <= 0)
			{
				InkDamageUtils.InkDamageSource damageSource = new InkDamageUtils.InkDamageSource(SplatcraftDamageTypes.get(entity.level(), damageData.damageKey()), entity, null, ItemStack.EMPTY, true);
				
				livingEntity.die(damageSource);
			}
			livingEntity.setHealth(newHealth);
			
			LivingEntityAccesor accesor = (LivingEntityAccesor) livingEntity;
			
			accesor.setLastDamageSource(source);
			accesor.setLastDamageStamp(gameTime);
			
			if (entity instanceof ServerPlayer serverPlayer)
			{
				((ServerPlayerAccesor) serverPlayer).setLastSentHealth(newHealth);
				SplatcraftPacketHandler.sendToPlayer(new SendEnemyInkDamagePacket(newHealth), serverPlayer);
			}
			
			InkOverlayData info = Components.INK_OVERLAY.getOrCreate(livingEntity);
			info.addAmount(accumulatedDamage);
			
			SplatcraftPacketHandler.sendToTrackersAndSelf(new UpdateInkOverlayPacket(livingEntity, info), entity);
		}
		
		accumulatedDamageData.put(entity, new AccumulatedDamageData(0, false, null, null));
	}
	public static void accumulateAltEnemyInkDamage(LivingEntity entity, InkColor color, float damage, boolean forceProcessing)
	{
		accumulateAltEnemyInkDamage(entity, color, damage, forceProcessing, SplatcraftDamageTypes.ENEMY_INK);
	}
	public static void accumulateAltEnemyInkDamage(LivingEntity entity, InkColor color, float damage, boolean forceProcessing, ResourceKey<DamageType> damageType)
	{
		accumulatedDamageData.compute(entity, (ent, data) ->
		{
			if (data == null)
			{
				return new AccumulatedDamageData(damage, forceProcessing, damageType, color);
			}
			return data.withAccumulatedDamage(data.accumulatedDamage() + damage).withForceDamage(forceProcessing).withDamageKey(damageType).withDamagingColor(color);
		});
	}
	public static void accumulateAltEnemyInkDamageWithLimit(LivingEntity entity, InkColor color, float damage, float totalDamageDone, float maxDamage)
	{
		accumulateAltEnemyInkDamageWithLimit(entity, color, damage, totalDamageDone, maxDamage, SplatcraftDamageTypes.ENEMY_INK);
	}
	public static void accumulateAltEnemyInkDamageWithLimit(LivingEntity entity, InkColor color, float damage, float totalDamageDone, float maxDamage, ResourceKey<DamageType> damageType)
	{
		AccumulatedDamageData enemyInkData = accumulatedDamageData.getOrDefault(entity, new AccumulatedDamageData(0f, false, damageType, color));
		
		boolean forceDamage = enemyInkData.forceDamage();
		
		if (totalDamageDone + damage >= maxDamage)
		{
			damage = maxDamage - totalDamageDone;
			forceDamage = true;
		}
		
		accumulateAltEnemyInkDamage(entity, color, damage, forceDamage, damageType);
	}
	public static class OldEntityTransformData
	{
		public Vec3 oldPosition, oldOldPosition;
		public Vec2 oldRot, oldOldRot;
		public OldEntityTransformData(Vec3 oldPosition, Vec3 oldOldPosition, Vec2 oldRot, Vec2 oldOldRot)
		{
			this.oldPosition = oldPosition;
			this.oldOldPosition = oldOldPosition;
			this.oldRot = oldRot;
			this.oldOldRot = oldOldRot;
		}
		public static Vec2 getRot(LivingEntity entity)
		{
			return new Vec2(entity.getXRot(), entity.getYRot());
		}
		public Vec3 getOldLerpedPosition(double partialTick)
		{
			return oldOldPosition.lerp(oldPosition, partialTick);
		}
	}
	public record AccumulatedDamageData(float accumulatedDamage, boolean forceDamage, ResourceKey<DamageType> damageKey,
	                                    InkColor damagingColor)
	{
		public AccumulatedDamageData withDamagingColor(InkColor damagingColor)
		{
			return new AccumulatedDamageData(accumulatedDamage, forceDamage, damageKey, damagingColor);
		}
		public AccumulatedDamageData withAccumulatedDamage(float accumulatedDamage)
		{
			return new AccumulatedDamageData(accumulatedDamage, forceDamage, damageKey, damagingColor);
		}
		public AccumulatedDamageData withForceDamage(boolean forceDamage)
		{
			return new AccumulatedDamageData(accumulatedDamage, forceDamage, damageKey, damagingColor);
		}
		public AccumulatedDamageData withDamageKey(ResourceKey<DamageType> damageKey)
		{
			return new AccumulatedDamageData(accumulatedDamage, forceDamage, damageKey, damagingColor);
		}
	}
}