package net.splatcraft.registries.neoforge;

import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.registry.Registries;
import net.splatcraft.Splatcraft;
import net.splatcraft.commands.arguments.ColorCriterionArgument;
import net.splatcraft.commands.arguments.InkColorArgument;

public class SplatcraftCommandsImpl
{
    public static void registerArguments()
    {
        DeferredRegister<ArgumentSerializer<?, ?>> registry = Splatcraft.deferredRegistryOf(Registries.COMMAND_ARGUMENT_TYPE);

        registry.register("ink_color", () -> ArgumentTypes.registerByClass(InkColorArgument.class, ConstantArgumentSerializer.of(InkColorArgument::inkColor)));
        registry.register("color_criterion", () -> ArgumentTypes.registerByClass(ColorCriterionArgument.class, ConstantArgumentSerializer.of(ColorCriterionArgument::colorCriterion)));
    }
}
