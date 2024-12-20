package net.splatcraft.registries.neoforge;

import dev.architectury.registry.CreativeTabRegistry;
import net.minecraft.item.ItemGroups;
import net.splatcraft.registries.SplatcraftItems;

public class SplatcraftItemGroupsImpl
{
    public static void addSplatcraftItemsToVanillaGroups()
    {
        CreativeTabRegistry.append(ItemGroups.REDSTONE, SplatcraftItems.splatSwitch.get());
        CreativeTabRegistry.append(ItemGroups.REDSTONE, SplatcraftItems.remotePedestal.get());
    }
}
