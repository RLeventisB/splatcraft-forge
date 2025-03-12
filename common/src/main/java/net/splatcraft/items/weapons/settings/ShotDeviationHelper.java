package net.splatcraft.items.weapons.settings;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
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
		return stack.get(SplatcraftComponents.WEAPON_PRECISION_DATA);
	}
	public static float updateShotDeviation(ItemStack stack, RandomSource random, CommonRecords.ShotDeviationDataRecord shotDeviationData)
	{
		SplatcraftComponents.WeaponPrecisionData data = getDeviationData(stack);
		float chance = data.chance();
		float airborneInfluence = data.airborneInfluence();
		float maxAngle = 0;
		
		if (random.nextFloat() <= chance)
		{
			maxAngle = Mth.lerp(getModifiedAirInfluence(airborneInfluence), shotDeviationData.airborneShotDeviation(), shotDeviationData.groundShotDeviation());
		}
		
		if (chance < shotDeviationData.maxDeviateChance())
			chance += Math.min(shotDeviationData.maxDeviateChance() - chance, shotDeviationData.chanceIncreasePerShot());
		
		stack.set(SplatcraftComponents.WEAPON_PRECISION_DATA, data.withChanceDecreaseDelay(shotDeviationData.chanceDecreaseDelay() + 1).withChance(chance));
		return maxAngle;
	}
	public static void registerJumpForShotDeviation(ItemStack stack, CommonRecords.ShotDeviationDataRecord shotDeviationData)
	{
		stack.update(
			SplatcraftComponents.WEAPON_PRECISION_DATA,
			SplatcraftComponents.WeaponPrecisionData.DEFAULT,
			v -> v.registerJump(shotDeviationData)
		);
		SplatcraftComponents.WeaponPrecisionData data = getDeviationData(stack);
		stack.set(SplatcraftComponents.WEAPON_PRECISION_DATA, data
			.withAirborneDecreaseDelay(shotDeviationData.airborneContractDelay())
			.withAirborneInfluence(1)
			.withChance(shotDeviationData.deviationChanceWhenAirborne())
		);
	}
	public static float getModifiedAirInfluence(float airborneInfluence)
	{
		return (float) Math.pow(1 - airborneInfluence, 2);
	}
}
