package net.splatcraft.neoforge;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.google.common.base.Joiner;
import dev.architectury.utils.Env;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;

import java.io.File;
import java.nio.file.Path;

public class SplatcraftConfigImpl
{
	private static final Joiner DOT_JOINER = Joiner.on(".");
	static ModConfigSpec clientConfig;
	static ModConfigSpec serverConfig;
	public static Path getModConfigPath()
	{
		return FMLPaths.CONFIGDIR.get();
	}
	public static void initializeConfigs()
	{
		ModConfigSpec.Builder configBuilder = new ModConfigSpec.Builder();
		
		Client.init(configBuilder);
		clientConfig = configBuilder.build();
		
		configBuilder = new ModConfigSpec.Builder();
		Server.init(configBuilder);
		serverConfig = configBuilder.build();
	}
	public static void loadConfig(Env env)
	{
		String path = SplatcraftConfig.getModConfigPathString(env);
		final CommentedFileConfig file = CommentedFileConfig.builder(new File(path)).sync().autosave().writingMode(WritingMode.REPLACE).build();
		
		file.load();
		ModConfigSpec config = env == Env.CLIENT ? clientConfig : serverConfig;
		config.correct(file);
		
		ModLoadingContext.get().getActiveContainer().registerConfig(env == Env.CLIENT ? ModConfig.Type.CLIENT : ModConfig.Type.SERVER, config);
	}
	public static <T> void registerSetting(ModConfigSpec.ConfigValue<T> configValue)
	{
		SplatcraftConfig.registerConfigAccessor(DOT_JOINER.join(configValue.getPath()), configValue, configValue::set);
	}
	public static class Server
	{
//        public static ModConfigSpec.BooleanValue limitFallSpeed;
		public static void init(ModConfigSpec.Builder server)
		{
			server.comment("Server Settings");
			registerSetting(server.comment("Specifies whether to limit the maximum fall speed of players").define("splatcraft.limitFallSpeed", false));
		}
	}
	public static class Client
	{
//        public static ModConfigSpec.EnumValue<SplatcraftKeyHandler.KeyMode> squidKeyMode;
//        public static ModConfigSpec.EnumValue<SplatcraftConfig.InkIndicator> inkIndicator;
//        public static ModConfigSpec.BooleanRule vanillaInkDurability;
//        public static ModConfigSpec.BooleanRule holdBarrierToRender;
//        public static ModConfigSpec.IntValue barrierRenderDistance;
//        public static ModConfigSpec.DoubleValue inkTankGuiScale;
//        public static ModConfigSpec.BooleanRule colorLock;
//        public static ModConfigSpec.BooleanRule makeShinier;
//        public static ModConfigSpec.EnumValue<SplatcraftConfig.PreventBobView> preventBobView;
//        public static ModConfigSpec.BooleanRule lowInkWarning;
//        public static ModConfigSpec.BooleanRule coloredPlayerNames;
//        public static String inkColoredSkinLayerPath = "config\\splatcraft\\player_ink_color.png";
//        public static File playerSkinInkColoredLayer;
		public static void init(ModConfigSpec.Builder client)
		{
			client.comment("Accessibility Settings");
			registerSetting(client.comment("Squid Key Mode").defineEnum("splatcraft.squidKeyMode", SplatcraftKeyHandler.KeyMode.TOGGLE));
			registerSetting(client.comment("Determines how the amount of ink left in your tank is visualized.").defineEnum("splatcraft.inkIndicator", SplatcraftConfig.InkIndicator.BOTH));
			registerSetting(client.comment("Determines whether the indicator that determines how much ink you have left matches vanilla durability colors instead of your ink color.")
				.define("splatcraft.vanillaInkDurabilityColor", false));
			registerSetting(client.comment("Prevents Stage Barriers from rendering in creative mode unless the player is holding one in their hand.")
				.define("splatcraft.holdBarrierToRender", true));
			registerSetting(client.comment("How far away stage barriers or voids will render away from you.")
				.defineInRange("splatcraft.barrierRenderDistance", 40, 4, 80));
			registerSetting(client.comment("Sets a static color for friendly and hostile colors").define("splatcraft.colorLock", false));
			registerSetting(client.comment("Makes projectiles colors more palid and more \"resistant\" to lightning changes").define("splatcraft.shinierConfig", false));
			registerSetting(client.comment("Prevents changing FOV when in Squid Mode").defineEnum("splatcraft.preventBobView", SplatcraftConfig.PreventBobView.OFF));
			registerSetting(client.comment("Determines whether the ink indicator near your crosshair warns you if your ink is low.")
				.define("splatcraft.lowInkWarning", true));
			registerSetting(client.comment("Determines whether instances of player names share the same color as their ink.")
				.define("splatcraft.coloredPlayerNames", true));
			registerSetting(client.comment("Specifies the scale of the GUI that is associated with the ink tank.").defineInRange("splatcraft.inkTankGuiScale", 1f, 0.1f, 10f));
		}
	}
}
