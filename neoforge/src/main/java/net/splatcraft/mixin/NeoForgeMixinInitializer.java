package net.splatcraft.mixin;

import net.neoforged.fml.loading.FMLLoader;
import net.splatcraft.platform.services.ModInfo;

import java.util.List;

public class NeoForgeMixinInitializer extends BaseMixinInitializer
{
	@Override
	public List<ModInfo> collectMods()
	{
		return FMLLoader.getLoadingModList().getMods().stream().map(NeoForgeMixinInitializer::createModInfo).toList();
	}
	private static ModInfo createModInfo(net.neoforged.fml.loading.moddiscovery.ModInfo modInfo)
	{
		return new ModInfo(
			modInfo.getModId(),
			modInfo.getVersion().toString(),
			modInfo.getDisplayName(),
			modInfo.getDescription()
		);
	}
}
