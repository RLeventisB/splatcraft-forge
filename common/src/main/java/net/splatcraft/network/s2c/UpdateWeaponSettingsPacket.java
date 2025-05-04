package net.splatcraft.network.s2c;

import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.handlers.DataHandler;
import net.splatcraft.items.weapons.settings.AbstractWeaponSettings;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UpdateWeaponSettingsPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateWeaponSettingsPacket.class);
	private static final HashMap<Class<? extends AbstractWeaponSettings<?, ?>>, String> CLASS_TO_TYPE = new HashMap<>()
	{{
		for (Entry<String, Class<? extends AbstractWeaponSettings<?, ?>>> entry : DataHandler.WeaponStatsListener.SETTING_TYPES.entrySet())
			put(entry.getValue(), entry.getKey());
	}};
	public final Set<Map.Entry<ResourceLocation, AbstractWeaponSettings<?, ?>>> settings;
	public UpdateWeaponSettingsPacket(Set<Map.Entry<ResourceLocation, AbstractWeaponSettings<?, ?>>> settings)
	{
		this.settings = settings;
	}
	public UpdateWeaponSettingsPacket()
	{
		this(DataHandler.WeaponStatsListener.SETTINGS.entrySet());
	}
	public static UpdateWeaponSettingsPacket decode(RegistryFriendlyByteBuf buffer)
	{
		SplatcraftConvertors.SkipConverting = true;
		Set<Map.Entry<ResourceLocation, AbstractWeaponSettings<?, ?>>> settings = new HashSet<>();
		for (int i = buffer.readInt(); i > 0; i--)
		{
			ResourceLocation key = buffer.readResourceLocation();
			try
			{
				AbstractWeaponSettings<?, ?> setting = DataHandler.WeaponStatsListener.SETTING_TYPES.get(buffer.readUtf()).getConstructor(String.class).newInstance(key.toString());
				JsonObject json = GsonHelper.parse(buffer.readUtf());
				setting.deserialize(key, json);

				setting.registerStatTooltips();
				settings.add(Map.entry(key, setting));
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException |
			       NoSuchMethodException | ClassCastException e)
			{
				Splatcraft.LOGGER.error("Error upon reading data for {}", key);
			}
		}

		SplatcraftConvertors.SkipConverting = false;
		return new UpdateWeaponSettingsPacket(settings);
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeInt(settings.size());

		for (Map.Entry<ResourceLocation, AbstractWeaponSettings<?, ?>> entry : settings)
		{
			buffer.writeResourceLocation(entry.getKey());
			buffer.writeUtf(CLASS_TO_TYPE.get(entry.getValue().getClass()));
			entry.getValue().serializeToBuffer(buffer);
		}
	}
	@Override
	public void execute()
	{
		DataHandler.WeaponStatsListener.SETTINGS.clear();
		settings.forEach(entry -> DataHandler.WeaponStatsListener.SETTINGS.put(entry.getKey(), entry.getValue()));
	}
}
