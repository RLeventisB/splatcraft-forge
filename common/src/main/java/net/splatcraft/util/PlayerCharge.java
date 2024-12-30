package net.splatcraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.IChargeableWeapon;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.UpdateChargeStatePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerCharge
{
	public static final Codec<PlayerCharge> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		ItemStack.OPTIONAL_CODEC.fieldOf("charged_weapon").forGetter(v -> v.chargedWeapon),
		Codec.FLOAT.fieldOf("charge").forGetter(v -> v.charge),
		Codec.FLOAT.fieldOf("max_charge").forGetter(v -> v.maxCharge),
		Codec.FLOAT.fieldOf("prev_charge").forGetter(v -> v.prevCharge),
		Codec.INT.fieldOf("discharged_ticks").forGetter(v -> v.dischargedTicks),
		Codec.INT.fieldOf("prev_discharged_ticks").forGetter(v -> v.prevDischargedTicks),
		Codec.INT.fieldOf("total_charges").forGetter(v -> v.totalCharges),
		Codec.BOOL.fieldOf("store_partial").forGetter(v -> v.storePartial)
	).apply(inst, PlayerCharge::new));
	private static final Map<UUID, Boolean> hasChargeServerPlayerMap = new HashMap<>();
	public ItemStack chargedWeapon;
	public float charge;
	public float maxCharge;
	public float prevCharge;
	public int dischargedTicks;
	public int prevDischargedTicks;
	public int totalCharges;
	public boolean storePartial;
	public PlayerCharge(ItemStack stack, float charge, boolean chargeDecay)
	{
		this(stack, charge, chargeDecay, 1);
	}
	public PlayerCharge(ItemStack stack, float charge, boolean chargeDecay, int totalCharges)
	{
		chargedWeapon = stack;
		this.charge = charge;
		maxCharge = charge;
		this.totalCharges = totalCharges;
		storePartial = chargeDecay;
	}
	public PlayerCharge(ItemStack chargedWeapon,
	                    float charge,
	                    float maxCharge,
	                    float prevCharge,
	                    int dischargedTicks,
	                    int prevDischargedTicks,
	                    int totalCharges,
	                    boolean storePartial)
	{
		this.chargedWeapon = chargedWeapon;
		this.charge = charge;
		this.maxCharge = maxCharge;
		this.prevCharge = prevCharge;
		this.dischargedTicks = dischargedTicks;
		this.prevDischargedTicks = prevDischargedTicks;
		this.totalCharges = totalCharges;
		this.storePartial = storePartial;
	}
	public static PlayerCharge getCharge(PlayerEntity player)
	{
		return EntityInfoCapability.get(player).getPlayerCharge();
	}
	public static void setCharge(PlayerEntity player, PlayerCharge charge)
	{
		EntityInfoCapability.get(player).setPlayerCharge(charge);
	}
	public static boolean hasCharge(PlayerEntity player)
	{
		if (player == null)
		{
			throw new IllegalArgumentException("Attempted to retrieve charge for a null player");
		}
		
		if (player instanceof ServerPlayerEntity serverPlayer)
		{
			return hasChargeServerPlayerMap.getOrDefault(serverPlayer.getUuid(), false);
		}
		
		if (!EntityInfoCapability.hasCapability(player))
		{
			return false;
		}
		
		EntityInfo capability = EntityInfoCapability.get(player);
		return capability.getPlayerCharge() != null && capability.getPlayerCharge().charge > 0;
	}
	public static boolean shouldCreateCharge(PlayerEntity player)
	{
		if (player == null)
		{
			return false;
		}
		EntityInfo capability = EntityInfoCapability.get(player);
		return capability.getPlayerCharge() == null;
	}
	public static boolean chargeMatches(PlayerEntity player, ItemStack stack)
	{
		return hasCharge(player) && ItemStack.areEqual(getCharge(player).chargedWeapon, stack);
	}
	public static void addChargeValue(PlayerEntity player, ItemStack stack, float value, boolean storePartial)
	{
		addChargeValue(player, stack, value, storePartial, 1);
	}
	public static void addChargeValue(PlayerEntity player, ItemStack stack, float value, boolean storePartial, int totalCharges)
	{
		if (value < 0.0f)
		{
			throw new IllegalArgumentException("Attempted to add negative charge: " + value);
		}
		if (shouldCreateCharge(player))
		{
			setCharge(player, new PlayerCharge(stack, 0, storePartial, totalCharges));
		}
		
		PlayerCharge charge = getCharge(player);
		if (charge.charge <= 0.0f && value > 0.0f && player.equals(ClientUtils.getClientPlayer()))
		{
			SplatcraftPacketHandler.sendToServer(new UpdateChargeStatePacket(true));
		}
		
		if (chargeMatches(player, stack))
		{
			charge.prevCharge = charge.charge;
			charge.charge = Math.max(0.0f, Math.min(charge.totalCharges, charge.charge + value));
			charge.maxCharge = charge.charge;
			
			charge.dischargedTicks = 0;
			charge.prevDischargedTicks = 0;
		}
		else
		{
			setCharge(player, new PlayerCharge(stack, value, storePartial, totalCharges));
		}
	}
	public static float getChargeValue(PlayerEntity player, ItemStack stack)
	{
		return chargeMatches(player, stack) ? getCharge(player).charge : 0;
	}
	public static void dischargeWeapon(PlayerEntity player)
	{
		if (!player.getWorld().isClient() || !hasCharge(player))
			return;
		
		PlayerCharge charge = getCharge(player);
		Item dischargeItem = charge.chargedWeapon.getItem();
		
		charge.prevDischargedTicks = charge.dischargedTicks;
		charge.prevCharge = charge.charge;
		
		if (dischargeItem instanceof IChargeableWeapon chargeable)
		{
			if (!EntityInfoCapability.isSquid(player))
			{
				int decayTicks = chargeable.getDecayTicks(charge.chargedWeapon);
				if (decayTicks > 0)
				{
					charge.charge = Math.max(0, charge.charge - 1f / decayTicks);
					return;
				}
			}
			else if ((charge.storePartial || charge.charge >= 1.0f) &&
				charge.dischargedTicks < chargeable.getDischargeTicks(charge.chargedWeapon))
			{
				charge.dischargedTicks++;
				return;
			}
		}
		
		charge.charge = 0f;
		charge.prevCharge = 0;
		charge.dischargedTicks = 0;
		
		if (player.equals(ClientUtils.getClientPlayer()))
			SplatcraftPacketHandler.sendToServer(new UpdateChargeStatePacket(false));
	}
	public static void updateServerMap(PlayerEntity player, boolean hasCharge)
	{
		if (!(player instanceof ServerPlayerEntity serverPlayer))
		{
			throw new IllegalStateException("Client attempted to modify server charge map");
		}
		
		if (hasChargeServerPlayerMap.containsKey(serverPlayer.getUuid()) && hasChargeServerPlayerMap.get(serverPlayer.getUuid()) == hasCharge)
		{
			throw new IllegalStateException("Charge state did not change: " + hasCharge);
			//return;
		}
		
		hasChargeServerPlayerMap.put(serverPlayer.getUuid(), hasCharge);
	}
	public float getDischargeValue(float partialTicks)
	{
		if (chargedWeapon.getItem() instanceof IChargeableWeapon chargeable)
		{
			float maxDischargeTicks = chargeable.getDischargeTicks(chargedWeapon);
			return maxDischargeTicks <= 0 ? 1 : 1 - MathHelper.lerp(partialTicks, prevDischargedTicks / maxDischargeTicks, dischargedTicks / maxDischargeTicks);
		}
		
		return 1;
	}
	public void reset()
	{
		chargedWeapon = ItemStack.EMPTY;
		charge = 0;
		prevCharge = 0;
		dischargedTicks = 0;
		prevDischargedTicks = 0;
	}
	@Override
	public String toString()
	{
		return "PlayerCharge: [" + chargedWeapon + " x " + charge + "] (" + super.toString() + ")";
	}
}