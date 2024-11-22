package net.splatcraft.forge.handlers;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.splatcraft.forge.Splatcraft;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = Splatcraft.MODID)
public class ShootingHandler
{
    public static Map<Player, Float> firingTimer = new HashMap<>();

    public static void handleShooting(Player player, float startupFrames, float endlagFrames, BiConsumer<Float, Player> onEndlagEnd, BiConsumer<Float, Player> onShoot)
    {

    }

    @SubscribeEvent
    public static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event)
    {
        firingTimer.remove(event.getEntity());
    }

    public record FiringData(float timer, Item item)
    {

    }
}