package net.splatcraft.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.entities.IColoredEntity;
import net.splatcraft.entities.SpawnShieldEntity;
import net.splatcraft.entities.SquidBumperEntity;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateInkOverlayPacket;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.registries.SplatcraftGameRules;
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
	public static boolean canDamage(Entity target, InkColor color)
	{
		boolean result = canDamageColor(target.getWorld(), target.getBlockPos(), ColorUtils.getEntityColor(target), color);
		
		if (result && !target.getWorld().getEntitiesByClass(SpawnShieldEntity.class, target.getBoundingBox(), (shield) -> ColorUtils.colorEquals(target.getWorld(), target.getBlockPos(), ColorUtils.getEntityColor(shield), ColorUtils.getEntityColor(target))).isEmpty())
			return false;
		
		return result;
	}
	public static boolean canDamageColor(World level, BlockPos pos, InkColor targetColor, InkColor sourceColor)
	{
		return SplatcraftGameRules.getLocalizedRule(level, pos, SplatcraftGameRules.INK_FRIENDLY_FIRE) || !ColorUtils.colorEquals(level, pos, targetColor, sourceColor);
	}
	public static boolean doDamage(LivingEntity target, float damage, Entity projectile, Entity owner, ItemStack sourceItem, RegistryKey<DamageType> damageType, boolean applyHurtCooldown, AttackId attackId)
	{
		//Negate ink damage when super jumping
		if (EntityInfoCapability.hasCapability(target))
		{
			EntityInfo info = EntityInfoCapability.get(target);
			if (info.hasPlayerCooldown() && info.getPlayerCooldown() instanceof SuperJumpCommand.SuperJump)
				return false;
		}
		
		World targetLevel = target.getWorld();
		InkColor color = ColorUtils.getEntityColor(projectile);
		InkDamageSource damageSource = new InkDamageSource(SplatcraftDamageTypes.get(targetLevel, damageType), owner, projectile, sourceItem);
		
		boolean attackIdValid = attackId != null && !attackId.checkEntity(target);
		if (attackIdValid || damage <= 0 || (target.isInvulnerableTo(damageSource) && !(target instanceof SquidBumperEntity)))
			return false;
		
		if (InkOverlayCapability.get(target).isInkproof())
			return false;
		
		float mobDmgPctg = SplatcraftGameRules.getIntRuleValue(targetLevel, SplatcraftGameRules.INK_MOB_DAMAGE_PERCENTAGE) * 0.01f;
		
		InkColor targetColor = ColorUtils.getEntityColor(target);
		boolean doDamage = target instanceof PlayerEntity || mobDmgPctg > 0;
		boolean canInk = canDamage(target, color);
		
		if (targetColor.isValid())
		{
			doDamage = canInk;
		}
		
		if (target instanceof IColoredEntity coloredEntity)
		{
			target.timeUntilRegen = (!applyHurtCooldown && !SplatcraftGameRules.getBooleanRuleValue(target.getWorld(), SplatcraftGameRules.INK_DAMAGE_COOLDOWN)) ? 0 : 20;
			doDamage = coloredEntity.onEntityInked(damageSource, damage, color);
		}
		else if (target instanceof SheepEntity sheep)
		{
			if (!sheep.isSheared())
			{
				doDamage = false;
				canInk = false;
				targetColor = InkColor.INVALID;
				
				InkOverlayInfo info = InkOverlayCapability.get(target);
				
				info.setWoolColor(color);
				if (!targetLevel.isClient())
				{
					SplatcraftPacketHandler.sendToTrackers(new UpdateInkOverlayPacket(target, info), target);
				}
			}
		}
		
		if (!(target instanceof SquidBumperEntity) && doDamage)
		{
			doDamage = target.damage(damageSource, damage * (target instanceof PlayerEntity || target instanceof IColoredEntity ? 1 : mobDmgPctg));
			target.velocityModified = false;
		}
		
		if ((!targetColor.isValid() || canInk) && !target.isSubmergedInWater() && !(target instanceof IColoredEntity coloredEntity && !coloredEntity.handleInkOverlay()))
		{
			InkOverlayInfo info = InkOverlayCapability.get(target);
			if (info.getAmount() < target.getMaxHealth() * 1.5)
				info.addAmount(damage * (target instanceof IColoredEntity ? 1 : Math.max(0.5f, mobDmgPctg)));
			info.setColor(color);
			if (!targetLevel.isClient())
			{
				SplatcraftPacketHandler.sendToTrackersAndSelf(new UpdateInkOverlayPacket(target, info), target);
			}
		}
		
		if (!applyHurtCooldown && !SplatcraftGameRules.getBooleanRuleValue(target.getWorld(), SplatcraftGameRules.INK_DAMAGE_COOLDOWN))
			target.hurtTime = 0;
		
		return doDamage;
	}
	public static boolean isSplatted(LivingEntity target)
	{
		return target instanceof SquidBumperEntity bumperEntity ? bumperEntity.getInkHealth() <= 0 : target.isDead();
	}
	public static class InkDamageSource extends DamageSource
	{
		private final ItemStack weapon;
		public InkDamageSource(RegistryEntry.Reference<DamageType> pType, @Nullable Entity pDirectEntity, @Nullable Entity pCausingEntity, ItemStack weapon)
		{
			super(pType, pDirectEntity, pCausingEntity);
			this.weapon = weapon;
		}
		@Override
		public Text getDeathMessage(LivingEntity killed)
		{
			String base = "death.attack." + getType().msgId();
			
			if (getAttacker() == null && getSource() == null)
			{
				return !weapon.isEmpty() ? Text.translatable(base + ".item", killed.getDisplayName(), weapon.getName()) : Text.translatable(base, killed.getDisplayName());
			}
			base += ".player";
			
			Text itextcomponent = getAttacker() == null ? Objects.requireNonNull(getSource()).getDisplayName() : getAttacker().getDisplayName();
			
			return !weapon.isEmpty() ? Text.translatable(base + ".item", killed.getDisplayName(), itextcomponent, weapon.getName()) : Text.translatable(base, killed.getDisplayName(), itextcomponent);
		}
	}
}