package net.splatcraft.items.remotes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkColor;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class InkDisruptorItem extends RemoteItem
{
	public InkDisruptorItem()
	{
		super(new Properties().stacksTo(1));
	}
	public static RemoteResult clearInk(Level world, BlockPos from, BlockPos to, boolean removePermanent)
	{
		if (!world.isInWorldBounds(from) || !world.isInWorldBounds(to))
			return createResult(false, Component.translatable("status.clear_ink.out_of_world"));
		
		AABB bounds = AABB.encapsulatingFullBlocks(from, to);
		AtomicInteger count = new AtomicInteger();
		int blockTotal = (int) (bounds.getXsize() * bounds.getYsize() * bounds.getZsize());
		
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
		
		return createResult(true, Component.translatable("status.clear_ink." + (count.get() > 0 ? "success" : "no_ink"), count)).setIntResults(count.get(), blockTotal == 0 ? 0 : count.get() * 15 / blockTotal);
	}
	@Override
	public RemoteResult onRemoteUse(Level world, BlockPos posA, BlockPos posB, ItemStack stack, InkColor colorIn, int mode, Collection<ServerPlayer> targets)
	{
		return clearInk(getLevel(world, stack), posA, posB, false);
	}
}