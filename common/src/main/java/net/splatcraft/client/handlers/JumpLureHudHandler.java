package net.splatcraft.client.handlers;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.splatcraft.client.gui.SuperJumpSelectorScreen;
import net.splatcraft.items.JumpLureItem;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.UseJumpLurePacket;
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

    public static void registerEvents()
    {
        ClientRawInputEvent.MOUSE_SCROLLED.register(JumpLureHudHandler::onMouseScroll);
        ClientRawInputEvent.MOUSE_CLICKED_PRE.register(JumpLureHudHandler::onMouseClick);
        ClientTickEvent.CLIENT_PRE.register(JumpLureHudHandler::onKeypadInput);
    }

    public static void renderGui(DrawContext context, RenderTickCounter tickCounter)
    {
        ClientPlayerEntity player = ClientUtils.getClientPlayer();

        if (player == null ||
            !(player.getActiveItem().getItem() instanceof JumpLureItem) || targets == null)
            return;

        scrollDelta = selectorGui.render(context, tickCounter, targets, scrollDelta, clickedThisFrame);
        clickedThisFrame = false;
    }

    public static EventResult onMouseScroll(MinecraftClient client, double horizontalScroll, double verticalScroll)
    {
        ClientPlayerEntity player = ClientUtils.getClientPlayer();
        if (player != null && player.getActiveItem().getItem() instanceof JumpLureItem && player.isUsingItem())
        {
            scrollDelta -= horizontalScroll;
            return EventResult.interruptFalse();
        }
        return EventResult.pass();
    }

    public static EventResult onMouseClick(MinecraftClient client, int button, int action, int mods)
    {
        ClientPlayerEntity player = ClientUtils.getClientPlayer();
        if (player != null && player.getActiveItem().getItem() instanceof JumpLureItem && button == 0 && action == 1)
            clickedThisFrame = true;
        return EventResult.pass();
    }

    public static void onKeypadInput(MinecraftClient client)
    {
        ClientPlayerEntity player = ClientUtils.getClientPlayer();
        if (player == null)
            return;

        if (player.getActiveItem().getItem() instanceof JumpLureItem)
        {
            int totalOptions = targets.playerTargetUuids.size() + (targets.canTargetSpawn ? 2 : 1);

            for (int i = 0; i < totalOptions; i++) // ok tbh this is personal preference but for now this is going to work like this
            {
                KeyBinding key = MinecraftClient.getInstance().options.hotbarKeys[i];

                if (key.wasPressed())
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
        ClientPlayerEntity player = ClientUtils.getClientPlayer();

        if (player == null || targets == null) return;

        ArrayList<UUID> playerUuids = new ArrayList<>(targets.playerTargetUuids);
        playerUuids.removeIf(uuid -> !player.networkHandler.getPlayerUuids().contains(uuid));

        int entryCount = playerUuids.size() + (targets.canTargetSpawn ? 2 : 1);
        int index = Math.floorMod((int) scrollDelta, entryCount);

        SuperJumpTargets targets = JumpLureHudHandler.targets;
        updateTargetData(null);

        if (index == 0) return;
        UUID target = (targets.canTargetSpawn && index == 1) ? null : playerUuids.get(index - (targets.canTargetSpawn ? 2 : 1));

        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.remoteUse, SoundCategory.PLAYERS, 0.8F, 1);
        SplatcraftPacketHandler.sendToServer(new UseJumpLurePacket(targets.color, target));
        scrollDelta = 0;
    }

    public static class SuperJumpTargets
    {
        public final ArrayList<UUID> playerTargetUuids;
        public final boolean canTargetSpawn;
        public final InkColor color;
        public final BlockPos spawnPosition;

        public SuperJumpTargets(ArrayList<UUID> playerTargetUuids, boolean canTargetSpawn, InkColor color, BlockPos spawnPosition)
        {
            this.playerTargetUuids = playerTargetUuids;
            this.canTargetSpawn = canTargetSpawn;
            this.color = color;
            this.spawnPosition = spawnPosition;
        }
    }
}
