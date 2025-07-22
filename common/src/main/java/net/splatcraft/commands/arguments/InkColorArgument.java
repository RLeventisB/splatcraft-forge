package net.splatcraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.util.structs.InkColor;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class InkColorArgument implements ArgumentType<InkColor>
{
	public static final DynamicCommandExceptionType COLOR_NOT_FOUND = new DynamicCommandExceptionType(input -> Component.translatable("arg.inkColor.notFound", input));
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
	public static InkColor getInkColor(CommandContext<CommandSourceStack> context, String name)
	{
		return context.getArgument(name, InkColor.class);
	}
	public static InkColor parseStatic(StringReader reader) throws CommandSyntaxException
	{
		final int start = reader.getCursor();
		
		try
		{
			ResourceLocation colorAlias = ResourceLocation.read(reader);
			return InkColorRegistry.getColorByAliasOrHex(colorAlias.toString()).orElseThrow(() ->
				COLOR_NOT_FOUND.create(colorAlias.toString()));
		}
		catch (Exception ignored)
		{
		
		}
		reader.setCursor(start);
		String string = reader.readString();
		return InkColorRegistry.getColorByAliasOrHex(string).orElseThrow(() ->
		{
			return COLOR_NOT_FOUND.create(string);
		});
	}
	@Override
	public InkColor parse(StringReader reader) throws CommandSyntaxException
	{
		return parseStatic(reader);
	}
	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)
	{
		return SharedSuggestionProvider.suggest(InkColorRegistry.getAllAliases().stream().map(ResourceLocation::toString).collect(Collectors.toSet()), builder);
	}
	@Override
	public Collection<String> getExamples()
	{
		return EXAMPLES;
	}
}
