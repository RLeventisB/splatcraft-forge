package net.splatcraft.items.weapons;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftSounds;
import org.jetbrains.annotations.NotNull;

public class CurlingSubWeaponItem extends SubWeaponItem
{
    public static final float MAX_INK_RECOVERY_COOLDOWN = 70f / 3f;
    public static final float INK_RECOVERY_COOLDOWN_MULTIPLIER = 40f / 3f;

    public CurlingSubWeaponItem(RegistrySupplier<? extends EntityType<? extends AbstractSubWeaponEntity>> entityType, String settings, SubWeaponAction useTick)
    {
        super(entityType, settings, useTick);
    }

    @Override
    protected void throwSub(@NotNull ItemStack stack, @NotNull World world, LivingEntity entity)
    {
        entity.swingHand(entity.getOffHandStack().equals(stack) ? Hand.OFF_HAND : Hand.MAIN_HAND, false);

        SubWeaponSettings settings = getSettings(stack);
        if (!world.isClient())
        {
            AbstractSubWeaponEntity proj = AbstractSubWeaponEntity.create(entityType.get(), world, entity, stack.copy());

            stack.remove(SplatcraftComponents.SUB_WEAPON_DATA);

            proj.setItem(stack);
            proj.shoot(entity, entity.getPitch(), entity.getYaw(), settings.subDataRecord.throwAngle(), settings.subDataRecord.throwVelocity(), 0);
            proj.setVelocity(proj.getVelocity().add(entity.getVelocity().multiply(1, 0, 1)));
            world.spawnEntity(proj);
        }
        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.subThrow, SoundCategory.PLAYERS, 0.7F, 1);
        if (singleUse(stack))
        {
            if (entity instanceof PlayerEntity player && !player.isCreative())
                stack.decrement(1);
        }
        else
        {
            int cookTime = stack.get(SplatcraftComponents.SUB_WEAPON_DATA).getInt("CookTime");
            reduceInk(entity, this, settings.subDataRecord.inkConsumption(), (int) (MAX_INK_RECOVERY_COOLDOWN - (float) cookTime / Math.max(settings.subDataRecord.curlingData().cookTime(), 1) * INK_RECOVERY_COOLDOWN_MULTIPLIER), false);
        }
    }
}
