package net.splatcraft.forge.items.weapons.settings;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.forge.util.CommonUtils;

public class ShotDeviationHelper
{
    public static void tickDeviation(ItemStack stack, CommonRecords.ShotDeviationDataRecord data, float timeDelta)
    {
        DeviationData deviationData = getDeviationData(stack);

        CommonUtils.Result actualChanceResult = CommonUtils.tickValue(deviationData.chanceDecreaseDelay(), deviationData.chance(), data.chanceDecreasePerTick(), data.minDeviateChance(), timeDelta);
        CommonUtils.Result airInfluenceResult = CommonUtils.tickValue(deviationData.airborneDecreaseDelay(), deviationData.airborneInfluence(), data.airborneContractTimeToDecrease() == 0 ? Float.NaN : 1f / data.airborneContractTimeToDecrease(), 0, timeDelta);
        /* if (airborneTime > 0)
        {
            airborneTime -= timeDelta;
            if (airborneTime < 0)
            {
                if (data.airborneContractTimeToDecrease == 0)
                    airborneInfluence = 0;
                else
                    airborneInfluence -= 1f / data.airborneContractTimeToDecrease * -airborneTime;
                airborneTime = 0;
            }
        }
        else
        {
            if (data.airborneContractTimeToDecrease == 0)
                airborneInfluence = 0;
            else
            {
                if (airborneInfluence > 0)
                    airborneInfluence -= 1f / data.airborneContractTimeToDecrease;
                if (airborneInfluence < 0)
                    airborneInfluence = 0;
            }
        } */

        deviationData.setChanceDecreaseDelay(actualChanceResult.delay());
        deviationData.setChance(actualChanceResult.value());
        deviationData.setAirborneDecreaseDelay(airInfluenceResult.delay());
        deviationData.setAirborneInfluence(airInfluenceResult.value());
    }

    public static DeviationData getDeviationData(ItemStack stack)
    {
        return new DeviationData(stack.getOrCreateTag());
    }

    public static final class DeviationData // you wouldn't guess how many times the code that gets these floats has changed
    {
        private final CompoundTag nbt;

        public DeviationData(CompoundTag nbt)
        {
            this.nbt = nbt;
        }

        public float chanceDecreaseDelay() {return nbt.getFloat("Deviation_Remaining_Time_Decrease");}

        public float chance() {return nbt.getFloat("Deviation_Chance");}

        public float airborneDecreaseDelay() {return nbt.getFloat("Deviation_Airborne_Time");}

        public float airborneInfluence() {return nbt.getFloat("Deviation_Airborne_Influence");}

        public void setChanceDecreaseDelay(float chanceDecreaseDelay)
        {
            nbt.putFloat("Deviation_Remaining_Time_Decrease", chanceDecreaseDelay);
        }

        public void setChance(float chance)
        {
            nbt.putFloat("Deviation_Chance", chance);
        }

        public void setAirborneDecreaseDelay(float airborneDecreaseDelay)
        {
            nbt.putFloat("Deviation_Airborne_Time", airborneDecreaseDelay);
        }

        public void setAirborneInfluence(float airborneInfluence)
        {
            nbt.putFloat("Deviation_Airborne_Influence", airborneInfluence);
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj == this || obj instanceof DeviationData that && that.nbt.equals(nbt);
        }

        @Override
        public int hashCode()
        {
            return nbt.hashCode();
        }

        @Override
        public String toString()
        {
            return "DeviationData[" +
                    "nbt=" + nbt + ']';
        }
    }

    public static float updateShotDeviation(ItemStack stack, RandomSource random, CommonRecords.ShotDeviationDataRecord shotDeviationData)
    {
        CompoundTag nbt = stack.getOrCreateTag();
        float chance = nbt.getFloat("Deviation_Chance");
        float airborneInfluence = nbt.getFloat("Deviation_Airborne_Influence");
        float maxAngle = chance; // could be 0, but chance is a value less than 1 anyways and these water guns look wrong with perfect accuracy lol

        if (random.nextFloat() <= chance)
        {
            maxAngle = Mth.lerp(getModifiedAirInfluence(airborneInfluence), shotDeviationData.airborneShotDeviation(), shotDeviationData.groundShotDeviation());
        }

        if (chance < shotDeviationData.maxDeviateChance())
            chance += Math.min(shotDeviationData.maxDeviateChance() - chance, shotDeviationData.chanceIncreasePerShot());

        nbt.putFloat("Deviation_Remaining_Time_Decrease", shotDeviationData.chanceDecreaseDelay() + 1);
        nbt.putFloat("Deviation_Chance", chance);
        return maxAngle;
    }

    public static void registerJumpForShotDeviation(ItemStack stack, CommonRecords.ShotDeviationDataRecord shotDeviationData)
    {
        CompoundTag nbt = stack.getOrCreateTag();
        nbt.putFloat("Deviation_Airborne_Time", shotDeviationData.airborneContractDelay());
        nbt.putFloat("Deviation_Airborne_Influence", 1);
        nbt.putFloat("Deviation_Chance", shotDeviationData.deviationChanceWhenAirborne());
    }

    public static float getModifiedAirInfluence(float airborneInfluence)
    {
        return (float) Math.pow(1 - airborneInfluence, 2);
    }
}