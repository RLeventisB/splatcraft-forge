package net.splatcraft.forge.items.weapons;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.splatcraft.forge.entities.ExtraSaveData;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.handlers.PlayerPosingHandler;
import net.splatcraft.forge.items.weapons.settings.BlasterWeaponSettings;
import net.splatcraft.forge.items.weapons.settings.CommonRecords;
import net.splatcraft.forge.items.weapons.settings.SlosherWeaponSettings;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.util.AttackId;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.PlayerCooldown;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SlosherItem extends WeaponBaseItem<SlosherWeaponSettings>
{
    public List<SlosherWeaponSettings.SingularSloshShotData> pendingSloshes = new ArrayList<>();
    public Type slosherType = Type.DEFAULT;

    public static RegistryObject<SlosherItem> create(DeferredRegister<Item> register, String settings, String name, Type slosherType)
    {
        return register.register(name, () -> new SlosherItem(settings).setSlosherType(slosherType));
    }

    public static RegistryObject<SlosherItem> create(DeferredRegister<Item> register, RegistryObject<SlosherItem> parent, String name)
    {
        return register.register(name, () -> new SlosherItem(parent.get().settingsId.toString()).setSlosherType(parent.get().slosherType));
    }

    protected SlosherItem(String settings)
    {
        super(settings);
    }

    @Override
    public Class<SlosherWeaponSettings> getSettingsClass()
    {
        return SlosherWeaponSettings.class;
    }

    public SlosherItem setSlosherType(Type type)
    {
        this.slosherType = type;
        return this;
    }

    @Override
    public void weaponUseTick(Level level, LivingEntity entity, ItemStack stack, int timeLeft)
    {
        SlosherWeaponSettings settings = getSettings(stack);
        if (entity instanceof Player player && getUseDuration(stack) - timeLeft < settings.shotData.endlagTicks())
        {
            ItemCooldowns cooldownTracker = player.getCooldowns();
            if (!cooldownTracker.isOnCooldown(this))
            {
                pendingSloshes.addAll(settings.shotData.sloshes());
                PlayerCooldown.setPlayerCooldown(player, new SloshCooldown(player, stack, player.getInventory().selected, entity.getUsedItemHand(), settings, settings.shotData.endlagTicks()));
                if (!level.isClientSide && settings.shotData.endlagTicks() > 0)
                {
                    cooldownTracker.addCooldown(this, settings.shotData.endlagTicks());
                }
            }
        }
    }

    @Override
    public PlayerPosingHandler.WeaponPose getPose(ItemStack stack)
    {
        return PlayerPosingHandler.WeaponPose.BUCKET_SWING;
    }

    public enum Type
    {
        DEFAULT,
        EXPLODING,
        CYCLONE,
        BUBBLES
    }

    public static class SloshCooldown extends PlayerCooldown
    {
        public record CalculatedSloshData(float time, byte indexInSlosh, int sloshDataIndex)
        {
        }

        public SlosherWeaponSettings sloshData = null;
        public List<CalculatedSloshData> sloshes = new ArrayList<>();
        public boolean didSound;
        public AttackId attackId;
        public float xRot, xDelta, yRot, yDelta, xRotOld, yRotOld;

        public SloshCooldown(Player player, ItemStack stack, int slotIndex, InteractionHand hand, SlosherWeaponSettings sloshData, int duration)
        {
            super(stack, duration, slotIndex, hand, true, false, true, false);
            xRot = xRotOld = player.getXRot();
            yRot = yRotOld = player.getYRot();
            this.sloshData = sloshData;
            for (int i = 0; i < sloshData.shotData.sloshes().size(); i++)
            {
                SlosherWeaponSettings.SingularSloshShotData slosh = sloshData.shotData.sloshes().get(i);
                for (byte j = 0; j < slosh.count(); j++)
                {
                    sloshes.add(new CalculatedSloshData(slosh.startupTicks() + j * slosh.delayBetweenProjectiles(), j, i));
                }
            }
            attackId = AttackId.registerAttack().countProjectile(sloshes.size());
        }

        public SloshCooldown(CompoundTag nbt)
        {
            super(ItemStack.of(nbt.getCompound("StoredStack")), nbt.getInt("MaxTime"), nbt.getInt("SlotIndex"), nbt.getBoolean("MainHand") ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true, false, true, false);
            setTime(nbt.getInt("Time"));
            didSound = nbt.getBoolean("DidSound");
            fromNbt(nbt);
        }

        @Override
        public void tick(Player player)
        {
            Level level = player.level();

            if (level.isClientSide || sloshData == null)
                return;

            float frame = getMaxTime() - getTime();
            SlosherWeaponSettings.SlosherShotDataRecord shotSetting = sloshData.shotData;
            SlosherItem slosherItem = (SlosherItem) storedStack.getItem();

            if (shotSetting.allowFlicking())
            {
                xDelta = xDelta * 0.7f + (Mth.degreesDifference(xRot, player.getXRot())) * 0.12f;
                yDelta = yDelta * 0.7f + (Mth.degreesDifference(yRot, player.getYRot())) * 0.12f;
                xRotOld = xRot;
                yRotOld = yRot;

                xRot += xDelta * (didSound ? 1 : 0.4f);
                yRot += yDelta * (didSound ? 1 : 0.4f);
            }
            else
            {
                xRotOld = xRot;
                yRotOld = yRot;
                xRot = player.getXRot();
                yRot = player.getYRot();
            }

            for (int i = 0; i < sloshes.size(); i++)
            {
                CalculatedSloshData calculatedSloshData = sloshes.get(i);
                if (calculatedSloshData.time <= frame)
                {
                    float extraTime = frame - calculatedSloshData.time;
                    float partialTick = 1 - extraTime;

                    if (didSound || reduceInk(player, slosherItem, shotSetting.inkConsumption(), shotSetting.inkRecoveryCooldown(), true))
                    {
                        SlosherWeaponSettings.SingularSloshShotData projectileSetting = shotSetting.sloshes().get(calculatedSloshData.sloshDataIndex);
                        CommonRecords.ProjectileDataRecord projectileData = sloshData.getProjectileDataAtIndex(calculatedSloshData.sloshDataIndex);

                        InkProjectileEntity proj = new InkProjectileEntity(level, player, storedStack, InkBlockUtils.getInkType(player), projectileData.size(), sloshData);
                        proj.setSlosherStats(projectileData);

                        float xRotation = Mth.lerp(partialTick, yRotOld, yRot);
                        proj.shootFromRotation(
                                Mth.lerp(partialTick, xRotOld, xRot),
                                xRotation + projectileSetting.offsetAngle() - 3,
                                shotSetting.pitchCompensation(),
                                projectileData.speed() - projectileSetting.speedSubstract() * calculatedSloshData.indexInSlosh,
                                0,
                                partialTick);
                        proj.setAttackId(attackId);

                        proj.moveTo(proj.position().add(Entity.getInputVector(new Vec3(-0.4, -1, 0), 1, xRotation)));

                        switch (slosherItem.slosherType)
                        {
                            case EXPLODING:
                                Optional<BlasterWeaponSettings.DetonationRecord> detonationData = projectileSetting.detonationData();
                                if (detonationData.isPresent())
                                {
                                    proj.explodes = true;
//                                    proj.setProjectileType(InkProjectileEntity.Types.BLASTER);
                                    BlasterWeaponSettings.DetonationRecord detonationRecord = detonationData.get();
                                    proj.addExtraData(new ExtraSaveData.ExplosionExtraData(detonationRecord));
                                }
                            case CYCLONE:
                                proj.canPierce = true;
                        }
                        proj.addExtraData(new ExtraSaveData.SloshExtraData(calculatedSloshData.sloshDataIndex, proj.getY()));
                        level.addFreshEntity(proj);

                        proj.tick(extraTime);

                        if (!didSound)
                        {
                            level.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.slosherShot, SoundSource.PLAYERS, 0.7F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
                            didSound = true;
                        }
                    }
                    else
                    {
                        setTime(0);
                        break;
                    }

                    sloshes.remove(i);
                    i--;
                }
            }
        }

        @Override
        public CompoundTag writeNBT(CompoundTag nbt)
        {
            nbt.putInt("Time", getTime());
            nbt.putInt("MaxTime", getMaxTime());
            nbt.putInt("SlotIndex", getSlotIndex());
            nbt.putBoolean("DidSound", didSound);
            nbt.putBoolean("MainHand", getHand().equals(InteractionHand.MAIN_HAND));
            if (storedStack.getItem() != Items.AIR)
            {
                nbt.put("StoredStack", storedStack.serializeNBT());
            }

            nbt.putBoolean("SloshCooldown", true);

            return nbt;
        }
    }
}