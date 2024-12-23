package net.splatcraft.items.weapons.settings;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.util.CommonUtils;

public class ShotDeviationHelper
{
	public static void tickDeviation(ItemStack stack, CommonRecords.ShotDeviationDataRecord data, float timeDelta)
	{
		SplatcraftComponents.WeaponPrecisionData deviationData = getDeviationData(stack);
		
		CommonUtils.Result actualChanceResult = CommonUtils.tickValue(deviationData.chanceDecreaseDelay(), deviationData.chance(), data.chanceDecreasePerTick(), data.minDeviateChance(), timeDelta);
		CommonUtils.Result airInfluenceResult = CommonUtils.tickValue(deviationData.airborneDecreaseDelay(), deviationData.airborneInfluence(), data.airborneContractTimeToDecrease() == 0 ? Float.NaN : 1f / data.airborneContractTimeToDecrease(), 0, timeDelta);
		
		deviationData.setChanceDecreaseDelay(actualChanceResult.delay());
		deviationData.setChance(actualChanceResult.value());
		deviationData.setAirborneDecreaseDelay(airInfluenceResult.delay());
		deviationData.setAirborneInfluence(airInfluenceResult.value());
	}
	public static SplatcraftComponents.WeaponPrecisionData getDeviationData(ItemStack stack)
	{
		return stack.getComponents().get(SplatcraftComponents.WEAPON_PRECISION_DATA);
	}
	public static float updateShotDeviation(ItemStack stack, Random random, CommonRecords.ShotDeviationDataRecord shotDeviationData)
	{
		SplatcraftComponents.WeaponPrecisionData nbt = getDeviationData(stack);
		float chance = nbt.chance();
		float airborneInfluence = nbt.airborneInfluence();
		float maxAngle = chance; // could be 0, but chance is a value less than 1 anyways and these water guns look wrong with perfect accuracy lol
		
		if (random.nextFloat() <= chance)
		{
			maxAngle = MathHelper.lerp(getModifiedAirInfluence(airborneInfluence), shotDeviationData.airborneShotDeviation(), shotDeviationData.groundShotDeviation());
		}
		
		if (chance < shotDeviationData.maxDeviateChance())
			chance += Math.min(shotDeviationData.maxDeviateChance() - chance, shotDeviationData.chanceIncreasePerShot());
		
		nbt.setChanceDecreaseDelay(shotDeviationData.chanceDecreaseDelay() + 1);
		nbt.setChance(chance);
		return maxAngle;
	}
	public static void registerJumpForShotDeviation(ItemStack stack, CommonRecords.ShotDeviationDataRecord shotDeviationData)
	{
		stack.apply(
            SplatcraftComponents.WEAPON_PRECISION_DATA,
            SplatcraftComponents.WeaponPrecisionData.DEFAULT,
            v -> v.registerJump(shotDeviationData)
        );
		SplatcraftComponents.WeaponPrecisionData nbt = getDeviationData(stack);
		nbt.setAirborneDecreaseDelay(shotDeviationData.airborneContractDelay());
		nbt.setAirborneInfluence(1);
		nbt.setChance(shotDeviationData.deviationChanceWhenAirborne());
	}
	public static float getModifiedAirInfluence(float airborneInfluence)
	{
		return (float) Math.pow(1 - airborneInfluence, 2);
	}
}
