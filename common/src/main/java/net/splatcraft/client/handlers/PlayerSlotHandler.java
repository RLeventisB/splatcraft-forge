package net.splatcraft.client.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.util.action.EntityAction;

import java.util.Optional;

public class PlayerSlotHandler
{
	public static Optional<Integer> queuedSlot = Optional.empty();
	public static int slotToAssign(Player player)
	{
		if (player != null && !player.isSpectator())
		{
			Optional<EntityAction> action = EntityAction.getEntityActionOptional(player);
			ItemStack selected = player.getInventory().getSelected();
			if (action.isPresent() && action.get().getItemSlot() instanceof EntitySlot.PlayerInventorySlot slot)
			{
				return slot.getSlotIndex();
			}
			else if (selected.getItem() instanceof WeaponBaseItem<?> weaponBaseItem && weaponBaseItem.preventsChanging(selected, player))
			{
				return player.getInventory().selected;
			}
		}
		return -1;
	}
	public static void reassignSlot(Minecraft client)
	{
		Player player = client.player;
		int slot = slotToAssign(player);
		if (slot == -1 && queuedSlot.isPresent())
		{
			slot = queuedSlot.get();
			queuedSlot = Optional.empty();
		}

		if (slot != -1)
		{
			player.getInventory().selected = slot;
		}
	}
	public static void enqueueSlot(int value)
	{
		queuedSlot = Optional.of(value);
	}
}
