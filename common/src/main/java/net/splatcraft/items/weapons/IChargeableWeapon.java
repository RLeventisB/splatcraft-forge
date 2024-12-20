package net.splatcraft.items.weapons;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface IChargeableWeapon
{
    int getDischargeTicks(ItemStack stack);

    int getDecayTicks(ItemStack stack);

    void onReleaseCharge(World world, PlayerEntity player, ItemStack stack, float charge);
}
