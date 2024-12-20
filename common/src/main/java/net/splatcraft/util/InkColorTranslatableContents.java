package net.splatcraft.util;

import net.minecraft.text.StringVisitable;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Language;
import net.splatcraft.data.InkColorRegistry;

import java.util.Optional;

public class InkColorTranslatableContents extends TranslatableTextContent
{
    private final TranslatableTextContent inverted;
    private InkColor color;
    private Language decomposedWith;

    public InkColorTranslatableContents(InkColor color, Object... pArgs)
    {
        super(getKeyForColor(color), "#" + String.format("%06X", color.getColor()).toUpperCase(), pArgs);
        inverted = new TranslatableTextContent("ink_color.invert", null, new TranslatableTextContent[]{new TranslatableTextContent(InkColorRegistry.getFirstAliasForColor(0xFFFFFF - color.getColor()).toTranslationKey(), getFallback(), pArgs)});
    }

    private static String getKeyForColor(InkColor color)
    {
        return color.getTranslationKey();
    }

    @Override
    public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor)
    {
        Language language = Language.getInstance();

        if (!language.hasTranslation(getKey()) && language.hasTranslation(InkColorRegistry.getFirstAliasForColor(0xFFFFFF - color.getColor()).toTranslationKey()))
            return inverted.visit(visitor);

        return super.visit(visitor);
    }
}
