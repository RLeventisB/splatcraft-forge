package net.splatcraft.forge.items.weapons;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.handlers.PlayerPosingHandler;
import net.splatcraft.forge.items.weapons.settings.ShooterWeaponSettings;
import net.splatcraft.forge.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.PlayerCooldown;

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
    public void weaponUseTick(Level level, LivingEntity entity, ItemStack stack, int timeLeft)
    {
        ShooterWeaponSettings settings = getSettings(stack);

        int time = getUseDuration(stack) - timeLeft;

        if (time <= 0)
        {
            if (settings.shotData.startupTicks() > 0 && entity instanceof Player player)
                PlayerCooldown.setPlayerCooldown(player, new PlayerCooldown(stack, settings.shotData.startupTicks(), player.getInventory().selected, player.getUsedItemHand(), true, false, true, player.onGround()));
        }
        else time -= settings.shotData.startupTicks();

        if (!level.isClientSide && settings.shotData.getFiringSpeed() > 0 && (time - 1) % settings.shotData.getFiringSpeed() == 0)
        {
            if (reduceInk(entity, this, settings.shotData.inkConsumption(), settings.shotData.inkRecoveryCooldown(), true))
            {
                for (int i = 0; i < settings.shotData.projectileCount(); i++)
                {
                    InkProjectileEntity proj = new InkProjectileEntity(level, entity, stack, InkBlockUtils.getInkType(entity), settings.projectileData.size(), settings);
                    proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), settings.shotData.pitchCompensation(), settings.projectileData.speed(), ShotDeviationHelper.updateShotDeviation(stack, level.getRandom(), settings.getShotDeviationData(stack, entity)));
                    proj.setShooterStats(settings);
                    level.addFreshEntity(proj);
                }

                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.shooterShot, SoundSource.PLAYERS, 0.7F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
            }
        }
    }

    @Override
    public PlayerPosingHandler.WeaponPose getPose(ItemStack stack)
    {
        return PlayerPosingHandler.WeaponPose.FIRE;
    }
}
