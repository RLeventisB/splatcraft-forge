package net.splatcraft.forge.util;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.forge.data.InkColorAliases;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class InkColorTranslatableContents extends TranslatableContents
{

    private int color;
    private final TranslatableContents inverted;

    public InkColorTranslatableContents(int color, Object... pArgs)
    {
        super(getKeyForColor(color), "#" + String.format("%06X", color).toUpperCase(), pArgs);
        inverted = new TranslatableContents("ink_color.invert", null, new TranslatableContents[]{new TranslatableContents(getKeyForColor(0xFFFFFF - color), getFallback(), pArgs)});
    }

    private Language decomposedWith;

    @Override
    public <T> @NotNull Optional<T> visit(FormattedText.@NotNull ContentConsumer<T> pContentConsumer)
    {
        Language language = Language.getInstance();

        if (!language.has(getKey()) && language.has(getKeyForColor(0xFFFFFF - color)))
            return inverted.visit(pContentConsumer);

        return super.visit(pContentConsumer);
    }

    @Override
    public @NotNull String getKey()
    {
        return super.getKey();
    }

    private static String getKeyForColor(int color)
    {
        String hex = "ink_color." + String.format("%06X", color).toLowerCase();
        Language language = Language.getInstance();
        if (!language.has(hex))
        {
            List<ResourceLocation> aliases = InkColorAliases.getAliasesForColor(color);
            for (ResourceLocation alias : aliases)
            {
                String key = "ink_color." + alias.getNamespace() + "." + alias.getPath();
                if (language.has(key))
                    return key;
            }
        }
        return hex;
    }
}
