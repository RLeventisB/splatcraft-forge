package net.splatcraft.registries;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.splatcraft.commands.*;

public class SplatcraftCommands
{

    public static void registerCommands()
    {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            InkColorCommand.register(dispatcher);
            ScanTurfCommand.register(dispatcher);
            ClearInkCommand.register(dispatcher);
            ReplaceColorCommand.register(dispatcher);
            ColorScoresCommand.register(dispatcher);
            StageCommand.register(dispatcher);
            SuperJumpCommand.register(dispatcher);
        });
    }

    @ExpectPlatform
    public static void registerArguments()
    {
        throw new AssertionError();
    }
}
