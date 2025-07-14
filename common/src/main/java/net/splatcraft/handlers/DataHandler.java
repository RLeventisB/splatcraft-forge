package net.splatcraft.handlers;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.InkColorGroups;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.items.weapons.settings.*;
import net.splatcraft.platform.Services;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataHandler
{
	public static final WeaponStatsListener WEAPON_STATS_LISTENER = new WeaponStatsListener();
	public static final InkColorGroups.Listener INK_COLOR_TAGS_LISTENER = new InkColorGroups.Listener();
	public static final InkColorRegistry.Listener INK_COLOR_ALIASES_LISTENER = new InkColorRegistry.Listener();
	public static void addReloadListeners()
	{
		Services.PLATFORM.registerReloadListener(PackType.SERVER_DATA, WEAPON_STATS_LISTENER);
		Services.PLATFORM.registerReloadListener(PackType.SERVER_DATA, INK_COLOR_TAGS_LISTENER);
		Services.PLATFORM.registerReloadListener(PackType.SERVER_DATA, INK_COLOR_ALIASES_LISTENER);
	}
	public static class WeaponStatsListener extends SimpleJsonResourceReloadListener
	{
		public static final HashMap<String, Class<? extends AbstractWeaponSettings<?, ?>>> SETTING_TYPES = new HashMap<>()
		{{
			put(Splatcraft.MODID + ":shooter", ShooterWeaponSettings.class);
			put(Splatcraft.MODID + ":blaster", BlasterWeaponSettings.class);
			put(Splatcraft.MODID + ":roller", RollerWeaponSettings.class);
			put(Splatcraft.MODID + ":charger", ChargerWeaponSettings.class);
			put(Splatcraft.MODID + ":slosher", SlosherWeaponSettings.class);
			put(Splatcraft.MODID + ":dualie", DualieWeaponSettings.class);
			put(Splatcraft.MODID + ":splatling", SplatlingWeaponSettings.CLASS);
			try
			{
				put(Splatcraft.MODID + ":sub_weapon", (Class<? extends AbstractWeaponSettings<?, ?>>) Class.forName("net.splatcraft.items.weapons.settings.SubWeaponSettings"));
				put(Splatcraft.MODID + ":special_weapon", (Class<? extends AbstractWeaponSettings<?, ?>>) Class.forName("net.splatcraft.items.weapons.settings.SpecialWeaponSettings"));
			}
			catch (ClassNotFoundException ignored)
			{
			}
		}};
		public static final BiMap<ResourceLocation, AbstractWeaponSettings<?, ?>> SETTINGS = HashBiMap.create();
		public static final CommonUtils.ReseteableMemoizedFunction<Class<? extends AbstractWeaponSettings<?, ?>>, List<ResourceLocation>> CLASS_SETTINGS_MAP
			= CommonUtils.memoizeResetable((Class<? extends AbstractWeaponSettings<?, ?>> clazz) -> SETTINGS.entrySet().stream().filter(v -> clazz.isInstance(v.getValue())).map(Map.Entry::getKey).toList());
		private static final Gson GSON_INSTANCE = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		private static final String folder = "weapon_settings";
		public WeaponStatsListener()
		{
			super(GSON_INSTANCE, folder);
		}
		public static List<ResourceLocation> getSettingsForClass(Class<? extends AbstractWeaponSettings<?, ?>> clazz)
		{
			return CLASS_SETTINGS_MAP.apply(clazz);
		}
		@Override
		protected void apply(Map<ResourceLocation, JsonElement> resourceList, @NotNull ResourceManager manager, @NotNull ProfilerFiller profilerIn)
		{
			CLASS_SETTINGS_MAP.reset();
			SETTINGS.clear();
			
			resourceList.forEach((key, element) ->
			{
				JsonObject json = element.getAsJsonObject();
				try
				{
					String type = GsonHelper.getAsString(json, "type");
					
					if (!SETTING_TYPES.containsKey(type))
						return;
					
					AbstractWeaponSettings<?, ?> settings = SETTING_TYPES.get(type).getConstructor(String.class).newInstance(key.toString());
					settings.deserialize(key, json);
					
					settings.registerStatTooltips();
					SETTINGS.put(key, settings);
				}
				catch (InstantiationException | IllegalAccessException | InvocationTargetException |
				       NoSuchMethodException e)
				{
					throw new RuntimeException(e);
				}
			});
		}
	}
}
