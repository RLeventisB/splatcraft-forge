package net.splatcraft.handlers;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.blocks.SpawnPadBlock;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.registries.SplatcraftStats;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkBlockUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.Map;

public class SquidFormHandler
{
	private static final Map<LivingEntity, SquidState> squidSubmergeMode = new LinkedHashMap<>();
	public static void registerEvents()
	{
		PlayerEvent.ATTACK_ENTITY.register(SquidFormHandler::onPlayerAttackEntity);
		InteractionEvent.CLIENT_LEFT_CLICK_AIR.register(SquidFormHandler::onPlayerInteract);
		InteractionEvent.CLIENT_RIGHT_CLICK_AIR.register(SquidFormHandler::onPlayerInteract);
		InteractionEvent.LEFT_CLICK_BLOCK.register(SquidFormHandler::onPlayerInteract);
		InteractionEvent.RIGHT_CLICK_BLOCK.register(SquidFormHandler::onPlayerInteract);
		InteractionEvent.RIGHT_CLICK_ITEM.register(SquidFormHandler::onPlayerInteractItem);
		InteractionEvent.INTERACT_ENTITY.register(SquidFormHandler::onPlayerInteract);
		TickEvent.PLAYER_POST.register(SquidFormHandler::playerTick);
	}
	public static void onLivingHurt(LivingEntity entity, DamageSource source, CallbackInfoReturnable<Boolean> cir)
	{
		if (source.isOf(SplatcraftDamageTypes.ENEMY_INK) && entity.getHealth() <= 4)
			cir.cancel();
	}
	public static void playerTick(PlayerEntity player)
	{
		if (InkBlockUtils.onEnemyInk(player))
		{
			if (player.age % 20 == 0 && player.getHealth() > 4 && player.getWorld().getDifficulty() != Difficulty.PEACEFUL)
				player.damage(SplatcraftDamageTypes.of(player.getWorld(), SplatcraftDamageTypes.ENEMY_INK), Math.min(player.getHealth() - 4, 2f));
			if (player.getWorld().getRandom().nextFloat() < 0.5f)
			{
				ColorUtils.addStandingInkSplashParticle(player.getWorld(), player, 1);
			}
		}
		
		if (SplatcraftGameRules.getLocalizedRule(player.getWorld(), player.getBlockPos(), SplatcraftGameRules.WATER_DAMAGE) && player.isSubmergedInWater() && player.age % 10 == 0 && !StatusEffectUtil.hasWaterBreathing(player))
			player.damage(SplatcraftDamageTypes.of(player.getWorld(), SplatcraftDamageTypes.WATER), 8f);
		
		if (!EntityInfoCapability.hasCapability(player))
			return;
		
		EntityInfo info = EntityInfoCapability.get(player);
//        if (event.phase == TickEvent.Phase.START)
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
				player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.inkSubmerge, SoundCategory.PLAYERS, 0.5F, ((player.getWorld().getRandom().nextFloat() - player.getWorld().getRandom().nextFloat()) * 0.2F + 1.0F) * 0.95F);
				
				if (player.getWorld() instanceof ServerWorld serverLevel)
				{
					for (int i = 0; i < 2; i++)
						ColorUtils.addInkSplashParticle(serverLevel, player, 1.4f);
				}
			}
			else if (state == SquidState.SURFACING)
			{
				player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.inkSurface, SoundCategory.PLAYERS, 0.5F, ((player.getWorld().getRandom().nextFloat() - player.getWorld().getRandom().nextFloat()) * 0.2F + 1.0F) * 0.95F);
			}
			
			squidSubmergeMode.put(player, state);
		}
		
		if (info.isSquid())
		{
			if (!player.getAbilities().flying)
			{
				player.setSprinting(player.isSubmergedInWater());
				player.horizontalSpeed = player.prevHorizontalSpeed;
			}
			
			player.setPose(EntityPose.SWIMMING);
			player.stopUsingItem();
			
			player.incrementStat(SplatcraftStats.SQUID_TIME);
			
			if (InkBlockUtils.canSquidHide(player))
			{
				if (player.getHealth() < player.getMaxHealth() && SplatcraftGameRules.getLocalizedRule(player.getWorld(), player.getBlockPos(), SplatcraftGameRules.INK_HEALING) && player.age % 5 == 0 && !player.hasStatusEffect(StatusEffects.POISON) && !player.hasStatusEffect(StatusEffects.WITHER))
				{
					player.heal(0.5f);
					if (SplatcraftGameRules.getLocalizedRule(player.getWorld(), player.getBlockPos(), SplatcraftGameRules.INK_HEALING_CONSUMES_HUNGER))
						player.addExhaustion(0.25f);
					if (InkOverlayCapability.hasCapability(player))
					{
						InkOverlayCapability.get(player).addAmount(-0.49f);
					}
				}
				
				boolean crouch = player.isSneaking();
				if (!crouch && player.getWorld().getRandom().nextFloat() <= 0.6f && (Math.abs(player.getX() - player.prevX) > 0.14 || Math.abs(player.getY() - player.prevY) > 0.07 || Math.abs(player.getZ() - player.prevZ) > 0.14))
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
				ColorUtils.setPlayerColor(player, ColorUtils.getEffectiveColor(player.getWorld(), posBelow));
			}
			
			if (blockBelow instanceof SpawnPadBlock)
			{
				InkColorTileEntity spawnPad = (InkColorTileEntity) player.getWorld().getBlockEntity(posBelow);
				
				if (player instanceof ServerPlayerEntity serverPlayer && ColorUtils.colorEquals(player, spawnPad))
				{
					serverPlayer.setSpawnPoint(player.getWorld().getRegistryKey(), posBelow, player.getWorld().getBlockState(posBelow).get(SpawnPadBlock.DIRECTION).asRotation(), false, true);
				}
			}
		}
		if (InkOverlayCapability.hasCapability(player))
		{
			InkOverlayCapability.get(player).addAmount(-0.01f);
		}
	}
	public static void cancelDamageIfSquid(LivingEntity entity, float fallDistance, CallbackInfoReturnable<Boolean> cir)
	{
		if (entity instanceof ServerPlayerEntity player && EntityInfoCapability.get(player).isSquid())
		{
			if (InkBlockUtils.canSquidHide(player))
			{
				SplatcraftStats.FALL_INTO_INK_TRIGGER.get().trigger(player, fallDistance);
				cir.setReturnValue(false);
			}
		}
	}
	public static double modifyVisibility(LivingEntity entity, double original)
	{
		if (EntityInfoCapability.hasCapability(entity) && EntityInfoCapability.get(entity).isSquid() && InkBlockUtils.canSquidHide(entity))
		{
			return (Math.abs(entity.getX() - entity.prevX) > 0.14 || Math.abs(entity.getY() - entity.prevY) > 0.07 || Math.abs(entity.getZ() - entity.prevZ) > 0.14 ? 0.7 : 0);
		}
		return original;
	}
	public static void onGameModeSwitch(PlayerEntity player, GameMode newGameMode)
	{
		if (newGameMode != GameMode.SPECTATOR) return;
		player.stopUsingItem();
		EntityInfoCapability.get(player).setIsSquid(false);
		SplatcraftPacketHandler.sendToTrackersAndSelf(new PlayerSetSquidS2CPacket(player.getUuid(), false), player);
	}
	public static EventResult onPlayerAttackEntity(PlayerEntity player, World level, Entity target, Hand hand, @Nullable EntityHitResult result)
	{
		if (EntityInfoCapability.isSquid(player))
			return EventResult.interruptFalse();
		return EventResult.pass();
	}
	public static EventResult onPlayerInteract(PlayerEntity player, Object... params)
	{
		if (EntityInfoCapability.isSquid(player))
			return EventResult.interruptFalse();
		return EventResult.pass();
	}
	public static CompoundEventResult<ItemStack> onPlayerInteractItem(PlayerEntity player, Object... params)
	{
		if (EntityInfoCapability.isSquid(player))
			return CompoundEventResult.interruptFalse(ItemStack.EMPTY);
		return CompoundEventResult.pass();
	}
	@Environment(EnvType.CLIENT)
	public static void doSquidRotation(Entity entity)
	{
		if (!entity.getWorld().isClient() || !(entity instanceof LivingEntity living))
			return;
		if (InkOverlayCapability.hasCapability(living))
		{
			InkOverlayInfo info = InkOverlayCapability.get(living);
			Vec3d prev = living.getLerpedPos(0);
			
			info.setSquidRot(Math.abs(living.getY() - prev.y) * living.getPos().subtract(prev).normalize().y);
		}
	}
	public static void modifyJumpSpeed(LivingEntity entity)
	{
		if (EntityInfoCapability.get(entity).isSquid() && InkBlockUtils.canSquidSwim(entity))
		{
			entity.setVelocity(entity.getVelocity().x, entity.getVelocity().y * 1.1, entity.getVelocity().z);
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