package net.splatcraft.items;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import org.jetbrains.annotations.NotNull;

public class PowerEggCanItem extends Item
{
    public PowerEggCanItem()
    {
        super(new Settings().maxCount(16));
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(World world, PlayerEntity player, @NotNull Hand handIn)
    {
        ItemStack itemstack = player.getStackInHand(handIn);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.powerEggCanOpen, SoundCategory.PLAYERS, 0.5F, 0.4F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!world.isClient())
        {
            double d0 = player.getEyeY() - (double) 0.3F;
            ItemEntity itementity = new ItemEntity(world, player.getX(), d0, player.getZ(), new ItemStack(SplatcraftItems.powerEgg.get(), (world.random.nextInt(4) + 1) * 10));
            itementity.resetPickupDelay();
            itementity.setThrower(player);

            float f = world.random.nextFloat() * 0.5F;
            float f1 = world.random.nextFloat() * ((float) Math.PI * 2F);
            itementity.setVelocity(-Math.sin(f1) * f, 0.2F, Math.cos(f1) * f);

            world.spawnEntity(itementity);
        }

        player.incrementStat(Stats.USED.getOrCreateStat(this));
        if (!player.isCreative())
        {
            itemstack.decrement(1);
        }

        return TypedActionResult.success(itemstack, world.isClient());
    }
}