package net.splatcraft.forge.items.weapons;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.splatcraft.forge.entities.ExtraSaveData;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.handlers.PlayerPosingHandler;
import net.splatcraft.forge.items.weapons.settings.BlasterWeaponSettings;
import net.splatcraft.forge.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.util.AttackId;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.PlayerCooldown;

public class BlasterItem extends WeaponBaseItem<BlasterWeaponSettings>
{
    public static RegistryObject<BlasterItem> createBlaster(DeferredRegister<Item> registry, String settings, String name)
    {
        return registry.register(name, () -> new BlasterItem(settings));
    }

    public static RegistryObject<BlasterItem> createBlaster(DeferredRegister<Item> registry, RegistryObject<BlasterItem> parent, String name)
    {
        return registry.register(name, () -> new BlasterItem(parent.get().settingsId.toString()));
    }

    protected BlasterItem(String settings)
    {
        super(settings);
    }

    @Override
    public Class<BlasterWeaponSettings> getSettingsClass()
    {
        return BlasterWeaponSettings.class;
    }

    @Override
    public void weaponUseTick(Level level, LivingEntity entity, ItemStack stack, int timeLeft)
    {
        Player player = (Player) entity;
        ItemCooldowns cooldownTracker = player.getCooldowns();

        if (!cooldownTracker.isOnCooldown(this) && !PlayerCooldown.hasPlayerCooldown(player))
        {
            BlasterWeaponSettings settings = getSettings(stack);
            PlayerCooldown.setPlayerCooldown(player, new BlasterCooldown(stack, settings.shotData.startupTicks(), settings.shotData.getFiringSpeed(), player.getInventory().selected, entity.getUsedItemHand(), player.onGround()));
            if (!level.isClientSide())
            {
                cooldownTracker.addCooldown(this, settings.shotData.getFiringSpeed());
            }
        }
    }
    
    @Override
    public PlayerPosingHandler.WeaponPose getPose(ItemStack stack)
    {
        return PlayerPosingHandler.WeaponPose.FIRE;
    }

    public static class BlasterCooldown extends PlayerCooldown
    {
        private final int startupFrames;

        public BlasterCooldown(ItemStack stack, int startupFrames, int time, int slotIndex, InteractionHand hand, boolean isGrounded)
        {
            super(stack, time, slotIndex, hand, true, false, true, isGrounded);
            this.startupFrames = time - startupFrames;
        }

        public BlasterCooldown(CompoundTag nbt)
        {
            super(nbt);
            startupFrames = nbt.getInt("StartupFrames");
        }

        @Override
        public void tick(Player player)
        {
            Level level = player.level();
            if (getTime() == startupFrames && !level.isClientSide())
            {
                BlasterItem item = (BlasterItem) storedStack.getItem();
                BlasterWeaponSettings settings = item.getSettings(storedStack);

                if (reduceInk(player, item, settings.shotData.inkConsumption(), settings.shotData.inkRecoveryCooldown(), true))
                {
                    InkProjectileEntity proj = new InkProjectileEntity(level, player, storedStack, InkBlockUtils.getInkType(player), settings.projectileData.size(), settings);
                    proj.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, settings.projectileData.speed(), ShotDeviationHelper.updateShotDeviation(storedStack, level.getRandom(), settings.getShotDeviationData(storedStack, player)));
                    proj.setBlasterStats(settings);
                    proj.setAttackId(AttackId.registerAttack().countProjectile());
                    proj.addExtraData(new ExtraSaveData.ExplosionExtraData(settings.blasterData));
                    level.addFreshEntity(proj);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.blasterShot, SoundSource.PLAYERS, 0.7F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
                }
            }
        }

        @Override
        public CompoundTag writeNBT(CompoundTag nbt)
        {
            nbt.putInt("StartupFrames", startupFrames);
            return super.writeNBT(nbt);
        }
    }
}