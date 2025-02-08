package net.splatcraft.handlers;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameRules;
import net.splatcraft.client.particles.SquidSoulParticleData;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.InkWaxerItem;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.RequestEntityInfoPacket;
import net.splatcraft.network.s2c.*;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.util.*;
import net.splatcraft.util.action.EntityAction;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SplatcraftCommonHandler
{
	public static void registerEvents()
	{
		PlayerEvent.PLAYER_CLONE.register(SplatcraftCommonHandler::onPlayerClone);
		EntityEvent.LIVING_DEATH.register(SplatcraftCommonHandler::onLivingDeath);
		PlayerEvent.PLAYER_JOIN.register(SplatcraftCommonHandler::onPlayerLoggedIn);
		TickEvent.PLAYER_PRE.register(SplatcraftCommonHandler::capabilityUpdateEvent);
		TickEvent.SERVER_LEVEL_PRE.register(SplatcraftCommonHandler::onWorldTick);
		
		InteractionEvent.LEFT_CLICK_BLOCK.register(SplatcraftCommonHandler::onBlockLeftClick);
	}
	public static void onPlayerJump(LivingEntity entity)
	{
		if (InkBlockUtils.onEnemyInk(entity))
			entity.setVelocity(entity.getVelocity().x, Math.min(entity.getVelocity().y, 0.1f), entity.getVelocity().z);
	}
	// todo: uhhh this thing checks for another events
    /*public static void onLivingDestroyBlock(LivingDestroyBlockEvent event)
    {
        if (!(event.getEntity().getWorld().getBlockEntity(event.getPos()) instanceof InkedBlockTileEntity te))
        {
            return;
        }

        BlockState savedState = te.getSavedState();
        if (event.getState().getBlock() instanceof IColoredBlock block && (event.isCanceled() ||
            (event.getEntity() instanceof EnderDragon && savedState.isIn(BlockTags.DRAGON_IMMUNE)) ||
            (event.getEntity() instanceof WitherBoss && savedState.isIn(BlockTags.WITHER_IMMUNE))))
        {
            block.remoteInkClear(event.getEntity().getWorld(), event.getPos());
            event.setCanceled(true);
        }
    }*/
	public static void onPlayerClone(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive)
	{
		if (alive)
		{
			return;
		}
		
		Object2ObjectOpenHashMap<Integer, ItemStack> matchInv = EntityInfoCapability.get(oldPlayer).getMatchInventory();
		
		if (!matchInv.isEmpty())
		{
			tryToInsertItems(newPlayer, matchInv, true);
			
			EntityInfoCapability.get(newPlayer).setMatchInventory(new Object2ObjectOpenHashMap<>());
		}
		EntityAction.setEntityAction(newPlayer, null);
	}
	private static void tryToInsertItems(PlayerEntity player, Object2ObjectOpenHashMap<Integer, ItemStack> matchInv, boolean dropItemIfFail)
	{
		PlayerInventory inventory = player.getInventory();
		for (int i = 0; i < inventory.size(); i++)
		{
			ItemStack stack = matchInv.get(i);
			if (stack != null && !stack.isEmpty() && !putStackInSlot(inventory, stack, i) && !inventory.insertStack(stack) && dropItemIfFail)
			{
				player.dropItem(stack, true, true);
			}
		}
	}
	private static boolean putStackInSlot(PlayerInventory inventory, ItemStack stack, int slot)
	{
		ItemStack invStack = inventory.getStack(slot);
		
		if (invStack.isEmpty())
		{
			inventory.setStack(slot, stack);
			return true;
		}
		if (invStack.isOf(stack.getItem()))
		{
			int invCount = invStack.getCount();
			int count = Math.min(invStack.getMaxCount(), stack.getCount() + invStack.getCount());
			invStack.setCount(count);
			stack.decrement(count - invCount);
			
			return stack.isEmpty();
		}
		return false;
	}
	public static EventResult onLivingDeath(LivingEntity entity, DamageSource source)
	{
		ItemStack stack = entity.getEquippedStack(EquipmentSlot.CHEST);
		
		if (stack.getItem() instanceof InkTankItem item)
		{
			item.refill(stack);
		}
		
		return keepAliveIfOnMatch(entity, source);
	}
	private static EventResult keepAliveIfOnMatch(LivingEntity entity, DamageSource source)
	{
		AtomicBoolean keepAlive = new AtomicBoolean(false);
		EntityInfoCapability.getOptional(entity).ifPresent(info ->
		{
			InkColor color = ColorUtils.getEntityColor(entity);
			if (info.isPlaying())
			{
				if (!info.isMatchRespawning())
				{
					info.setMatchRespawnTimeLeft(100);
					info.setIsMatchRespawning(true);
					
					if (entity instanceof ServerPlayerEntity player)
					{
						Entity attacker = source.getAttacker();
						Vector3f killCamDirection = new Vector3f();
						if (attacker instanceof LivingEntity livingAttacker)
						{
							InkExplosion.createInkExplosion(attacker, player.getPos(), 2f, DamageRangesRecord.DEFAULT, InkBlockUtils.getInkType(livingAttacker), source.getWeaponStack(), AttackId.NONE);
							killCamDirection = attacker.getPos().subtract(entity.getPos()).toVector3f().sub(0, 3, 0).normalize();
						}
						
						SplatcraftPacketHandler.sendToPlayer(new SendPlayerDeathMatchPacket(100, source.getAttacker(), killCamDirection), player);
					}
					
					((ServerWorld) entity.getWorld()).spawnParticles(new SquidSoulParticleData(color), entity.getX(), entity.getY() + 0.5f, entity.getZ(), 1, 0, 1, 0, 1.5f);
				}
				
				keepAlive.set(true);
				
				WeaponHandler.doScoreboardLogicOnDeath(source, entity, color);
			}
		});
		return keepAlive.get() ? EventResult.interruptFalse() : EventResult.pass();
	}
	public static void onLivingDeathDrops(LivingEntity entity, Collection<ItemEntity> drops)
	{
		//Handle keepMatchItems
		if (entity instanceof PlayerEntity player)
		{
			EntityInfoCapability.getOptional(player).ifPresent(info ->
			{
				Object2ObjectOpenHashMap<Integer, ItemStack> matchInv = info.getMatchInventory();
				
				drops.removeIf(o -> matchInv.containsValue(o.getStack()));
				
				tryToInsertItems(player, matchInv, false);
			});
		}
	}
	public static void onPlayerAboutToDie(LivingEntity entity, float amount)
	{
		if (!(entity instanceof PlayerEntity player) || entity.getHealth() - amount > 0)
		{
			return;
		}
		
		if (!player.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY) && SplatcraftGameRules.getLocalizedRule(player.getWorld(), player.getBlockPos(), SplatcraftGameRules.KEEP_MATCH_ITEMS))
		{
			EntityInfoCapability.getOptional(player).ifPresent(info ->
			{
				PlayerInventory inventory = player.getInventory();
				Object2ObjectOpenHashMap<Integer, ItemStack> matchInv = new Object2ObjectOpenHashMap<>(inventory.size());
				
				for (int i = 0; i < inventory.size(); i++)
				{
					ItemStack stack = inventory.getStack(i);
					if (stack.isIn(SplatcraftTags.Items.MATCH_ITEMS))
					{
						matchInv.put(i, stack);
					}
				}
				info.setMatchInventory(matchInv);
			});
		}
	}
	public static void onPlayerLoggedIn(ServerPlayerEntity player)
	{
		SplatcraftPacketHandler.sendToPlayer(new UpdateBooleanGamerulesPacket(SplatcraftGameRules.booleanRules), player);
		SplatcraftPacketHandler.sendToPlayer(new UpdateIntGamerulesPacket(SplatcraftGameRules.intRules), player);
		SplatcraftPacketHandler.sendToPlayer(new UpdateWeaponSettingsPacket(), player);
		
		TreeMap<UUID, InkColor> playerColors = new TreeMap<>();
		
		for (PlayerEntity p : player.getWorld().getPlayers())
		{
			if (EntityInfoCapability.hasCapability(p))
			{
				playerColors.put(p.getUuid(), EntityInfoCapability.get(p).getColor());
			}
		}
		
		SplatcraftPacketHandler.sendToAll(new UpdateClientColorsPacket(player.getUuid(), EntityInfoCapability.get(player).getColor()));
		SplatcraftPacketHandler.sendToPlayer(new UpdateClientColorsPacket(playerColors), player);
		SplatcraftPacketHandler.sendToPlayer(new SendColorRegistryPacket(InkColorRegistry.REGISTRY), player);
		SplatcraftPacketHandler.sendToPlayer(new UpdateColorScoresPacket(true, true, new ArrayList<>(ScoreboardHandler.getCriteriaKeySet())), player);
		SplatcraftPacketHandler.sendToPlayer(new UpdateStageListPacket(SaveInfoCapability.get().stages()), player);
	}
	public static void capabilityUpdateEvent(PlayerEntity player)
	{
		if (EntityInfoCapability.hasCapability(player))
		{
			EntityInfo info = EntityInfoCapability.get(player);
			if (player.deathTime <= 0 && !info.isInitialized())
			{
				info.setInitialized(true);
				
				if (player.getWorld().isClient() && player instanceof ClientPlayerEntity)
				{
					SplatcraftPacketHandler.sendToServer(new RequestEntityInfoPacket(player));
				}
			}
			
			if (player instanceof ServerPlayerEntity)
			{
				ItemStack inkBand = CommonUtils.getItemInInventory(player, itemStack -> itemStack.isIn(SplatcraftTags.Items.INK_BANDS) && InkBlockUtils.hasInkType(itemStack));
				
				if (!ItemStack.areItemsEqual(info.getInkBand(), inkBand))
				{
					info.setInkBand(inkBand);
					SplatcraftPacketHandler.sendToTrackersAndSelf(new UpdateEntityInfoPacket(player), player);
				}
			}
		}
	}
	public static void onWorldTick(ServerWorld world)
	{
		for (Map.Entry<Integer, Boolean> rule : SplatcraftGameRules.booleanRules.entrySet())
		{
			boolean levelValue = world.getGameRules().getBoolean(SplatcraftGameRules.getRuleFromIndex(rule.getKey()));
			if (rule.getValue() != levelValue)
			{
				SplatcraftGameRules.booleanRules.put(rule.getKey(), levelValue);
				SplatcraftPacketHandler.sendToAll(new UpdateBooleanGamerulesPacket(SplatcraftGameRules.getRuleFromIndex(rule.getKey()), rule.getValue()));
			}
		}
		for (Map.Entry<Integer, Integer> rule : SplatcraftGameRules.intRules.entrySet())
		{
			int levelValue = world.getGameRules().getInt(SplatcraftGameRules.getRuleFromIndex(rule.getKey()));
			if (rule.getValue() != levelValue)
			{
				SplatcraftGameRules.intRules.put(rule.getKey(), levelValue);
				SplatcraftPacketHandler.sendToAll(new UpdateIntGamerulesPacket(SplatcraftGameRules.getRuleFromIndex(rule.getKey()), rule.getValue()));
			}
		}
	}
	public static void onLivingTick(Entity entity)
	{
		if (entity instanceof LivingEntity livingEntity)
		{
			if (InkOverlayCapability.hasCapability(livingEntity))
			{
				InkOverlayInfo overlayInfo = InkOverlayCapability.get(livingEntity);
				if (entity.isSubmergedInWater())
				{
					overlayInfo.setAmount(0);
				}
				else if (entity.isWet())
				{
					overlayInfo.addAmount(-0.5f);
				}
				else
				{
					overlayInfo.addAmount(-0.01f);
				}
				InkOverlayCapability.set(livingEntity, overlayInfo);
			}
			
			EntityInfoCapability.getOptional(livingEntity).ifPresent(info ->
			{
				if (info.isPlaying())
				{
					boolean sessionExists = SaveInfoCapability.get().playSessions().containsKey(info.getPlayingStageId());
					if (sessionExists)
					{
						if (livingEntity.isDead() && info.isMatchRespawning())
						{
							if (info.getMatchRespawnTimeLeft() <= 0)
							{
								if (livingEntity instanceof ServerPlayerEntity serverPlayer)// make the server handle the respawning or else a laggy client will teleport after being actionable lol
								{
									livingEntity.setHealth(livingEntity.getMaxHealth());
									info.setIsMatchRespawning(false);
									SplatcraftPacketHandler.sendToPlayer(new SendPlayerRespawnMatchPacket(), serverPlayer);
									BlockPos spawnPadPos = SuperJumpCommand.getSpawnPadPos(serverPlayer);
									if (spawnPadPos != null)
									{
										livingEntity.teleport(spawnPadPos.getX() + 0.5f, spawnPadPos.getY() + 0.5f, spawnPadPos.getZ() + 0.5, false);
									}
								}
							}
							else
							{
								info.setMatchRespawnTimeLeft(info.getMatchRespawnTimeLeft() - 1);
							}
						}
					}
					else
					{
						info.setPlayingStageId(null);
						info.setMatchRespawnTimeLeft(0);
						info.setIsMatchRespawning(false);
						if (livingEntity instanceof PlayerEntity player)
							SplatcraftPacketHandler.sendToAll(new UpdateEntityInfoPacket(player));
					}
				}
			});
		}
	}
	public static EventResult onBlockLeftClick(PlayerEntity player,
	                                           Hand hand,
	                                           BlockPos pos,
	                                           Direction face)
	{
		if (player.getStackInHand(hand).getItem() instanceof InkWaxerItem waxItem)
		{
			waxItem.onBlockStartBreak(pos, player.getWorld(), face);
		}
		return EventResult.pass();
	}
}