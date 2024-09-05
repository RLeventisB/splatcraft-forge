package net.splatcraft.forge.items.weapons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.splatcraft.forge.client.audio.SplatlingChargingTickableSound;
import net.splatcraft.forge.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.handlers.PlayerPosingHandler;
import net.splatcraft.forge.items.InkTankItem;
import net.splatcraft.forge.items.weapons.settings.CommonRecords;
import net.splatcraft.forge.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.forge.items.weapons.settings.SplatlingWeaponSettings;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.c2s.ReleaseChargePacket;
import net.splatcraft.forge.network.c2s.UpdateChargeStatePacket;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.PlayerCharge;
import net.splatcraft.forge.util.PlayerCooldown;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class SplatlingItem extends WeaponBaseItem<SplatlingWeaponSettings> implements IChargeableWeapon
{
    public SplatlingChargingTickableSound chargingSound;

    protected SplatlingItem(String settingsId)
    {
        super(settingsId);
    }

    @Override
    public Class<SplatlingWeaponSettings> getSettingsClass()
    {
        return SplatlingWeaponSettings.class;
    }

    public static RegistryObject<SplatlingItem> create(DeferredRegister<Item> register, String settings, String name)
    {
        return register.register(name, () -> new SplatlingItem(settings));
    }

    public static RegistryObject<SplatlingItem> create(DeferredRegister<Item> register, RegistryObject<SplatlingItem> parent, String name)
    {
        return register.register(name, () -> new SplatlingItem(parent.get().settingsId.toString()));
    }

    @OnlyIn(Dist.CLIENT)
    protected static void playChargeReadySound(Player player, float pitch)
    {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getUUID().equals(player.getUUID()))
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SplatcraftSounds.splatlingReady, pitch, Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.PLAYERS)));
    }

    @OnlyIn(Dist.CLIENT)
    protected void playChargingSound(Player player, ItemStack stack)
    {
        if (Minecraft.getInstance().player == null || !Minecraft.getInstance().player.getUUID().equals(player.getUUID()))
        {
            return;
        }

        SoundEvent soundEvent = PlayerCharge.getChargeValue(player, stack) > 1 ? SplatcraftSounds.splatlingChargeSecondLevel : SplatcraftSounds.splatlingCharge;

        if (chargingSound == null || chargingSound.isStopped() || !chargingSound.getSoundEvent().equals(soundEvent))
        {
            boolean exsistingSound = chargingSound != null;
            if (exsistingSound)
                chargingSound.fadeOut();
            chargingSound = new SplatlingChargingTickableSound(Minecraft.getInstance().player, soundEvent);
            if (exsistingSound)
                chargingSound.fadeIn();
            Minecraft.getInstance().getSoundManager().play(chargingSound);
        }
    }

    private static final int maxCharges = 2;

    @Override
    public void weaponUseTick(Level level, LivingEntity entity, ItemStack stack, int timeLeft)
    {
        if (!(entity instanceof Player player))
            return;

        if (PlayerCooldown.hasPlayerCooldown(player))
            PlayerCooldown.setPlayerCooldown(player, null);

        SplatlingWeaponSettings settings = getSettings(stack);

        if (level.isClientSide)
        {

            float prevCharge = PlayerCharge.getChargeValue(player, stack);
            float newCharge = prevCharge + 1f / (prevCharge >= 1 ? settings.chargeData.secondChargeTime() : settings.chargeData.firstChargeTime());

            if (!enoughInk(entity, this, Mth.lerp(newCharge * 0.5f, 0, settings.inkConsumption), 0, timeLeft % 4 == 0))
            {
                if (!hasInkInTank(player, this) || !InkTankItem.canRecharge(player.getItemBySlot(EquipmentSlot.CHEST), true))
                    return;
                newCharge = prevCharge + 1f / (prevCharge >= 1 ? settings.chargeData.emptyTankSecondChargeTime() : settings.chargeData.emptyTankFirstChargeTime());
            }

            playChargingSound(player, stack);

            if (prevCharge < maxCharges && newCharge >= Math.ceil(prevCharge) && prevCharge > 0)
                playChargeReadySound(player, newCharge / maxCharges);

            PlayerCharge.addChargeValue(player, stack, newCharge - prevCharge, true, maxCharges);
        }
        else if (timeLeft % 4 == 0 && !enoughInk(entity, this, 0.1f, 0, false))
            playNoInkSound(player, SplatcraftSounds.noInkMain);
    }

    @Override
    public void onPlayerCooldownEnd(Level level, Player player, ItemStack stack, PlayerCooldown cooldown)
    {
        if (cooldown.getTime() > 0)
        {
            if (!level.isClientSide)
            {
                SplatlingWeaponSettings settings = getSettings(stack);

                float chargeLevel = cooldown.getMaxTime() / (float) settings.chargeData.firingDuration(); //yeah idk about this
                float cooldownLeft = cooldown.getTime() / (float) cooldown.getMaxTime();
                float inkConsumed = Mth.lerp(chargeLevel * 0.5f, 0, settings.inkConsumption);
                float inkRefunded = inkConsumed * cooldownLeft;

                refundInk(player, inkRefunded);
            }
            else if (PlayerCharge.hasCharge(player) && player.equals(Minecraft.getInstance().player))
            {
                PlayerCharge charge = PlayerCharge.getCharge(player);
                charge.reset();
                SplatcraftPacketHandler.sendToServer(new UpdateChargeStatePacket(false));
            }
        }
    }

    @Override
    public void onPlayerCooldownTick(Level level, Player player, ItemStack stack, PlayerCooldown cooldown)
    {
        if (level.isClientSide)
            return;

        SplatlingWeaponSettings settings = getSettings(stack);
        float charge = stack.getOrCreateTag().getFloat("Charge");

        boolean secondData = charge > 1;
        SplatlingWeaponSettings.ShotDataRecord firingData = secondData ? settings.secondChargeLevelShot : settings.firstChargeLevelShot;
        CommonRecords.ProjectileDataRecord projectileData = secondData ? settings.secondChargeLevelProjectile : settings.firstChargeLevelProjectile;

        int firingSpeed = getScaledShotSettingInt(settings, charge, SplatlingWeaponSettings.ShotDataRecord::firingSpeed);

        if (firingSpeed > 0 && (cooldown.getTime() - 1) % firingSpeed == 0)
        {
            for (int i = 0; i < firingData.projectileCount(); i++)
            {
                InkProjectileEntity proj = new InkProjectileEntity(level, player, stack, InkBlockUtils.getInkType(player), projectileData.size(), settings);
                proj.shootFromRotation(player, player.getXRot(), player.getYRot(), firingData.pitchCompensation(), getScaledProjectileSettingFloat(settings, charge, CommonRecords.ProjectileDataRecord::speed),
                        ShotDeviationHelper.updateShotDeviation(stack, level.getRandom(), firingData.accuracyData()));
                proj.setSplatlingStats(settings, charge);
                level.addFreshEntity(proj);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.splatlingShot, SoundSource.PLAYERS, 0.7F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
        }
    }

    @Override
    public void onReleaseCharge(Level level, Player player, ItemStack stack, float charge)
    {
        SplatlingWeaponSettings settings = getSettings(stack);

        stack.getOrCreateTag().putFloat("Charge", charge);

        int cooldownTime = (int) (getDecayTicks(stack) * charge);
        reduceInk(player, this, Mth.lerp(charge * 0.5f, 0, settings.inkConsumption), cooldownTime + settings.inkRecoveryCooldown, true, true);
        PlayerCooldown.setPlayerCooldown(player, new PlayerCooldown(stack, cooldownTime, player.getInventory().selected, player.getUsedItemHand(), true, false, !settings.chargeData.canRechargeWhileFiring(), player.onGround()).setCancellable());
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, LivingEntity entity, int timeLeft)
    {
        super.releaseUsing(stack, level, entity, timeLeft);

        if (level.isClientSide && entity instanceof Player player && player.equals(Minecraft.getInstance().player))
        {
            if (PlayerCooldown.hasPlayerCooldown(player) && PlayerCooldown.getPlayerCooldown(player).preventWeaponUse())
                return;

            PlayerCharge charge = PlayerCharge.getCharge(player);

            if (charge == null)
                return;
            if (!SplatcraftKeyHandler.isSquidKeyDown() && charge.charge > 0.05f) //checking for squid key press so it doesn't immediately release charge when squidding
            {
                SplatlingWeaponSettings settings = getSettings(stack);
                PlayerCooldown.setPlayerCooldown(player, new PlayerCooldown(stack, (int) (settings.chargeData.firingDuration() * charge.charge), player.getInventory().selected, player.getUsedItemHand(), true, false, !settings.chargeData.canRechargeWhileFiring(), player.onGround()).setCancellable());
                SplatcraftPacketHandler.sendToServer(new ReleaseChargePacket(charge.charge, stack, false));
            }
        }
    }

    // its time for boilerplate code
    public static float getScaledShotSettingFloat(SplatlingWeaponSettings settings, float charge, Function<SplatlingWeaponSettings.ShotDataRecord, Float> getter)
    {
        float min = getter.apply(settings.firstChargeLevelShot);
        float max = getter.apply(settings.secondChargeLevelShot);
        return min + (max - min) * Mth.clamp(charge, 0, 1);
    }

    public static float getScaledProjectileSettingFloat(SplatlingWeaponSettings settings, float charge, Function<CommonRecords.ProjectileDataRecord, Float> getter)
    {
        float min = getter.apply(settings.firstChargeLevelProjectile);
        float max = getter.apply(settings.secondChargeLevelProjectile);
        return min + (max - min) * Mth.clamp(charge, 0, 1);
    }

    public static int getScaledShotSettingInt(SplatlingWeaponSettings settings, float charge, Function<SplatlingWeaponSettings.ShotDataRecord, Integer> getter)
    {
        float min = getter.apply(settings.firstChargeLevelShot);
        float max = getter.apply(settings.secondChargeLevelShot);
        return Math.round(min + (max - min) * Mth.clamp(charge, 0, 1));
    }

    public static int getScaledProjectileSettingInt(SplatlingWeaponSettings settings, float charge, Function<CommonRecords.ProjectileDataRecord, Integer> getter)
    {
        float min = getter.apply(settings.firstChargeLevelProjectile);
        float max = getter.apply(settings.secondChargeLevelProjectile);
        return Math.round(min + (max - min) * Mth.clamp(charge, 0, 1));
    }

    @Override
    public PlayerPosingHandler.WeaponPose getPose(ItemStack stack)
    {
        return PlayerPosingHandler.WeaponPose.SPLATLING;
    }

    @Override
    public int getDischargeTicks(ItemStack stack)
    {
        return getSettings(stack).chargeData.chargeStorageTime();
    }

    @Override
    public int getDecayTicks(ItemStack stack)
    {
        return getSettings(stack).chargeData.firingDuration();
    }

    @Override
    public AttributeModifier getSpeedModifier(LivingEntity entity, ItemStack stack)
    {
        SplatlingWeaponSettings settings = getSettings(stack);

        double appliedMobility = entity.getUseItem().equals(stack) && settings.chargeData.moveSpeed().isPresent() ? settings.chargeData.moveSpeed().get() : settings.moveSpeed;

        return new AttributeModifier(SplatcraftItems.SPEED_MOD_UUID, "Splatling Mobility", appliedMobility - 1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}