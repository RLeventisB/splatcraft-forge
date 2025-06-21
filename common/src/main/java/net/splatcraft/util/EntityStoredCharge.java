package net.splatcraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.IChargeableWeapon;

import java.util.Optional;

public class EntityStoredCharge
{
	public static final Codec<EntityStoredCharge> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		EntitySlot.SERIALIZER_CODEC.fieldOf("weapon_slot").forGetter(v -> v.weaponSlot),
		Codec.FLOAT.fieldOf("charge").forGetter(v -> v.charge),
		Codec.INT.fieldOf("remaining_time").forGetter(v -> v.remainingTime)
	).apply(inst, EntityStoredCharge::new));
	public EntitySlot weaponSlot;
	public float charge;
	public int remainingTime;
	public EntityStoredCharge(EntitySlot slot, float charge, int time)
	{
		weaponSlot = slot;
		this.charge = charge;
		remainingTime = time;
	}
	public static Optional<EntityStoredCharge> getChargeOptional(LivingEntity entity)
	{
		return EntityInfoCapability.getOptional(entity).flatMap(EntityInfo::getStoredCharge);
	}
	public static void setCharge(LivingEntity entity, EntityStoredCharge charge)
	{
		EntityInfoCapability.get(entity).setStoredCharge(charge);
	}
	public static boolean hasCharge(LivingEntity entity)
	{
		if (entity == null)
		{
			throw new IllegalArgumentException("Attempted to retrieve charge for a null entity");
		}

		return EntityInfoCapability.getOptional(entity).map(v -> v.getStoredCharge().isPresent()).orElse(false);
	}
	public static boolean chargeMatches(LivingEntity entity, ItemStack stack)
	{
		return getChargeOptional(entity).map(v -> v.weaponSlot.isItemForSlot(entity, stack)).orElse(false);
	}
	public static void storeCharge(LivingEntity entity, ItemStack stack)
	{
		if (!(stack.getItem() instanceof IChargeableWeapon chargeableWeapon))
		{
			return;
		}

		entity.stopUsingItem();
		setCharge(entity, new EntityStoredCharge(EntitySlot.searchAndCreateWithStack(entity, stack), chargeableWeapon.getCharge(stack), chargeableWeapon.getStorageTime(stack)));
	}
	public static void retrieveCharge(LivingEntity entity, ItemStack stack)
	{
		if (!(stack.getItem() instanceof IChargeableWeapon chargeableWeapon) || !chargeMatches(entity, stack))
		{
			throw new IllegalArgumentException("Attempted to retrieve charge from another item");
		}
		else if (!hasCharge(entity))
		{
			throw new IllegalArgumentException("Attempted to retrieve empty charge");
		}
		EntityStoredCharge charge = getChargeOptional(entity).get();
		chargeableWeapon.retrieveCharge(entity, stack, charge.charge);
		emptyCharge(entity);
	}

	public static void dischargeWeapon(LivingEntity entity)
	{
		Optional<EntityStoredCharge> charge = getChargeOptional(entity);
		if (charge.isEmpty())
		{
			return;
		}
		else if (!chargeMatches(entity, entity.getMainHandItem()))
		{
			emptyCharge(entity);
			return;
		}

		EntityStoredCharge storedCharge = charge.get();
		storedCharge.remainingTime--;
		if (storedCharge.remainingTime <= 0)
		{
			emptyCharge(entity);
			return;
		}
		setCharge(entity, storedCharge);
	}
	public static void emptyCharge(LivingEntity entity)
	{
		setCharge(entity, null);
	}

	public void reset()
	{
		weaponSlot = EntitySlot.EMPTY;
		charge = 0;
		remainingTime = 0;
	}
	@Override
	public String toString()
	{
		return "PlayerCharge: [" + weaponSlot + " x " + charge + "] (" + super.toString() + ")";
	}
}