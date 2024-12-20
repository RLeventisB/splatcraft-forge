package net.splatcraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.util.InkColor;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class InkColorArgument implements ArgumentType<InkColor>
{
    public static final DynamicCommandExceptionType COLOR_NOT_FOUND = new DynamicCommandExceptionType(p_208663_0_ -> Text.translatable("arg.inkColor.notFound", p_208663_0_));
    public static final int max = 0xFFFFFF;
    private static final Collection<String> EXAMPLES = Arrays.asList("splatcraft:orange", "blue", "#C83D79", "4234555");

    protected InkColorArgument()
    {
        super();
    }

    public static InkColorArgument inkColor()
    {
        return new InkColorArgument();
    }

    public static InkColor getInkColor(CommandContext<ServerCommandSource> context, String name)
    {
        return context.getArgument(name, InkColor.class);
    }

    public static InkColor parseStatic(StringReader reader) throws CommandSyntaxException
    {
        final int start = reader.getCursor();

        Identifier resourceLocation = Identifier.fromCommandInputNonEmpty(reader);
        if (!InkColorRegistry.containsAlias(resourceLocation))
        {
            try
            {
                reader.setCursor(start);
                int hexCode = parseNum(reader.readString().toLowerCase(), reader);
                if (hexCode < 0)
                {
                    reader.setCursor(start);
                    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().createWithContext(reader, hexCode, 0);
                }
                if (hexCode > max)
                {
                    reader.setCursor(start);
                    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().createWithContext(reader, hexCode, max);
                }
                return InkColor.constructOrReuse(hexCode);
            }
            catch (CommandSyntaxException e)
            {
                throw COLOR_NOT_FOUND.create(resourceLocation);
            }
        }

        return InkColorRegistry.getInkColorByAlias(resourceLocation);
    }

    public static int parseNum(String input, StringReader reader) throws CommandSyntaxException
    {
        try
        {
            return Integer.decode(input);
        }
        catch (NumberFormatException var2)
        {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().createWithContext(reader, input);
        }
    }

    @Override
    public InkColor parse(StringReader reader) throws CommandSyntaxException
    {
        return parseStatic(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)
    {
//        CommandSource.suggestMatching() what
        return CommandSource.suggestMatching(InkColorRegistry.getAllAliases().stream().map(Identifier::toString).collect(Collectors.toSet()), builder);
    }

    @Override
    public Collection<String> getExamples()
    {
        return EXAMPLES;
    }
}
