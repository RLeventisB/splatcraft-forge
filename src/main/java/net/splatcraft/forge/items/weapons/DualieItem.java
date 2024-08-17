package net.splatcraft.forge.items.weapons;

import com.google.common.collect.Lists;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.entities.ExtraSaveData;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.handlers.PlayerPosingHandler;
import net.splatcraft.forge.items.weapons.settings.CommonRecords;
import net.splatcraft.forge.items.weapons.settings.DualieWeaponSettings;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.c2s.DodgeRollEndPacket;
import net.splatcraft.forge.network.c2s.DodgeRollPacket;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.util.ClientUtils;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.InkExplosion;
import net.splatcraft.forge.util.PlayerCooldown;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@SuppressWarnings("UnusedReturnValue")
public class DualieItem extends WeaponBaseItem<DualieWeaponSettings>
{
    public static final ArrayList<DualieItem> dualies = Lists.newArrayList();
    public String settings;

    public static RegistryObject<DualieItem> create(DeferredRegister<Item> registry, String settings)
    {
        return registry.register(settings, () -> new DualieItem(settings));
    }

    public static RegistryObject<DualieItem> create(DeferredRegister<Item> registry, RegistryObject<DualieItem> parent, String name)
    {
        return registry.register(name, () -> new DualieItem(parent.get().settingsId.toString()));
    }

    public static RegistryObject<DualieItem> create(DeferredRegister<Item> registry, String settings, String name)
    {
        return registry.register(name, () -> new DualieItem(settings));
    }

    protected DualieItem(String settings)
    {
        super(settings);

        this.settings = settings;

        dualies.add(this);
    }

    @Override
    public Class<DualieWeaponSettings> getSettingsClass()
    {
        return DualieWeaponSettings.class;
    }

    private static float getInkForRoll(ItemStack stack)
    {
        return stack.getItem() instanceof DualieItem ? ((DualieItem) stack.getItem()).getSettings(stack).rollData.inkConsumption() : 0;
    }

    public static int getRollTurretDuration(ItemStack stack, boolean lastRoll)
    {
        if (stack.getItem() instanceof DualieItem dualie)
            return lastRoll ? dualie.getSettings(stack).rollData.lastRollTurretDuration() : dualie.getSettings(stack).rollData.turretDuration();

        return 0;
    }

    public void performRoll(Player player, ItemStack activeDualie, InteractionHand hand, int maxRolls, Vec2 rollDirection, boolean local)
    {
        int rollCount = getRollCount(player);

        boolean lastRoll = rollCount == maxRolls - 1;
        if (rollCount > maxRolls - 1)
        {
            return;
        }
        DualieWeaponSettings activeSettings = getSettings(activeDualie);

        if (reduceInk(player, this, getInkForRoll(activeDualie), activeSettings.rollData.inkRecoveryCooldown(), !player.level().isClientSide()))
        {
            int turretDuration = getRollTurretDuration(activeDualie, lastRoll);
            PlayerCooldown.setPlayerCooldown(player, new DodgeRollCooldown(activeDualie, player.getInventory().selected, hand, rollDirection, activeSettings.rollData.rollStartup(), activeSettings.rollData.rollEndlag(), (byte) turretDuration, activeSettings.rollData.canMove(), lastRoll));

            PlayerInfoCapability.get(player).setDodgeCount(rollCount + 1);
            activeSettings.rollData.speed();
        }
    }

    public static int getRollCount(Player player)
    {
        return PlayerInfoCapability.hasCapability(player) ? PlayerInfoCapability.get(player).getDodgeCount() : -1;
    }

    public static int getMaxRollCount(Player player)
    {
        float maxRolls = 0;
        if (player.getMainHandItem().getItem() instanceof DualieItem dualieItem)
        {
            maxRolls += dualieItem.getSettings(player.getMainHandItem()).rollData.count();
        }
        if (player.getOffhandItem().getItem() instanceof DualieItem dualieItem)
        {
            maxRolls += dualieItem.getSettings(player.getOffhandItem()).rollData.count();
        }
        return (int) maxRolls;
    }

    public ClampedItemPropertyFunction getIsLeft()
    {
        return (stack, level, entity, seed) ->
        {
            if (entity == null)
            {
                return 0;
            }
            else
            {
                entity.getMainArm();
            }
            boolean mainLeft = entity.getMainArm().equals(HumanoidArm.LEFT);
            return mainLeft && entity.getMainHandItem().equals(stack) || !mainLeft && entity.getOffhandItem().equals(stack) ? 1 : 0;
        };
    }

    @Override
    public @NotNull String getDescriptionId(ItemStack stack)
    {
        if (stack.getOrCreateTag().getBoolean("IsPlural"))
        {
            return getDescriptionId() + ".plural";
        }
        return super.getDescriptionId(stack);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack)
    {
        return super.getName(stack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int itemSlot, boolean isSelected)
    {
        super.inventoryTick(stack, level, entity, itemSlot, isSelected);

        CompoundTag nbt = stack.getOrCreateTag();

        nbt.putBoolean("IsPlural", false);
        if (entity instanceof LivingEntity livingEntity)
        {
            InteractionHand hand = livingEntity.getItemInHand(InteractionHand.MAIN_HAND).equals(stack) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

            if (livingEntity.getItemInHand(hand).equals(stack) && livingEntity.getItemInHand(InteractionHand.values()[(hand.ordinal() + 1) % InteractionHand.values().length]).getItem().equals(stack.getItem()))
            {
                nbt.putBoolean("IsPlural", true);
            }
            if (entity instanceof LocalPlayer localPlayer && localPlayer.getItemInHand(hand) == stack && localPlayer.getUsedItemHand() == hand)
            {
                ItemStack offhandDualie = ItemStack.EMPTY;
                if (localPlayer.getUsedItemHand().equals(InteractionHand.OFF_HAND) && localPlayer.getOffhandItem().equals(stack) && localPlayer.getOffhandItem().getItem() instanceof DualieItem)
                {
                    offhandDualie = localPlayer.getOffhandItem();
                }

                int rollCount = getRollCount(localPlayer);
                int maxRolls = getMaxRollCount(localPlayer);
                if (rollCount < maxRolls && ClientUtils.canPerformRoll(localPlayer))
                {
                    ItemStack activeDualie;
                    boolean lastRoll = rollCount == maxRolls - 1;
                    if (lastRoll)
                    {
                        activeDualie = getRollTurretDuration(stack, true) >= getRollTurretDuration(offhandDualie, true) ? stack : offhandDualie;
                    }
                    else
                    {
                        activeDualie = maxRolls % 2 == 1 && offhandDualie.getItem() instanceof DualieItem ? offhandDualie : stack;
                    }
                    DualieWeaponSettings.RollDataRecord activeSettings = getSettings(activeDualie).rollData;
                    // why does vec2 use floats but vec3 use doubles

                    if (reduceInk(localPlayer, this, getInkForRoll(activeDualie), activeSettings.inkRecoveryCooldown(), false))
                    {
                        Vec2 rollDirection = ClientUtils.getDodgeRollVector(localPlayer, activeSettings.speed());

                        performRoll(localPlayer, stack, hand, maxRolls, rollDirection, true);
                        SplatcraftPacketHandler.sendToServer(new DodgeRollPacket(localPlayer.getUUID(), activeDualie, hand, maxRolls, rollDirection));
                    }
                }
            }
        }
    }

    @Override
    public void weaponUseTick(Level level, LivingEntity entity, ItemStack stack, int timeLeft)
    {
        ItemStack offhandDualie = ItemStack.EMPTY;
        if (entity.getUsedItemHand().equals(InteractionHand.MAIN_HAND) && entity.getMainHandItem().equals(stack) && entity.getOffhandItem().getItem() instanceof DualieItem)
        {
            offhandDualie = entity.getOffhandItem();
        }

        Player player = (Player) entity;
        player.setYBodyRot(player.getYHeadRot()); // actually uncanny in third person but itll be useful when making dualies shoot actually from their muzzles

        if (!level.isClientSide())
        {
            player.yBodyRotO = player.getYHeadRot();
            int rollCount = getRollCount(player);

            boolean hasCooldown = PlayerCooldown.hasPlayerCooldown(player);
            boolean onRollCooldown = hasCooldown && rollCount >= 1;

            if (offhandDualie.getItem() instanceof DualieItem dualieItem)
            {
                DualieWeaponSettings settings = dualieItem.getSettings(offhandDualie);
                CommonRecords.ShotDataRecord firingData = onRollCooldown ? settings.turretShotData : settings.standardShotData;

                dualieItem.fireDualie(level, entity, offhandDualie, timeLeft + firingData.getFiringSpeed() / 2, entity.onGround() && hasCooldown);
            }
            fireDualie(level, entity, stack, timeLeft, onRollCooldown);
        }
    }

    protected void fireDualie(Level level, LivingEntity entity, ItemStack stack, int timeLeft, boolean onRollCooldown)
    {
        DualieWeaponSettings settings = getSettings(stack);
        CommonRecords.ShotDataRecord firingData = onRollCooldown ? settings.turretShotData : settings.standardShotData;
        CommonRecords.ProjectileDataRecord projectileData = onRollCooldown ? settings.turretProjectileData : settings.standardProjectileData;
        if (!level.isClientSide() && (getUseDuration(stack) - timeLeft - 1) % (firingData.getFiringSpeed()) == 0)
        {
            if (reduceInk(entity, this, firingData.inkConsumption(), firingData.inkRecoveryCooldown(), true))
            {
                for (int i = 0; i < firingData.projectileCount(); i++)
                {
                    InkProjectileEntity proj = new InkProjectileEntity(level, entity, stack, InkBlockUtils.getInkType(entity), projectileData.size(), settings);
                    proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), firingData.pitchCompensation(), projectileData.speed(), entity.onGround() ? firingData.groundInaccuracy() : firingData.airborneInaccuracy());
                    proj.addExtraData(new ExtraSaveData.DualieExtraData(onRollCooldown));
                    proj.setDualieStats(projectileData);
                    level.addFreshEntity(proj);
                }

                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.dualieShot, SoundSource.PLAYERS, 0.7F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
            }
        }
    }

    @Override
    public PlayerPosingHandler.WeaponPose getPose(ItemStack stack)
    {
        return PlayerPosingHandler.WeaponPose.DUAL_FIRE;
    }

    public static class DodgeRollCooldown extends PlayerCooldown
    {
        final byte rollFrame, rollEndFrame, turretModeFrame;
        static final byte ROLL_DURATION = 4;
        final boolean lastRoll;
        final Vec2 rollDirection;
        boolean canSlide;

        public DodgeRollCooldown(ItemStack stack, int slotIndex, InteractionHand hand, Vec2 rollDirection, byte startupFrames, byte endlagFrames, byte turretModeFrames, boolean canSlide, boolean lastRoll)
        {
            super(stack, startupFrames + ROLL_DURATION + endlagFrames + turretModeFrames, slotIndex, hand, false, false, true, false);
            this.rollDirection = rollDirection;
            this.rollFrame = (byte) (ROLL_DURATION + turretModeFrames + endlagFrames);
            this.rollEndFrame = (byte) (turretModeFrames + endlagFrames);
            this.turretModeFrame = turretModeFrames;
            this.lastRoll = lastRoll;
            this.canSlide = canSlide;
        }

        public DodgeRollCooldown(CompoundTag nbt)
        {
            this(ItemStack.of(nbt.getCompound("StoredStack")),
                    nbt.getInt("SlotIndex"),
                    nbt.getBoolean("Hand") ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND,
                    new Vec2(nbt.getFloat("RollDirectionX"), nbt.getFloat("RollDirectionZ")),
                    nbt.getByte("StartupFrames"),
                    nbt.getByte("EndlagFrames"),
                    nbt.getByte("TurretModeFrames"),
                    nbt.getBoolean("CanSlide"),
                    nbt.getBoolean("LastRoll"));
            setTime(nbt.getInt("Time"));
        }

        @Override
        public void tick(Player player)
        {
            boolean local = player.level().isClientSide;
            if (getTime() == rollFrame)
            {
                if (!local)
                {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.dualieDodge, SoundSource.PLAYERS, 0.7F, ((player.level().random.nextFloat() - player.level().getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
                    InkExplosion.createInkExplosion(player, player.position(), 0.9f, 0, 0, InkBlockUtils.getInkType(player), storedStack);
                }
                player.setDeltaMovement(rollDirection.x, -0.5, rollDirection.y);
            }
            else if (getTime() == rollEndFrame + 1)
            {
                player.setDiscardFriction(false);
            }
            else if (getTime() == 1)
            {
                if (local)
                {
                    Input input = ClientUtils.getUnmodifiedInput((LocalPlayer) player);
                    boolean endedTurretMode = input.jumping || input.forwardImpulse != 0 || input.leftImpulse != 0 || !player.isUsingItem() || player.getDeltaMovement().y > 0.1;
                    if (endedTurretMode)
                    {
                        setTime(0);
                        PlayerInfoCapability.get(player).setDodgeCount(0);
                        SplatcraftPacketHandler.sendToServer(new DodgeRollEndPacket(player.getUUID()));
                    }
                    else
                    {
                        setTime(2);
                    }
                }
                else if (getTime() > 0)
                {
                    setTime(2);
                }
            }
        }

        @Override
        public void onStart(Player player)
        {
            player.setDiscardFriction(true);
        }

        @Override
        public CompoundTag writeNBT(CompoundTag nbt)
        {
            nbt.putBoolean("DodgeRollCooldown", true);
            int endlagFrames = rollEndFrame - turretModeFrame;
            nbt.put("StoredStack", storedStack.serializeNBT());
            nbt.putInt("SlotIndex", getSlotIndex());
            nbt.putBoolean("Hand", getHand() == InteractionHand.MAIN_HAND);
            nbt.putByte("StartupFrames", (byte) (getMaxTime() - ROLL_DURATION - endlagFrames - turretModeFrame));
            nbt.putByte("EndlagFrames", (byte) (endlagFrames));
            nbt.putByte("TurretModeFrames", turretModeFrame);
            nbt.putBoolean("CanSlide", canSlide);
            nbt.putBoolean("LastRoll", isLastRoll());
            nbt.putFloat("RollDirectionX", rollDirection.x);
            nbt.putFloat("RollDirectionZ", rollDirection.y);
            nbt.putInt("Time", getTime());
            return nbt;
        }

        public boolean isLastRoll()
        {
            return lastRoll;
        }

        public boolean canCancelRoll()
        {
            return getTime() <= rollEndFrame;
        }

        @Override
        public boolean canMove()
        {
            return canSlide && getTime() <= rollEndFrame;
        }

        @Override
        public boolean forceCrouch()
        {
            return getTime() > turretModeFrame - 8;
        }

        @Override
        public boolean preventWeaponUse()
        {
            return getTime() > turretModeFrame;
        }
    }
}