package net.splatcraft.handlers;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameRules;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.data.capabilities.playerinfo.EntityInfo;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.InkWaxerItem;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.RequestPlayerInfoPacket;
import net.splatcraft.network.s2c.*;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkColor;
import net.splatcraft.util.PlayerCooldown;

import java.util.*;

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

//        oldPlayer.reviveCaps(); // Mod devs should not have to do this
		EntityInfoCapability.get(newPlayer).readNBT(EntityInfoCapability.get(oldPlayer).writeNBT(new NbtCompound(), newPlayer.getRegistryManager()), newPlayer.getRegistryManager());
//        event.getOriginal().invalidateCaps();
		
		DefaultedList<ItemStack> matchInv = EntityInfoCapability.get(newPlayer).getMatchInventory();
		
		if (!matchInv.isEmpty())
		{
			for (int i = 0; i < matchInv.size(); i++)
			{
				ItemStack stack = matchInv.get(i);
				if (!stack.isEmpty() && !putStackInSlot(newPlayer.getInventory(), stack, i) && !newPlayer.getInventory().insertStack(stack))
				{
					newPlayer.dropItem(stack, true, true);
				}
			}
			
			EntityInfoCapability.get(newPlayer).setMatchInventory(DefaultedList.of());
		}
		PlayerCooldown.setPlayerCooldown(newPlayer, null);
	}
	private static boolean putStackInSlot(PlayerInventory inventory, ItemStack stack, int i)
	{
		ItemStack invStack = inventory.getStack(i);
		
		if (invStack.isEmpty())
		{
			inventory.setStack(i, stack);
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
		return EventResult.pass();
	}
	public static void onLivingDeathDrops(LivingEntity entity, Collection<ItemEntity> drops)
	{
        /*
        //handle inked wool drops (should've handled in the mixin)
        if (event.getEntity() instanceof Sheep && InkOverlayCapability.hasCapability(event.getEntity()))
        {
            InkOverlayInfo info = InkOverlayCapability.get(event.getEntity());

            if (info.getWoolColor().isValid())
            {
                for (ItemEntity itemEntity : event.getDrops())
                {
                    ItemStack stack = itemEntity.getItem();
                    if (stack.is(ItemTags.WOOL))
                    {
                        itemEntity.setItem(ColorUtils.setColorLocked(ColorUtils.setInkColor(new ItemStack(SplatcraftItems.inkedWool.get(), stack.getCount()), info.getWoolColor()), true));
                    }
                }
            }
        }*/
		
		//Handle keepMatchItems
		if (entity instanceof PlayerEntity player)
		{
			DefaultedList<ItemStack> matchInv = EntityInfoCapability.get(player).getMatchInventory();
			
			drops.removeIf(o -> matchInv.contains(o.getStack()));
			
			for (int i = 0; i < matchInv.size(); i++)
			{
				ItemStack stack = matchInv.get(i);
				if (!stack.isEmpty() && !putStackInSlot(player.getInventory(), stack, i))
				{
					player.getInventory().insertStack(stack);
				}
			}
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
			EntityInfo playerCapability;
			try
			{
				playerCapability = EntityInfoCapability.get(player);
			}
			catch (NullPointerException e)
			{
				return;
			}
			
			DefaultedList<ItemStack> matchInv = DefaultedList.ofSize(player.getInventory().size(), ItemStack.EMPTY);
			
			for (int i = 0; i < matchInv.size(); i++)
			{
				ItemStack stack = player.getInventory().getStack(i);
				if (stack.isIn(SplatcraftTags.Items.MATCH_ITEMS))
				{
					matchInv.set(i, stack);
				}
			}
			playerCapability.setMatchInventory(matchInv);
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
		SplatcraftPacketHandler.sendToPlayer(new UpdateColorScoresPacket(true, true, new ArrayList<>(ScoreboardHandler.getCriteriaKeySet())), player);
		SplatcraftPacketHandler.sendToPlayer(new UpdateStageListPacket(SaveInfoCapability.get().getStages()), player);
	}
	public static void capabilityUpdateEvent(PlayerEntity player)
	{
		if (EntityInfoCapability.hasCapability(player))
		{
			EntityInfo info = EntityInfoCapability.get(player);
			if (player.deathTime <= 0 && !info.isInitialized())
			{
				info.setInitialized(true);
				
				if (player instanceof ClientPlayerEntity)
				{
					SplatcraftPacketHandler.sendToServer(new RequestPlayerInfoPacket(player));
				}
			}
			
			if (player instanceof ServerPlayerEntity)
			{
				ItemStack inkBand = CommonUtils.getItemInInventory(player, itemStack -> itemStack.isIn(SplatcraftTags.Items.INK_BANDS) && InkBlockUtils.hasInkType(itemStack));
				
				if (!ItemStack.areItemsEqual(info.getInkBand(), inkBand))
				{
					info.setInkBand(inkBand);
					SplatcraftPacketHandler.sendToTrackersAndSelf(new UpdatePlayerInfoPacket(player), player);
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
		if (entity instanceof LivingEntity livingEntity && InkOverlayCapability.hasCapability(livingEntity))
		{
			if (entity.isSubmergedInWater())
			{
				InkOverlayCapability.get(livingEntity).setAmount(0);
			}
			else
			{
				InkOverlayCapability.get(livingEntity).addAmount(-0.01f);
			}
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