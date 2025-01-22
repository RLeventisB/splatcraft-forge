package net.splatcraft.config;

import dev.architectury.platform.Mod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
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
