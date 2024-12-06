package net.splatcraft.forge.items.remotes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.InkBlockUtils;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class InkDisruptorItem extends RemoteItem
{
    public InkDisruptorItem()
    {
        super(new Properties().stacksTo(1));
    }

    public static RemoteResult clearInk(Level level, BlockPos from, BlockPos to, boolean removePermanent)
    {
        if (!level.isInWorldBounds(from) || !level.isInWorldBounds(to))
            return createResult(false, Component.translatable("status.clear_ink.out_of_world"));

        /*
        for (int j = blockpos2.getZ(); j <= blockpos3.getZ(); j += 16)
        {
            for (int k = blockpos2.getX(); k <= blockpos3.getX(); k += 16)
            {
                if (!level.isLoaded(new BlockPos(k, blockpos3.getY() - blockpos2.getY(), j)))
                {
                    return createResult(false, Component.translatable("status.clear_ink.out_of_world"));
                }
            }
        }
        */
        AABB bounds = new AABB(from, to).expandTowards(1, 1, 1);
        AtomicInteger count = new AtomicInteger();
        int blockTotal = (int) (bounds.getXsize() * bounds.getYsize() * bounds.getZsize());

        InkBlockUtils.forEachInkedBlockInBounds(level, bounds, ((pos, ink) ->
        {
            if (InkBlockUtils.clearBlock(level, pos, removePermanent))
                count.incrementAndGet();
        }));
        ColorUtils.forEachColoredBlockInBounds(level, bounds, ((pos, coloredBlock, blockEntity) ->
        {
            if (coloredBlock.remoteInkClear(level, pos))
                count.incrementAndGet();
        }));

        return createResult(true, Component.translatable("status.clear_ink." + (count.get() > 0 ? "success" : "no_ink"), count)).setIntResults(count.get(), blockTotal == 0 ? 0 : count.get() * 15 / blockTotal);
    }

    @Override
    public RemoteResult onRemoteUse(Level usedOnWorld, BlockPos posA, BlockPos posB, ItemStack stack, int colorIn, int mode, Collection<ServerPlayer> targets)
    {
        return clearInk(getLevel(usedOnWorld, stack), posA, posB, false);
    }
}