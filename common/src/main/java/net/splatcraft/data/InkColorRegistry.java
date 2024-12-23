package net.splatcraft.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleDefaultedRegistry;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.splatcraft.Splatcraft;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class InkColorRegistry
{
	public static final Registry<InkColor> REGISTRY = new SimpleDefaultedRegistry<>("default", RegistryKey.ofRegistry(Splatcraft.identifierOf("ink_colors")), Lifecycle.stable(), false);
	public static InkColor getInkColorByAlias(Identifier location)
	{
		return REGISTRY.get(location);
	}
	public static boolean containsAlias(Identifier location)
	{
		return REGISTRY.containsId(location);
	}
	/**
	 * @param value The identifier of the color, or the hex code
	 * @return The corresponding {@link InkColor}, or {@code ColorUtils.getDefaultColor()} if the value wasn't a valid {@link Identifier}, or was not registered, or the text wasn't a valid hex color.
	 */
	public static InkColor getColorByAliasOrHex(String value)
	{
		DataResult<Identifier> parsedIdentifier = Identifier.validate(value);
		if (parsedIdentifier.isSuccess())
		{
			Identifier location = parsedIdentifier.getOrThrow();
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
	public static List<Identifier> getAliasesForColor(int color)
	{
		List<Identifier> result = new ArrayList<>();
		REGISTRY.getEntrySet().forEach((entry) ->
		{
			if (entry.getValue().getColor() == color)
				result.add(entry.getKey().getValue());
		});
		
		return result;
	}
	public static Identifier getColorAlias(InkColor color)
	{
		return REGISTRY.getId(color);
	}
	public static Identifier getFirstAliasForColor(int color)
	{
		for (Map.Entry<RegistryKey<InkColor>, InkColor> entry : REGISTRY.getEntrySet())
		{
			RegistryKey<InkColor> alias = entry.getKey();
			InkColor c = entry.getValue();
			if (c.getColor() == color)
			{
				return alias.getValue();
			}
		}
		return null;
	}
	public static Set<Identifier> getAllAliases()
	{
		return REGISTRY.getKeys().stream().map(RegistryKey::getValue).collect(Collectors.toSet());
	}
	public static class Listener extends JsonDataLoader
	{
		private static final Gson GSON_INSTANCE = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		private static final String folder = "ink_colors";
		public Listener()
		{
			super(GSON_INSTANCE, folder);
		}
		@Override
		protected @NotNull Map<Identifier, JsonElement> prepare(@NotNull ResourceManager manager, @NotNull Profiler profiler)
		{
			REGISTRY.clearTags();
			return super.prepare(manager, profiler);
		}
		@Override
		protected void apply(Map<Identifier, JsonElement> resourceList, ResourceManager manager, Profiler profiler)
		{
			for (Map.Entry<Identifier, JsonElement> entry : resourceList.entrySet())
			{
				Identifier key = entry.getKey();
				JsonElement j = entry.getValue();
				JsonObject json = j.getAsJsonObject();
				if (json.has("value"))
				{
					DataResult<Pair<InkColor, JsonElement>> inkColorResult = InkColor.CODEC.decode(JsonOps.INSTANCE, json.get("value"));
					if (inkColorResult.isSuccess())
					{
						InkColor color = inkColorResult.map(Pair::getFirst).getOrThrow();
						Registry.register(REGISTRY, key, color);
					}
				}
			}
			InkColorGroups.Listener.doLoad();
		}
	}
}
