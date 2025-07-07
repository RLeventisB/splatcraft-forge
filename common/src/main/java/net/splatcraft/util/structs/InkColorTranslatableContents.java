package net.splatcraft.util.structs;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.data.InkColorRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class InkColorTranslatableContents extends TranslatableContents
{
	private final TranslatableContents inverted;
	private final InkColor color;
	public InkColorTranslatableContents(InkColor color, Object... pArgs)
	{
		super(getKeyForColor(color), "#" + String.format("%06X", color.getColor()).toUpperCase(), pArgs);
		inverted = new TranslatableContents("ink_color.invert", null, new MutableComponent[] {
			MutableComponent.create(new TranslatableContents(
				getKeyForColor(color.getInverted()),
				getFallback(),
				pArgs)
			)
		});
		this.color = color;
	}
	private static String getKeyForColor(InkColor color)
	{
		return color.getTranslationKey();
	}
	@Override
	public <T> @NotNull Optional<T> visit(FormattedText.@NotNull ContentConsumer<T> visitor)
	{
		Language language = Language.getInstance();
		
		if (!language.has(getKey()))
		{
			ResourceLocation alias = InkColorRegistry.getColorAlias(color.getInverted());
			if (alias != null && language.has(alias.toLanguageKey()))
			{
				return inverted.visit(visitor);
			}
		}
		
		return super.visit(visitor);
	}
}
