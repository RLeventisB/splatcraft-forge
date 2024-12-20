package net.splatcraft.registries;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.Stage;

import java.util.ArrayList;
import java.util.TreeMap;

public class SplatcraftGameRules
{
    public static final TreeMap<Integer, Boolean> booleanRules = new TreeMap<>();
    public static final TreeMap<Integer, Integer> intRules = new TreeMap<>();
    public static final ArrayList<GameRules.Key<?>> ruleList = new ArrayList<>();
    public static GameRules.Key<GameRules.BooleanRule> INK_DECAY;
    public static GameRules.Key<GameRules.IntRule> INK_DECAY_RATE;
    public static GameRules.Key<GameRules.BooleanRule> INKABLE_GROUND;
    public static GameRules.Key<GameRules.BooleanRule> INK_DESTROYS_FOLIAGE;
    public static GameRules.Key<GameRules.BooleanRule> KEEP_MATCH_ITEMS;
    public static GameRules.Key<GameRules.BooleanRule> UNIVERSAL_INK;
    public static GameRules.Key<GameRules.BooleanRule> DROP_CRATE_LOOT;
    public static GameRules.Key<GameRules.BooleanRule> WATER_DAMAGE;
    public static GameRules.Key<GameRules.BooleanRule> REQUIRE_INK_TANK;
    public static GameRules.Key<GameRules.IntRule> INK_MOB_DAMAGE_PERCENTAGE;
    public static GameRules.Key<GameRules.BooleanRule> INK_FRIENDLY_FIRE;
    public static GameRules.Key<GameRules.BooleanRule> INK_HEALING;
    public static GameRules.Key<GameRules.BooleanRule> INK_HEALING_CONSUMES_HUNGER;
    public static GameRules.Key<GameRules.BooleanRule> INK_DAMAGE_COOLDOWN;
    public static GameRules.Key<GameRules.BooleanRule> INFINITE_INK_IN_CREATIVE;
    public static GameRules.Key<GameRules.BooleanRule> RECHARGEABLE_INK_TANK;
    public static GameRules.Key<GameRules.BooleanRule> GLOBAL_SUPERJUMPING;
    public static GameRules.Key<GameRules.BooleanRule> BLOCK_DESTROY_INK;
    public static GameRules.Key<GameRules.IntRule> SUPERJUMP_DISTANCE_LIMIT;
    public static GameRules.Key<GameRules.IntRule> INK_PROJECTILE_FREQUENCY;

    public static void registerGamerules()
    {
        INK_DECAY = createBooleanRule("inkDecay", GameRules.Category.UPDATES, true);
        INK_DECAY_RATE = createIntRule("inkDecayRate", GameRules.Category.UPDATES, 3);
        KEEP_MATCH_ITEMS = createBooleanRule("keepMatchItems", GameRules.Category.PLAYER, false);
        UNIVERSAL_INK = createBooleanRule("universalInk", GameRules.Category.PLAYER, false);
        DROP_CRATE_LOOT = createBooleanRule("dropCrateLoot", GameRules.Category.DROPS, false);
        WATER_DAMAGE = createBooleanRule("waterDamage", GameRules.Category.PLAYER, false);
        REQUIRE_INK_TANK = createBooleanRule("requireInkTank", GameRules.Category.PLAYER, true);
        INK_FRIENDLY_FIRE = createBooleanRule("inkFriendlyFire", GameRules.Category.PLAYER, false);
        INK_HEALING = createBooleanRule("inkHealing", GameRules.Category.PLAYER, true);
        INK_HEALING_CONSUMES_HUNGER = createBooleanRule("inkHealingConsumesHunger", GameRules.Category.PLAYER, true);
        INK_DAMAGE_COOLDOWN = createBooleanRule("inkDamageCooldown", GameRules.Category.PLAYER, false);
        GLOBAL_SUPERJUMPING = createBooleanRule("globalSuperJumping", GameRules.Category.PLAYER, true);
        SUPERJUMP_DISTANCE_LIMIT = createIntRule("superJumpDistanceLimit", GameRules.Category.PLAYER, 1000);
        INK_MOB_DAMAGE_PERCENTAGE = createIntRule("inkMobDamagePercentage", GameRules.Category.MOBS, 70);
        INFINITE_INK_IN_CREATIVE = createBooleanRule("infiniteInkInCreative", GameRules.Category.PLAYER, true);
        INKABLE_GROUND = createBooleanRule("inkableGround", GameRules.Category.MISC, true);
        INK_DESTROYS_FOLIAGE = createBooleanRule("inkDestroysFoliage", GameRules.Category.MISC, true);
        RECHARGEABLE_INK_TANK = createBooleanRule("rechargeableInkTank", GameRules.Category.PLAYER, true);
        BLOCK_DESTROY_INK = createBooleanRule("blockDestroysInk", GameRules.Category.PLAYER, false);
        INK_PROJECTILE_FREQUENCY = createIntRule("projectileTickFrequencyPercent", GameRules.Category.UPDATES, 100);
    }

    public static boolean getLocalizedRule(World level, BlockPos pos, GameRules.Key<GameRules.BooleanRule> rule)
    {
        ArrayList<Stage> stages = Stage.getStagesForPosition(level, new Vec3d(pos.getX(), pos.getY(), pos.getZ()));

        Stage localStage = null;
        Box localStageBounds = null;

        for (Stage stage : stages)
        {
            Box stageBounds = stage.getBounds();

            if (localStage == null || stageBounds.getAverageSideLength() < localStageBounds.getAverageSideLength())
            {
                localStage = stage;
                localStageBounds = stage.getBounds();
            }
        }

        if (localStage != null && localStage.hasSetting(rule))
            return localStage.getSetting(rule);

        return getBooleanRuleValue(level, rule);
    }

    public static GameRules.Key<GameRules.BooleanRule> createBooleanRule(String name, GameRules.Category category, boolean defaultValue)
    {
        GameRules.Type<GameRules.BooleanRule> booleanValue = GameRules.BooleanRule.create(defaultValue);
        GameRules.Key<GameRules.BooleanRule> ruleKey = GameRules.register(Splatcraft.MODID + "." + name, category, booleanValue);
        ruleList.add(ruleKey);
        booleanRules.put(getRuleIndex(ruleKey), defaultValue);
        return ruleKey;
    }

    public static GameRules.Key<GameRules.IntRule> createIntRule(String name, GameRules.Category category, int defaultValue)
    {
        GameRules.Type<GameRules.IntRule> intValue = GameRules.IntRule.create(defaultValue);
        GameRules.Key<GameRules.IntRule> ruleKey = GameRules.register(Splatcraft.MODID + "." + name, category, intValue);

        ruleList.add(ruleKey);
        intRules.put(getRuleIndex(ruleKey), defaultValue);
        return ruleKey;
    }

    public static int getRuleIndex(GameRules.Key<?> rule)
    {
        return ruleList.indexOf(rule);
    }

    public static <T extends GameRules.Rule<T>> GameRules.Key<T> getRuleFromIndex(int index)
    {
        return (GameRules.Key<T>) ruleList.get(index);
    }

    public static boolean getBooleanRuleValue(World world, GameRules.Key<GameRules.BooleanRule> rule)
    {
        return world.isClient() ? getClientsideBooleanValue(rule) : world.getGameRules().getBoolean(rule);
    }

    public static int getIntRuleValue(World world, GameRules.Key<GameRules.IntRule> rule)
    {
        return world.isClient() ? getClientsideIntValue(rule) : world.getGameRules().getInt(rule);
    }

    public static boolean getClientsideBooleanValue(GameRules.Key<GameRules.BooleanRule> rule)
    {
        return booleanRules.get(getRuleIndex(rule));
    }

    public static int getClientsideIntValue(GameRules.Key<GameRules.IntRule> rule)
    {
        return intRules.get(getRuleIndex(rule));
    }
}