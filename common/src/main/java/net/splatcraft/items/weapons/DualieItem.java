package net.splatcraft.items.weapons;

import com.google.common.collect.Lists;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
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
import net.splatcraft.registries.SplatcraftComponents;
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

    public static RegistrySupplier<DualieItem> create(DeferredRegister<Item> registry, String settings)
    {
        return registry.register(settings, () -> new DualieItem(settings));
    }

    public static RegistrySupplier<DualieItem> create(DeferredRegister<Item> registry, RegistrySupplier<DualieItem> parent, String name)
    {
        return registry.register(name, () -> new DualieItem(parent.get().settingsId.toString()));
    }

    public static RegistrySupplier<DualieItem> create(DeferredRegister<Item> registry, String settings, String name)
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

    public static int getRollCount(LivingEntity player)
    {
        return EntityInfoCapability.hasCapability(player) ? EntityInfoCapability.get(player).getDodgeCount() : -1;
    }

    public static int getMaxRollCount(LivingEntity player)
    {
        float maxRolls = 0;
        if (player.getMainHandStack().getItem() instanceof DualieItem dualieItem)
        {
            maxRolls += dualieItem.getSettings(player.getMainHandStack()).rollData.count();
        }
        if (player.getOffHandStack().getItem() instanceof DualieItem dualieItem)
        {
            maxRolls += dualieItem.getSettings(player.getOffHandStack()).rollData.count();
        }
        return (int) maxRolls;
    }

    @Override
    public Class<DualieWeaponSettings> getSettingsClass()
    {
        return DualieWeaponSettings.class;
    }

    public void performRoll(PlayerEntity player, ItemStack activeDualie, Hand hand, int maxRolls, Vec2f rollPotency, boolean local)
    {
        int rollCount = getRollCount(player);

        DualieWeaponSettings activeSettings = getSettings(activeDualie);

        if (reduceInk(player, this, getInkForRoll(activeDualie), activeSettings.rollData.inkRecoveryCooldown(), !player.getWorld().isClient()))
        {
            ShootingHandler.notifyForceEndShooting(player);
            int turretDuration = getRollTurretDuration(activeDualie);
            PlayerCooldown.setPlayerCooldown(player, new DodgeRollCooldown(activeDualie, player.getInventory().selectedSlot, hand, rollPotency, activeSettings.rollData.rollStartup(), activeSettings.rollData.rollDuration(), activeSettings.rollData.rollEndlag(), (byte) turretDuration, activeSettings.rollData.canMove()));

            EntityInfoCapability.get(player).setDodgeCount(rollCount + 1);
        }
    }

    public ClampedModelPredicateProvider getIsLeft()
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
            boolean mainLeft = entity.getMainArm().equals(Arm.LEFT);
            return mainLeft && entity.getMainHandStack().equals(stack) || !mainLeft && entity.getOffHandStack().equals(stack) ? 1 : 0;
        };
    }

    @Override
    public @NotNull String getTranslationKey(ItemStack stack)
    {
        if (stack.get(SplatcraftComponents.IS_PLURAL))
        {
            return getTranslationKey() + ".plural";
        }
        return super.getTranslationKey(stack);
    }

    @Override
    public @NotNull Text getName(@NotNull ItemStack stack)
    {
        return super.getName(stack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull World world, @NotNull Entity entity, int itemSlot, boolean isSelected)
    {
        super.inventoryTick(stack, world, entity, itemSlot, isSelected);

        if (entity instanceof LivingEntity livingEntity)
        {
            Hand hand = livingEntity.getStackInHand(Hand.MAIN_HAND).equals(stack) ? Hand.MAIN_HAND : Hand.OFF_HAND;

            if (livingEntity.getStackInHand(hand).equals(stack) && livingEntity.getStackInHand(Hand.values()[(hand.ordinal() + 1) % Hand.values().length]).getItem().equals(stack.getItem()))
            {
                stack.set(SplatcraftComponents.IS_PLURAL, true);
            }
            if (entity instanceof ClientPlayerEntity localPlayer && localPlayer.getStackInHand(hand) == stack && localPlayer.getActiveHand() == hand && localPlayer.isUsingItem())
            {
                ItemStack offhandDualie = ItemStack.EMPTY;
                if (localPlayer.getActiveHand().equals(Hand.OFF_HAND) && localPlayer.getOffHandStack().equals(stack) && localPlayer.getOffHandStack().getItem() instanceof DualieItem)
                {
                    offhandDualie = localPlayer.getOffHandStack();
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
                        Vec2f rollPotency = ClientUtils.getDodgeRollVector(localPlayer, activeSettings.getRollImpulse());

                        performRoll(localPlayer, stack, hand, maxRolls, rollPotency, true);
                        SplatcraftPacketHandler.sendToServer(new DodgeRollPacket(localPlayer.getUuid(), activeDualie, hand, maxRolls, rollPotency));
                    }
                }
            }
        }
    }

    @Override
    public void weaponUseTick(World world, LivingEntity entity, ItemStack stack, int timeLeft)
    {
        ItemStack offhandDualie = ItemStack.EMPTY;
        if (entity.getActiveHand().equals(Hand.MAIN_HAND) && entity.getMainHandStack().equals(stack) && entity.getOffHandStack().getItem() instanceof DualieItem)
        {
            offhandDualie = entity.getOffHandStack();
        }

        PlayerEntity player = (PlayerEntity) entity;
        player.setBodyYaw(player.getBodyYaw()); // actually uncanny in third person but itll be useful when making dualies shoot actually from their muzzles

        ShootingHandler.notifyStartShooting(entity);

        /*if (!level.isClient())
        {
            player.yBodprevYaw = player.getYHeadRot();
            int rollCount = getRollCount(player);

            boolean hasCooldown = PlayerCooldown.hasPlayerCooldown(player);
            boolean onRollCooldown = hasCooldown && rollCount >= 1;

            if (offhandDualie.getItem() instanceof DualieItem dualieItem)
            {
                DualieWeaponSettings settings = dualieItem.getSettings(offhandDualie);
                CommonRecords.ShotDataRecord firingData = onRollCooldown ? settings.turretShotData : settings.standardShotData;

                dualieItem.fireDualie(level, entity, offhandDualie, timeLeft + firingData.getFiringSpeed() / 2, entity.isOnGround() && hasCooldown);
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
        World world = entity.getWorld();
        return new ShootingHandler.FiringStatData(shotData.squidStartupTicks(), shotData.startupTicks(), shotData.endlagTicks(),
            null,
            (data, accumulatedTime, entity1) ->
            {
                if (!world.isClient())
                {
                    if (reduceInk(entity1, this, shotData.inkConsumption(), shotData.inkRecoveryCooldown(), true))
                    {
                        float inaccuracy = ShotDeviationHelper.updateShotDeviation(stack, world.getRandom(), shotData.accuracyData());
                        SplatcraftComponents.ShotDeviationData deviationData = ShotDeviationHelper.getDeviationData(stack);
                        ItemStack otherHand = entity.getStackInHand(CommonUtils.otherHand(data.hand));
                        if (!otherHand.isEmpty() && otherHand.getItem() instanceof DualieItem)
                        {
                            deviationData.cloneTo(ShotDeviationHelper.getDeviationData(otherHand));
                        }
                        for (int i = 0; i < shotData.projectileCount(); i++)
                        {
                            InkProjectileEntity proj = new InkProjectileEntity(world, entity, stack, InkBlockUtils.getInkType(entity), projectileData.size(), settings);

                            proj.setVelocity(entity, entity.getPitch(), entity.getYaw(), shotData.pitchCompensation(), projectileData.speed(), inaccuracy);
                            proj.addExtraData(new ExtraSaveData.DualieExtraData(CommonUtils.isRolling(entity1)));
                            proj.setDualieStats(projectileData);
                            proj.tick(accumulatedTime);
                            world.spawnEntity(proj);
                        }

                        world.playSoundFromEntity(entity, SplatcraftSounds.dualieShot, SoundCategory.PLAYERS, 0.7F, (float) world.getRandom().nextTriangular(0.95f, 0.095f));
                    }
                }
            }, null);
    }

    @Override
    public PlayerPosingHandler.WeaponPose getPose(PlayerEntity player, ItemStack stack)
    {
        // loong if
        if (PlayerCooldown.hasPlayerCooldown(player) && ShootingHandler.isDoingShootingAction(player) && PlayerCooldown.getPlayerCooldown(player) instanceof DodgeRollCooldown dodgeRoll && dodgeRoll.rollState == DodgeRollCooldown.RollState.TURRET && ShootingHandler.shootingData.get(player).isDualFire())
            return PlayerPosingHandler.WeaponPose.TURRET_FIRE;
        return PlayerPosingHandler.WeaponPose.DUAL_FIRE;
    }

    public static class DodgeRollCooldown extends PlayerCooldown
    {
        final byte rollFrame, rollEndFrame, turretModeFrame;
        final Vec2f rollDirection;
        boolean canSlide;
        RollState rollState = RollState.BEFORE_ROLL;

        public DodgeRollCooldown(ItemStack stack, int slotIndex, Hand hand, Vec2f rollDirection, byte startupFrames, byte rollDuration, byte endlagFrames, byte turretModeFrames, boolean canSlide)
        {
            super(stack, startupFrames + rollDuration + endlagFrames + turretModeFrames, slotIndex, hand, false, false, true, false);
            this.rollDirection = rollDirection;
            rollFrame = (byte) (rollDuration + turretModeFrames + endlagFrames);
            rollEndFrame = (byte) (turretModeFrames + endlagFrames);
            turretModeFrame = turretModeFrames;
            this.canSlide = canSlide;
        }

        public DodgeRollCooldown(NbtCompound nbt)
        {
            super(ItemStack.fromNbtOrEmpty(MinecraftClient.getInstance().world.getRegistryManager(), nbt.getCompound("StoredStack")), nbt.getFloat("MaxTime"), nbt.getInt("SlotIndex"), nbt.getBoolean("Hand") ? Hand.MAIN_HAND : Hand.OFF_HAND, false, false, true, false);

            rollDirection = new Vec2f(nbt.getFloat("RollDirectionX"), nbt.getFloat("RollDirectionZ"));
            rollFrame = nbt.getByte("RollFrame");
            rollEndFrame = nbt.getByte("RollEndFrame");
            turretModeFrame = nbt.getByte("TurretModeFrame");
            canSlide = nbt.getBoolean("CanSlide");
            rollState = RollState.fromValue(nbt.getByte("RollState"));
            setTime(nbt.getFloat("Time"));
        }

        @Override
        public void tick(LivingEntity player)
        {
            boolean local = player.getWorld().isClient;
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
                                player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.dualieDodge, SoundCategory.PLAYERS, 0.7F, ((player.getWorld().random.nextFloat() - player.getWorld().getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
                                InkExplosion.createInkExplosion(player, player.getPos(), 0.9f, 0, 0, InkBlockUtils.getInkType(player), storedStack);
                            }
                            player.setNoDrag(true);

                            player.setVelocity(rollDirection.x, -0.5, rollDirection.y);
                            rollState = RollState.ROLL;
                            break;
                        }
                        doLogic = false;
                        break;
                    case ROLL:
                        if (getTime() <= rollEndFrame)
                        {
                            player.setNoDrag(false);

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
                                Input input = ClientUtils.getUnmodifiedInput((ClientPlayerEntity) player);
                                boolean endedTurretMode = input.jumping || input.movementForward != 0 || input.movementSideways != 0 || !player.isUsingItem() || player.getVelocity().y > 0.1;
                                if (endedTurretMode)
                                {
                                    setTime(0);
                                    EntityInfoCapability.get(player).setDodgeCount(0);
                                    SplatcraftPacketHandler.sendToServer(new DodgeRollEndPacket(player.getUuid()));
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
        public NbtCompound writeNBT(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
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
            NbtElement element = new NbtCompound();
            ItemStack.CODEC.encode(storedStack, NbtOps.INSTANCE, element);
            nbt.put("StoredStack", element);
            nbt.putBoolean("Hand", getHand() == Hand.MAIN_HAND);
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