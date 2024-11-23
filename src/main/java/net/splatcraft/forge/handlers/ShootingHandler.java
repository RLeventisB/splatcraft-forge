package net.splatcraft.forge.handlers;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.splatcraft.forge.Splatcraft;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = Splatcraft.MODID)
public class ShootingHandler
{
    public static Map<Player, FiringData> firingTimer = new HashMap<>();

    public static void handleShooting(Player player, float firstUseStartupFrames, float startupFrames, float endlagFrames, EndlagConsumer onEndlagEnd, BiConsumer<Float, Player> onShoot, BiConsumer<Float, Player> onEnd)
    {
        FiringData data;
        if (firingTimer.containsKey(player))
        {
            data = firingTimer.get(player);
        }
        else
        {
            data = new FiringData(player.getUseItem().copy(), player.getInventory().selected, firstUseStartupFrames);
        }
        boolean isUsing = player.isUsingItem() && player.getUseItem().equals(data.useItem);

        while (data.timer <= 0)
        {
            if (data.doingEndlag)
            {
                onEndlagEnd.accept(-data.timer, player, isUsing);
                if (isUsing)
                {
                    data.timer += startupFrames;
                }
                else
                {
                    onEnd.accept(data.timer, player);
//                    setTime(0);
//                    SplatcraftPacketHandler.sendToServer(new WeaponUseEndPacket(player.getUUID()));
                    break;
                }
            }
            else
            {
                onShoot.accept(-data.timer, player);
                data.timer += endlagFrames;
            }
            player.getInventory().selected = data.selected;

            data.doingEndlag = !data.doingEndlag;
        }
        data.usedThisTick = true;
        data.timer--;
        firingTimer.put(player, data);
    }

    public static boolean isDoingShootingAction(Player player)
    {
        return firingTimer.containsKey(player);
    }

    public interface EndlagConsumer
    {
        void accept(Float timeLeft, Player player, Boolean isStillUsing);
    }

    @SubscribeEvent
    public static void update(TickEvent.ServerTickEvent event)
    {
        for (Map.Entry<Player, FiringData> entry : firingTimer.entrySet())
        {
            if (!entry.getValue().usedThisTick)
            {
                firingTimer.remove(entry.getKey());
            }
            entry.getValue().usedThisTick = false;
        }
    }

    @SubscribeEvent
    public static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event)
    {
        firingTimer.remove(event.getEntity());
    }

    public static class FiringData
    {
        public boolean usedThisTick, doingEndlag = true;
        public final int selected;
        public float timer;
        public final ItemStack useItem;

        public FiringData(ItemStack item, int selected, float initialTimer)
        {
            this.selected = selected;
            this.timer = initialTimer;
            this.useItem = item;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (!(obj instanceof FiringData that)) return false;
            return Float.floatToIntBits(this.timer) == Float.floatToIntBits(that.timer) &&
                Objects.equals(this.useItem, that.useItem) && usedThisTick == that.usedThisTick && doingEndlag == that.doingEndlag;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(timer, useItem, usedThisTick, doingEndlag);
        }

        @Override
        public String toString()
        {
            return "FiringData[" +
                "timer=" + timer + ", " +
                "usedThisTick=" + usedThisTick + ", " +
                "doingEndlag=" + doingEndlag + ", " +
                "useItem=" + useItem + ']';
        }
    }
}