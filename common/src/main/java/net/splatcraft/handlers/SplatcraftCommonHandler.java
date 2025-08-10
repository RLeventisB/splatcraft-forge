package net.splatcraft.handlers;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.client.particles.SquidSoulParticleData;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.InkColorGroup;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.SaveInfoCapability;
import net.splatcraft.data.capabilities.structs.InkOverlayData;
import net.splatcraft.data.capabilities.structs.PlayerInfo;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.InkWaxerItem;
import net.splatcraft.items.SpecialProviderItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.RequestPlayerComponentsPacket;
import net.splatcraft.network.s2c.*;
import net.splatcraft.platform.Components;
import net.splatcraft.platform.Services;
import net.splatcraft.platform.event.*;
import net.splatcraft.registries.SplatcraftAttributes;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.util.*;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.structs.AttackId;
import net.splatcraft.util.structs.InkColor;
import net.splatcraft.util.structs.RangedValueCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SplatcraftCommonHandler
{
	public static void registerEvents()
	{
		Services.PLATFORM.registerListener(PlayerEvents.PlayerClone.class, SplatcraftCommonHandler::onPlayerClone);
		Services.PLATFORM.registerListener(EntityEvents.LivingDeath.class, SplatcraftCommonHandler::onLivingDeath);
		Services.PLATFORM.registerListener(PlayerEvents.LogIn.class, SplatcraftCommonHandler::onPlayerLoggedIn);
		Services.PLATFORM.registerListener(TickEvents.PlayerBefore.class, SplatcraftCommonHandler::capabilityUpdateEvent);
		Services.PLATFORM.registerListener(TickEvents.ServerLevelBefore.class, SplatcraftCommonHandler::onWorldTick);
		
		Services.PLATFORM.registerListener(InteractionEvents.LeftClickBlock.class, SplatcraftCommonHandler::onBlockLeftClick);
	}
	public static void onEntityJump(LivingEntity entity)
	{
		if (InkBlockUtils.onEnemyInk(entity) && entity.getAttributes().hasAttribute(SplatcraftAttributes.enemyInkJumpMultiplier))
		{
			Vec3 deltaMovement = entity.getDeltaMovement().multiply(1, entity.getAttributeValue(SplatcraftAttributes.enemyInkJumpMultiplier), 1);
			entity.setDeltaMovement(deltaMovement);
		}
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
	public static void onPlayerClone(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive)
	{
		if (alive)
		{
			return;
		}
		
		Map<Integer, ItemStack> matchInv = Components.PLAYER_INFO.getOrCreate(oldPlayer).matchInventory();
		
		if (!matchInv.isEmpty())
		{
			tryToInsertItems(newPlayer, matchInv, true);
			
			Components.PLAYER_INFO.getOrCreate(newPlayer).setMatchInventory(new Object2ObjectOpenHashMap<>());
		}
		EntityAction.setEntityAction(newPlayer, null, true, true);
	}
	private static void tryToInsertItems(Player player, Map<Integer, ItemStack> matchInv, boolean dropItemIfFail)
	{
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++)
		{
			ItemStack stack = matchInv.get(i);
			if (stack != null && !stack.isEmpty() && !putStackInSlot(inventory, stack, i) && !inventory.add(stack) && dropItemIfFail)
			{
				player.drop(stack, true, true);
			}
		}
	}
	private static boolean putStackInSlot(Inventory inventory, ItemStack stack, int slot)
	{
		ItemStack invStack = inventory.getItem(slot);
		
		if (invStack.isEmpty())
		{
			inventory.setItem(slot, stack);
			return true;
		}
		if (invStack.is(stack.getItem()))
		{
			int invCount = invStack.getCount();
			int count = Math.min(invStack.getMaxStackSize(), stack.getCount() + invStack.getCount());
			invStack.setCount(count);
			stack.shrink(count - invCount);
			
			return stack.isEmpty();
		}
		return false;
	}
	public static EventResult onLivingDeath(LivingEntity entity, DamageSource source)
	{
		ItemStack stack = entity.getItemBySlot(EquipmentSlot.CHEST);
		
		if (stack.has(SplatcraftComponents.TANK_DATA))
		{
			InkTankItem.refill(stack);
		}
		
		EntityAction.setEntityAction(entity, null, true, true);
		SplatcraftPacketHandler.sendToTrackersAndSelf(UpdateEntityActionOnlyPacket.create(entity), entity);
		
		if (entity instanceof Player player)
		{
			EventResult eventResult = keepAliveIfOnMatch(player, source);
			if (!eventResult.interruptsOrFalse())
			{
				doOnDeathActions(entity);
			}
			return eventResult;
		}
		return EventResult.pass();
	}
	private static void doOnDeathActions(LivingEntity entity)
	{
		if (entity.getAttributes().hasAttribute(SplatcraftAttributes.specialLoss))
		{
			float specialLoss = (float) entity.getAttributeValue(SplatcraftAttributes.specialLoss);
			for (ItemStack providerStack : CommonUtils.getItemsInInventory(entity, v -> v.getItem() instanceof SpecialProviderItem))
			{
				providerStack.update(SplatcraftComponents.SPECIAL_PROVIDER_DATA,
					SplatcraftComponents.SpecialProviderData.DEFAULT,
					v -> v.withDelay(0).withStoredCharge(v.storedCharge() * specialLoss));
			}
		}
		for (ItemStack weaponStack : CommonUtils.getItemsInInventory(entity, stack -> stack.getItem() instanceof WeaponBaseItem<?>))
		{
			WeaponBaseItem<?> weapon = (WeaponBaseItem<?>) weaponStack.getItem();
			weapon.getResetShootingAction(weaponStack, entity).ifPresent(SpecialHandler.ResetAction::run);
		}
	}
	private static EventResult keepAliveIfOnMatch(Player player, DamageSource source)
	{
		PlayerInfo info = Components.PLAYER_INFO.getOrCreate(player);
		InkColor color = ColorUtils.getEntityColor(player);
		
		if (!info.isPlaying())
			return EventResult.pass();
		
		if (!info.isMatchRespawning())
		{
			WeaponHandler.onDeath(player, source);
			
			Components.PLAYER_INFO.set(player, info.setMatchRespawnTimeLeft(100).setIsMatchRespawning(true));
			
			if (!player.level().isClientSide())
			{
				Services.PLATFORM.getServerInstance().getPlayerList().broadcastSystemMessage(source.getLocalizedDeathMessage(player), false);
			}
			
			if (player instanceof ServerPlayer serverPlayer)
			{
				Entity attacker = source.getEntity();
				if (attacker instanceof LivingEntity livingAttacker && source instanceof InkDamageUtils.InkDamageSource inkDamageSource)
				{
					InkExplosion.createInkExplosion(attacker, player.position(), 2f, RangedValueCollection.EMPTY, InkBlockUtils.getInkType(livingAttacker), inkDamageSource.getWeaponItem(), AttackId.NONE);
				}
				SplatcraftPacketHandler.sendToPlayer(SendPlayerDeathMatchPacket.create(100, attacker, player), serverPlayer);
				
				SplatcraftPacketHandler.sendToTrackers(new UpdatePlayerComponentsPacket(player, Components.Bits.PLAYER_INFO), player);
			}
			
			((ServerLevel) player.level()).sendParticles(new SquidSoulParticleData(color), player.getX(), player.getY() + 0.5f, player.getZ(), 1, 0, 1, 0, 1.5f);
		}
		
		WeaponHandler.doScoreboardLogicOnDeath(source, player, color);
		
		return EventResult.interruptFalse();
	}
	public static void onLivingDeathDrops(LivingEntity entity, Collection<ItemEntity> drops)
	{
		//Handle keepMatchItems
		if (entity instanceof Player player)
		{
			PlayerInfo info = Components.PLAYER_INFO.getOrCreate(player);
			Map<Integer, ItemStack> matchInv = info.matchInventory();
			
			drops.removeIf(o -> matchInv.containsValue(o.getItem()));
			
			tryToInsertItems(player, matchInv, false);
		}
	}
	public static void onPlayerAboutToDie(LivingEntity entity, float amount)
	{
		if (!(entity instanceof Player player) || entity.getHealth() - amount > 0)
		{
			return;
		}
		
		if (!player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) && SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.KEEP_MATCH_ITEMS))
		{
			Components.PLAYER_INFO.updateOrCreate(player, info ->
			{
				Inventory inventory = player.getInventory();
				Object2ObjectOpenHashMap<Integer, ItemStack> matchInv = new Object2ObjectOpenHashMap<>(inventory.getContainerSize());
				
				for (int i = 0; i < inventory.getContainerSize(); i++)
				{
					ItemStack stack = inventory.getItem(i);
					if (stack.is(SplatcraftTags.Items.MATCH_ITEMS))
					{
						matchInv.put(i, stack);
					}
				}
				return info.setMatchInventory(matchInv);
			});
		}
	}
	public static void onPlayerLoggedIn(ServerPlayer player)
	{
		SplatcraftPacketHandler.sendToPlayer(new UpdateBooleanGamerulesPacket(SplatcraftGameRules.booleanRules), player);
		SplatcraftPacketHandler.sendToPlayer(new UpdateIntGamerulesPacket(SplatcraftGameRules.intRules), player);
		SplatcraftPacketHandler.sendToPlayer(new UpdateWeaponSettingsPacket(), player);
		SplatcraftPacketHandler.sendToAll(new PlayerColorPacket(player, Components.SQUID_INFO.getOrCreate(player).color()));
		SplatcraftPacketHandler.sendToPlayer(new SendColorRegistryPacket(InkColorRegistry.REGISTRY, InkColorGroup.getAllGroups()), player);
		SplatcraftPacketHandler.sendToPlayer(new UpdateColorScoresPacket(true, true, new ArrayList<>(ScoreboardHandler.getCriteriaKeySet())), player);
		SplatcraftPacketHandler.sendToPlayer(new UpdateStageListPacket(SaveInfoCapability.get().stages()), player);
		for (Player otherPlayer : player.level().players())
		{
			if (otherPlayer == player)
				continue;
			
			SplatcraftPacketHandler.sendToPlayer(new UpdatePlayerComponentsPacket(player, Components.Bits.ALL_PLAYER), player);
		}
	}
	public static void capabilityUpdateEvent(Player player)
	{
		PlayerInfo info = Components.PLAYER_INFO.getOrCreate(player);
		if (player.deathTime <= 0 && !info.isInitialized())
		{
			info = info.setInitialized(true);
			Components.PLAYER_INFO.set(player, info);
			
			if (player.level().isClientSide())
			{
				SplatcraftPacketHandler.sendToServer(new RequestPlayerComponentsPacket(player, Components.Bits.PLAYER_INFOS));
			}
		}
		
		if (!player.level().isClientSide())
		{
			ItemStack inkBand = CommonUtils.getItemInInventory(player, itemStack -> itemStack.is(SplatcraftTags.Items.INK_BANDS) && InkBlockUtils.hasInkType(itemStack));
			
			if (!ItemStack.isSameItem(info.inkBand(), inkBand))
			{
				info = info.setInkBand(inkBand);
				Components.PLAYER_INFO.set(player, info);
				SplatcraftPacketHandler.sendToTrackersAndSelf(new UpdatePlayerComponentsPacket(player, Components.Bits.PLAYER_INFO), player);
			}
		}
	}
	public static void onWorldTick(ServerLevel world)
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
		if (!(entity instanceof LivingEntity livingEntity))
			return;
		
		if (Components.INK_OVERLAY.has(livingEntity))
		{
			InkOverlayData overlayInfo = Components.INK_OVERLAY.getOrCreate(livingEntity);
			if (entity.isUnderWater())
			{
				overlayInfo.setAmount(0);
			}
			else if (entity.isInWaterRainOrBubble())
			{
				overlayInfo.addAmount(-0.5f);
			}
			else
			{
				overlayInfo.addAmount(-0.01f);
			}
			Components.INK_OVERLAY.set(livingEntity, overlayInfo);
		}
		
		if (!(livingEntity instanceof Player player))
			return;
		
		AtomicBoolean sendPacket = new AtomicBoolean(false);
		Components.PLAYER_INFO.updateOrCreate(player, info ->
		{
			if (!info.isPlaying())
				return info;
			
			boolean sessionExists = SaveInfoCapability.get().playSessions().containsKey(info.getPlayingStageId());
			if (sessionExists)
			{
				if (!player.isDeadOrDying() || !info.isMatchRespawning())
					return info;
				
				if (info.getMatchRespawnTimeLeft() <= 0)
				{
					doOnDeathActions(player);
					
					if (!(player instanceof ServerPlayer serverPlayer))// make the server handle the respawning or else a laggy client will teleport after being actionable lol
						return info;
					
					player.setHealth(player.getMaxHealth());
					info = info.setIsMatchRespawning(false);
					SplatcraftPacketHandler.sendToPlayer(new SendPlayerRespawnMatchPacket(), serverPlayer);
					BlockPos spawnPadPos = SuperJumpCommand.getSpawnPadPos(serverPlayer);
					if (spawnPadPos != null)
					{
						player.moveTo(spawnPadPos.getX() + 0.5f, spawnPadPos.getY() + 0.5f, spawnPadPos.getZ() + 0.5);
					}
					sendPacket.set(true);
				}
				else
				{
					info = info.setMatchRespawnTimeLeft(info.getMatchRespawnTimeLeft() - 1);
				}
			}
			else
			{
				info = info.setPlayingStageId(null).setMatchRespawnTimeLeft(0).setIsMatchRespawning(false);
				sendPacket.set(true);
			}
			return info;
		});
		
		if (sendPacket.get() && player instanceof ServerPlayer serverPlayer)
			SplatcraftPacketHandler.sendToAll(new UpdatePlayerComponentsPacket(serverPlayer, Components.Bits.PLAYER_INFO));
	}
	public static EventResult onBlockLeftClick(Player player,
	                                           InteractionHand hand,
	                                           Direction face,
	                                           ItemStack stack,
	                                           Level level,
	                                           BlockPos pos)
	{
		if (player.getItemInHand(hand).getItem() instanceof InkWaxerItem waxItem)
		{
			waxItem.onBlockStartBreak(pos, player.level(), face);
		}
		return EventResult.pass();
	}
}