package net.splatcraft.forge.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.data.InkColorAliases;
import net.splatcraft.forge.data.InkColorTags;
import net.splatcraft.forge.items.weapons.settings.*;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.s2c.UpdateWeaponSettingsPacket;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class DataHandler
{
    public static final WeaponStatsListener WEAPON_STATS_LISTENER = new WeaponStatsListener();
    public static final InkColorTags.Listener INK_COLOR_TAGS_LISTENER = new InkColorTags.Listener();
    public static final InkColorAliases.Listener INK_COLOR_ALIASES_LISTENER = new InkColorAliases.Listener();

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event)
    {
        event.addListener(WEAPON_STATS_LISTENER);
        event.addListener(INK_COLOR_TAGS_LISTENER);
        event.addListener(INK_COLOR_ALIASES_LISTENER);
    }

    @SubscribeEvent
    public static void onDataSync(OnDatapackSyncEvent event)
    {
        SplatcraftPacketHandler.sendToAll(new UpdateWeaponSettingsPacket());
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
            put(Splatcraft.MODID + ":splatling", SplatlingWeaponSettings.class);
            put(Splatcraft.MODID + ":sub_weapon", SubWeaponSettings.class);
        }}; //TODO make better registry probably
        public static final HashMap<ResourceLocation, AbstractWeaponSettings<?, ?>> SETTINGS = new HashMap<>();
        private static final Gson GSON_INSTANCE = Deserializers.createFunctionSerializer().create();
        private static final String folder = "weapon_settings";

        public WeaponStatsListener()
        {
            super(GSON_INSTANCE, folder);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resourceList, @NotNull ResourceManager resourceManagerIn, @NotNull ProfilerFiller profilerIn)
        {
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
