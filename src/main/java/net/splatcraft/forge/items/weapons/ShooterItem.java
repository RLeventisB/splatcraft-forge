package net.splatcraft.forge.items.weapons;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import net.splatcraft.forge.util.InkBlockUtils;
import org.jetbrains.annotations.NotNull;

public class ShooterItem extends WeaponBaseItem<ShooterWeaponSettings>
{
    protected ShooterItem(String settings)
    {
        super(settings);
    }

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

    @Override
    public Class<ShooterWeaponSettings> getSettingsClass()
    {
        return ShooterWeaponSettings.class;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int itemSlot, boolean isSelected)
    {
        super.inventoryTick(stack, level, entity, itemSlot, isSelected);
    }

    @Override
    public void weaponUseTick(Level level, LivingEntity entity, ItemStack stack, int timeLeft)
    {
        ShootingHandler.notifyStartShooting(entity);
    }

    @Override
    public ShootingHandler.FiringStatData getWeaponFireData(ItemStack stack, LivingEntity entity)
    {
        ShooterWeaponSettings settings = getSettings(stack);
        return new ShootingHandler.FiringStatData(settings.shotData.squidStartupTicks(), settings.shotData.startupTicks(), settings.shotData.endlagTicks(),
            null,
            (data, accumulatedTime, entity1) -> {
                Level level = entity1.level();
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
                            proj.tick(accumulatedTime);
                            level.addFreshEntity(proj);
                        }

                        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.shooterShot, SoundSource.PLAYERS, 0.7F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
                    }
                }
            }, null);
    }

    @Override
    public PlayerPosingHandler.WeaponPose getPose(Player player, ItemStack stack)
    {
        return ShootingHandler.isDoingShootingAction(player) && ShootingHandler.shootingData.get(player).isDualFire() ? PlayerPosingHandler.WeaponPose.DUAL_FIRE : PlayerPosingHandler.WeaponPose.FIRE;
    }
}
