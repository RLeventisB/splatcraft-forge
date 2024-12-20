package net.splatcraft.items.remotes;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkColor;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class InkDisruptorItem extends RemoteItem
{
    public InkDisruptorItem()
    {
        super(new Settings().maxCount(1));
    }

    public static RemoteResult clearInk(World world, BlockPos from, BlockPos to, boolean removePermanent)
    {
        if (!world.isInBuildLimit(from) || !world.isInBuildLimit(to))
            return createResult(false, Text.translatable("status.clear_ink.out_of_world"));

        /*
        for (int j = blockpos2.getZ(); j <= blockpos3.getZ(); j += 16)
        {
            for (int k = blockpos2.getX(); k <= blockpos3.getX(); k += 16)
            {
                if (!level.isLoaded(new BlockPos(k, blockpos3.getY() - blockpos2.getY(), j)))
                {
                    return createResult(false, Text.translatable("status.clear_ink.out_of_world"));
                }
            }
        }
        */
        Box bounds = Box.enclosing(from, to).stretch(1, 1, 1);
        AtomicInteger count = new AtomicInteger();
        int blockTotal = (int) (bounds.getLengthX() * bounds.getLengthY() * bounds.getLengthZ());

        InkBlockUtils.forEachInkedBlockInBounds(world, bounds, ((pos, ink) ->
        {
            if (InkBlockUtils.clearBlock(world, pos, removePermanent))
                count.incrementAndGet();
        }));
        ColorUtils.forEachColoredBlockInBounds(world, bounds, ((pos, coloredBlock, blockEntity) ->
        {
            if (coloredBlock.remoteInkClear(world, pos))
                count.incrementAndGet();
        }));

        return createResult(true, Text.translatable("status.clear_ink." + (count.get() > 0 ? "success" : "no_ink"), count)).setIntResults(count.get(), blockTotal == 0 ? 0 : count.get() * 15 / blockTotal);
    }

    @Override
    public RemoteResult onRemoteUse(World world, BlockPos posA, BlockPos posB, ItemStack stack, InkColor colorIn, int mode, Collection<ServerPlayerEntity> targets)
    {
        return clearInk(getLevel(world, stack), posA, posB, false);
    }
}