package net.splatcraft.items.weapons.settings;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.Splatcraft;

import java.util.HashMap;
import java.util.Map;

public abstract class DynamicWeaponSettings<SELF extends AbstractWeaponSettings<SELF, COMMONDATA>, COMMONDATA, DATA, KEY> extends AbstractWeaponSettings<SELF, COMMONDATA>
{
	private static final Map<Class<? extends DynamicWeaponSettings<?, ?, ?, ?>>, Map<?, MapCodec<?>>> subTypeCodec = new HashMap<>();
	private MapCodec<DATA> dynamicCodec;
	public KEY getDynamicDataKey()
	{
		return dynamicDataKey;
	}
	private KEY dynamicDataKey;
	public DynamicWeaponSettings(ResourceLocation name)
	{
		super(name);
		Class<? extends DynamicWeaponSettings<?, ?, ?, ?>> clazz = (Class<? extends DynamicWeaponSettings<?, ?, ?, ?>>) getClass();
		subTypeCodec.computeIfAbsent(clazz, v -> Map.ofEntries(getDynamicCodecs()));
	}
	public abstract Map.Entry<KEY, MapCodec<? extends DATA>>[] getDynamicCodecs();
	protected abstract MapCodec<COMMONDATA> getMapCodec();
	public String getDynamicCodecKeyName()
	{
		return "sub_type";
	}
	public MapCodec<KEY> getFieldOfDynamicKey()
	{
		return getDynamicCodecKeyCodec().fieldOf(getDynamicCodecKeyName());
	}
	public abstract Codec<KEY> getDynamicCodecKeyCodec();
	public abstract DATA getDynamicDataToSerialize();
	@Override
	public void deserialize(ResourceLocation key, JsonObject json)
	{
		// if the field that defines the dynamic codec is not present and the mapcodec that processes
		// this field throws an error (for example, when it isnt an optional codec), then
		// process without the dynamic part
		if (!json.has(getDynamicCodecKeyName()) && getFieldOfDynamicKey().compressedDecode(JsonOps.INSTANCE, json).isError())
		{
			deserializeWithoutDynamicPart(key, json);
			return;
		}
		dynamicDataKey = getFieldOfDynamicKey().compressedDecode(JsonOps.INSTANCE, json).getOrThrow();
		dynamicCodec = (MapCodec<DATA>) subTypeCodec.get(getClass()).get(dynamicDataKey);
		if (dynamicCodec == null)
		{
			deserializeWithoutDynamicPart(key, json);
			return;
		}

		DataResult<COMMONDATA> common = getCodec().parse(JsonOps.INSTANCE, json);
		DataResult<DATA> dynamic = dynamicCodec.codec().parse(JsonOps.INSTANCE, json);
		common.ifError((msg) -> Splatcraft.LOGGER.error("Failed to load common part of the weapon settings for %s: %s".formatted(key, msg)));
		dynamic.ifError((msg) -> Splatcraft.LOGGER.error("Failed to load the dynamic part of the weapon settings for %s (%s): %s".formatted(key, TypeToken.of(getClass().getTypeParameters()[0]), msg)));
		if (common.hasResultOrPartial() && common.hasResultOrPartial())
		{
			processResult(common.getPartialOrThrow(), dynamic.getPartialOrThrow());
		}
	}
	private void deserializeWithoutDynamicPart(ResourceLocation key, JsonObject json)
	{
		DataResult<COMMONDATA> common = getCodec().parse(JsonOps.INSTANCE, json);
		common.ifError((msg) -> Splatcraft.LOGGER.error("Failed to load common part of the weapon settings for %s: %s".formatted(key, msg)));
		if (common.hasResultOrPartial() && common.hasResultOrPartial())
		{
			processResult(common.getPartialOrThrow(), null);
		}
		return;
	}
	@Override
	public final Codec<COMMONDATA> getCodec()
	{
		return getMapCodec().codec();
	}
	@Override
	public final void processData(COMMONDATA o)
	{

	}
	@Override
	public final void processResult(Object o)
	{
	}
	protected abstract void processResult(COMMONDATA commondata, DATA data);
	@Override
	public final void serializeToBuffer(RegistryFriendlyByteBuf buffer)
	{
		// lazily stitch the json elements because i dont know how mapcodecs do encoding :(
		RecordBuilder<JsonElement> builder = JsonOps.INSTANCE.mapBuilder();

		if (dynamicDataKey != null)
			getFieldOfDynamicKey().encode(dynamicDataKey, JsonOps.INSTANCE, builder);
		getMapCodec().encode(getDataToSerialize(), JsonOps.INSTANCE, builder);
		DATA dynamicData = getDynamicDataToSerialize();
		if (dynamicData != null)
			dynamicCodec.encode(dynamicData, JsonOps.INSTANCE, builder);

		DataResult<JsonElement> result = builder.build(new JsonObject());
		buffer.writeUtf(result.getOrThrow().toString());
	}
}
