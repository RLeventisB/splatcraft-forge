package net.splatcraft.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.handlers.DataHandler;
import net.splatcraft.items.weapons.settings.AbstractWeaponSettings;
import net.splatcraft.util.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UpdateWeaponSettingsPacket extends PlayS2CPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(UpdateWeaponSettingsPacket.class);
	private static final HashMap<Class<? extends AbstractWeaponSettings<?, ?>>, String> CLASS_TO_TYPE = new HashMap<>()
	{{
		for (Entry<String, Class<? extends AbstractWeaponSettings<?, ?>>> entry : DataHandler.WeaponStatsListener.SETTING_TYPES.entrySet())
			put(entry.getValue(), entry.getKey());
	}};
	public final Set<Map.Entry<Identifier, AbstractWeaponSettings<?, ?>>> settings;
	public UpdateWeaponSettingsPacket(Set<Map.Entry<Identifier, AbstractWeaponSettings<?, ?>>> settings)
	{
		this.settings = settings;
	}
	public UpdateWeaponSettingsPacket()
	{
		this(DataHandler.WeaponStatsListener.SETTINGS.entrySet());
	}
	public static UpdateWeaponSettingsPacket decode(RegistryByteBuf buffer)
	{
		SplatcraftConvertors.SkipConverting = true;
		Set<Map.Entry<Identifier, AbstractWeaponSettings<?, ?>>> settings = new HashSet<>();
		for (int i = buffer.readInt(); i > 0; i--)
		{
			Identifier key = buffer.readIdentifier();
			try
			{
				AbstractWeaponSettings<?, ?> setting = DataHandler.WeaponStatsListener.SETTING_TYPES.get(buffer.readString()).getConstructor(String.class).newInstance(key.toString());
				setting.castAndDeserialize(buffer.decodeAsJson(setting.getCodec()));
				
				setting.registerStatTooltips();
				settings.add(Map.entry(key, setting));
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException |
			       NoSuchMethodException e)
			{
				
				throw new RuntimeException(e);
			}
		}
		
		SplatcraftConvertors.SkipConverting = false;
		return new UpdateWeaponSettingsPacket(settings);
	}
	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
	@Override
	public void encode(RegistryByteBuf buffer)
	{
		buffer.writeInt(settings.size());
		
		for (Map.Entry<Identifier, AbstractWeaponSettings<?, ?>> entry : settings)
		{
			buffer.writeIdentifier(entry.getKey());
			buffer.writeString(CLASS_TO_TYPE.get(entry.getValue().getClass()));
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
