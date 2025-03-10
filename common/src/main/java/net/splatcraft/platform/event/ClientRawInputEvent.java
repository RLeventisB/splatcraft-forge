package net.splatcraft.platform.event;

import net.minecraft.client.Minecraft;

public interface ClientRawInputEvent
{
	@FunctionalInterface
	interface MouseScrolled
	{
		EventResult mouseScrolled(Minecraft client, double amountX, double amountY);
	}
	@FunctionalInterface
	interface MouseClicked
	{
		EventResult mouseClicked(Minecraft client, int button, int action, int mods);
	}
	@FunctionalInterface
	interface KeyPressed
	{
		EventResult keyPressed(Minecraft client, int keyCode, int scanCode, int action, int modifiers);
	}
}
