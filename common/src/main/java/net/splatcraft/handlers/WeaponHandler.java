package net.splatcraft.handlers;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.platform.Services;
import net.splatcraft.platform.event.EntityEvents;
import net.splatcraft.platform.event.EventResult;
import net.splatcraft.platform.event.TickEvents;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.EntityStoredCharge;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.structs.InkColor;

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
	public static void registerEvents()
	{
		Services.PLATFORM.registerListener(EntityEvents.LivingDeath.class, (entity, dmgSource) ->
		{
			prevPosMap.remove(entity);
			weaponUseTime.remove(entity);
			lastGroundedPos.remove(entity);
			return EventResult.pass();
		});
		
		Services.PLATFORM.registerListener(TickEvents.PlayerAfter.class, (player) ->
		{
			Optional<EntityAction> cooldown = EntityAction.getEntityActionOptional(player);
			boolean usagePreventedByCooldown = false;
			
			if (cooldown.isPresent())
			{
				if (cooldown.get().getItemSlot() instanceof EntitySlot.PlayerInventorySlot playerSlot)
					player.getInventory().selected = playerSlot.getSlotIndex();
				
				usagePreventedByCooldown = tickEntityActions(player, cooldown.get());
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
		Services.PLATFORM.registerListener(TickEvents.ServerLevelAfter.class, (level) -> level.getEntities().get(EntityTypeTest.forClass(LivingEntity.class), entity ->
		{
			if (EntityInfoCapability.hasCapability(entity))
			{
				EntityInfo entityInfo = EntityInfoCapability.get(entity);
				entityInfo.reduceSquidAnimationTick();
			}
			if (entity.onGround() && EntityInfoCapability.getOptional(entity).map(v -> !v.isMatchRespawning()).orElse(true))
				lastGroundedPos.put(entity, entity.position());
			
			return AbortableIterationConsumer.Continuation.CONTINUE;
		}));
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
	private static boolean tickEntityActions(Player player, EntityAction action)
	{
		boolean preventedByCooldown;
		if (action.getTime() == action.getMaxTime())
			action.onStart(player);
		if (action.isCancellable() && EntityInfoCapability.isSquid(player))
		{
			if (action.endWhenOnSquid(player))
				doEndActions(player, action);
		}
		else
		{
			action.tick(player);
			player.setSprinting(false);
		}
		preventedByCooldown = action.preventWeaponUse();
		
		if (action.getTime() <= 1)
		{
			if (doEndActions(player, action))
				return false;
		}
		action.setTime(action.getTime() - 1);
		
		return preventedByCooldown;
	}
	private static boolean doEndActions(Player player, EntityAction action)
	{
		if (action.canEnd(player))
		{
			EntityAction.setEntityAction(player, null);
			return true;
		}
		return false;
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
		return living.isUsingItem() && !EntityAction.hasActionAnd(living, EntityAction::preventWeaponUse) && !EntityInfoCapability.isSquid(living);
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
}