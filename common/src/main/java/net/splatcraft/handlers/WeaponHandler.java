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
import net.minecraft.util.math.Vec3d;
import net.splatcraft.client.particles.SquidSoulParticleData;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.util.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class WeaponHandler
{
	private static final Map<LivingEntity, OldPosData> prevPosMap = new LinkedHashMap<>();
	public static void registerEvents()
	{
		EntityEvent.LIVING_DEATH.register((entity, dmgSource) ->
		{
			Optional<EntityInfo> info = EntityInfoCapability.getOptional(entity);
			if (!entity.isSpectator() && entity instanceof LivingEntity target && (info.isEmpty() || !info.get().isPlaying() || info.get().getMatchRespawnTimeLeft() == 0))
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
			Optional<PlayerCooldown> cooldown = PlayerCooldown.getPlayerCooldownOptional(player);
			boolean usagePreventedByCooldown = false;
			
			if (cooldown.isPresent())
			{
				if (cooldown.get().getSlotIndex() >= 0)
					player.getInventory().selectedSlot = cooldown.get().getSlotIndex();
				
				usagePreventedByCooldown = tickCooldownActions(player, cooldown.get());
			}
			if (usagePreventedByCooldown || !player.isUsingItem() || player.getItemUseTimeLeft() <= 0 || CommonUtils.anyWeaponOnCooldown(player))
			{
				PlayerCharge.dischargeWeapon(player);
			}
		});
		
		TickEvent.SERVER_LEVEL_POST.register((level) -> level.getEntityLookup().forEach(TypeFilter.instanceOf(LivingEntity.class), entity ->
		{
			tickPreviousPosMap(entity);
			
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
	private static boolean tickCooldownActions(PlayerEntity player, PlayerCooldown cooldown)
	{
		boolean preventedByCooldown = false;
		if (cooldown.cancellable && EntityInfoCapability.isSquid(player))
		{
			ItemStack stack = cooldown.storedStack;
			
			doEndActions(player, cooldown, stack);
		}
		else
		{
			if (cooldown.getTime() == cooldown.getMaxTime())
				cooldown.onStart(player);
			cooldown.tick(player);
			player.setSprinting(false);
			
			preventedByCooldown = cooldown.preventWeaponUse();
			ItemStack stack = cooldown.storedStack;
			
			if (cooldown.getTime() <= 1)
			{
				doEndActions(player, cooldown, stack);
			}
			else if (cooldown.getTime() > 1 && stack.getItem() instanceof WeaponBaseItem<?> weapon)
			{
				weapon.onPlayerCooldownTick(player.getWorld(), player, stack, cooldown);
			}
			cooldown.setTime(cooldown.getTime() - 1);
		}
		return preventedByCooldown;
	}
	private static void doEndActions(PlayerEntity player, PlayerCooldown cooldown, ItemStack stack)
	{
		if (stack.getItem() instanceof WeaponBaseItem<?> weapon)
			weapon.onPlayerCooldownEnd(player.getWorld(), player, stack, cooldown);
		if (cooldown.canEnd(player))
		{
			PlayerCooldown.setPlayerCooldown(player, null);
		}
	}
	public static void tickPreviousPosMap(LivingEntity entity)
	{
		Vec3d oldOldPos = entity.getLerpedPos(0);
		if (prevPosMap.containsKey(entity))
		{
			oldOldPos = prevPosMap.get(entity).oldPosition;
		}
		OldPosData posData = new OldPosData(entity.getLerpedPos(0), oldOldPos);
		prevPosMap.put(entity, posData);
	}
	public static OldPosData getPlayerPrevPos(LivingEntity entity)
	{
		return prevPosMap.containsKey(entity) ? prevPosMap.get(entity) : new OldPosData(entity.getPos(), entity.getLerpedPos(0));
	}
	public static class OldPosData
	{
		public Vec3d oldPosition, oldOldPosition;
		public OldPosData(Vec3d oldPosition, Vec3d oldOldPosition)
		{
			this.oldPosition = oldPosition;
			this.oldOldPosition = oldOldPosition;
		}
		public Vec3d getPosition(double partialTick)
		{
			return oldOldPosition.lerp(oldPosition, partialTick);
		}
	}
}