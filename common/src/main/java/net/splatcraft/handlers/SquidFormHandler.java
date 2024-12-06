package net.splatcraft.handlers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerWorld;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.blocks.SpawnPadBlock;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.registries.SplatcraftStats;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkBlockUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class SquidFormHandler
{
    private static final Map<Player, SquidState> squidSubmergeMode = new LinkedHashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getSource().is(SplatcraftDamageTypes.ENEMY_INK) && event.getEntity().getHealth() <= 4)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event)
    {
        Player player = event.player;

        if (InkBlockUtils.onEnemyInk(player))
        {
            if (player.tickCount % 20 == 0 && player.getHealth() > 4 && player.getWorld().getDifficulty() != Difficulty.PEACEFUL)
                player.hurt(player.damageSources().source(SplatcraftDamageTypes.ENEMY_INK), Math.min(player.getHealth() - 4, 2f));
            if (player.getWorld().getRandom().nextFloat() < 0.5f)
            {
                ColorUtils.addStandingInkSplashParticle(player.getWorld(), player, 1);
            }
        }

        if (SplatcraftGameRules.getLocalizedRule(player.getWorld(), player.blockPosition(), SplatcraftGameRules.WATER_DAMAGE) && player.isInWater() && player.tickCount % 10 == 0 && !MobEffectUtil.hasWaterBreathing(player))
            player.hurt(player.damageSources().source(SplatcraftDamageTypes.WATER), 8f);

        if (!PlayerInfoCapability.hasCapability(player))
            return;

        PlayerInfo info = PlayerInfoCapability.get(player);
        if (event.phase == TickEvent.Phase.START)
        {
            SquidState state = SquidState.SURFACED; // this is more readable with enums though :(

            if (squidSubmergeMode.containsKey(player))
                state = squidSubmergeMode.get(player);

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
                player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.inkSubmerge, SoundSource.PLAYERS, 0.5F, ((player.getWorld().getRandom().nextFloat() - player.getWorld().getRandom().nextFloat()) * 0.2F + 1.0F) * 0.95F);

                if (player.getWorld() instanceof ServerWorld serverLevel)
                {
                    for (int i = 0; i < 2; i++)
                        ColorUtils.addInkSplashParticle(serverLevel, player, 1.4f);
                }
            }
            else if (state == SquidState.SURFACING)
            {
                player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.inkSurface, SoundSource.PLAYERS, 0.5F, ((player.getWorld().getRandom().nextFloat() - player.getWorld().getRandom().nextFloat()) * 0.2F + 1.0F) * 0.95F);
            }

            squidSubmergeMode.put(player, state);
        }

        if (info.isSquid())
        {
            if (!player.getAbilities().flying)
            {
                player.setSprinting(player.isInWater());
                player.walkDist = player.walkDistO;
            }

            player.setPose(Pose.SWIMMING);
            player.stopUsingItem();

            player.awardStat(SplatcraftStats.SQUID_TIME);

            if (InkBlockUtils.canSquidHide(player))
            {
                if (player.getHealth() < player.getMaxHealth() && SplatcraftGameRules.getLocalizedRule(player.getWorld(), player.blockPosition(), SplatcraftGameRules.INK_HEALING) && player.tickCount % 5 == 0 && !player.hasEffect(MobEffects.POISON) && !player.hasEffect(MobEffects.WITHER))
                {
                    player.heal(0.5f);
                    if (SplatcraftGameRules.getLocalizedRule(player.getWorld(), player.blockPosition(), SplatcraftGameRules.INK_HEALING_CONSUMES_HUNGER))
                        player.causeFoodExhaustion(0.25f);
                    if (InkOverlayCapability.hasCapability(player))
                    {
                        InkOverlayCapability.get(player).addAmount(-0.49f);
                    }
                }

                boolean crouch = player.isCrouching();
                if (!crouch && player.getWorld().getRandom().nextFloat() <= 0.6f && (Math.abs(player.getX() - player.xo) > 0.14 || Math.abs(player.getY() - player.yo) > 0.07 || Math.abs(player.getZ() - player.zo) > 0.14))
                {
                    ColorUtils.addInkSplashParticle(player.getWorld(), player, 1.1f);
                }
            }
            if (info.getSquidSurgeCharge() > 0 && player.getWorld().getRandom().nextFloat() <= info.getSquidSurgeCharge() / 30)
            {
                ColorUtils.addInkSplashParticle(player.getWorld(), player, 0.9f);
            }

            BlockPos posBelow = InkBlockUtils.getBlockStandingOnPos(player);
            Block blockBelow = player.getWorld().getBlockState(posBelow).getBlock();

            if (blockBelow instanceof SpawnPadBlock.Aux aux)
            {
                BlockPos newPos = aux.getParentPos(player.getWorld().getBlockState(posBelow), posBelow);
                if (player.getWorld().getBlockState(newPos).getBlock() instanceof SpawnPadBlock)
                {
                    posBelow = newPos;
                    blockBelow = player.getWorld().getBlockState(newPos).getBlock();
                }
            }

            if (blockBelow instanceof InkwellBlock || (SplatcraftGameRules.getLocalizedRule(player.getWorld(), posBelow, SplatcraftGameRules.UNIVERSAL_INK) && blockBelow instanceof SpawnPadBlock))
            {
                ColorUtils.setPlayerColor(player, ColorUtils.getInkColorOrInverted(player.getWorld(), posBelow));
            }

            if (blockBelow instanceof SpawnPadBlock)
            {
                InkColorTileEntity spawnPad = (InkColorTileEntity) player.getWorld().getBlockEntity(posBelow);

                if (player instanceof ServerPlayer serverPlayer && ColorUtils.colorEquals(player, spawnPad))
                {
                    serverPlayer.setRespawnPosition(player.getWorld().dimension(), posBelow, player.getWorld().getBlockState(posBelow).getValue(SpawnPadBlock.DIRECTION).toYRot(), false, true);
                }
            }
        }
        if (InkOverlayCapability.hasCapability(player))
        {
            InkOverlayCapability.get(player).addAmount(-0.01f);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player && PlayerInfoCapability.get(player).isSquid())
        {
            if (InkBlockUtils.canSquidHide(player))
            {
                SplatcraftStats.FALL_INTO_INK_TRIGGER.trigger(player, event.getDistance());
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void playerVisibility(LivingEvent.LivingVisibilityEvent event)
    {
        if (!(event.getEntity() instanceof Player player))
        {
            return;
        }

        if (PlayerInfoCapability.hasCapability(player) && PlayerInfoCapability.get(player).isSquid() && InkBlockUtils.canSquidHide(player))
        {
            event.modifyVisibility(Math.abs(player.getX() - player.xo) > 0.14 || Math.abs(player.getY() - player.yo) > 0.07 || Math.abs(player.getZ() - player.zo) > 0.14 ? 0.7 : 0);
        }
    }

    @SubscribeEvent
    public static void onGameModeSwitch(PlayerEvent.PlayerChangeGameModeEvent event)
    {
        if (event.getNewGameMode() != GameType.SPECTATOR) return;
        event.getEntity().stopUsingItem();
        PlayerInfoCapability.get(event.getEntity()).setIsSquid(false);
        SplatcraftPacketHandler.sendToTrackersAndSelf(new PlayerSetSquidS2CPacket(event.getEntity().getUUID(), false), event.getEntity());
    }

    @SubscribeEvent
    public static void playerBreakSpeed(PlayerEvent.BreakSpeed event)
    {
        if (PlayerInfoCapability.isSquid(event.getEntity()))
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event)
    {
        if (PlayerInfoCapability.isSquid(event.getEntity()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent event)
    {
        if (PlayerInfoCapability.isSquid(event.getEntity()) && event.isCancelable())
        {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientLivingTick(LivingEvent.LivingTickEvent event)
    {
        if (!event.getEntity().getWorld().isClientSide())
            return;
        LivingEntity living = event.getEntity();
        if (InkOverlayCapability.hasCapability(living))
        {
            InkOverlayInfo info = InkOverlayCapability.get(living);
            Vec3d prev = living.getPosition(0);

            info.setSquidRot(Math.abs(living.getY() - prev.y()) * living.position().subtract(prev).normalize().y);
        }
    }

    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event)
    {
        if (!(event.getEntity() instanceof Player player) || !PlayerInfoCapability.hasCapability(event.getEntity()))
        {
            return;
        }

        if (PlayerInfoCapability.get(player).isSquid() && InkBlockUtils.canSquidSwim(player))
        {
            player.setDeltaMovement(player.getDeltaMovement().x(), player.getDeltaMovement().y() * 1.1, player.getDeltaMovement().z());
        }
    }

    public enum SquidState
    {
        SUBMERGED(0),
        SUBMERGING(1),
        SURFACING(2),
        SURFACED(3);
        public final byte state;

        SquidState(int state)
        {
            this.state = (byte) state;
        }
    }
}