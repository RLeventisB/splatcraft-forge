package net.splatcraft.dummys;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface ISplatcraftForgeBlockEntityDummy
{
	default BlockEntity self()
	{
		return (BlockEntity) this;
	}
	default void phOnDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider)
	{
		CompoundTag compoundtag = pkt.getTag();
		if (!compoundtag.isEmpty())
		{
			self().loadWithComponents(compoundtag, lookupProvider);
		}
	}
	default void phHandleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider)
	{
		self().loadWithComponents(tag, lookupProvider);
	}
}
