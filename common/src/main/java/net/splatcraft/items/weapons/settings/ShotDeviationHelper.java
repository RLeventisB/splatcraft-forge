package net.splatcraft.items.weapons.settings;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.util.CommonUtils;

public class ShotDeviationHelper
{
	public static void tickDeviation(ItemStack stack, CommonRecords.ShotDeviationDataRecord shotData, float timeDelta)
	{
		SplatcraftComponents.WeaponPrecisionData data = getDeviationData(stack);
		
		CommonUtils.Result actualChanceResult = CommonUtils.tickValue(data.chanceDecreaseDelay(), data.chance(), shotData.chanceDecreasePerTick(), shotData.minDeviateChance(), timeDelta);
		CommonUtils.Result airInfluenceResult = CommonUtils.tickValue(data.airborneDecreaseDelay(), data.airborneInfluence(), shotData.airborneContractTimeToDecrease() == 0 ? Float.NaN : 1f / shotData.airborneContractTimeToDecrease(), 0, timeDelta);
		
		stack.set(SplatcraftComponents.WEAPON_PRECISION_DATA, data
			.withChanceDecreaseDelay(actualChanceResult.delay())
			.withChance(actualChanceResult.value())
			.withAirborneDecreaseDelay(airInfluenceResult.delay())
			.withAirborneInfluence(airInfluenceResult.value())
		);
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
		
		nbt.withChanceDecreaseDelay(shotDeviationData.chanceDecreaseDelay() + 1);
		nbt.withChance(chance);
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
		nbt.withAirborneDecreaseDelay(shotDeviationData.airborneContractDelay());
		nbt.withAirborneInfluence(1);
		nbt.withChance(shotDeviationData.deviationChanceWhenAirborne());
	}
	public static float getModifiedAirInfluence(float airborneInfluence)
	{
		return (float) Math.pow(1 - airborneInfluence, 2);
	}
}
