package net.splatcraft.util;

import net.minecraft.core.Holder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.World;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.entities.IColoredEntity;
import net.splatcraft.entities.SpawnShieldEntity;
import net.splatcraft.entities.SquidBumperEntity;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateInkOverlayPacket;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.registries.SplatcraftGameRules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class InkDamageUtils
{
    public static boolean doSplatDamage(LivingEntity target, float damage, Entity source, ItemStack sourceItem, AttackId attackId)
    {
        return doDamage(target, damage, source, source, sourceItem, SplatcraftDamageTypes.INK_SPLAT, false, attackId);
    }

    public static boolean doRollDamage(LivingEntity target, float damage, Entity owner, Entity source, ItemStack sourceItem)
    {
        return doDamage(target, damage, source, owner, sourceItem, SplatcraftDamageTypes.ROLL_CRUSH, true, AttackId.NONE);
    }

    public static boolean canDamage(Entity target, Entity source)
    {
        return canDamage(target, ColorUtils.getEntityColor(source));
    }

    public static boolean canDamage(Entity target, int color)
    {
        boolean result = canDamageColor(target.getWorld(), target.blockPosition(), ColorUtils.getEntityColor(target), color);

        if (result)
            for (SpawnShieldEntity shield : target.getWorld().getEntitiesOfClass(SpawnShieldEntity.class, target.getBoundingBox()))
                if (ColorUtils.colorEquals(target.getWorld(), target.blockPosition(), ColorUtils.getEntityColor(shield), ColorUtils.getEntityColor(target)))
                    return false;

        return result;
    }

    public static boolean canDamageColor(World level, BlockPos pos, int targetColor, int sourceColor)
    {
        return SplatcraftGameRules.getLocalizedRule(level, pos, SplatcraftGameRules.INK_FRIENDLY_FIRE) || !ColorUtils.colorEquals(level, pos, targetColor, sourceColor);
    }

    public static boolean doDamage(LivingEntity target, float damage, Entity projectile, Entity owner, ItemStack sourceItem, ResourceKey<DamageType> damageType, boolean applyHurtCooldown, AttackId attackId)
    {
        //Negate ink damage when super jumping
        if (PlayerInfoCapability.hasCapability(target))
        {
            PlayerInfo info = PlayerInfoCapability.get(target);
            if (info.hasPlayerCooldown() && info.getPlayerCooldown() instanceof SuperJumpCommand.SuperJump)
                return false;
        }

        Level targetLevel = target.getWorld();
        int color = ColorUtils.getEntityColor(projectile);
        InkDamageSource damageSource = new InkDamageSource(SplatcraftDamageTypes.get(targetLevel, damageType), owner, projectile, sourceItem);

        boolean attackIdValid = attackId != null && !attackId.checkEntity(target);
        if (attackIdValid || damage <= 0 || (target.isInvulnerableTo(damageSource) && !(target instanceof SquidBumperEntity)))
            return false;

        if (InkOverlayCapability.hasCapability(target) && InkOverlayCapability.get(target).isInkproof())
            return false;

        float mobDmgPctg = SplatcraftGameRules.getIntRuleValue(targetLevel, SplatcraftGameRules.INK_MOB_DAMAGE_PERCENTAGE) * 0.01f;

        int targetColor = ColorUtils.getEntityColor(target);
        boolean doDamage = target instanceof Player || mobDmgPctg > 0;
        boolean canInk = canDamage(target, color);

        if (targetColor > -1)
        {
            doDamage = canInk;
        }

        if (target instanceof IColoredEntity coloredEntity)
        {
            target.invulnerableTime = (!applyHurtCooldown && !SplatcraftGameRules.getBooleanRuleValue(target.getWorld(), SplatcraftGameRules.INK_DAMAGE_COOLDOWN)) ? 0 : 20;
            doDamage = coloredEntity.onEntityInked(damageSource, damage, color);
        }
        else if (target instanceof Sheep sheep)
        {
            if (!sheep.isSheared())
            {
                doDamage = false;
                canInk = false;
                targetColor = 1;

                InkOverlayInfo info = InkOverlayCapability.get(target);

                info.setWoolColor(color);
                if (!targetLevel.isClientSide())
                {
                    SplatcraftPacketHandler.sendToTrackers(new UpdateInkOverlayPacket(target, info), target);
                }
            }
        }

        if (!(target instanceof SquidBumperEntity) && doDamage)
        {
            doDamage = target.hurt(damageSource, damage * (target instanceof PlayerEntity || target instanceof IColoredEntity ? 1 : mobDmgPctg));
            target.hurtMarked = false;
        }

        if ((targetColor <= -1 || canInk) && !target.isInWater() && !(target instanceof IColoredEntity coloredEntity && !coloredEntity.handleInkOverlay()))
        {
            if (InkOverlayCapability.hasCapability(target))
            {
                InkOverlayInfo info = InkOverlayCapability.get(target);
                if (info.getAmount() < target.getMaxHealth() * 1.5)
                    info.addAmount(damage * (target instanceof IColoredEntity ? 1 : Math.max(0.5f, mobDmgPctg)));
                info.setColor(color);
                if (!targetLevel.isClientSide())
                {
                    SplatcraftPacketHandler.sendToTrackersAndSelf(new UpdateInkOverlayPacket(target, info), target);
                }
            }
        }

        if (!applyHurtCooldown && !SplatcraftGameRules.getBooleanRuleValue(target.getWorld(), SplatcraftGameRules.INK_DAMAGE_COOLDOWN))
            target.hurtTime = 0;

        return doDamage;
    }

    public static boolean isSplatted(LivingEntity target)
    {
        return target instanceof SquidBumperEntity bumperEntity ? bumperEntity.getInkHealth() <= 0 : target.isDeadOrDying();
    }

    public static class InkDamageSource extends DamageSource
    {
        private final ItemStack weapon;

        public InkDamageSource(Holder<DamageType> pType, @Nullable Entity pDirectEntity, @Nullable Entity pCausingEntity, ItemStack weapon)
        {
            super(pType, pDirectEntity, pCausingEntity);
            this.weapon = weapon;
        }

        @Override
        public @NotNull Component getLocalizedDeathMessage(@NotNull LivingEntity entityLivingBaseIn)
        {
            String base = "death.attack." + this.type().msgId();

            if (getEntity() == null && getDirectEntity() == null)
            {
                return !weapon.isEmpty() ? Component.translatable(base + ".item", entityLivingBaseIn.getDisplayName(), weapon.getDisplayName()) : Component.translatable(base, entityLivingBaseIn.getDisplayName());
            }
            base += ".player";

            Component itextcomponent = this.getEntity() == null ? Objects.requireNonNull(this.getDirectEntity()).getDisplayName() : this.getEntity().getDisplayName();

            return !weapon.isEmpty() ? Component.translatable(base + ".item", entityLivingBaseIn.getDisplayName(), itextcomponent, weapon.getDisplayName()) : Component.translatable(base, entityLivingBaseIn.getDisplayName(), itextcomponent);
        }
    }
}