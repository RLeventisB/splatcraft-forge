package net.splatcraft.client.handlers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.splatcraft.client.gui.SuperJumpSelectorScreen;
import net.splatcraft.items.JumpLureItem;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.UseJumpLurePacket;
import net.splatcraft.platform.Services;
import net.splatcraft.platform.event.ClientRawInputEvent;
import net.splatcraft.platform.event.EventResult;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class JumpLureHudHandler
{
	private static final SuperJumpSelectorScreen selectorGui = new SuperJumpSelectorScreen();
	public static boolean clickedThisFrame = false;
	private static SuperJumpTargets targets;
	private static double scrollDelta = 0;
	@Environment(EnvType.CLIENT)
	public static void registerEvents()
	{
		Services.PLATFORM.registerListener(ClientRawInputEvent.MouseScrolled.class, JumpLureHudHandler::onMouseScroll);
		ClientRawInputEvent.MOUSE_SCROLLED.register(JumpLureHudHandler::onMouseScroll);
		ClientRawInputEvent.MOUSE_CLICKED_PRE.register(JumpLureHudHandler::onMouseClick);
		ClientTickEvent.CLIENT_PRE.register(JumpLureHudHandler::onKeypadInput);
	}
	public static void renderGui(GuiGraphics context, DeltaTracker tickCounter)
	{
		LocalPlayer player = ClientUtils.getClientPlayer();
		
		if (player == null ||
			!(player.getUseItem().getItem() instanceof JumpLureItem) || targets == null)
			return;
		
		scrollDelta = selectorGui.render(context, tickCounter, targets, scrollDelta, clickedThisFrame);
		clickedThisFrame = false;
	}
	public static EventResult onMouseScroll(Minecraft client, double horizontalScroll, double verticalScroll)
	{
		LocalPlayer player = ClientUtils.getClientPlayer();
		if (player != null && player.getUseItem().getItem() instanceof JumpLureItem && player.isUsingItem())
		{
			scrollDelta -= horizontalScroll;
			return EventResult.interruptFalse();
		}
		return EventResult.pass();
	}
	public static EventResult onMouseClick(Minecraft client, int button, int action, int mods)
	{
		LocalPlayer player = ClientUtils.getClientPlayer();
		if (player != null && player.getUseItem().getItem() instanceof JumpLureItem && button == 0 && action == 1)
			clickedThisFrame = true;
		return EventResult.pass();
	}
	public static void onKeypadInput(Minecraft client)
	{
		LocalPlayer player = ClientUtils.getClientPlayer();
		if (player == null)
			return;
		
		if (player.getUseItem().getItem() instanceof JumpLureItem)
		{
			int totalOptions = targets.playerTargetUuids.size() + (targets.canTargetSpawn ? 2 : 1);
			
			for (int i = 0; i < totalOptions; i++) // ok tbh this is personal preference but for now this is going to work like this
			{
				KeyMapping key = Minecraft.getInstance().options.keyHotbarSlots[i];
				
				if (key.consumeClick())
				{
					scrollDelta = i + 1;
					return;
				}
			}
		}
	}
	public static void updateTargetData(@Nullable SuperJumpTargets targets)
	{
		JumpLureHudHandler.targets = targets;
	}
	public static void releaseLure()
	{
		LocalPlayer player = ClientUtils.getClientPlayer();
		
		if (player == null || targets == null) return;
		
		ArrayList<UUID> playerUuids = new ArrayList<>(targets.playerTargetUuids);
		playerUuids.removeIf(uuid -> !player.connection.getOnlinePlayerIds().contains(uuid));
		
		int entryCount = playerUuids.size() + (targets.canTargetSpawn ? 2 : 1);
		int index = Math.floorMod((int) scrollDelta, entryCount);
		
		SuperJumpTargets targets = JumpLureHudHandler.targets;
		updateTargetData(null);
		
		if (index == 0) return;
		UUID target = (targets.canTargetSpawn && index == 1) ? null : playerUuids.get(index - (targets.canTargetSpawn ? 2 : 1));
		
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.remoteUse, SoundSource.PLAYERS, 0.8F, 1);
		SplatcraftPacketHandler.sendToServer(new UseJumpLurePacket(targets.color, target));
		scrollDelta = 0;
	}
	public record SuperJumpTargets(ArrayList<UUID> playerTargetUuids, boolean canTargetSpawn, InkColor color,
	                               BlockPos spawnPosition)
	{
	}
}
