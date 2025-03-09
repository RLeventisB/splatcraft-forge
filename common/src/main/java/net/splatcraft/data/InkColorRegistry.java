package net.splatcraft.data;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.splatcraft.crafting.InkVatColorRecipe;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class InkColorRegistry
{
	public static final BiMap<ResourceLocation, InkColor> REGISTRY = HashBiMap.create();
	public static InkColor getInkColorByAlias(ResourceLocation location)
	{
		return REGISTRY.get(location);
	}
	public static boolean containsAlias(ResourceLocation location)
	{
		return REGISTRY.containsKey(location);
	}
	/**
	 * @param value The identifier of the color, or the hex code
	 * @return The corresponding {@link InkColor}, or {@code ColorUtils.getDefaultColor()} if the value wasn't a valid {@link ResourceLocation}, or was not registered, or the text wasn't a valid hex color.
	 */
	public static InkColor getColorByAliasOrHex(String value)
	{
		DataResult<ResourceLocation> parsedIdentifier = ResourceLocation.read(value);
		if (parsedIdentifier.isSuccess())
		{
			ResourceLocation location = parsedIdentifier.getOrThrow();
			if (containsAlias(location))
				return getInkColorByAlias(location);
		}
		try
		{
			return InkColor.constructOrReuse(Integer.decode(value));
		}
		catch (NumberFormatException ignored)
		{
		}
		return ColorUtils.getDefaultColor();
	}
	public static List<ResourceLocation> getAliasesForColor(int color)
	{
		List<ResourceLocation> result = new ArrayList<>();
		REGISTRY.forEach((key, value) ->
		{
			if (value.getColor() == color)
				result.add(key);
		});
		
		return result;
	}
	public static ResourceLocation getColorAlias(InkColor color)
	{
		return REGISTRY.inverse().get(color);
	}
	public static ResourceLocation getFirstAliasForColor(int color)
	{
		for (Map.Entry<ResourceLocation, InkColor> entry : REGISTRY.entrySet())
		{
			ResourceLocation alias = entry.getKey();
			InkColor c = entry.getValue();
			if (c.getColor() == color)
			{
				return alias;
			}
		}
		return null;
	}
	public static Set<ResourceLocation> getAllAliases()
	{
		return new HashSet<>(REGISTRY.keySet());
	}
	public static class Listener extends SimpleJsonResourceReloadListener
	{
		private static final Gson GSON_INSTANCE = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		private static final String folder = "ink_colors";
		public static Map<ResourceLocation, JsonElement> resourceList;
		public Listener()
		{
			super(GSON_INSTANCE, folder);
		}
		@Override
		protected @NotNull Map<ResourceLocation, JsonElement> prepare(@NotNull ResourceManager manager, @NotNull ProfilerFiller profiler)
		{
			REGISTRY.clear();
			resourceList = super.prepare(manager, profiler);
			InkVatColorRecipe.getOmniList().clear();
			return resourceList;
		}
		@Override
		protected void apply(Map<ResourceLocation, JsonElement> resourceList, ResourceManager manager, ProfilerFiller profiler)
		{
			for (Map.Entry<ResourceLocation, JsonElement> entry : resourceList.entrySet())
			{
				ResourceLocation key = entry.getKey();
				JsonElement j = entry.getValue();
				JsonObject json = j.getAsJsonObject();
				if (json.has("value"))
				{
					DataResult<Pair<InkColor, JsonElement>> inkColorResult = InkColor.HEX_CODEC.decode(JsonOps.INSTANCE, json.get("value"));
					if (inkColorResult.isSuccess())
					{
						InkColor color = inkColorResult.map(Pair::getFirst).getOrThrow();
						REGISTRY.put(key, color);
					}
				}
			}
			InkColorGroups.Listener.doLoadIfNecessary();
		}
	}
}
