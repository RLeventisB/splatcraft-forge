package net.splatcraft.forge.client.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.splatcraft.forge.client.handlers.JumpLureHudHandler;
import net.splatcraft.forge.registries.SplatcraftItems;

public class SuperJumpSelectorScreen
{


	public SuperJumpSelectorScreen()
	{
	}

	private static final Minecraft mc = Minecraft.getInstance();

	public void render(GuiGraphics graphics, float partialTicks, JumpLureHudHandler.SuperJumpTargets targets, double scrollDelta)
	{

		ArrayList<UUID> playerUuids = new ArrayList<>(targets.playerTargetUuids);
		int entryCount = playerUuids.size() + (targets.canTargetSpawn ? 2 : 1);
		int index = Math.floorMod((int)scrollDelta, entryCount);

		ArrayList<MenuItem> options = new ArrayList<>(playerUuids.stream().map(uuid -> new PlayerMenuItem(mc.getConnection().getPlayerInfo(uuid).getProfile())).toList());

		if(targets.canTargetSpawn)
			options.add(0, new ItemStackMenuItem(new ItemStack(SplatcraftItems.spawnPad.get()), Component.literal("Go to Spawn")));
		options.add(0, new ItemStackMenuItem(new ItemStack(Items.BARRIER), Component.literal("Cancel")));

		int screenWidth = mc.getWindow().getGuiScaledWidth();
		int screenHeight = mc.getWindow().getGuiScaledHeight();

		for(int i = -Math.min(entryCount / 2, 4); i <= Math.min(entryCount / 2, 4); i++)
		{
			options.get(Math.floorMod((i + index), entryCount)).renderIcon(graphics, screenWidth / 2 - 10 + i * 20, 10, partialTicks, 1);
		}

        graphics.drawCenteredString(mc.font, options.get(index).getName(), screenWidth / 2, 32, 0xFFFFFF);

	}

	interface MenuItem
	{
		Component getName();
		void renderIcon(GuiGraphics graphics, int x, int y, float partialTicks, float alpha);
	}

	static class ItemStackMenuItem implements MenuItem
	{

		final Component name;
		final ItemStack stack;

		ItemStackMenuItem(ItemStack stack, Component name) {
			this.name = name;
			this.stack = stack;
		}

		@Override
		public Component getName() {
			return name;
		}

		@Override
		public void renderIcon(GuiGraphics graphics, int x, int y, float partialTicks, float alpha)
		{
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
			graphics.renderItem(this.stack, x, y);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1);
		}
	}

	static class PlayerMenuItem implements MenuItem
	{
		private final GameProfile profile;
		private final ResourceLocation location;
		private final Component name;

		public PlayerMenuItem(GameProfile profile) {
			this.profile = profile;
			Minecraft minecraft = Minecraft.getInstance();
			Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = minecraft.getSkinManager().getInsecureSkinInformation(profile);
			if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
				this.location = minecraft.getSkinManager().registerTexture(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
			} else {
				this.location = DefaultPlayerSkin.getDefaultSkin(UUIDUtil.getOrCreatePlayerUUID(profile));
			}

			this.name = Component.literal(profile.getName());
		}

		@Override
		public Component getName() {
			return name;
		}

		@Override
		public void renderIcon(GuiGraphics graphics, int x, int y, float partialTicks, float alpha)
		{
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            graphics.blit(location, x, y, 16, 16, 8.0F, 8.0F, 8, 8, 64, 64);
            graphics.blit(location, x, y, 16, 16, 40.0F, 8.0F, 8, 8, 64, 64);
			RenderSystem.setShaderColor(1, 1, 1, 1);
		}
	}
}
