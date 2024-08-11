package net.splatcraft.forge.handlers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.splatcraft.forge.blocks.InkwellBlock;
import net.splatcraft.forge.blocks.SpawnPadBlock;
import net.splatcraft.forge.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.forge.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.forge.registries.SplatcraftGameRules;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.registries.SplatcraftStats;
import net.splatcraft.forge.tileentities.InkColorTileEntity;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.InkDamageUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber
public class SquidFormHandler
{
	private static final Map<Player, List<BlockPos>> collidedBlockMap = new LinkedHashMap<>();
	private static final Map<Player, Integer> squidSubmergeMode = new LinkedHashMap<>();
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingHurt(LivingHurtEvent event)
	{
		if (event.getSource().is(InkDamageUtils.ENEMY_INK) && event.getEntity().getHealth() <= 4)
			event.setCanceled(true);
	}
	@SubscribeEvent
	public static void playerTick(TickEvent.PlayerTickEvent event)
	{
		Player player = event.player;

		if (InkBlockUtils.onEnemyInk(player))
		{
			if (player.tickCount % 20 == 0 && player.getHealth() > 4 && player.level().getDifficulty() != Difficulty.PEACEFUL)
				player.hurt(player.damageSources().source(InkDamageUtils.ENEMY_INK), Math.min(-4 + player.getHealth(), 2f));
			if (player.level().getRandom().nextFloat() < 0.5f) {
				ColorUtils.addStandingInkSplashParticle(player.level(), player, 1);
            }
		}

		if (SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.WATER_DAMAGE) && player.isInWater() && player.tickCount % 10 == 0 && !MobEffectUtil.hasWaterBreathing(player))
			player.hurt(player.damageSources().source(InkDamageUtils.WATER), 8f);

		if (!PlayerInfoCapability.hasCapability(player))
			return;

		PlayerInfo info = PlayerInfoCapability.get(player);
		if (event.phase == TickEvent.Phase.START)
		{
			//if(!shouldBeInvisible(player))
			//    player.setInvisible(shouldBeInvisible(player));

			if (!squidSubmergeMode.containsKey(player))
				squidSubmergeMode.put(player, -2);

			if (InkBlockUtils.canSquidHide(player) && info.isSquid())
			{
				squidSubmergeMode.put(player, Math.min(2, Math.max(squidSubmergeMode.get(player) + 1, 1)));
				//if(!player.isInvisible())
				//    player.setInvisible(true);
			}
			else
				squidSubmergeMode.put(player, Math.max(-2, Math.min(squidSubmergeMode.get(player) - 1, -1)));

			if (squidSubmergeMode.get(player) == 1)
			{
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.inkSubmerge, SoundSource.PLAYERS, 0.5F, ((player.level().getRandom().nextFloat() - player.level().getRandom().nextFloat()) * 0.2F + 1.0F) * 0.95F);


				if (player.level() instanceof ServerLevel serverLevel)
				{
					for (int i = 0; i < 2; i++)
						ColorUtils.addInkSplashParticle(serverLevel, player, 1.4f);
				}
			}
			else if (squidSubmergeMode.get(player) == -1) {
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.inkSurface, SoundSource.PLAYERS, 0.5F, ((player.level().getRandom().nextFloat() - player.level().getRandom().nextFloat()) * 0.2F + 1.0F) * 0.95F);
            }
		}

		if (PlayerInfoCapability.isSquid(player))
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
				if (player.getHealth() < player.getMaxHealth() && SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.INK_HEALING) && player.tickCount % 5 == 0 && !player.hasEffect(MobEffects.POISON) && !player.hasEffect(MobEffects.WITHER))
				{
					player.heal(0.5f);
                    if (SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.INK_HEALING_CONSUMES_HUNGER))
						player.causeFoodExhaustion(0.25f);
					if (InkOverlayCapability.hasCapability(player))
					{
						InkOverlayCapability.get(player).addAmount(-0.49f);
					}
				}

				boolean crouch = player.isCrouching();
				if (!crouch && player.level().getRandom().nextFloat() <= 0.6f && (Math.abs(player.getX() - player.xo) > 0.14 || Math.abs(player.getY() - player.yo) > 0.07 || Math.abs(player.getZ() - player.zo) > 0.14))
				{
					ColorUtils.addInkSplashParticle(player.level(), player, 1.1f);
				}
			}

			BlockPos posBelow = InkBlockUtils.getBlockStandingOnPos(player);
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

			if (blockBelow instanceof InkwellBlock || (SplatcraftGameRules.getLocalizedRule(player.level(), posBelow, SplatcraftGameRules.UNIVERSAL_INK) && blockBelow instanceof SpawnPadBlock)) {
				ColorUtils.setPlayerColor(player, ColorUtils.getInkColorOrInverted(player.level(), posBelow));
            }

			if (blockBelow instanceof SpawnPadBlock)
			{
				InkColorTileEntity spawnPad = (InkColorTileEntity) player.level().getBlockEntity(posBelow);

				if (player instanceof ServerPlayer serverPlayer && ColorUtils.colorEquals(player, spawnPad)){
					serverPlayer.setRespawnPosition(player.level().dimension(), posBelow, player.level().getBlockState(posBelow).getValue(SpawnPadBlock.DIRECTION).toYRot(), false, true);}
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
	public static void onEntitySize(EntityEvent.EntityConstructing event)
	{
		if (!event.getEntity().isAddedToWorld() || !(event.getEntity() instanceof Player player) || !PlayerInfoCapability.hasCapability(player))
			return;

		PlayerInfo info = PlayerInfoCapability.get(player);

		if (info.isSquid())
		{
//			player.setNewSize(new EntityDimensions(0.6f, 0.5f, false));
//			event.setNewEyeHeight(InkBlockUtils.canSquidHide(player) ? 0.3f : 0.4f);
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
		if (!event.getEntity().level().isClientSide())
			return;
		LivingEntity living = event.getEntity();
		if (InkOverlayCapability.hasCapability(living))
		{
			InkOverlayInfo info = InkOverlayCapability.get(living);
			Vec3 prev = living.getPosition(0);

			info.setSquidRot(Math.abs(living.getY() - prev.y()) * new Vec3((living.getX() - prev.x), (living.getY() - prev.y), (living.getZ() - prev.z)).normalize().y);
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
	public static List<BlockPos> getPlayerCollidedBlocks(Player player)
	{
		return collidedBlockMap.get(player);
	}
}