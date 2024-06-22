package net.splatcraft.forge.items.weapons;

import com.google.common.collect.Lists;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.splatcraft.forge.client.handlers.PlayerMovementHandler;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.handlers.PlayerPosingHandler;
import net.splatcraft.forge.items.weapons.settings.DualieWeaponSettings;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.c2s.DodgeRollPacket;
import net.splatcraft.forge.network.s2c.UpdatePlayerInfoPacket;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.util.ClientUtils;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.InkExplosion;
import net.splatcraft.forge.util.PlayerCooldown;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;

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

    protected DualieItem(String settings) {
        super(settings);

        this.settings = settings;

        dualies.add(this);
    }

    @Override
    public Class<DualieWeaponSettings> getSettingsClass() {
        return DualieWeaponSettings.class;
    }

    private static float getInkForRoll(ItemStack stack)
    {
        return stack.getItem() instanceof DualieItem ? ((DualieItem) stack.getItem()).getSettings(stack).rollInkConsumption : 0;
    }

    public static int getRollCooldown(ItemStack stack, int maxRolls, int rollCount)
    {
        if (!(stack.getItem() instanceof DualieItem dualie))
        {
            return 0;
        }

        return rollCount >= maxRolls - 1 ? dualie.getSettings(stack).lastRollCooldown : dualie.getSettings(stack).rollCooldown;
    }

    public float performRoll(Player player, ItemStack mainDualie, ItemStack offhandDualie) {
        int rollCount = getRollString(mainDualie);
        float maxRolls = 0;
        ItemStack activeDualie;

        if (mainDualie.getItem() instanceof DualieItem dualieItem) {
            maxRolls += dualieItem.getSettings(mainDualie).rollCount;
        }
        if (offhandDualie.getItem() instanceof DualieItem dualieItem)
        {
            maxRolls += dualieItem.getSettings(offhandDualie).rollCount;
        }

        boolean lastRoll = rollCount >= maxRolls - 1;
        if (lastRoll)
        {
            activeDualie = getRollCooldown(mainDualie, (int) maxRolls, rollCount) >= getRollCooldown(offhandDualie, (int) maxRolls, rollCount) ? mainDualie : offhandDualie;
        }
        else
        {
            activeDualie = maxRolls % 2 == 1 && offhandDualie.getItem() instanceof DualieItem ? offhandDualie : mainDualie;
        }

        DualieWeaponSettings activeSettings = getSettings(activeDualie);

        if (reduceInk(player, this, getInkForRoll(activeDualie), activeSettings.rollInkRecoveryCooldown, !player.level.isClientSide))
        {
            PlayerCooldown.setPlayerCooldown(player, new RollCooldown(activeDualie, player.getInventory().selected, player.getUsedItemHand(), 4, activeSettings.canMoveAsTurret, lastRoll));

//            PlayerCooldown.setPlayerCooldown(player, new PlayerCooldown(activeDualie, getRollCooldown(activeDualie, (int) maxRolls, rollCount), player.getInventory().selected, player.getUsedItemHand(), activeSettings.canMoveAsTurret, false, false, player.isOnGround()));
            if (!player.level.isClientSide)
            {
                player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.dualieDodge, SoundSource.PLAYERS, 0.7F, ((player.level.random.nextFloat() - player.level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
                InkExplosion.createInkExplosion(player, player.blockPosition(), 1f, 0, 0, false, InkBlockUtils.getInkType(player), activeDualie);
            }
            setRollString(mainDualie, rollCount + 1);
            setRollCooldown(mainDualie, (int) (getRollCooldown(mainDualie, (int) maxRolls, (int) maxRolls) * 0.75f));
            return activeDualie.getItem() instanceof DualieItem ? activeSettings.rollSpeed : 0;
        }

        return 0;
    }
    @Override
    public void onPlayerCooldownEnd(Level level, Player player, ItemStack stack, PlayerCooldown cooldown)
    {
        if(cooldown instanceof RollCooldown rollCooldown)
        {
            player.setDiscardFriction(false);
            TurretCooldown playerCooldown = new TurretCooldown(stack, cooldown.getSlotIndex(), player.getUsedItemHand(), getRollCooldown(stack), rollCooldown.canMoveAfter, rollCooldown.lastRoll);
            PlayerCooldown.setPlayerCooldown(player, playerCooldown);
        }
        else if (cooldown instanceof TurretCooldown turretCooldown)
        {
            if(player.isLocalPlayer())
            {
                Input input = ClientUtils.GetUnmodifiedInput(player);
                boolean endedTurretMode = input.forwardImpulse != 0 || input.leftImpulse != 0 || !player.isUsingItem();
                if(!endedTurretMode)
                {
                    cooldown.setTime(2);
                }
            }
        }
        else if(cooldown instanceof StartupCooldown startupCooldown)
        {
            ItemStack offhandDualie = ItemStack.EMPTY;
            if (player.getUsedItemHand().equals(InteractionHand.MAIN_HAND) && player.getMainHandItem().equals(stack) && player.getOffhandItem().getItem() instanceof DualieItem)
            {
                offhandDualie = player.getOffhandItem();
            }

            performRoll(player, player.getMainHandItem(), offhandDualie);
            player.setDeltaMovement(startupCooldown.getRollDirection().x, -0.5, startupCooldown.getRollDirection().y);
            player.getAbilities().flying = false;

            SplatcraftPacketHandler.sendToServer(new DodgeRollPacket(player, stack, offhandDualie));
        }
        super.onPlayerCooldownEnd(level, player, stack, cooldown);
    }
    @Override
    public void onPlayerCooldownTick(Level level, Player player, ItemStack stack, PlayerCooldown cooldown)
    {
        super.onPlayerCooldownEnd(level, player, stack, cooldown);
    }
    public static int getRollString(ItemStack stack)
    {
        if (!stack.hasTag() || !Objects.requireNonNull(stack.getTag()).contains("RollString"))
        {
            return 0;
        }
        return stack.getTag().getInt("RollString");
    }

    public static ItemStack setRollString(ItemStack stack, int rollString)
    {
        stack.getOrCreateTag().putInt("RollString", rollString);
        return stack;
    }

    public static int getRollCooldown(ItemStack stack)
    {
        //noinspection ConstantConditions
        if (!stack.hasTag() || !stack.getTag().contains("RollCooldown"))
        {
            return 0;
        }
        return stack.getTag().getInt("RollCooldown");
    }

    public static ItemStack setRollCooldown(ItemStack stack, int rollCooldown)
    {
        stack.getOrCreateTag().putInt("RollCooldown", rollCooldown);
        return stack;
    }

    public ClampedItemPropertyFunction getIsLeft()
    {
        return (stack, level, entity, seed) ->
        {
            if (entity == null)
            {
                return 0;
            } else
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
    public Component getName(@NotNull ItemStack stack)
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
        }

        int rollCooldown = getRollCooldown(stack);
        if (rollCooldown > 0)
        {
            setRollCooldown(stack, rollCooldown - 1);
        } else if (getRollString(stack) > 0)
        {
            setRollString(stack, 0);
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

        if (level.isClientSide)
        {
            if (entity instanceof Player player && player.isLocalPlayer() && ClientUtils.canPerformRoll(player))
            {
                LocalPlayer localPlayer = (LocalPlayer) player;
                ItemStack activeDualie;
                int rollCount = getRollString(stack);
                float maxRolls = 0;
                if (stack.getItem() instanceof DualieItem dualieItem)
                {
                    maxRolls += dualieItem.getSettings(stack).rollCount;
                }
                if (offhandDualie.getItem() instanceof DualieItem dualieItem)
                {
                    maxRolls += dualieItem.getSettings(offhandDualie).rollCount;
                }
                if (rollCount >= maxRolls - 1)
                {
                    activeDualie = getRollCooldown(stack, (int) maxRolls, rollCount) >= getRollCooldown(offhandDualie, (int) maxRolls, rollCount) ? stack : offhandDualie;
                }
                else
                {
                    activeDualie = maxRolls % 2 == 1 && offhandDualie.getItem() instanceof DualieItem ? offhandDualie : stack;
                }
                DualieWeaponSettings activeSettings = getSettings(activeDualie);
                if (reduceInk(player, this, getInkForRoll(activeDualie), activeSettings.rollInkRecoveryCooldown, !player.level.isClientSide))
                {
                    Vec3 vec3 = getInputVector(ClientUtils.getDodgeRollVector(localPlayer), activeSettings.rollSpeed, player.getYRot());

                    //performRoll(player, stack, offhandDualie)
                    // dualies have like, 4/60 of a second of startup, which is nothing but this also fixes not being able to make the player stop crouching if they are in turret mode
                    // also why does vec2 use floats but vec3 use doubles
                    PlayerCooldown.setPlayerCooldown(player, new StartupCooldown(activeDualie, player.getInventory().selected, player.getUsedItemHand(), 2, new Vec2((float) vec3.x, (float) vec3.z)));
                    player.getAbilities().flying = true;
                    SplatcraftPacketHandler.sendToDim(new UpdatePlayerInfoPacket(player), level.dimension());
                }
            }
        } else
        {
            int rollCount = getRollString(stack);
            float maxRolls = 0;

            if (stack.getItem() instanceof DualieItem)
            {
                maxRolls += ((DualieItem) stack.getItem()).getSettings(stack).rollCount;
            }
            if (offhandDualie.getItem() instanceof DualieItem)
            {
                maxRolls += ((DualieItem) offhandDualie.getItem()).getSettings(offhandDualie).rollCount;
            }

            boolean hasCooldown = PlayerInfoCapability.get(entity).hasPlayerCooldown() && PlayerInfoCapability.get(entity).getPlayerCooldown() instanceof TurretCooldown;
            boolean onRollCooldown = entity.isOnGround() && hasCooldown && rollCount >= Math.max(2, maxRolls);

            if (offhandDualie.getItem() instanceof DualieItem) {
                if (!entity.isOnGround() && !hasCooldown || entity.isOnGround())
                {
                    DualieWeaponSettings settings = ((DualieItem) offhandDualie.getItem()).getSettings(offhandDualie);
                    DualieWeaponSettings.FiringData firingData = onRollCooldown ? settings.turretData : settings.standardData;

                    ((DualieItem) offhandDualie.getItem()).fireDualie(level, entity, offhandDualie, timeLeft + firingData.firingSpeed /2, entity.isOnGround() && hasCooldown);
                }
            }
            if (!entity.isOnGround() && !hasCooldown || entity.isOnGround()) {
                fireDualie(level, entity, stack, timeLeft, onRollCooldown);
            }
        }
    }

    protected void fireDualie(Level level, LivingEntity entity, ItemStack stack, int timeLeft, boolean onRollCooldown)
    {
        DualieWeaponSettings settings = getSettings(stack);
        DualieWeaponSettings.FiringData firingData = onRollCooldown ? settings.turretData : settings.standardData;

        if (!level.isClientSide && (getUseDuration(stack) - timeLeft - 1) % (firingData.firingSpeed) == 0)
        {
            if (reduceInk(entity, this, firingData.inkConsumption, firingData.inkRecoveryCooldown, true))
            {
                for(int i = 0; i < firingData.projectileCount; i++)
                {
                    InkProjectileEntity proj = new InkProjectileEntity(level, entity, stack, InkBlockUtils.getInkType(entity), firingData.projectileSize, settings);
                    proj.isOnRollCooldown = onRollCooldown;
                    proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), firingData.pitchCompensation, firingData.projectileSpeed, entity.isOnGround() ? firingData.groundInaccuracy : firingData.airInaccuracy);
                    proj.setDualieStats(firingData);
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

    public static Vec3 getInputVector(Vec3 p_20016_, float p_20017_, float p_20018_) {
        double d0 = p_20016_.lengthSqr();
        if (d0 < 1.0E-7D) {
            return Vec3.ZERO;
        } else {
            Vec3 vec3 = (d0 > 1.0D ? p_20016_.normalize() : p_20016_).scale(p_20017_);
            float f = Mth.sin(p_20018_ * ((float)Math.PI / 180F));
            float f1 = Mth.cos(p_20018_ * ((float)Math.PI / 180F));
            return new Vec3(vec3.x * (double)f1 - vec3.z * (double)f, vec3.y, vec3.z * (double)f1 + vec3.x * (double)f);
        }
    }
    public static class RollCooldown extends PlayerCooldown
    {
        boolean canMoveAfter;
        boolean lastRoll;
        public RollCooldown(ItemStack stack, int slotIndex, InteractionHand hand, int duration, boolean canMoveAfter, boolean lastRoll)
        {
            super(stack, duration, slotIndex, hand, false, false, true, false);
            this.lastRoll = lastRoll;
            this.canMoveAfter = canMoveAfter;
        }
        public RollCooldown(CompoundTag nbt)
        {
            this(ItemStack.of(nbt.getCompound("StoredStack")), nbt.getInt("SlotIndex"), nbt.getBoolean("Hand") ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, nbt.getInt("Duration"), nbt.getBoolean("CanMoveAfter"), nbt.getBoolean("LastRoll"));
            setForceCrouch(nbt.getBoolean("ForceCrouch"));
        }

        @Override
        public CompoundTag writeNBT(CompoundTag nbt)
        {
            nbt.put("StoredStack", storedStack.serializeNBT());
            nbt.putInt("SlotIndex", getSlotIndex());
            nbt.putBoolean("Hand", getHand() == InteractionHand.MAIN_HAND);
            nbt.putBoolean("CanMoveAfter", canMoveAfter);
            nbt.putBoolean("RollCooldown", true);
            nbt.putBoolean("CanSlide", canMove());
            nbt.putBoolean("LastRoll", isLastRoll());

            return nbt;
        }
        public boolean isLastRoll()
        {
            return lastRoll;
        }
        public void setLastRoll(boolean lastRoll)
        {
            this.lastRoll = lastRoll;
        }
    }
    public static class TurretCooldown extends PlayerCooldown
    {
        private boolean lastRoll;
        public TurretCooldown(ItemStack stack, int slotIndex, InteractionHand hand, int duration, boolean canSlide, boolean lastRoll)
        {
            super(stack, duration, slotIndex, hand, canSlide, true, false, false);
            this.lastRoll = lastRoll;
        }
        public TurretCooldown(CompoundTag nbt)
        {
            this(ItemStack.of(nbt.getCompound("StoredStack")), nbt.getInt("SlotIndex"), nbt.getBoolean("Hand") ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, nbt.getInt("Duration"), nbt.getBoolean("CanSlide"), nbt.getBoolean("LastRoll"));
            setForceCrouch(nbt.getBoolean("ForceCrouch"));
        }

        @Override
        public CompoundTag writeNBT(CompoundTag nbt)
        {
            nbt.put("StoredStack", storedStack.serializeNBT());
            nbt.putInt("SlotIndex", getSlotIndex());
            nbt.putBoolean("Hand", getHand() == InteractionHand.MAIN_HAND);
            nbt.putBoolean("LastRoll", isLastRoll());
            nbt.putBoolean("TurretCooldown", true);
            nbt.putBoolean("ForceCrouch", forceCrouch());
            nbt.putBoolean("CanSlide", canMove());
            nbt.putInt("Duration", getTime());

            return nbt;
        }
        public boolean isLastRoll()
        {
            return lastRoll;
        }
        public void setLastRoll(boolean lastRoll)
        {
            this.lastRoll = lastRoll;
        }
    }
    public static class StartupCooldown extends PlayerCooldown
    {
        private Vec2 rollDirection;
        public StartupCooldown(ItemStack stack, int slotIndex, InteractionHand hand, int duration, Vec2 rollDirection)
        {
            super(stack, duration, slotIndex, hand, false, true, false, false);
            this.rollDirection = rollDirection;
        }
        public StartupCooldown(CompoundTag nbt)
        {
            this(ItemStack.of(nbt.getCompound("StoredStack")), nbt.getInt("SlotIndex"), nbt.getBoolean("Hand") ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, nbt.getInt("Duration"), new Vec2(nbt.getFloat("RollX"), nbt.getFloat("RollZ")));
            setForceCrouch(nbt.getBoolean("ForceCrouch"));
        }

        @Override
        public CompoundTag writeNBT(CompoundTag nbt)
        {
            nbt.put("StoredStack", storedStack.serializeNBT());
            nbt.putInt("SlotIndex", getSlotIndex());
            nbt.putBoolean("Hand", getHand() == InteractionHand.MAIN_HAND);
            nbt.putBoolean("StartupCooldown", true);
            nbt.putBoolean("ForceCrouch", forceCrouch());
            nbt.putInt("Duration", getTime());
            nbt.putFloat("RollX", rollDirection.x);
            nbt.putFloat("RollZ", rollDirection.y);

            return nbt;
        }
        public Vec2 getRollDirection()
        {
            return rollDirection;
        }
        public void setRollDirection(Vec2 rollDirection)
        {
            this.rollDirection = rollDirection;
        }
    }
}