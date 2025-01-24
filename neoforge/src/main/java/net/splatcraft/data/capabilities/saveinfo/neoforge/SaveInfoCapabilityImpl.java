package net.splatcraft.data.capabilities.saveinfo.neoforge;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.architectury.utils.GameInstance;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.neoforge.SplatcraftNeoForgeDataAttachments;
import org.apache.commons.lang3.NotImplementedException;

public class SaveInfoCapabilityImpl
{
	public static SaveInfo get()
	{
		if (Platform.getEnvironment() == Env.CLIENT && !GameInstance.getClient().isIntegratedServerRunning())
			return SaveInfoCapability.clientSaveInfo;
		return GameInstance.getServer().getOverworld().getData(SplatcraftNeoForgeDataAttachments.SAVE_INFO);
	}
	public static void set(SaveInfo newData)
	{
		if (Platform.getEnvironment() == Env.CLIENT && !GameInstance.getClient().isIntegratedServerRunning())
			throw new NotImplementedException("SaveInfo cannot be set on the client");
		GameInstance.getServer().getOverworld().setData(SplatcraftNeoForgeDataAttachments.SAVE_INFO, newData);
	}
	public static void markUpdated()
	{
	}
}
