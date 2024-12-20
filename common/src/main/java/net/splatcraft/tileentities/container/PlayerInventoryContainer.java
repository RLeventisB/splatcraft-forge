package net.splatcraft.tileentities.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.splatcraft.registries.SplatcraftBlocks;
import org.jetbrains.annotations.NotNull;

public abstract class PlayerInventoryContainer<T extends PlayerInventoryContainer<?>> extends ScreenHandler
{
    protected final ScreenHandlerContext levelPosCallable;
    int xPos;
    int yPos;

    public PlayerInventoryContainer(ScreenHandlerType<T> containerType, PlayerInventory player, ScreenHandlerContext levelPosCallable, int invX, int invY, int id)
    {
        super(containerType, id);
        this.levelPosCallable = levelPosCallable;
        xPos = invX;
        yPos = invY;

        for (int xx = 0; xx < 9; xx++)
        {
            for (int yy = 0; yy < 3; yy++)
            {
                addSlot(new Slot(player, xx + yy * 9 + 9, xPos + xx * 18, yPos + yy * 18));
            }
        }

        for (int xx = 0; xx < 9; xx++)
        {
            addSlot(new Slot(player, xx, xPos + xx * 18, yPos + 58));
        }
    }

    @Override
    public boolean canUse(@NotNull PlayerEntity playerIn)
    {
        return canUse(levelPosCallable, playerIn, SplatcraftBlocks.weaponWorkbench.get());
    }

    @Override
    public @NotNull ItemStack quickMove(@NotNull PlayerEntity playerIn, int index)
    {
        Slot slot = slots.get(index);
        ItemStack stack = slot.getStack();

        if (!slot.hasStack())
        {
            return ItemStack.EMPTY;
        }

        if (index < slots.size() - 9)
        {
            if (!insertItem(stack, slots.size() - 9, slots.size(), true))
            {
                return ItemStack.EMPTY;
            }
        }
        else
        {
            if (!insertItem(stack, 0, slots.size() - 9, false))
            {
                return ItemStack.EMPTY;
            }
        }
        return ItemStack.EMPTY;
    }
}
