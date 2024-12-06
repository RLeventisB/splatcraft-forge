package net.splatcraft.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.data.SplatcraftConvertors;
import net.splatcraft.handlers.DataHandler;
import net.splatcraft.items.weapons.settings.AbstractWeaponSettings;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class UpdateWeaponSettingsPacket extends PlayS2CPacket
{
    private static final HashMap<Class<? extends AbstractWeaponSettings<?, ?>>, String> CLASS_TO_TYPE = new HashMap<>()
    {{
        for (Entry<String, Class<? extends AbstractWeaponSettings<?, ?>>> entry : DataHandler.WeaponStatsListener.SETTING_TYPES.entrySet())
            put(entry.getValue(), entry.getKey());
    }};
    public final HashMap<ResourceLocation, AbstractWeaponSettings<?, ?>> settings;

    public UpdateWeaponSettingsPacket(HashMap<ResourceLocation, AbstractWeaponSettings<?, ?>> settings)
    {
        this.settings = settings;
    }

    public UpdateWeaponSettingsPacket()
    {
        this(DataHandler.WeaponStatsListener.SETTINGS);
    }

    public static UpdateWeaponSettingsPacket decode(FriendlyByteBuf buffer)
    {
        SplatcraftConvertors.SkipConverting = true;
        HashMap<ResourceLocation, AbstractWeaponSettings<?, ?>> settings = new HashMap<>();
        for (int i = buffer.readInt(); i > 0; i--)
        {
            ResourceLocation key = buffer.readResourceLocation();
            try
            {
                AbstractWeaponSettings<?, ?> setting = DataHandler.WeaponStatsListener.SETTING_TYPES.get(buffer.readUtf()).getConstructor(String.class).newInstance(key.toString());
                setting.castAndDeserialize(buffer.readJsonWithCodec(setting.getCodec()));

                setting.registerStatTooltips();
                settings.put(key, setting);
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
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeInt(settings.size());

        for (Map.Entry<ResourceLocation, AbstractWeaponSettings<?, ?>> entry : settings.entrySet())
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
        DataHandler.WeaponStatsListener.SETTINGS.putAll(settings);
    }
}
