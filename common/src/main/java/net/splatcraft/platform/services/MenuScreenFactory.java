package net.splatcraft.platform.services;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

@FunctionalInterface
public interface MenuScreenFactory<H extends AbstractContainerMenu, S extends Screen & MenuAccess<H>>
{
	/**
	 * Creates a new {@link S} that extends {@link Screen}
	 *
	 * @param containerMenu The {@link AbstractContainerMenu} that controls the game logic for the screen
	 * @param inventory     The {@link Inventory} for the screen
	 * @param component     The {@link Component} for the screen
	 * @return A new {@link S} that extends {@link Screen}
	 */
	S create(H containerMenu, Inventory inventory, Component component);
}
