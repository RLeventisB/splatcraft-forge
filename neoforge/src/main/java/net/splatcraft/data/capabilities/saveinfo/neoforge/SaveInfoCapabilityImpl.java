package net.splatcraft.data.capabilities.saveinfo.neoforge;

import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.neoforge.SplatcraftNeoForgeDataAttachments;

public class SaveInfoCapabilityImpl
{
	public static SaveInfo get()
	{
		return ServerLifecycleHooks.getCurrentServer().getOverworld().getData(SplatcraftNeoForgeDataAttachments.SAVE_INFO);
	}
	public static void set(SaveInfo newData)
	{
		ServerLifecycleHooks.getCurrentServer().getOverworld().setData(SplatcraftNeoForgeDataAttachments.SAVE_INFO, newData);
	}
	public static void markUpdated()
	{
	}
}
