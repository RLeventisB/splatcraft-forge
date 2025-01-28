package net.splatcraft.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import net.splatcraft.Splatcraft;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.splatcraft.data.InkColorGroups.Listener.getOrCreateTag;

public class InkColorGroups
{
	public static final InkColorGroups STARTER_COLORS = getOrCreateTag(Splatcraft.identifierOf("starter_colors"));
	public static final InkColorGroups INK_VAT_DEFAULT = getOrCreateTag(Splatcraft.identifierOf("ink_vat_default"));
	public static final InkColorGroups CLASSIC = getOrCreateTag(Splatcraft.identifierOf("classic"));
	public static final InkColorGroups PASTEL = getOrCreateTag(Splatcraft.identifierOf("pastel"));
	public static final InkColorGroups NEON = getOrCreateTag(Splatcraft.identifierOf("neon"));
	public static final InkColorGroups OVERGROWN = getOrCreateTag(Splatcraft.identifierOf("overgrown"));
	public static final InkColorGroups MIDNIGHT = getOrCreateTag(Splatcraft.identifierOf("midnight"));
	public static final InkColorGroups ENCHANTED = getOrCreateTag(Splatcraft.identifierOf("enchanted"));
	public static final InkColorGroups CREATIVE_TAB_COLORS = getOrCreateTag(Splatcraft.identifierOf("creative_tab_colors"));
	private final List<InkColor> list;
	public InkColorGroups()
	{
		this(new ObjectArrayList<>());
	}
	public InkColorGroups(List<InkColor> list)
	{
		this.list = list;
	}
	public void clear()
	{
		list.clear();
	}
	public void addAll(Collection<InkColor> values)
	{
		list.addAll(values);
	}
	public InkColor getRandom(Random random)
	{
		Listener.doLoadIfNecessary();
		return list.isEmpty() ? ColorUtils.getDefaultColor() : list.get(random.nextInt(list.size()));
	}
	public Collection<InkColor> getAll()
	{
		Listener.doLoadIfNecessary();
		return Collections.unmodifiableList(list);
	}
	public static class Listener extends JsonDataLoader
	{
		private static final HashMap<Identifier, InkColorGroups> REGISTRY = new HashMap<>();
		private static final Gson GSON_INSTANCE = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		private static final String folder = "tags/ink_colors";
		private static final List<Map.Entry<Identifier, JsonElement>> entries = new ArrayList<>(), entriesThatReferenceAnotherTag = new ArrayList<>();
		private static boolean loaded;
		public Listener()
		{
			super(GSON_INSTANCE, folder);
		}
		public static InkColorGroups getOrCreateTag(Identifier name)
		{
			return REGISTRY.computeIfAbsent(name, v -> new InkColorGroups());
		}
		public static void doLoadIfNecessary()
		{
			synchronized (REGISTRY) // i have traumas with parallel loading so this is just in case that happens
			{
				if (loaded)
					return;
				
				// this literally exists so java loads the class for this lol
				STARTER_COLORS.clear();
				
				loaded = true;
				for (var entry : REGISTRY.entrySet())
				{
					entry.getValue().clear();
				}
				if (entries.isEmpty() && entriesThatReferenceAnotherTag.isEmpty())
				{
					Splatcraft.LOGGER.warn("The entries for the color groups is empty! Maybe this was called to early?");
					return;
				}
				for (Map.Entry<Identifier, JsonElement> entry : entries)
				{
					loadTag(entry.getKey(), entry.getValue(), false);
				}
				for (Map.Entry<Identifier, JsonElement> entry : entriesThatReferenceAnotherTag)
				{
					loadTag(entry.getKey(), entry.getValue(), true);
				}
			}
		}
		private static void loadTag(Identifier key, JsonElement j, boolean hasReferenceToOtherTags)
		{
			InkColorGroups tag = getOrCreateTag(key);
			JsonObject json = j.getAsJsonObject();
			
			if (JsonHelper.getBoolean(json, "replace", false))
				tag.clear();
			
			ArrayList<InkColor> newColors = new ArrayList<>();
			
			for (JsonElement jsonElement : JsonHelper.getArray(json, "values"))
			{
				String str = jsonElement.getAsString();
				if (hasReferenceToOtherTags && str.indexOf('#') == 0 && str.contains(":"))
				{
					Identifier referencedKey = Identifier.of(str.substring(1));
					if (REGISTRY.containsKey(referencedKey))
					{
						for (InkColor color : REGISTRY.get(referencedKey).getAll())
						{
							if (!newColors.contains(color))
								newColors.add(color);
						}
						continue;
					}
				}
				
				try
				{
					InkColor.NAME_CODEC.parse(JsonOps.INSTANCE, jsonElement).ifSuccess(newColors::add);
				}
				catch (Exception ignored)
				{
					// WHAT HAVE YOU DONE :(
				}
			}
			
			if (newColors.isEmpty())
				return;
			
			newColors.removeIf(i -> !i.isValid());
			tag.addAll(newColors);
		}
		public static boolean hasReferenceToAnotherTag(JsonObject json)
		{
			for (JsonElement jsonElement : JsonHelper.getArray(json, "values"))
			{
				String str = jsonElement.getAsString();
				if (str.indexOf('#') == 0 && str.contains(":")) // very weak condition but it does what its supposed to do
				{
					return true;
				}
			}
			return false;
		}
		@Override
		protected Map<Identifier, JsonElement> prepare(ResourceManager resourceManager, Profiler profiler)
		{
			loaded = false;
			entries.clear();
			entriesThatReferenceAnotherTag.clear();
			return super.prepare(resourceManager, profiler);
		}
		@Override
		protected void apply(@NotNull Map<Identifier, JsonElement> resourceList, @NotNull ResourceManager resourceManagerIn, @NotNull Profiler profilerIn)
		{
			for (Map.Entry<Identifier, JsonElement> entry : resourceList.entrySet())
			{
				Identifier key = entry.getKey();
				JsonElement j = entry.getValue();
				JsonObject json = j.getAsJsonObject();
				if (json.has("values"))
				{
					// this is cursed syntax
					(hasReferenceToAnotherTag(json) ? entriesThatReferenceAnotherTag : entries).add(entry);
				}
				else
				{
					resourceList.remove(key);
				}
			}
		}
	}
}