package net.splatcraft.registries.fabric;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.splatcraft.Splatcraft;
import net.splatcraft.commands.arguments.ColorCriterionArgument;
import net.splatcraft.commands.arguments.InkColorArgument;

public class SplatcraftCommandsImpl
{
    public static void registerArguments()
    {
        ArgumentTypeRegistry.registerArgumentType(
            Splatcraft.identifierOf("ink_color"),
            InkColorArgument.class,
            ConstantArgumentSerializer.of(InkColorArgument::inkColor)
        );
        ArgumentTypeRegistry.registerArgumentType(
            Splatcraft.identifierOf("color_criterion"),
            ColorCriterionArgument.class,
            ConstantArgumentSerializer.of(ColorCriterionArgument::colorCriterion)
        );
    }
}
