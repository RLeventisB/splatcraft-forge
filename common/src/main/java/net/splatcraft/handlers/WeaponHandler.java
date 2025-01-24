package net.splatcraft.handlers;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.splatcraft.client.particles.SquidSoulParticleData;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.WeaponBaseItem;
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
		EntityEvent.LIVING_DEATH.register((entity, dmgSource) ->
		{
			Optional<EntityInfo> info = EntityInfoCapability.getOptional(entity);
			if (!entity.getWorld().isClient() && !entity.isSpectator() && entity instanceof LivingEntity target && (info.isEmpty() || !info.get().isPlaying() || info.get().getMatchRespawnTimeLeft() == 0))
			{
				InkColor color = ColorUtils.getEntityColor(target);
				((ServerWorld) target.getWorld()).spawnParticles(new SquidSoulParticleData(color), target.getX(), target.getY() + 0.5f, target.getZ(), 1, 0, 0, 0, 1.5f);
				
				doScoreboardLogicOnDeath(dmgSource, target, color);
				
				if (info.isPresent() && info.get().isPlaying())
				{
					return EventResult.interruptFalse();
				}
				
				prevPosMap.remove(entity);
				return EventResult.pass();
			}
			
			return EventResult.pass();
		});
		
		TickEvent.PLAYER_POST.register((player) ->
		{
			Optional<EntityAction> cooldown = EntityAction.getEntityActionOptional(player);
			boolean usagePreventedByCooldown = false;
			
			if (cooldown.isPresent())
			{
				if (cooldown.get().getSlotIndex() >= 0)
					player.getInventory().selectedSlot = cooldown.get().getSlotIndex();
				
				usagePreventedByCooldown = tickEntityActions(player, cooldown.get());
			}
			if (usagePreventedByCooldown || !player.isUsingItem() || player.getItemUseTimeLeft() <= 0 || CommonUtils.anyWeaponOnCooldown(player))
			{
				PlayerCharge.dischargeWeapon(player);
			}
		});
		
		TickEvent.SERVER_LEVEL_PRE.register((level) -> level.getEntityLookup().forEach(TypeFilter.instanceOf(LivingEntity.class), entity ->
		{
			tickPreviousPosMap(entity);
			return LazyIterationConsumer.NextIteration.CONTINUE;
		}));
		TickEvent.SERVER_LEVEL_POST.register((level) -> level.getEntityLookup().forEach(TypeFilter.instanceOf(LivingEntity.class), entity ->
		{
			if (EntityInfoCapability.hasCapability(entity))
			{
				EntityInfo playerInfo = EntityInfoCapability.get(entity);
				playerInfo.reduceSquidAnimationTick();
			}
			return LazyIterationConsumer.NextIteration.CONTINUE;
		}));
	}
	private static void doScoreboardLogicOnDeath(DamageSource dmgSource, LivingEntity target, InkColor color)
	{
		Scoreboard scoreboard = target.getWorld().getScoreboard();
		if (ScoreboardHandler.hasColorCriterion(color))
		{
			scoreboard.forEachScore(ScoreboardHandler.getDeathsAsColor(color), target, score -> score.incrementScore(1));
		}
		if (dmgSource.getSource() instanceof LivingEntity source)
		{
			if (ScoreboardHandler.hasColorCriterion(color))
				scoreboard.forEachScore(ScoreboardHandler.getColorKills(color), target, score -> score.incrementScore(1));
			if (ScoreboardHandler.hasColorCriterion(ColorUtils.getEntityColor(source)))
				scoreboard.forEachScore(ScoreboardHandler.getKillsAsColor(ColorUtils.getEntityColor(source)), target, score -> score.incrementScore(1));
		}
	}
	private static boolean tickEntityActions(PlayerEntity player, EntityAction action)
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
				weapon.onPlayerCooldownTick(player.getWorld(), player, stack, action);
			}
			action.setTime(action.getTime() - 1);
		}
		return preventedByCooldown;
	}
	private static boolean doEndActions(PlayerEntity player, EntityAction action, ItemStack stack)
	{
		if (stack.getItem() instanceof WeaponBaseItem<?> weapon)
			weapon.onPlayerCooldownEnd(player.getWorld(), player, stack, action);
		if (action.canEnd(player))
		{
			EntityAction.setEntityAction(player, null);
			return true;
		}
		return false;
	}
	public static void tickPreviousPosMap(LivingEntity entity)
	{
		Vec3d oldOldPos = entity.getLerpedPos(0);
		Vec2f oldOldRot = new Vec2f(entity.prevPitch, entity.prevYaw);
		OldEntityTransformData oldData = prevPosMap.get(entity);
		if (oldData != null)
		{
			oldOldPos = oldData.oldPosition;
			oldOldRot = oldData.oldRot;
		}
		OldEntityTransformData posData = new OldEntityTransformData(entity.getLerpedPos(0), oldOldPos, new Vec2f(entity.prevPitch, entity.prevYaw), oldOldRot);
		prevPosMap.put(entity, posData);
	}
	public static OldEntityTransformData getEntityPrevPos(LivingEntity entity)
	{
		return prevPosMap.containsKey(entity) ? prevPosMap.get(entity) : new OldEntityTransformData(
			entity.getPos(), entity.getLerpedPos(0),
			new Vec2f(entity.getPitch(), entity.getYaw()), new Vec2f(entity.prevPitch, entity.prevYaw));
	}
	public static class OldEntityTransformData
	{
		public Vec3d oldPosition, oldOldPosition;
		public Vec2f oldRot, oldOldRot;
		public OldEntityTransformData(Vec3d oldPosition, Vec3d oldOldPosition, Vec2f oldRot, Vec2f oldOldRot)
		{
			this.oldPosition = oldPosition;
			this.oldOldPosition = oldOldPosition;
			this.oldRot = oldRot;
			this.oldOldRot = oldOldRot;
		}
		public static Vec2f getRot(LivingEntity entity)
		{
			return new Vec2f(entity.getPitch(), entity.getYaw());
		}
		public Vec3d getOldLerpedPosition(double partialTick)
		{
			return oldOldPosition.lerp(oldPosition, partialTick);
		}
	}
}