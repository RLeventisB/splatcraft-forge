package net.splatcraft.forge.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.util.ColorUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.splatcraft.forge.data.InkColorTags.Listener.getOrCreateTag;

public class InkColorTags
{
    public static final InkColorTags STARTER_COLORS = getOrCreateTag(new ResourceLocation(Splatcraft.MODID, "starter_colors"));
    public static final InkColorTags INK_VAT_DEFAULT = getOrCreateTag(new ResourceLocation(Splatcraft.MODID, "ink_vat_default"));
    public static final InkColorTags CLASSIC = getOrCreateTag(new ResourceLocation(Splatcraft.MODID, "classic"));
    public static final InkColorTags PASTEL = getOrCreateTag(new ResourceLocation(Splatcraft.MODID, "pastel"));
    public static final InkColorTags NEON = getOrCreateTag(new ResourceLocation(Splatcraft.MODID, "neon"));
    public static final InkColorTags OVERGROWN = getOrCreateTag(new ResourceLocation(Splatcraft.MODID, "overgrown"));
    public static final InkColorTags MIDNIGHT = getOrCreateTag(new ResourceLocation(Splatcraft.MODID, "midnight"));
    public static final InkColorTags ENCHANTED = getOrCreateTag(new ResourceLocation(Splatcraft.MODID, "enchanted"));
    public static final InkColorTags CREATIVE_TAB_COLORS = getOrCreateTag(new ResourceLocation(Splatcraft.MODID, "creative_tab_colors"));

    private final List<Integer> list;

    public InkColorTags(List<Integer> list)
    {
        this.list = list;
    }

    public void clear()
    {
        list.clear();
    }

    public void addAll(Collection<Integer> values)
    {
        list.addAll(values);
    }

    public int getRandom(Random random)
    {
        return list.isEmpty() ? ColorUtils.DEFAULT : list.get(random.nextInt(list.size()));
    }

    public List<Integer> getAll()
    {
        return new ArrayList<>(list);
    }

    public static class Listener extends SimpleJsonResourceReloadListener
    {
        private static final HashMap<ResourceLocation, InkColorTags> REGISTRY = new HashMap<>();

        private static final Gson GSON_INSTANCE = Deserializers.createFunctionSerializer().create();
        private static final String folder = "tags/ink_colors";

        public Listener()
        {
            super(GSON_INSTANCE, folder);
        }

        public static InkColorTags getOrCreateTag(ResourceLocation name)
        {
            if (REGISTRY.containsKey(name))
                return REGISTRY.get(name);

            InkColorTags result = new InkColorTags(new ArrayList<>());
            REGISTRY.put(name, result);

            return result;
        }

        @Override
        protected @NotNull Map<ResourceLocation, JsonElement> prepare(@NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler)
        {
            REGISTRY.clear();
            return super.prepare(pResourceManager, pProfiler);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resourceList, @NotNull ResourceManager resourceManagerIn, @NotNull ProfilerFiller profilerIn)
        {
            List<Map.Entry<ResourceLocation, JsonElement>> entries = new ArrayList<>();
            List<Map.Entry<ResourceLocation, JsonElement>> entriesWithReferenceToAnotherTag = new ArrayList<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : resourceList.entrySet())
            {
                ResourceLocation key = entry.getKey();
                JsonElement j = entry.getValue();
                JsonObject json = j.getAsJsonObject();
                if (json.has("values"))
                {
                    if (hasReferenceToAnotherTag(json))
                    {
                        entriesWithReferenceToAnotherTag.add(entry);
                    }
                    else
                    {
                        entries.add(entry);
                    }
                }
                else
                {
                    resourceList.remove(key);
                }
            }
            for (Map.Entry<ResourceLocation, JsonElement> entry : entries)
            {
                ResourceLocation key = entry.getKey();
                JsonElement j = entry.getValue();
                InkColorTags tag = getOrCreateTag(key);
                JsonObject json = j.getAsJsonObject();
                if (GsonHelper.getAsBoolean(json, "replace", false))
                {
                    tag.clear();
                }
                ArrayList<Integer> newColors = new ArrayList<>();
                for (JsonElement jsonElement : GsonHelper.getAsJsonArray(json, "values"))
                {
                    ResourceLocation loc = new ResourceLocation(jsonElement.getAsString());
                    if (InkColorAliases.isValidAlias(loc))
                    {
                        newColors.add(InkColorAliases.getColorByAlias(loc));
                    }
                }
                newColors.removeIf(i -> i < 0 || i > 0xFFFFFF);
                tag.addAll(newColors);
            }
            for (Map.Entry<ResourceLocation, JsonElement> entry : entriesWithReferenceToAnotherTag)
            {
                ResourceLocation key = entry.getKey();
                JsonElement j = entry.getValue();
                InkColorTags tag = getOrCreateTag(key);
                JsonObject json = j.getAsJsonObject();

                if (GsonHelper.getAsBoolean(json, "replace", false))
                    tag.clear();

                ArrayList<Integer> newColors = new ArrayList<>();

                for (JsonElement jsonElement : GsonHelper.getAsJsonArray(json, "values"))
                {
                    String str = jsonElement.getAsString();
                    if (str.indexOf('#') == 0 && str.contains(":") && REGISTRY.containsKey(new ResourceLocation(str.substring(1))))
                    {
                        for (Integer color : REGISTRY.get(new ResourceLocation(str.substring(1))).getAll())
                        {
                            if (!newColors.contains(color))
                                newColors.add(color);
                        }
                    }
                    else
                    {
                        try
                        {
                            ResourceLocation loc = new ResourceLocation(str);
                            if (InkColorAliases.isValidAlias(loc))
                                newColors.add(InkColorAliases.getColorByAlias(loc));
                        }
                        catch (Exception ignored)
                        {
                            // WHAT HAVE YOU DONE :(
                        }
                    }
                }

                newColors.removeIf(i -> i < 0 || i > 0xFFFFFF);
                tag.addAll(newColors);
            }
        }

        public static boolean hasReferenceToAnotherTag(JsonObject json)
        {
            for (JsonElement jsonElement : GsonHelper.getAsJsonArray(json, "values"))
            {
                String str = jsonElement.getAsString();
                if (str.indexOf('#') == 0 && str.contains(":")) // very weak condition but it does what its supposed to do
                {
                    return true;
                }
            }
            return false;
        }
    }
}