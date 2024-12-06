package net.splatcraft.neoforge;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.splatcraft.Splatcraft;
import net.splatcraft.SplatcraftConfig;

@Mod(Splatcraft.MODID)
public final class SplatcraftNeoForge
{
	public SplatcraftNeoForge()
	{
		// Run our common setup.
		Splatcraft.init();
		
		if (FMLLoader.getDist().isClient())
		{
			Splatcraft.initClient();
		}
		else if (FMLLoader.getDist().isDedicatedServer())
		{
			Splatcraft.initServer();
		}
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SplatcraftConfig.clientConfig);
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SplatcraftConfig.serverConfig);
		SplatcraftConfig.loadConfig(SplatcraftConfig.clientConfig, FMLPaths.CONFIGDIR.get().resolve(Splatcraft.MODID + "-client.toml").toString());
		SplatcraftConfig.loadConfig(SplatcraftConfig.serverConfig, FMLPaths.CONFIGDIR.get().resolve(Splatcraft.MODID + "-server.toml").toString());
	}
}
