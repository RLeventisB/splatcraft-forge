package net.splatcraft.platform;

import net.minecraft.network.RegistryFriendlyByteBuf;

public interface IExtraDataOnAddEntity
{
	void readExtraData(RegistryFriendlyByteBuf buf);
	void writeExtraData(RegistryFriendlyByteBuf buf);
}
