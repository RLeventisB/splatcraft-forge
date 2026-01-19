package net.splatcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.capabilities.structs.InkOverlayData;
import net.splatcraft.entities.IColoredEntity;
import net.splatcraft.entities.ShieldingEntity;
import net.splatcraft.entities.SpawnShieldEntity;
import net.splatcraft.entities.SquidBumperEntity;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateInkOverlayPacket;
import net.splatcraft.platform.Components;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.structs.AttackId;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class InkDamageUtils
{
	public static boolean doSplatDamage(Entity target, float damage, Entity source, ItemStack sourceItem, AttackId attackId)
	{
		return doDamage(target, damage, source, source, sourceItem, SplatcraftDamageTypes.INK_SPLAT, false, attackId);
	}
	public static boolean doRollDamage(Entity target, float damage, Entity owner, Entity source, ItemStack sourceItem)
	{
		return doDamage(target, damage, source, owner, sourceItem, SplatcraftDamageTypes.ROLL_CRUSH, true, AttackId.NONE);
	}
	public static boolean doRollDamage(Entity target, float damage, Entity owner, Entity source, ItemStack sourceItem, AttackId attackId)
	{
		return doDamage(target, damage, source, owner, sourceItem, SplatcraftDamageTypes.ROLL_CRUSH, false, attackId);
	}
	public static boolean canDamage(Entity target, Entity source)
	{
		return canDamage(target, ColorUtils.getEntityColor(source));
	}
	public static boolean canDamage(Entity target, InkColor color)
	{
		if (target instanceof LivingEntity livingTarget && EntityAction.hasSpecificEntityAction(livingTarget, SuperJumpCommand.SuperJump.class))
		{
			return false;
		}
		
		InkColor targetColor = ColorUtils.getEntityColor(target);
		boolean canDamage = canDamageColor(target.level(), target.blockPosition(), targetColor, color);
		
		if (canDamage && !(target instanceof ShieldingEntity) && SpawnShieldEntity.isSpawnShieldPresent(target.level(), target.blockPosition(), target.getBoundingBox(), targetColor))
			return false;
		
		return canDamage;
	}
	public static boolean canDamageColor(Level level, BlockPos pos, InkColor targetColor, InkColor sourceColor)
	{
		return SplatcraftGameRules.getLocalizedRule(level, pos, SplatcraftGameRules.INK_FRIENDLY_FIRE) || !ColorUtils.colorEquals(level, pos, targetColor, sourceColor);
	}
	public static boolean doDamage(Entity target, float damage, Entity projectile, Entity owner, ItemStack sourceItem, ResourceKey<DamageType> damageType, boolean applyHurtCooldown, @Nullable AttackId attackId)
	{
		//Negate ink damage when super jumping
		boolean isLiving = target instanceof LivingEntity;
		LivingEntity livingTarget = isLiving ? (LivingEntity) target : null;
		
		Level targetLevel = target.level();
		InkColor damageColor = ColorUtils.getEntityColor(projectile);
		InkColor targetColor = ColorUtils.getEntityColor(target);
		
		InkDamageSource damageSource = new InkDamageSource(SplatcraftDamageTypes.get(targetLevel, damageType), owner, projectile, sourceItem);
		
		if (target.isInvulnerableTo(damageSource) && !(target instanceof SquidBumperEntity))
			return false;
		
		boolean attackIdIsNull = attackId == null;
		if (!attackIdIsNull)
			damage = attackId.getDamage(target, damage);
		
		if (damage <= 0)
			return false;
		
		if (isLiving && Components.INK_OVERLAY.hasAnd(livingTarget, InkOverlayData::isInkproof)) return false;
		
		float mobDmgPctg = SplatcraftGameRules.getIntRuleValue(targetLevel, SplatcraftGameRules.INK_MOB_DAMAGE_PERCENTAGE) * 0.01f;
		
		boolean doDamage = target instanceof Player || mobDmgPctg > 0;
		boolean canInk = canDamage(target, damageColor);
		
		if (targetColor.isValid())
		{
			doDamage = canInk;
		}
		
		if (target instanceof IColoredEntity coloredEntity)
		{
			boolean applyCooldown = applyHurtCooldown || SplatcraftGameRules.getBooleanRuleValue(targetLevel, SplatcraftGameRules.INK_DAMAGE_COOLDOWN);
			target.invulnerableTime = applyCooldown ? 20 : 0;
			doDamage = coloredEntity.onEntityInked(damageSource, damage, damageColor);
		}
		else if (target instanceof Sheep sheep)
		{
			if (!sheep.isSheared())
			{
				doDamage = false;
				canInk = false;
				targetColor = InkColor.INVALID;
				
				sheep.setColor(damageColor.getDyeColor());
			}
		}
		
		if (!(target instanceof SquidBumperEntity) && doDamage)
		{
			damage *= (target instanceof Player || target instanceof IColoredEntity ? 1 : mobDmgPctg);
			doDamage = target.hurt(damageSource, damage);
			target.hurtMarked = false;
		}
		
		if (isLiving)
		{
			if (doDamage && (targetColor.isInvalid() || canInk) && !target.isUnderWater() && !(target instanceof IColoredEntity coloredEntity && !coloredEntity.handleInkOverlay()))
			{
				InkOverlayData info = Components.INK_OVERLAY.getOrCreate(livingTarget);
				if (info.getAmount() < livingTarget.getMaxHealth())
					info.addAmount(damage);
				
				info.setColor(damageColor);
				if (!targetLevel.isClientSide())
				{
					SplatcraftPacketHandler.sendToTrackersAndSelf(new UpdateInkOverlayPacket(livingTarget, info), target);
				}
			}
			
			if (!applyHurtCooldown && !SplatcraftGameRules.getBooleanRuleValue(target.level(), SplatcraftGameRules.INK_DAMAGE_COOLDOWN))
			{
				livingTarget.hurtTime = 0;
				livingTarget.invulnerableTime = 0;
			}
		}
		
		return doDamage;
	}
	public static boolean isSplatted(Entity target)
	{
		return target instanceof SquidBumperEntity bumperEntity ? !bumperEntity.isPickable() : (target instanceof LivingEntity living && living.isDeadOrDying()) || !target.isAlive();
	}
	public static class InkDamageSource extends DamageSource
	{
		private final ItemStack weapon;
		public final boolean doSound;
		public InkDamageSource(Holder.Reference<DamageType> type, @Nullable Entity damager, @Nullable Entity owner, ItemStack weapon)
		{
			this(type, damager, owner, weapon, true);
		}
		public InkDamageSource(Holder.Reference<DamageType> type, @Nullable Entity damager, @Nullable Entity owner, ItemStack weapon, boolean doSound)
		{
			super(type, damager, owner);
			this.weapon = weapon;
			this.doSound = doSound;
		}
		@Override
		public @Nullable ItemStack getWeaponItem()
		{
			return weapon;
		}
		@Override
		public @NotNull Component getLocalizedDeathMessage(@NotNull LivingEntity killed)
		{
			String base = "death.attack." + type().msgId();
			
			if (getEntity() == null && getDirectEntity() == null)
			{
				return !weapon.isEmpty() ? Component.translatable(base + ".item", killed.getDisplayName(), weapon.getHoverName()) : Component.translatable(base, killed.getDisplayName());
			}
			base += ".player";
			
			Component itextcomponent = getEntity() == null ? Objects.requireNonNull(getDirectEntity()).getDisplayName() : getEntity().getDisplayName();
			
			return !weapon.isEmpty() ? Component.translatable(base + ".item", killed.getDisplayName(), itextcomponent, weapon.getHoverName()) : Component.translatable(base, killed.getDisplayName(), itextcomponent);
		}
	}
}