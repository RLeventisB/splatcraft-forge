package net.splatcraft.tileentities.container;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandlerContext;
import net.splatcraft.registries.SplatcraftTileEntities;

public class WeaponWorkbenchContainer extends PlayerInventoryContainer<WeaponWorkbenchContainer>
{
    public WeaponWorkbenchContainer(PlayerInventory player, ScreenHandlerContext callable, int id)
    {
        super(SplatcraftTileEntities.weaponWorkbenchContainer.get(), player, callable, 8, 144, id);
    }

    public WeaponWorkbenchContainer(int id, PlayerInventory playerInventory)
    {
        this(playerInventory, ScreenHandlerContext.EMPTY, id);
    }
}
