package net.splatcraft.registries.fabric;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import net.splatcraft.registries.SplatcraftItems;

public class SplatcraftItemGroupsImpl
{
    public static void addSplatcraftItemsToVanillaGroups()
    {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register((entries -> {
            entries.add(SplatcraftItems.splatSwitch.get());
            entries.add(SplatcraftItems.remotePedestal.get());
        }));
    }
}
