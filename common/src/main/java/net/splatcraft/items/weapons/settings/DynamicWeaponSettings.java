package net.splatcraft.items.weapons.settings;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.*;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.splatcraft.Splatcraft;

import java.util.HashMap;
import java.util.Map;

public abstract class DynamicWeaponSettings<SELF extends AbstractWeaponSettings<SELF, COMMONDATA>, COMMONDATA, DATA> extends AbstractWeaponSettings<SELF, COMMONDATA>
{
	private static final Gson GSON = new Gson();
	private static final Map<Class<? extends DynamicWeaponSettings<?, ?, ?>>, Map<String, MapCodec<?>>> subTypeCodec = new HashMap<>();
	private MapCodec<DATA> dynamicCodec;
	private String subTypeName;
	public DynamicWeaponSettings(String name)
	{
		super(name);
		Class<? extends DynamicWeaponSettings<?, ?, ?>> clazz = (Class<? extends DynamicWeaponSettings<?, ?, ?>>) getClass();
		subTypeCodec.computeIfAbsent(clazz, v -> Map.ofEntries(getDynamicCodecs()));
	}
	public abstract Map.Entry<String, MapCodec<? extends DATA>>[] getDynamicCodecs();
	protected abstract MapCodec<COMMONDATA> getMapCodec();
	public abstract DATA getDynamicDataToSerialize();
	@Override
	public void deserialize(Identifier key, JsonObject json)
	{
		onStartReading(json);
		subTypeName = JsonHelper.getString(json, "sub_type");
		dynamicCodec = (MapCodec<DATA>) subTypeCodec.get(getClass()).get(subTypeName);
		DataResult<COMMONDATA> common = getCodec().parse(JsonOps.INSTANCE, json);
		DataResult<DATA> dynamic = dynamicCodec.codec().parse(JsonOps.INSTANCE, json);
		common.ifError((msg) -> Splatcraft.LOGGER.error("Failed to load common part of the weapon settings for %s: %s".formatted(key, msg)));
		dynamic.ifError((msg) -> Splatcraft.LOGGER.error("Failed to load the dynamic part of the weapon settings for %s (%s): %s".formatted(key, TypeToken.of(getClass().getTypeParameters()[0]), msg)));
		if (common.hasResultOrPartial() && common.hasResultOrPartial())
		{
			processResult(common.getPartialOrThrow(), dynamic.getPartialOrThrow());
		}
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
	public final void serializeToBuffer(RegistryByteBuf buffer)
	{
		// lazily stitch the json elements because i dont know how mapcodecs do encoding :(
		RecordBuilder<JsonElement> builder = new RecordBuilder.MapBuilder<>(JsonOps.INSTANCE);
		
		Codec.STRING.fieldOf("sub_type").encode(subTypeName, JsonOps.INSTANCE, builder);
		getMapCodec().encode(getDataToSerialize(), JsonOps.INSTANCE, builder);
		dynamicCodec.encode(getDynamicDataToSerialize(), JsonOps.INSTANCE, builder);
		
		DataResult<JsonElement> result = builder.build(new JsonObject());
		result.ifSuccess(v -> buffer.writeString(v.toString()));
	}
}
