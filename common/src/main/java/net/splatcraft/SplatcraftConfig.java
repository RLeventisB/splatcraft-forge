package net.splatcraft;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.utils.Env;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SplatcraftConfig
{
	// todo: rework this entire class plz
	public static boolean loaded = false;
	static Map<String, Setting<?>> settingsMap = new HashMap<>();
	public static String getModConfigPathString(Env env)
	{
		return getModConfigPath().resolve(Splatcraft.MODID + (env == Env.CLIENT ? "-client" : "-server") + ".toml").toString();
	}
	public static void initializeSettingsMap()
	{
		// server settings
		settingsMap.put("limitFallSpeed", new Setting<>(Boolean.class));
		
		// client settings
		settingsMap.put("squidKeyMode", new Setting<>(SplatcraftKeyHandler.KeyMode.class));
		settingsMap.put("inkIndicator", new Setting<>(InkIndicator.class));
		settingsMap.put("vanillaInkDurability", new Setting<>(Boolean.class));
		settingsMap.put("holdBarrierToRender", new Setting<>(Boolean.class));
		settingsMap.put("barrierRenderDistance", new Setting<>(Integer.class));
		settingsMap.put("colorLock", new Setting<>(Double.class));
		settingsMap.put("makeShinier", new Setting<>(Boolean.class));
		settingsMap.put("preventBobView", new Setting<>(Boolean.class));
		settingsMap.put("lowInkWarning", new Setting<>(PreventBobView.class));
		settingsMap.put("coloredPlayerNames", new Setting<>(Boolean.class));
		settingsMap.put("inkTankGuiScale", new Setting<>(Boolean.class));
	}
	@ExpectPlatform
	public static Path getModConfigPath()
	{
		throw new AssertionError();
	}
	@ExpectPlatform
	public static void loadConfig()
	{
		throw new AssertionError();
	}
	@ExpectPlatform
	public static void initializeConfigs()
	{
		throw new AssertionError();
	}
	public static <T> void registerConfigAccessor(String name, Supplier<T> getter, Consumer<T> setter)
	{
		if (!settingsMap.containsKey(name))
		{
			throw new IllegalArgumentException("No setting named " + name + "exists");
		}
		Setting<?> setting = settingsMap.get(name);
		if (setter == null || getter == null)
		{
			throw new IllegalArgumentException("Getter or setter is null.");
		}
		
		Setting<T> castedSetting = (Setting<T>) setting;
		if (!setting.isIncomplete())
			throw new IllegalStateException("Setting is already complete");
		
		castedSetting.setGetter(getter);
		castedSetting.setSetter(setter);
	}
	public static <T> T get(String name)
	{
		if (!settingsMap.containsKey(name))
		{
			throw new IllegalArgumentException("No setting named " + name + " exists");
		}
		
		Setting<T> setting = (Setting<T>) settingsMap.get(name);
		return setting.get();
	}
	public static void initialize()
	{
		initializeSettingsMap();
		initializeConfigs();
		
		loadConfig();
	}
	public enum InkIndicator
	{
		CROSSHAIR,
		DURABILITY,
		BOTH,
		NONE
	}
	public enum PreventBobView
	{
		SUBMERGED,
		ALWAYS,
		OFF
	}
	public static final class Setting<T>
	{
		@NotNull
		private final Class<T> settingType;
		private Supplier<T> getter;
		private Consumer<T> setter;
		public Setting(Class<T> settingType)
		{
			this(null, null, settingType);
		}
		public Setting(Supplier<T> getter, Consumer<T> setter, @NotNull Class<T> settingType)
		{
			this.getter = getter;
			this.setter = setter;
			this.settingType = settingType;
		}
		public boolean isIncomplete()
		{
			return getter == null && setter == null;
		}
		public void setGetter(Supplier<T> getter)
		{
			if (setter != null)
				return;
			
			this.getter = getter;
		}
		public void setSetter(Consumer<T> setter)
		{
			if (this.setter != null)
				return;
			
			this.setter = setter;
		}
		public T get()
		{
			return getter.get();
		}
		public void set(T value)
		{
			setter.accept(value);
		}
		public Class<T> settingType()
		{
			return settingType;
		}
		@Override
		public String toString()
		{
			return "Setting[" +
				"getter=" + getter + ", " +
				"setter=" + setter + ", " +
				"settingType=" + settingType + ']';
		}
		@Override
		public boolean equals(Object o)
		{
			if (this == o) return true;
			if (!(o instanceof Setting<?> setting)) return false;
			return Objects.equals(getter, setting.getter) && Objects.equals(setter, setting.setter) && Objects.equals(settingType, setting.settingType);
		}
		@Override
		public int hashCode()
		{
			return Objects.hash(getter, setter, settingType);
		}
	}
}
