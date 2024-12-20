package net.splatcraft.commands.arguments;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.registries.SplatcraftGameRules;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class JumpTargetArgument extends EntityArgumentType
{
    protected JumpTargetArgument()
    {
        super(true, false);
    }

    public static JumpTargetArgument target()
    {
        return new JumpTargetArgument();
    }

    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(CommandContext<S> command, @NotNull SuggestionsBuilder suggestionsBuilder)
    {
        if (command.getSource() instanceof CommandSource sharedsuggestionprovider)
        {
            StringReader stringreader = new StringReader(suggestionsBuilder.getInput());
            stringreader.setCursor(suggestionsBuilder.getStart());
            EntitySelectorReader entityselectorparser = new EntitySelectorReader(stringreader, sharedsuggestionprovider.hasPermissionLevel(2));

            try
            {
                entityselectorparser.read();
            }
            catch (CommandSyntaxException commandsyntaxexception)
            {
            }

            return entityselectorparser.listSuggestions(suggestionsBuilder, (p_91457_) ->
            {
                Collection<String> collection = sharedsuggestionprovider.getPlayerNames();
                Entity source = ((ServerCommandSource) command.getSource()).getEntity();
                List<Stage> validStages = SaveInfoCapability.get().getStages().values().stream().filter(stage -> stage.getBounds().contains(source.getPos())).toList();

                if (!SplatcraftGameRules.getLocalizedRule(source.getWorld(), source.getBlockPos(), SplatcraftGameRules.GLOBAL_SUPERJUMPING))
                    collection.removeIf((str) ->
                    {
                        PlayerEntity player = ((ServerCommandSource) command.getSource()).getServer().getPlayerManager().getPlayer(str);

                        return validStages.stream().filter(stage -> stage.getBounds().contains(player.getPos())).toList().isEmpty();
                    });

                Iterable<String> iterable = Iterables.concat(collection, sharedsuggestionprovider.getEntitySuggestions());
                CommandSource.suggestMatching(iterable, p_91457_);
            });
        }
        else
        {
            return Suggestions.empty();
        }
    }
}
