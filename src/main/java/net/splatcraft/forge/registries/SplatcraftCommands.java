package net.splatcraft.forge.registries;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.commands.*;
import net.splatcraft.forge.commands.arguments.ColorCriterionArgument;
import net.splatcraft.forge.commands.arguments.InkColorArgument;

@Mod.EventBusSubscriber(modid = Splatcraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SplatcraftCommands
{
	public static final DeferredRegister<ArgumentTypeInfo<?, ?>> REGISTRY = DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, Splatcraft.MODID);
	public static final RegistryObject<SingletonArgumentInfo<InkColorArgument>> INK_COLOR_ARGUMENT_TYPE = REGISTRY.register("ink_color", () -> ArgumentTypeInfos.registerByClass(InkColorArgument.class, SingletonArgumentInfo.contextFree(InkColorArgument::inkColor)));
	public static final RegistryObject<SingletonArgumentInfo<ColorCriterionArgument>> COLOR_CRITERION_ARGUMENT_TYPE = REGISTRY.register("color_criterion", () -> ArgumentTypeInfos.registerByClass(ColorCriterionArgument.class, SingletonArgumentInfo.contextFree(ColorCriterionArgument::colorCriterion)));
	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event)
	{
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		
		InkColorCommand.register(dispatcher);
		ScanTurfCommand.register(dispatcher);
		ClearInkCommand.register(dispatcher);
		ReplaceColorCommand.register(dispatcher);
		ColorScoresCommand.register(dispatcher);
		StageCommand.register(dispatcher);
		SuperJumpCommand.register(dispatcher);
	}
	public static void registerArguments()
	{
	}
}
