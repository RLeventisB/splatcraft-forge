package net.splatcraft.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.InkColorGroups;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.items.weapons.settings.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class DataHandler
{
    public static final WeaponStatsListener WEAPON_STATS_LISTENER = new WeaponStatsListener();
    public static final InkColorGroups.Listener INK_COLOR_TAGS_LISTENER = new InkColorGroups.Listener();
    public static final InkColorRegistry.Listener INK_COLOR_ALIASES_LISTENER = new InkColorRegistry.Listener();

    public static void addReloadListeners()
    {
        ReloadListenerRegistry.register(ResourceType.SERVER_DATA, WEAPON_STATS_LISTENER);
        ReloadListenerRegistry.register(ResourceType.SERVER_DATA, INK_COLOR_TAGS_LISTENER);
        ReloadListenerRegistry.register(ResourceType.SERVER_DATA, INK_COLOR_ALIASES_LISTENER);
    }

    public static class WeaponStatsListener extends JsonDataLoader
    {
        public static final HashMap<String, Class<? extends AbstractWeaponSettings<?, ?>>> SETTING_TYPES = new HashMap<>()
        {{
            put(Splatcraft.MODID + ":shooter", ShooterWeaponSettings.class);
            put(Splatcraft.MODID + ":blaster", BlasterWeaponSettings.class);
            put(Splatcraft.MODID + ":roller", RollerWeaponSettings.class);
            put(Splatcraft.MODID + ":charger", ChargerWeaponSettings.class);
            put(Splatcraft.MODID + ":slosher", SlosherWeaponSettings.class);
            put(Splatcraft.MODID + ":dualie", DualieWeaponSettings.class);
            put(Splatcraft.MODID + ":splatling", SplatlingWeaponSettings.class);
            put(Splatcraft.MODID + ":sub_weapon", SubWeaponSettings.class);
        }}; //TODO make better registry probably
        public static final HashMap<Identifier, AbstractWeaponSettings<?, ?>> SETTINGS = new HashMap<>();
        private static final Gson GSON_INSTANCE = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        private static final String folder = "weapon_settings";

        public WeaponStatsListener()
        {
            super(GSON_INSTANCE, folder);
        }

        @Override
        protected void apply(Map<Identifier, JsonElement> resourceList, @NotNull ResourceManager manager, @NotNull Profiler profilerIn)
        {
            SETTINGS.clear();

            resourceList.forEach((key, element) ->
            {
                JsonObject json = element.getAsJsonObject();
                try
                {
                    String type = JsonHelper.getString(json, "type");

                    if (!SETTING_TYPES.containsKey(type))
                        return;

                    AbstractWeaponSettings<?, ?> settings = SETTING_TYPES.get(type).getConstructor(String.class).newInstance(key.toString());
                    settings.getCodec().parse(JsonOps.INSTANCE, json).resultOrPartial(msg -> Splatcraft.LOGGER.error("Failed to load weapon settings for %s: %s".formatted(key, msg))).ifPresent(
                        settings::castAndDeserialize
                    );

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
