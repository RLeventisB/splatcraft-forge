package net.splatcraft.forge.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.forge.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.forge.entities.IColoredEntity;
import net.splatcraft.forge.entities.SpawnShieldEntity;
import net.splatcraft.forge.entities.SquidBumperEntity;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.s2c.UpdateInkOverlayPacket;
import net.splatcraft.forge.registries.SplatcraftGameRules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class InkDamageUtils
{
	public static final ResourceKey<DamageType> SPLAT = register("splat");
	public static final ResourceKey<DamageType> ROLL = register("roll");
	public static final ResourceKey<DamageType> ENEMY_INK = register("enemy_ink");
	public static final ResourceKey<DamageType> WATER = register("water");
	public static final ResourceKey<DamageType> OUT_OF_STAGE = register("out_of_stage");
	private static ResourceKey<DamageType> register(String name)
	{
		return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(Splatcraft.MODID, name));
	}
	public static boolean doSplatDamage(LivingEntity target, float damage, Entity source, ItemStack sourceItem, AttackId attackId)
	{
		return doDamage(target, damage, source, source, sourceItem, SPLAT, false, attackId);
	}
	public static boolean doRollDamage(LivingEntity target, float damage, Entity owner, Entity source, ItemStack sourceItem)
	{
		return doDamage(target, damage, source, owner, sourceItem, ROLL, true, AttackId.NONE);
	}
	public static boolean canDamage(Entity target, Entity source)
	{
		return canDamage(target, ColorUtils.getEntityColor(source));
	}
	public static boolean canDamage(Entity target, int color)
	{
		boolean result = canDamageColor(target.level(), target.blockPosition(), ColorUtils.getEntityColor(target), color);
		
		if (result)
			for (SpawnShieldEntity shield : target.level().getEntitiesOfClass(SpawnShieldEntity.class, target.getBoundingBox()))
				if (ColorUtils.colorEquals(target.level(), target.blockPosition(), ColorUtils.getEntityColor(shield), ColorUtils.getEntityColor(target)))
					return false;
		
		return result;
	}
	public static boolean canDamageColor(Level level, BlockPos pos, int targetColor, int sourceColor)
	{
		return SplatcraftGameRules.getLocalizedRule(level, pos, SplatcraftGameRules.INK_FRIENDLY_FIRE) || !ColorUtils.colorEquals(level, pos, targetColor, sourceColor);
	}
	public static boolean doDamage(LivingEntity target, float damage, Entity projectile, Entity owner, ItemStack sourceItem, ResourceKey<DamageType> damageType, boolean applyHurtCooldown, AttackId attackId)
	{
		Level targetLevel = target.level();
		int color = ColorUtils.getEntityColor(projectile);
//		InkDamageSource damageSource = new InkDamageSource(targetLevel.damageSources().source(damageType).type(), projectile, owner, sourceItem);
		DamageSource damageSource = targetLevel.damageSources().source(damageType);
		
		if (!attackId.checkEntity(target) || damage <= 0 || (target.isInvulnerableTo(damageSource) && !(target instanceof SquidBumperEntity)))
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
			target.invulnerableTime = (!applyHurtCooldown && !SplatcraftGameRules.getBooleanRuleValue(target.level(), SplatcraftGameRules.INK_DAMAGE_COOLDOWN)) ? 1 : 20;
			doDamage = coloredEntity.onEntityInked(damageSource, damage, color);
		}
		else if (target instanceof Sheep)
		{
			if (!((Sheep) target).isSheared())
			{
				doDamage = false;
				canInk = false;
				targetColor = 1;
				
				InkOverlayInfo info = InkOverlayCapability.get(target);
				
				info.setWoolColor(color);
				if (!targetLevel.isClientSide)
					SplatcraftPacketHandler.sendToAll(new UpdateInkOverlayPacket(target, info));
			}
		}
		
		if (!(target instanceof SquidBumperEntity) && doDamage)
		{
//			Vec3 deltaMovement = target.getDeltaMovement();
			doDamage = target.hurt(damageSource, damage * (target instanceof Player || target instanceof IColoredEntity ? 1 : mobDmgPctg));
//			target.setDeltaMovement(deltaMovement); // trying to prevent knockback... (this game is so dumb)  should be ignored now!!!!
			target.hurtMarked = false;
		}
		
		if ((targetColor <= -1 || canInk) && !target.isInWater() && !(target instanceof IColoredEntity && !((IColoredEntity) target).handleInkOverlay()))
		{
			if (InkOverlayCapability.hasCapability(target))
			{
				InkOverlayInfo info = InkOverlayCapability.get(target);
				if (info.getAmount() < target.getMaxHealth() * 1.5)
					info.addAmount(damage * (target instanceof IColoredEntity ? 1 : Math.max(0.5f, mobDmgPctg)));
				info.setColor(color);
				if (!targetLevel.isClientSide)
					SplatcraftPacketHandler.sendToAll(new UpdateInkOverlayPacket(target, info));
			}
		}
		
		if (!applyHurtCooldown && !SplatcraftGameRules.getBooleanRuleValue(target.level(), SplatcraftGameRules.INK_DAMAGE_COOLDOWN))
			target.hurtTime = 1;
		
		return doDamage;
	}
	public static boolean isSplatted(LivingEntity target)
	{
		return target instanceof SquidBumperEntity bumperEntity ? bumperEntity.getInkHealth() <= 0 : target.isDeadOrDying();
	}
	public static class InkDamageSource extends DamageSource
	{
		private final ItemStack weapon;
		public InkDamageSource(DamageType damageType, Entity source, @Nullable Entity owner, ItemStack weapon)
		{
			super(new Holder.Direct<>(damageType), source, owner);
			this.weapon = weapon;
		}
		@Override
		public @NotNull Component getLocalizedDeathMessage(@NotNull LivingEntity target)
		{
			String base = "death.attack." + this.getMsgId();
			
			if (getEntity() == null && getDirectEntity() == null)
			{
				return weapon.isEmpty() ? Component.translatable(base, target.getDisplayName()) : Component.translatable(base + ".item", target.getDisplayName(), weapon.getDisplayName());
			}
			base += ".player";
			
			Component itextcomponent = this.getEntity() == null ? Objects.requireNonNull(getDirectEntity()).getDisplayName() : this.getEntity().getDisplayName();
			
			return weapon.isEmpty() ? Component.translatable(base, target.getDisplayName(), itextcomponent) : Component.translatable(base + ".item", target.getDisplayName(), itextcomponent, weapon.getDisplayName());
		}
	}
}
