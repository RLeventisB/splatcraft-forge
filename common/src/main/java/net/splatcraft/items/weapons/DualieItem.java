package net.splatcraft.items.weapons;

import com.google.common.collect.Lists;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.nbt.NbtCompound;
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
import net.splatcraft.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.handlers.ShootingHandler;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.items.weapons.settings.DualieWeaponSettings;
import net.splatcraft.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.DodgeRollEndPacket;
import net.splatcraft.network.c2s.DodgeRollPacket;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@SuppressWarnings("UnusedReturnValue")
public class DualieItem extends WeaponBaseItem<DualieWeaponSettings>
{
    public static final ArrayList<DualieItem> dualies = Lists.newArrayList();
    public String settings;

    protected DualieItem(String settings)
    {
        super(settings);

        this.settings = settings;

        dualies.add(this);
    }

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

    private static float getInkForRoll(ItemStack stack)
    {
        return stack.getItem() instanceof DualieItem item ? item.getSettings(stack).rollData.inkConsumption() : 0;
    }

    public static int getRollTurretDuration(ItemStack stack)
    {
        if (stack.getItem() instanceof DualieItem dualie)
            return dualie.getSettings(stack).rollData.turretDuration();

        return 0;
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

    @Override
    public Class<DualieWeaponSettings> getSettingsClass()
    {
        return DualieWeaponSettings.class;
    }

    public void performRoll(Player player, ItemStack activeDualie, InteractionHand hand, int maxRolls, Vec2 rollPotency, boolean local)
    {
        int rollCount = getRollCount(player);

        DualieWeaponSettings activeSettings = getSettings(activeDualie);

        if (reduceInk(player, this, getInkForRoll(activeDualie), activeSettings.rollData.inkRecoveryCooldown(), !player.getWorld().isClientSide()))
        {
            ShootingHandler.notifyForceEndShooting(player);
            int turretDuration = getRollTurretDuration(activeDualie);
            PlayerCooldown.setPlayerCooldown(player, new DodgeRollCooldown(activeDualie, player.getInventory().selected, hand, rollPotency, activeSettings.rollData.rollStartup(), activeSettings.rollData.rollDuration(), activeSettings.rollData.rollEndlag(), (byte) turretDuration, activeSettings.rollData.canMove()));

            PlayerInfoCapability.get(player).setDodgeCount(rollCount + 1);
        }
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

        NbtCompound nbt = stack.getOrCreateTag();

        nbt.putBoolean("IsPlural", false);
        if (entity instanceof LivingEntity livingEntity)
        {
            InteractionHand hand = livingEntity.getItemInHand(InteractionHand.MAIN_HAND).equals(stack) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

            if (livingEntity.getItemInHand(hand).equals(stack) && livingEntity.getItemInHand(InteractionHand.values()[(hand.ordinal() + 1) % InteractionHand.values().length]).getItem().equals(stack.getItem()))
            {
                nbt.putBoolean("IsPlural", true);
            }
            if (entity instanceof LocalPlayer localPlayer && localPlayer.getItemInHand(hand) == stack && localPlayer.getUsedItemHand() == hand && localPlayer.isUsingItem())
            {
                ItemStack offhandDualie = ItemStack.EMPTY;
                if (localPlayer.getUsedItemHand().equals(InteractionHand.OFF_HAND) && localPlayer.getOffhandItem().equals(stack) && localPlayer.getOffhandItem().getItem() instanceof DualieItem)
                {
                    offhandDualie = localPlayer.getOffhandItem();
                }

                int rollCount = getRollCount(localPlayer);
                int maxRolls = getMaxRollCount(localPlayer);
                if (rollCount > 0 && !PlayerCooldown.hasPlayerCooldown(localPlayer)) // fix just in case
                {
                    rollCount = 0;
                }
                if (rollCount < maxRolls && ClientUtils.canPerformRoll(localPlayer))
                {
                    ItemStack activeDualie;
                    boolean lastRoll = false;
                    if (lastRoll)
                    {
                        activeDualie = getRollTurretDuration(stack) >= getRollTurretDuration(offhandDualie) ? stack : offhandDualie;
                    }
                    else
                    {
                        activeDualie = rollCount % 2 == 1 && offhandDualie.getItem() instanceof DualieItem ? offhandDualie : stack;
                    }
                    DualieWeaponSettings.RollDataRecord activeSettings = getSettings(activeDualie).rollData;
                    // why does vec2 use floats but vec3 use doubles

                    if (enoughInk(localPlayer, this, getInkForRoll(activeDualie), activeSettings.inkRecoveryCooldown(), false))
                    {
                        Vec2 rollPotency = ClientUtils.getDodgeRollVector(localPlayer, activeSettings.getRollImpulse());

                        performRoll(localPlayer, stack, hand, maxRolls, rollPotency, true);
                        SplatcraftPacketHandler.sendToServer(new DodgeRollPacket(localPlayer.getUUID(), activeDualie, hand, maxRolls, rollPotency));
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

        ShootingHandler.notifyStartShooting(entity);

        /*if (!level.isClientSide())
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
        }*/
    }

    @Override
    public ShootingHandler.FiringStatData getWeaponFireData(ItemStack stack, LivingEntity entity)
    {
        DualieWeaponSettings settings = getSettings(stack);
        CommonRecords.ShotDataRecord shotData = settings.getShotData(entity);
        CommonRecords.ProjectileDataRecord projectileData = settings.getProjectileData(entity);
        Level level = entity.getWorld();
        return new ShootingHandler.FiringStatData(shotData.squidStartupTicks(), shotData.startupTicks(), shotData.endlagTicks(),
            null,
            (data, accumulatedTime, entity1) ->
            {
                if (!level.isClientSide())
                {
                    if (reduceInk(entity1, this, shotData.inkConsumption(), shotData.inkRecoveryCooldown(), true))
                    {
                        float inaccuracy = ShotDeviationHelper.updateShotDeviation(stack, level.getRandom(), shotData.accuracyData());
                        ShotDeviationHelper.DeviationData deviationData = ShotDeviationHelper.getDeviationData(stack);
                        ItemStack otherHand = entity.getItemInHand(CommonUtils.otherHand(data.hand));
                        if (!otherHand.isEmpty() && otherHand.getItem() instanceof DualieItem)
                        {
                            deviationData.cloneTo(ShotDeviationHelper.getDeviationData(otherHand));
                        }
                        for (int i = 0; i < shotData.projectileCount(); i++)
                        {
                            InkProjectileEntity proj = new InkProjectileEntity(level, entity, stack, InkBlockUtils.getInkType(entity), projectileData.size(), settings);

                            proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), shotData.pitchCompensation(), projectileData.speed(), inaccuracy);
                            proj.addExtraData(new ExtraSaveData.DualieExtraData(CommonUtils.isRolling(entity1)));
                            proj.setDualieStats(projectileData);
                            proj.tick(accumulatedTime);
                            level.addFreshEntity(proj);
                        }

                        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.dualieShot, SoundSource.PLAYERS, 0.7F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
                    }
                }
            }, null);
    }

    @Override
    public PlayerPosingHandler.WeaponPose getPose(Player player, ItemStack stack)
    {
        // loong if
        if (PlayerCooldown.hasPlayerCooldown(player) && ShootingHandler.isDoingShootingAction(player) && PlayerCooldown.getPlayerCooldown(player) instanceof DodgeRollCooldown dodgeRoll && dodgeRoll.rollState == DodgeRollCooldown.RollState.TURRET && ShootingHandler.shootingData.get(player).isDualFire())
            return PlayerPosingHandler.WeaponPose.TURRET_FIRE;
        return PlayerPosingHandler.WeaponPose.DUAL_FIRE;
    }

    public static class DodgeRollCooldown extends PlayerCooldown
    {
        final byte rollFrame, rollEndFrame, turretModeFrame;
        final Vec2 rollDirection;
        boolean canSlide;
        RollState rollState = RollState.BEFORE_ROLL;

        public DodgeRollCooldown(ItemStack stack, int slotIndex, InteractionHand hand, Vec2 rollDirection, byte startupFrames, byte rollDuration, byte endlagFrames, byte turretModeFrames, boolean canSlide)
        {
            super(stack, startupFrames + rollDuration + endlagFrames + turretModeFrames, slotIndex, hand, false, false, true, false);
            this.rollDirection = rollDirection;
            this.rollFrame = (byte) (rollDuration + turretModeFrames + endlagFrames);
            this.rollEndFrame = (byte) (turretModeFrames + endlagFrames);
            this.turretModeFrame = turretModeFrames;
            this.canSlide = canSlide;
        }

        public DodgeRollCooldown(NbtCompound nbt)
        {
            super(ItemStack.of(nbt.getCompound("StoredStack")), nbt.getFloat("MaxTime"), nbt.getInt("SlotIndex"), nbt.getBoolean("Hand") ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, false, false, true, false);

            rollDirection = new Vec2(nbt.getFloat("RollDirectionX"), nbt.getFloat("RollDirectionZ"));
            rollFrame = nbt.getByte("RollFrame");
            rollEndFrame = nbt.getByte("RollEndFrame");
            turretModeFrame = nbt.getByte("TurretModeFrame");
            canSlide = nbt.getBoolean("CanSlide");
            rollState = RollState.fromValue(nbt.getByte("RollState"));
            setTime(nbt.getFloat("Time"));
        }

        @Override
        public void tick(Player player)
        {
            boolean local = player.getWorld().isClientSide;
            boolean doLogic = true;
            while (doLogic)
            {
                switch (rollState)
                {
                    case BEFORE_ROLL:
                        if (getTime() <= rollFrame)
                        {
                            if (!local)
                            {
                                player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.dualieDodge, SoundSource.PLAYERS, 0.7F, ((player.getWorld().random.nextFloat() - player.getWorld().getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
                                InkExplosion.createInkExplosion(player, player.position(), 0.9f, 0, 0, InkBlockUtils.getInkType(player), storedStack);
                            }
                            player.setDiscardFriction(true);

                            player.setDeltaMovement(rollDirection.x, -0.5, rollDirection.y);
                            rollState = RollState.ROLL;
                            break;
                        }
                        doLogic = false;
                        break;
                    case ROLL:
                        if (getTime() <= rollEndFrame)
                        {
                            player.setDiscardFriction(false);

                            rollState = RollState.AFTER_ROLL;
                            break;
                        }
                        doLogic = false;
                        break;
                    case AFTER_ROLL:
                        if (getTime() <= turretModeFrame)
                        {
                            rollState = RollState.TURRET;
                            break;
                        }
                        doLogic = false;
                        break;
                    case TURRET:
                        doLogic = false;
                        if (getTime() <= 1)
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
                        break;
                }
            }
        }

        @Override
        public NbtCompound writeNBT(NbtCompound nbt)
        {
            nbt.putBoolean("DodgeRollCooldown", true);
            nbt.putByte("RollFrame", rollFrame);
            nbt.putByte("RollEndFrame", rollEndFrame);
            nbt.putByte("TurretModeFrame", turretModeFrame);
            nbt.putBoolean("CanSlide", canSlide);
            nbt.putFloat("RollDirectionX", rollDirection.x);
            nbt.putFloat("RollDirectionZ", rollDirection.y);
            nbt.putByte("RollState", rollState.value);

            nbt.putFloat("Time", getTime());
            nbt.putFloat("MaxTime", getMaxTime());
            nbt.putInt("SlotIndex", getSlotIndex());
            nbt.put("StoredStack", storedStack.serializeNBT());
            nbt.putBoolean("Hand", getHand() == InteractionHand.MAIN_HAND);
            return nbt;
        }

        public boolean canCancelRoll()
        {
            return rollState == RollState.AFTER_ROLL || rollState == RollState.TURRET;
        }

        @Override
        public boolean canMove()
        {
            return canSlide && canCancelRoll();
        }

        @Override
        public boolean forceCrouch()
        {
            return rollState == RollState.TURRET;
        }

        @Override
        public boolean preventWeaponUse()
        {
            return rollState != RollState.TURRET;
        }

        enum RollState
        {
            BEFORE_ROLL(0),
            ROLL(1),
            AFTER_ROLL(2),
            TURRET(3);
            final byte value;

            RollState(int value)
            {
                this.value = (byte) value;
            }

            public static RollState fromValue(byte value)
            {
                return switch (value)
                {
                    case 0 -> BEFORE_ROLL;
                    case 1 -> ROLL;
                    case 2 -> TURRET;
                    default -> throw new IllegalStateException("Unexpected value: " + value);
                };
            }
        }
    }
}