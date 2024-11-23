package net.splatcraft.forge.handlers;

import net.minecraft.world.entity.LivingEntity;
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

@Mod.EventBusSubscriber(modid = Splatcraft.MODID)
public class ShootingHandler
{
    public static Map<LivingEntity, FiringData> firingData = new HashMap<>();

    public static boolean notifyStartShooting(LivingEntity entity, float firstUseStartupFrames, float startupFrames, float endlagFrames, EndlagConsumer onEndlagEnd, ShootConsumer onShoot, EndConsumer onEnd)
    {
        if (entity.level().isClientSide() || isDoingShootingAction(entity))
            return false;

        FiringData data = new FiringData(entity, entity.getUseItem().copy(), firstUseStartupFrames, startupFrames, endlagFrames, onEndlagEnd, onShoot, onEnd);
        if (onEndlagEnd != null)
            onEndlagEnd.accept(data, 0f, entity, true);
        firingData.put(entity, data);
        return true;
    }

    public static void handleShooting(LivingEntity entity)
    {
        if (entity.level().isClientSide() || !isDoingShootingAction(entity))
            return;

        FiringData data = firingData.get(entity);

        data.update(entity);
    }

    public static boolean isDoingShootingAction(LivingEntity entity)
    {
        return firingData.containsKey(entity);
    }

    public interface EndlagConsumer
    {
        void accept(FiringData data, Float timeLeft, LivingEntity entity, Boolean isStillUsing);
    }

    public interface ShootConsumer
    {
        void accept(FiringData data, Float timeLeft, LivingEntity entity);
    }

    public interface EndConsumer
    {
        void accept(FiringData data, Float timeLeft, LivingEntity entity);
    }

    @SubscribeEvent
    public static void update(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
            return;

        for (Map.Entry<LivingEntity, FiringData> entry : firingData.entrySet())
        {
            if (!entry.getValue().usedThisTick)
            {
                firingData.remove(entry.getKey());
            }
            entry.getValue().usedThisTick = false;
        }
    }

    @SubscribeEvent
    public static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event)
    {
        firingData.remove(event.getEntity());
    }

    public static class FiringData
    {
        public boolean usedThisTick, doingEndlag;
        public final int selected;
        public float timer;
        public final ItemStack useItem;
        public final boolean isPlayer;
        public final Player player;
        public float startupFrames;
        public float endlagFrames;
        public EndlagConsumer onEndlagEnd;
        public ShootConsumer onShoot;
        public EndConsumer onEnd;
//        public static long milli;

        public FiringData(LivingEntity entity, ItemStack item, float initialTimer, float startupFrames, float endlagFrames, EndlagConsumer onEndlagEnd, ShootConsumer onShoot, EndConsumer onEnd)
        {
            isPlayer = entity instanceof Player;
            player = isPlayer ? (Player) entity : null;
            this.startupFrames = startupFrames;
            this.endlagFrames = endlagFrames;
            this.onEndlagEnd = onEndlagEnd;
            this.onShoot = onShoot;
            this.onEnd = onEnd;
            this.selected = isPlayer ? player.getInventory().selected : -1;
            this.timer = initialTimer;
            this.useItem = item;
        }

        public void update(LivingEntity entity)
        {
            boolean isUsing = entity.isUsingItem() && entity.getUseItem().getItem().equals(useItem.getItem());

            while (timer <= 0)
            {
                if (doingEndlag)
                {
                    if (onEndlagEnd != null)
                        onEndlagEnd.accept(this, -timer, entity, isUsing);
                    if (isUsing)
                    {
                        timer += startupFrames;
                    }
                    else
                    {
                        if (onEnd != null)
                        {
                            onEnd.accept(this, -timer, entity);
                        }
                        ShootingHandler.firingData.remove(entity);
                        return;
                    }
                }
                else
                {
//                    long currentMilli = System.currentTimeMillis() + (int) (timer * 1000 / 20);
//                    Splatcraft.LOGGER.log(Level.DEBUG, "Time between shots: " + ((currentMilli - milli) / 1000f * 20));
//                    milli = currentMilli;
                    if (onShoot != null)
                        onShoot.accept(this, -timer, entity);
                    timer += endlagFrames;
                }
                if (isPlayer)
                    player.getInventory().selected = selected;

                doingEndlag = !doingEndlag;
            }
            usedThisTick = true;
            timer--;
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