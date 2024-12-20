package net.splatcraft.config;

import dev.architectury.platform.Mod;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ConfigScreenProvider implements Mod.ConfigurationScreenProvider
{
    @Override
    public Screen provide(Screen parent)
    {
        return new Screen(Text.empty())
        {
            @Override
            public Text getTitle()
            {
                return super.getTitle();
            }
        };
    }
}
