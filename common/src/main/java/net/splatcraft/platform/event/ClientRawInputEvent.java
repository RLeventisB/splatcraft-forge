package net.splatcraft.platform.event;

import net.minecraft.client.Minecraft;
import net.splatcraft.platform.event.types.ConsumerEvent;
import net.splatcraft.platform.event.types.SimpleEvent;

public interface ClientRawInputEvent
{
	@FunctionalInterface
	interface MouseScrolled extends SimpleEvent.Tri<Minecraft, Double, Double>
	{
		EventResult invoke(Minecraft client, Double amountX, Double amountY);
	}
	@FunctionalInterface
	interface PreMouseClicked extends ConsumerEvent.Tetra<Minecraft, Integer, Integer, Integer>
	{
		void invoke(Minecraft client, Integer button, Integer action, Integer mods);
	}
	@FunctionalInterface
	interface KeyPressed extends ConsumerEvent.Penta<Minecraft, Integer, Integer, Integer, Integer>
	{
		void invoke(Minecraft client, Integer keyCode, Integer scanCode, Integer action, Integer modifiers);
	}
}
