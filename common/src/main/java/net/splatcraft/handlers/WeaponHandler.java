package net.splatcraft.handlers;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.splatcraft.VectorUtils;
import net.splatcraft.client.particles.SquidSoulParticleData;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.util.*;

import java.util.LinkedHashMap;
import java.util.Map;

public class WeaponHandler
{
    private static final Map<PlayerEntity, OldPosData> prevPosMap = new LinkedHashMap<>();

    public static void registerEvents()
    {
        EntityEvent.LIVING_DEATH.register((entity, dmgSource) -> {
            if (!entity.isSpectator() && entity instanceof PlayerEntity target)
            {
                InkColor color = ColorUtils.getEntityColor(target);
                ((ServerWorld) target.getWorld()).spawnParticles(new SquidSoulParticleData(color), target.getX(), target.getY() + 0.5f, target.getZ(), 1, 0, 0, 0, 1.5f);

                if (ScoreboardHandler.hasColorCriterion(color))
                {
                    target.getScoreboard().forEachScore(ScoreboardHandler.getDeathsAsColor(color), target, score -> score.incrementScore(1));
                }
                if (dmgSource.getSource() instanceof PlayerEntity source)
                {
                    if (ScoreboardHandler.hasColorCriterion(color))
                        target.getScoreboard().forEachScore(ScoreboardHandler.getColorKills(color), target, score -> score.incrementScore(1));
                    if (ScoreboardHandler.hasColorCriterion(ColorUtils.getEntityColor(source)))
                        target.getScoreboard().forEachScore(ScoreboardHandler.getKillsAsColor(ColorUtils.getEntityColor(source)), target, score -> score.incrementScore(1));
                }
            }
            return EventResult.pass();
        });

        TickEvent.PLAYER_PRE.register((player) -> {
            boolean hasCooldown = PlayerCooldown.hasPlayerCooldown(player);
            PlayerCooldown cooldown = PlayerCooldown.getPlayerCooldown(player);
            if (hasCooldown && cooldown.getSlotIndex() >= 0)
            {
                player.getInventory().selectedSlot = cooldown.getSlotIndex();
            }

            boolean notPreventedByCooldown = true;

            if (hasCooldown)
            {
                if (cooldown.cancellable && EntityInfoCapability.isSquid(player))
                {
                    ItemStack stack = cooldown.storedStack;

                    if (stack.getItem() instanceof WeaponBaseItem<?> weapon)
                        weapon.onPlayerCooldownEnd(player.getWorld(), player, stack, cooldown);
                    PlayerCooldown.setPlayerCooldown(player, null);
                }
                else
                {
                    if (cooldown.getTime() == cooldown.getMaxTime())
                        cooldown.onStart(player);
                    cooldown.tick(player);
                    player.setSprinting(false);

                    notPreventedByCooldown = !cooldown.preventWeaponUse();
                    ItemStack stack = cooldown.storedStack;

                    if (cooldown.getTime() <= 1)
                    {
                        if (stack.getItem() instanceof WeaponBaseItem<?> weapon)
                            weapon.onPlayerCooldownEnd(player.getWorld(), player, stack, cooldown);
                        cooldown.onEnd(player);
                        PlayerCooldown.setPlayerCooldown(player, null);
                        hasCooldown = false;
                    }
                    else if (cooldown.getTime() > 1 && stack.getItem() instanceof WeaponBaseItem<?> weapon)
                    {
                        weapon.onPlayerCooldownTick(player.getWorld(), player, stack, cooldown);
                    }
                    cooldown.setTime(cooldown.getTime() - 1);
                }
            }
            if (notPreventedByCooldown && player.getItemUseTimeLeft() > 0 && !CommonUtils.anyWeaponOnCooldown(player))
            {
                ItemStack stack = player.getStackInHand(player.getActiveHand());
                if (stack.getItem() instanceof WeaponBaseItem<?> weapon)
                {
                    weapon.weaponUseTick(player.getWorld(), player, stack, player.getItemUseTimeLeft());
                    player.setSprinting(false);
                }
            }
            else
            {
                PlayerCharge.dischargeWeapon(player);
            }
            Vec3d oldOldPos = player.getPos();
            if (prevPosMap.containsKey(player))
            {
                oldOldPos = prevPosMap.get(player).oldPosition;
            }
            OldPosData posData = new OldPosData(player.getPos(), oldOldPos);
            prevPosMap.put(player, posData);
        });

        TickEvent.PLAYER_POST.register((player) -> {
            if (EntityInfoCapability.hasCapability(player))
            {
                EntityInfo playerInfo = EntityInfoCapability.get(player);
                playerInfo.removeSquidCancelFlag();
            }
        });
    }

    public static OldPosData getPlayerPrevPos(PlayerEntity player)
    {
        return prevPosMap.containsKey(player) ? prevPosMap.get(player) : new OldPosData(player.getPos(), player.getLerpedPos(0));
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
            return VectorUtils.lerp(partialTick, oldOldPosition, oldPosition);
        }
    }
}