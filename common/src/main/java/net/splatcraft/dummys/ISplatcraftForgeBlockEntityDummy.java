package net.splatcraft.dummys;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;

public interface ISplatcraftForgeBlockEntityDummy
{
	default BlockEntity self()
	{
		return (BlockEntity) this;
	}
	default void phOnDataPacket(ClientConnection net, BlockEntityUpdateS2CPacket pkt, RegistryWrapper.WrapperLookup lookupProvider)
	{
		NbtCompound compoundtag = pkt.getNbt();
		if (!compoundtag.isEmpty())
		{
			self().read(compoundtag, lookupProvider);
		}
	}
	default void phHandleUpdateTag(NbtCompound tag, RegistryWrapper.WrapperLookup lookupProvider)
	{
		self().read(tag, lookupProvider);
	}
}
