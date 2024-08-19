package net.splatcraft.forge.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InkColorAliases
{
    private static final HashMap<ResourceLocation, Integer> MAP = new HashMap<>();

    public static int getColorByAlias(ResourceLocation location)
    {
        return MAP.getOrDefault(location, ColorUtils.DEFAULT);
    }

    public static boolean isValidAlias(ResourceLocation location)
    {
        return MAP.containsKey(location);
    }

    public static int getColorByAliasOrHex(String value)
    {
        if (CommonUtils.isResourceNameValid(value))
        {
            ResourceLocation location = new ResourceLocation(value);
            if (isValidAlias(location))
                return getColorByAlias(location);
        }
        try
        {
            if (value.charAt(0) == '#')
                return Integer.parseInt(value.substring(1), 16);
            else
                return Integer.parseInt(value);
        }
        catch (NumberFormatException ignored)
        {
        }
        return ColorUtils.DEFAULT;
    }

    public static List<ResourceLocation> getAliasesForColor(int color)
    {
        List<ResourceLocation> result = new ArrayList<>();
        MAP.forEach((alias, c) ->
        {
            if (c == color)
                result.add(alias);
        });

        return result;
    }

    public static ResourceLocation getFirstAliasForColor(int color)
    {
        for (Map.Entry<ResourceLocation, Integer> entry : MAP.entrySet())
        {
            ResourceLocation alias = entry.getKey();
            Integer c = entry.getValue();
            if (c == color)
            {
                return alias;
            }
        }
        return null;
    }

    public static Iterable<ResourceLocation> getAllAliases()
    {
        return MAP.keySet();
    }

    public static class Listener extends SimpleJsonResourceReloadListener
    {
        private static final Gson GSON_INSTANCE = Deserializers.createFunctionSerializer().create();
        private static final String folder = "ink_colors";

        public Listener()
        {
            super(GSON_INSTANCE, folder);
        }

        @Override
        protected @NotNull Map<ResourceLocation, JsonElement> prepare(@NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler)
        {
            MAP.clear();
            return super.prepare(pResourceManager, pProfiler);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resourceList, @NotNull ResourceManager resourceManagerIn, @NotNull ProfilerFiller profilerIn)
        {
            for (Map.Entry<ResourceLocation, JsonElement> entry : resourceList.entrySet())
            {
                ResourceLocation key = entry.getKey();
                JsonElement j = entry.getValue();
                JsonObject json = j.getAsJsonObject();
                if (json.has("value"))
                {
                    JsonElement numberValue = json.get("value");
                    String str = numberValue.getAsString();
                    int color = Integer.decode(str);
                    if (color == -1)
                    {
//                        ResourceLocation loc = new ResourceLocation(key); ??????
                        if (InkColorAliases.isValidAlias(key))
                        {
                            color = InkColorAliases.getColorByAlias(key);
                        }
                    }
                    if (color >= 0 && color <= 0xFFFFFF)
                    {
                        MAP.put(key, color);
                    }
                }
            }
            InkColorTags.Listener.doLoad();
        }
    }
}
