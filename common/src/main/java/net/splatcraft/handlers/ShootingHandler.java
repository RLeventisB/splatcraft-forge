package net.splatcraft.handlers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.splatcraft.Splatcraft;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = Splatcraft.MODID)
public class ShootingHandler
{
    public static Map<LivingEntity, EntityData> shootingData = new HashMap<>();

    public static boolean notifyStartShooting(LivingEntity entity)
    {
        if (entity.getWorld().isClientSide())
        {
            return false;
        }
        EntityData entityData;
        if (shootingData.containsKey(entity))
        {
            entityData = shootingData.get(entity);
        }
        else
        {
            entityData = new EntityData(entity);
        }

        entityData.startShooting();
        entityData.doStartningEndlagAction();
        shootingData.put(entity, entityData);
        return true;
    }

    public static boolean notifyForceEndShooting(LivingEntity entity)
    {
        if (entity.getWorld().isClientSide())
        {
            return false;
        }
        EntityData entityData;
        if (shootingData.containsKey(entity))
        {
            entityData = shootingData.get(entity);
            entityData.mainHandData.end();
            entityData.offHandData.end();
            return true;
        }
        return false;
    }

    public static boolean isDoingShootingAction(LivingEntity entity)
    {
        return shootingData.containsKey(entity) && shootingData.get(entity).usedThisTick;
    }

    @SubscribeEvent
    public static void update(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
            return;

        for (Map.Entry<LivingEntity, EntityData> entry : shootingData.entrySet())
        {
            entry.getValue().usedThisTick = false;
            entry.getValue().update();
        }
    }

    @SubscribeEvent
    public static void onEntityDie(EntityLeaveLevelEvent event)
    {
        if (event.getEntity() instanceof LivingEntity livingEntity)
            shootingData.remove(livingEntity);
    }

    @SubscribeEvent
    public static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event)
    {
        shootingData.remove(event.getEntity());
    }

    public interface EndlagConsumer
    {
        boolean accept(WeaponShootingData data, Float timeLeft, LivingEntity entity, Boolean isStillUsing);
    }

    public interface ShootConsumer
    {
        void accept(WeaponShootingData data, Float timeLeft, LivingEntity entity);
    }

    public interface EndConsumer
    {
        void accept(WeaponShootingData data, Float timeLeft, LivingEntity entity);
    }

    public static class EntityData
    {
        public final boolean isPlayer;
        public final Player player;
        public final LivingEntity entity;
        public final WeaponShootingData mainHandData;
        public final WeaponShootingData offHandData;
        public boolean usedThisTick;
        public int selected;

        public EntityData(LivingEntity entity)
        {
            isPlayer = entity instanceof Player;
            player = isPlayer ? (Player) entity : null;
            this.entity = entity;
            mainHandData = new WeaponShootingData(this, InteractionHand.MAIN_HAND);
            offHandData = new WeaponShootingData(this, InteractionHand.OFF_HAND);
        }

        public boolean isDualFire()
        {
            return mainHandData.active && offHandData.active;
        }

        public void update()
        {
            if (mainHandData.active || offHandData.active)
            {
                if (isPlayer)
                {
                    player.getInventory().selected = selected;
                }
                if (mainHandData.active)
                {
                    boolean isUsing = entity.isUsingItem() && mainHandData.isUsingWeaponEqualToStoredWeapon(entity);
                    mainHandData.update(entity, isUsing);
                }
                if (offHandData.active)
                {
                    boolean isUsing = entity.isUsingItem() && offHandData.isUsingWeaponEqualToStoredWeapon(entity);
                    offHandData.update(entity, isUsing);
                }
            }

            if (mainHandData.active || offHandData.active)
            {
                usedThisTick = true;
            }
        }

        public void startShooting()
        {
            if (mainHandData.active || offHandData.active)
                return;

            if (isPlayer)
                selected = player.getInventory().selected;
            ItemStack mainHand = entity.getItemInHand(InteractionHand.MAIN_HAND);
            FiringStatData weaponFireData = null;
            if (mainHand.getItem() instanceof WeaponBaseItem<?> mainHandWeapon)
            {
                weaponFireData = mainHandWeapon.getWeaponFireData(mainHand, entity);
                mainHandData.start(mainHand, weaponFireData);
            }
            ItemStack offHand = entity.getItemInHand(InteractionHand.OFF_HAND);
            if (offHand.getItem() instanceof WeaponBaseItem<?> offHandWeapon)
            {
                FiringStatData offHandFireData = offHandWeapon.getWeaponFireData(offHand, entity);
                if (weaponFireData != null)
                    offHandData.start(offHand, offHandFireData, CommonUtils.startupSquidSwitch(entity, weaponFireData), offHandFireData.getFiringSpeed() / 2);
                else
                    offHandData.start(offHand, offHandFireData);
            }
            usedThisTick = true;
        }

        public void doStartningEndlagAction()
        {
            if (mainHandData.active && mainHandData.firingData.onEndlagEnd != null)
                mainHandData.firingData.onEndlagEnd.accept(mainHandData, 0f, entity, true);
            if (offHandData.active && offHandData.firingData.onEndlagEnd != null)
                offHandData.firingData.onEndlagEnd.accept(offHandData, 0f, entity, true);
        }

        public void recalculateFiringData()
        {
            ItemStack mainHand = entity.getItemInHand(InteractionHand.MAIN_HAND);
            if (mainHand.getItem() instanceof WeaponBaseItem<?> mainHandWeapon)
            {
                FiringStatData weaponFireData = mainHandWeapon.getWeaponFireData(mainHand, entity);
                mainHandData.modifyFiringData(weaponFireData);
            }
            ItemStack offHand = entity.getItemInHand(InteractionHand.OFF_HAND);
            if (offHand.getItem() instanceof WeaponBaseItem<?> offHandWeapon)
            {
                FiringStatData weaponFireData = offHandWeapon.getWeaponFireData(mainHand, entity);
                offHandData.modifyFiringData(weaponFireData);
            }
        }
    }

    public static class WeaponShootingData
    {
        public final InteractionHand hand;
        public final EntityData entityData;
        public boolean doingEndlag, active;
        public float timer, offHandTimeout;
        public ItemStack useItem;
        public FiringStatData firingData;

        //        public static long milli;
        public WeaponShootingData(EntityData data, InteractionHand hand)
        {
            this.entityData = data;
            this.hand = hand;
        }

        public void start(ItemStack item, FiringStatData firingData)
        {
            start(item, firingData, CommonUtils.startupSquidSwitch(entityData.entity, firingData), 0);
        }

        public void modifyFiringData(FiringStatData data)
        {
            if (active && data.getFiringSpeed() != firingData.getFiringSpeed())
            {
                // TODO: do conversion in case of dualies having different firing speeds, maybe, i think
            }
            firingData = data;
        }

        public void start(ItemStack item, FiringStatData firingData, float initialTimer, float offHandTimeout)
        {
            if (active)
                return;

            this.firingData = firingData;
            this.timer = initialTimer;
            doingEndlag = false;
            this.useItem = item.copy();
            this.offHandTimeout = offHandTimeout;
            active = true;
        }

        public void update(LivingEntity entity, boolean isUsing)
        {
            if (!isUsingWeaponEqualToStoredWeapon(entity))
            {
                end();
                return;
            }
            if (offHandTimeout > 0)
            {
                if (!isUsing)
                {
                    end();
                    return;
                }
                offHandTimeout--;
                if (offHandTimeout < 0)
                {
                    timer += offHandTimeout;
                    offHandTimeout = 0;
                }
                else
                    return;
            }
            while (timer <= 0)
            {
                if (doingEndlag)
                {
                    if (firingData.onEndlagEnd != null)
                        isUsing = firingData.onEndlagEnd.accept(this, -timer, entity, isUsing);
                    if (isUsing)
                    {
                        timer += firingData.startupFrames;
                    }
                    else
                    {
                        if (firingData.onEnd != null)
                        {
                            firingData.onEnd.accept(this, -timer, entity);
                        }
                        end();
                        return;
                    }
                }
                else
                {
//                    long currentMilli = System.currentTimeMillis() + (int) (timer * 1000 / 20);
//                    Splatcraft.LOGGER.debug("Time between shots: " + ((currentMilli - milli) / 1000f * 20));
//                    milli = currentMilli;
                    if (firingData.onShoot != null)
                        firingData.onShoot.accept(this, -timer, entity);
                    timer += firingData.endlagFrames;
                }

                doingEndlag = !doingEndlag;
            }
            timer--;
        }

        public void end()
        {
            active = false;
            doingEndlag = false;
        }

        public boolean isUsingWeaponEqualToStoredWeapon(@NotNull LivingEntity entity)
        {
            return entity.getItemInHand(hand).is(useItem.getItem());
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof WeaponShootingData that)) return false;
            return doingEndlag == that.doingEndlag && active == that.active && Float.compare(timer, that.timer) == 0 && hand == that.hand && Objects.equals(entityData, that.entityData) && Objects.equals(useItem, that.useItem) && Objects.equals(firingData, that.firingData);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(hand, entityData, doingEndlag, active, timer, useItem, firingData);
        }
    }

    public record FiringStatData(float squidStartupFrames, float startupFrames, float endlagFrames,
                                 EndlagConsumer onEndlagEnd,
                                 ShootConsumer onShoot, EndConsumer onEnd)
    {
        public static final Codec<FiringStatData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("squid_startup_frames", 2f).forGetter(FiringStatData::squidStartupFrames),
                Codec.FLOAT.fieldOf("startup_frames").forGetter(FiringStatData::startupFrames),
                Codec.FLOAT.fieldOf("endlag_frames").forGetter(FiringStatData::endlagFrames)
            ).apply(instance, (Float squidStartupFrames, Float startupFrames, Float endlagFrames) -> new FiringStatData(squidStartupFrames, startupFrames, endlagFrames, null, null, null))
        );
        public static final FiringStatData DEFAULT = new FiringStatData(2, 1, 1, null, null, null);

        public float getFiringSpeed()
        {
            return startupFrames + endlagFrames;
        }
    }
}