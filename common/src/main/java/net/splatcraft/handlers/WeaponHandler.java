package net.splatcraft.handlers;

import net.minecraft.server.level.ServerWorld;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3d;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.splatcraft.Splatcraft;
import net.splatcraft.VectorUtils;
import net.splatcraft.client.particles.SquidSoulParticleData;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.PlayerCharge;
import net.splatcraft.util.PlayerCooldown;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Splatcraft.MODID)
public class WeaponHandler
{
    private static final Map<Player, OldPosData> prevPosMap = new LinkedHashMap<>();

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        if (event.getEntity() instanceof Player target && !event.getEntity().isSpectator())
        {
            int color = ColorUtils.getPlayerColor(target);
            ((ServerWorld) target.level()).sendParticles(new SquidSoulParticleData(color), target.getX(), target.getY() + 0.5f, target.getZ(), 1, 0, 0, 0, 1.5f);

            if (ScoreboardHandler.hasColorCriterion(color))
            {
                target.getScoreboard().forAllObjectives(ScoreboardHandler.getDeathsAsColor(color), target.getScoreboardName(), score -> score.add(1));
            }
            if (event.getSource().getDirectEntity() instanceof Player source)
            {
                if (ScoreboardHandler.hasColorCriterion(color))
                    target.getScoreboard().forAllObjectives(ScoreboardHandler.getColorKills(color), source.getScoreboardName(), score -> score.add(1));
                if (ScoreboardHandler.hasColorCriterion(ColorUtils.getPlayerColor(source)))
                    target.getScoreboard().forAllObjectives(ScoreboardHandler.getKillsAsColor(ColorUtils.getPlayerColor(source)), source.getScoreboardName(), score -> score.add(1));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        Player player = event.player;
        boolean hasCooldown = PlayerCooldown.hasPlayerCooldown(player);
        PlayerCooldown cooldown = PlayerCooldown.getPlayerCooldown(player);
        if (hasCooldown && cooldown.getSlotIndex() >= 0)
        {
            player.getInventory().selected = cooldown.getSlotIndex();
        }

        if (event.phase != TickEvent.Phase.START)
        {
            if (PlayerInfoCapability.hasCapability(player))
            {
                PlayerInfo playerInfo = PlayerInfoCapability.get(player);
                playerInfo.removeSquidCancelFlag();
            }
            return;
        }

        boolean notPreventedByCooldown = true;

        if (hasCooldown)
        {
            if (cooldown.cancellable && PlayerInfoCapability.isSquid(player))
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
        if (notPreventedByCooldown && player.getUseItemRemainingTicks() > 0 && !CommonUtils.anyWeaponOnCooldown(player))
        {
            ItemStack stack = player.getItemInHand(player.getUsedItemHand());
            if (stack.getItem() instanceof WeaponBaseItem<?> weapon)
            {
                weapon.weaponUseTick(player.getWorld(), player, stack, player.getUseItemRemainingTicks());
                player.setSprinting(false);
            }
        }
        else
        {
            PlayerCharge.dischargeWeapon(player);
        }
        Vec3d oldOldPos = player.position();
        if (prevPosMap.containsKey(player))
        {
            oldOldPos = prevPosMap.get(player).oldPosition;
        }
        OldPosData posData = new OldPosData(player.position(), oldOldPos);
        prevPosMap.put(player, posData);
    }

    public static OldPosData getPlayerPrevPos(Player player)
    {
        return prevPosMap.containsKey(player) ? prevPosMap.get(player) : new OldPosData(player.position(), player.position());
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