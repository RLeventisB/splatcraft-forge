package net.splatcraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.splatcraft.commands.InkColorCommand;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.util.InkColor;

import java.util.concurrent.CompletableFuture;

public class ColorCriterionArgument extends InkColorArgument
{
    public static final DynamicCommandExceptionType CRITERION_NOT_FOUND = new DynamicCommandExceptionType(p_208663_0_ -> Text.translatable("arg.colorCriterion.notFound", p_208663_0_));

    private ColorCriterionArgument()
    {
        super();
    }

    public static ColorCriterionArgument colorCriterion()
    {
        return new ColorCriterionArgument();
    }

    @Override
    public InkColor parse(StringReader reader) throws CommandSyntaxException
    {
        InkColor color = super.parse(reader);

        if (!ScoreboardHandler.hasColorCriterion(color))
        {
            throw CRITERION_NOT_FOUND.create(InkColorCommand.getColorName(color));
        }
        return color;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)
    {
        return CommandSource.suggestMatching(ScoreboardHandler.getCriteriaSuggestions(), builder);
    }
}
