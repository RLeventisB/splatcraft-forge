package net.splatcraft.forge.items.remotes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.forge.registries.SplatcraftItemGroups;
import net.splatcraft.forge.util.InkBlockUtils;

import java.util.Collection;

public class InkDisruptorItem extends RemoteItem
{
	public InkDisruptorItem()
	{
		super(new Properties().stacksTo(1));
		SplatcraftItemGroups.addGeneralItem(this);
	}
	@Override
	public RemoteResult onRemoteUse(Level usedOnWorld, BlockPos posA, BlockPos posB, ItemStack stack, int colorIn, int mode, Collection<ServerPlayer> targets)
	{
		return clearInk(getLevel(usedOnWorld, stack), posA, posB, false);
	}
	public static RemoteResult clearInk(Level level, BlockPos posA, BlockPos posB, boolean removePermanent)
	{
		BlockPos minPos = new BlockPos(Math.min(posA.getX(), posB.getX()), Math.min(posB.getY(), posA.getY()), Math.min(posA.getZ(), posB.getZ()));
		BlockPos maxPos = new BlockPos(Math.max(posA.getX(), posB.getX()), Math.max(posB.getY(), posA.getY()), Math.max(posA.getZ(), posB.getZ()));
		
		if (!level.isInWorldBounds(minPos) || !level.isInWorldBounds(maxPos))
			return createResult(false, Component.translatable("status.clear_ink.out_of_stage"));
		
		int count = 0;
		int blocksTotal = 0;
		for (int x = minPos.getX(); x <= maxPos.getX(); x++)
		{
			for (int y = minPos.getY(); y <= maxPos.getY(); y++)
			{
				for (int z = minPos.getZ(); z <= maxPos.getZ(); z++)
				{
					BlockPos pos = new BlockPos(x, y, z);
					
					if (InkBlockUtils.clearBlock(level, pos, removePermanent))
					{
						count++;
					}
					blocksTotal++;
				}
			}
		}
		
		return createResult(true, Component.translatable("status.clear_ink." + (count > 0 ? "success" : "no_ink"), count)).setIntResults(count, count * 15 / blocksTotal);
	}
}
