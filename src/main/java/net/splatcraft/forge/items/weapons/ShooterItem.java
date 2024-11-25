package net.splatcraft.forge.items.weapons;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.handlers.PlayerPosingHandler;
import net.splatcraft.forge.handlers.ShootingHandler;
import net.splatcraft.forge.items.weapons.settings.ShooterWeaponSettings;
import net.splatcraft.forge.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.util.CommonUtils;
import net.splatcraft.forge.util.InkBlockUtils;
import org.jetbrains.annotations.NotNull;

public class ShooterItem extends WeaponBaseItem<ShooterWeaponSettings>
{
    public static RegistryObject<ShooterItem> create(DeferredRegister<Item> registry, String settings, String name)
    {
        return registry.register(name, () -> new ShooterItem(settings));
    }

    public static RegistryObject<ShooterItem> create(DeferredRegister<Item> registry, RegistryObject<ShooterItem> parent, String name)
    {
        return create(registry, parent, name, false);
    }

    public static RegistryObject<ShooterItem> create(DeferredRegister<Item> registry, RegistryObject<ShooterItem> parent, String name, boolean secret)
    {
        return registry.register(name, () -> new ShooterItem(parent.get().settingsId.toString()).setSecret(secret));
    }

    protected ShooterItem(String settings)
    {
        super(settings);
    }

    @Override
    public Class<ShooterWeaponSettings> getSettingsClass()
    {
        return ShooterWeaponSettings.class;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int itemSlot, boolean isSelected)
    {
        super.inventoryTick(stack, level, entity, itemSlot, isSelected);

        if (entity instanceof LivingEntity livingEntity && ShootingHandler.isDoingShootingAction(livingEntity))
        {
            ShootingHandler.FiringData firingData = ShootingHandler.firingData.get(livingEntity);
            if (firingData.isFor(stack, livingEntity))
                ShootingHandler.handleShootingUnsafe(livingEntity, firingData);
        }
    }

    @Override
    public void weaponUseTick(Level level, LivingEntity entity, ItemStack stack, int timeLeft)
    {
        ShooterWeaponSettings settings = getSettings(stack);

        ShootingHandler.notifyStartShooting(entity, CommonUtils.startupSquidSwitch(entity, settings.shotData), settings.shotData.startupTicks(), settings.shotData.endlagTicks(),
            null,
            (data, accumulatedTime, entity1) -> {
                if (!level.isClientSide())
                {
                    if (reduceInk(entity, this, settings.shotData.inkConsumption(), settings.shotData.inkRecoveryCooldown(), true))
                    {
                        float inaccuracy = ShotDeviationHelper.updateShotDeviation(stack, level.getRandom(), settings.getShotDeviationData(stack, entity));
                        for (int i = 0; i < settings.shotData.projectileCount(); i++)
                        {
                            InkProjectileEntity proj = new InkProjectileEntity(level, entity, stack, InkBlockUtils.getInkType(entity), settings.projectileData.size(), settings);
                            proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), settings.shotData.pitchCompensation(), settings.projectileData.speed(), inaccuracy, accumulatedTime);
                            proj.setShooterStats(settings);
                            level.addFreshEntity(proj);
                        }

                        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.shooterShot, SoundSource.PLAYERS, 0.7F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
                    }
                }
            }, null);
    }

    @Override
    public PlayerPosingHandler.WeaponPose getPose(ItemStack stack)
    {
        return PlayerPosingHandler.WeaponPose.FIRE;
    }
}
