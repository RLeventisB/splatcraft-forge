package net.splatcraft.forge.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.CommonUtils;

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
        if(CommonUtils.isResourceNameValid(value)) {
            ResourceLocation location = new ResourceLocation(value);
            if (isValidAlias(location)) {
                return getColorByAlias(location);
            }
        }

        try {
            if (value.charAt(0) == '#') {
                return Integer.parseInt(value.substring(1), 16);
            } else {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException ignored) {
        }

        return ColorUtils.DEFAULT;
    }

    public static List<ResourceLocation> getAliasesForColor(int color)
    {
        List<ResourceLocation> result = new ArrayList<>();
        MAP.forEach((alias, c) ->
        {
            if(c == color)
                result.add(alias);
        });

        return result;
    }

    public static Iterable<ResourceLocation> getAllAliases() {
        return MAP.keySet();
    }

    public static class Listener extends SimpleJsonResourceReloadListener
    {
        private static final Gson GSON_INSTANCE = Deserializers.createFunctionSerializer().create();
        private static final String folder = "ink_colors";

        public Listener() {
            super(GSON_INSTANCE, folder);
        }

        @Override
        protected Map<ResourceLocation, JsonElement> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler)
        {
            MAP.clear();
            return super.prepare(pResourceManager, pProfiler);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resourceList, ResourceManager resourceManagerIn, ProfilerFiller profilerIn)
        {
            resourceList.forEach((key, j) ->
            {

                JsonObject json = j.getAsJsonObject();
                int color = -1;
                if(json.has("value"))
                {
                    if(GsonHelper.isNumberValue(json))
                        color = json.getAsInt();
                    else
                    {
                        String str = json.getAsString();
                        if(str.indexOf('#') == 0)
                            color =  Integer.parseInt(str);
                        else
                        {
                            ResourceLocation loc = new ResourceLocation(str);
                            if(InkColorAliases.isValidAlias(loc))
                                color =  InkColorAliases.getColorByAlias(loc);
                        }
                    }

                    if (color >= 0 && color <= 0xFFFFFF) {
                        MAP.put(key, color);
                    }
                }
            });

        }
    }
}
