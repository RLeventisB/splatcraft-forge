package net.splatcraft.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.splatcraft.Splatcraft;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public record InkColorGroup(List<ResourceLocation> idList)
{
	public static final StreamCodec<ByteBuf, InkColorGroup> STREAM_CODEC = StreamCodec.composite(
		ResourceLocation.STREAM_CODEC.apply(v -> ByteBufCodecs.collection(ObjectArrayList::new, v)), v -> v.idList,
		InkColorGroup::new
	);
	public static final ResourceLocation STARTER_COLORS = Splatcraft.identifierOf("starter_colors");
	public static final ResourceLocation INK_VAT_DEFAULT = Splatcraft.identifierOf("ink_vat_default");
	public static final ResourceLocation CLASSIC = Splatcraft.identifierOf("classic");
	public static final ResourceLocation PASTEL = Splatcraft.identifierOf("pastel");
	public static final ResourceLocation NEON = Splatcraft.identifierOf("neon");
	public static final ResourceLocation OVERGROWN = Splatcraft.identifierOf("overgrown");
	public static final ResourceLocation MIDNIGHT = Splatcraft.identifierOf("midnight");
	public static final ResourceLocation ENCHANTED = Splatcraft.identifierOf("enchanted");
	public static final ResourceLocation CREATIVE_TAB_COLORS = Splatcraft.identifierOf("creative_tab_colors");
	public InkColorGroup()
	{
		this(new ObjectArrayList<>());
	}
	public static void setRegistry(Map<ResourceLocation, InkColorGroup> groups)
	{
		Listener.REGISTRY.clear();
		Listener.REGISTRY.putAll(groups);
	}
	public void clear()
	{
		idList.clear();
	}
	public void addAll(Collection<ResourceLocation> values)
	{
		idList.addAll(values);
	}
	public InkColor getRandomColor(Random random)
	{
		return InkColorRegistry.getColorByAlias(getRandom(random)).get();
	}
	public ResourceLocation getRandom(Random random)
	{
		return idList.isEmpty() ? null : idList.get(random.nextInt(idList.size()));
	}
	public Collection<ResourceLocation> getIds()
	{
		return Collections.unmodifiableList(idList);
	}
	public Collection<InkColor> getColors()
	{
		return getIds().stream().map(InkColorRegistry::getColorByAlias).filter(Optional::isPresent).map(Optional::get).toList();
	}
	public static Map<ResourceLocation, InkColorGroup> getAllGroups()
	{
		return Map.copyOf(Listener.REGISTRY);
	}
	public static Optional<InkColorGroup> getGroup(ResourceLocation id)
	{
		return Optional.ofNullable(Listener.REGISTRY.get(id));
	}
	public static class Listener extends SimpleJsonResourceReloadListener
	{
		private static final HashMap<ResourceLocation, InkColorGroup> REGISTRY = new HashMap<>();
		private static final HashMap<ResourceLocation, List<ResourceLocation>> TAG_REFERENCE_QUEUE = new HashMap<>();
		private static final Gson GSON_INSTANCE = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		private static final String folder = "tags/ink_colors";
		public Listener()
		{
			super(GSON_INSTANCE, folder);
		}
		public static InkColorGroup getOrCreateTag(ResourceLocation name)
		{
			return REGISTRY.computeIfAbsent(name, v -> new InkColorGroup());
		}
		@Override
		protected void apply(@NotNull Map<ResourceLocation, JsonElement> resourceList, @NotNull ResourceManager resourceManagerIn, @NotNull ProfilerFiller profilerIn)
		{
			for (Map.Entry<ResourceLocation, JsonElement> entry : resourceList.entrySet())
			{
				ResourceLocation key = entry.getKey();
				JsonElement j = entry.getValue();
				JsonObject json = j.getAsJsonObject();
				if (!json.has("values"))
					continue;
				
				if (GsonHelper.getAsBoolean(json, "replace", false))
					getOrCreateTag(key).clear();
				
				HashSet<ResourceLocation> newColors = new HashSet<>();
				
				for (JsonElement jsonElement : GsonHelper.getAsJsonArray(json, "values"))
				{
					String str = jsonElement.getAsString();
					if (str.indexOf('#') == 0 && str.contains(":"))
					{
						ResourceLocation referencedKey = ResourceLocation.parse(str.substring(1));
						if (REGISTRY.containsKey(referencedKey))
						{
							newColors.addAll(REGISTRY.get(referencedKey).getIds());
						}
						else
						{
							TAG_REFERENCE_QUEUE.computeIfAbsent(referencedKey, v -> new ArrayList<>()).add(key);
						}
					}
					else
					{
						newColors.add(ResourceLocation.parse(str));
					}
				}
				
				// the ink color registry isnt loaded at this point :(
//				newColors.removeIf(i -> !InkColorRegistry.containsAlias(i));
				getOrCreateTag(key).addAll(newColors);
				
				List<ResourceLocation> groupsThatReferencedThisTag = TAG_REFERENCE_QUEUE.remove(key);
				if (groupsThatReferencedThisTag != null)
				{
					for (ResourceLocation id : groupsThatReferencedThisTag)
					{
						REGISTRY.get(id).addAll(newColors);
					}
				}
			}
		}
	}
}