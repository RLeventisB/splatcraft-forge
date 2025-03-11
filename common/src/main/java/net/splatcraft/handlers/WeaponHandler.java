package net.splatcraft.handlers;

import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.platform.Services;
import net.splatcraft.platform.event.EntityEvents;
import net.splatcraft.platform.event.EventResult;
import net.splatcraft.platform.event.TickEvents;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;
import net.splatcraft.util.PlayerCharge;
import net.splatcraft.util.action.EntityAction;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class WeaponHandler
{
	private static final Map<LivingEntity, OldEntityTransformData> prevPosMap = new LinkedHashMap<>();
	public static void registerEvents()
	{
		Services.PLATFORM.registerListener(EntityEvents.LivingDeath.class, (entity, dmgSource) ->
		{
			prevPosMap.remove(entity);
			return EventResult.pass();
		});
		
		Services.PLATFORM.registerListener(TickEvents.PlayerAfter.class, (player) ->
		{
			Optional<EntityAction> cooldown = EntityAction.getEntityActionOptional(player);
			boolean usagePreventedByCooldown = false;
			
			if (cooldown.isPresent())
			{
				if (cooldown.get().getSlotIndex() >= 0)
					player.getInventory().selected = cooldown.get().getSlotIndex();
				
				usagePreventedByCooldown = tickEntityActions(player, cooldown.get());
			}
			if (usagePreventedByCooldown || !player.isUsingItem() || player.getUseItemRemainingTicks() <= 0 || CommonUtils.anyWeaponOnCooldown(player))
			{
				PlayerCharge.dischargeWeapon(player);
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
				EntityInfo playerInfo = EntityInfoCapability.get(entity);
				playerInfo.reduceSquidAnimationTick();
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
		boolean preventedByCooldown = false;
		if (action.isCancellable() && EntityInfoCapability.isSquid(player))
		{
			ItemStack stack = action.getStoredStack();
			
			doEndActions(player, action, stack);
		}
		else
		{
			if (action.getTime() == action.getMaxTime())
				action.onStart(player);
			action.tick(player);
			player.setSprinting(false);
			
			preventedByCooldown = action.preventWeaponUse();
			ItemStack stack = action.getStoredStack();
			
			if (action.getTime() <= 1)
			{
				if (doEndActions(player, action, stack))
					return false;
			}
			else if (action.getTime() > 1 && stack.getItem() instanceof WeaponBaseItem<?> weapon)
			{
				weapon.onPlayerCooldownTick(player.level(), player, stack, action);
			}
			action.setTime(action.getTime() - 1);
		}
		return preventedByCooldown;
	}
	private static boolean doEndActions(Player player, EntityAction action, ItemStack stack)
	{
		if (stack.getItem() instanceof WeaponBaseItem<?> weapon)
			weapon.onPlayerCooldownEnd(player.level(), player, stack, action);
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
	public static OldEntityTransformData getEntityPrevPos(LivingEntity entity)
	{
		return prevPosMap.containsKey(entity) ? prevPosMap.get(entity) : new OldEntityTransformData(
			entity.position(), entity.getPosition(0),
			new Vec2(entity.getXRot(), entity.getYRot()), new Vec2(entity.xRotO, entity.yRotO));
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