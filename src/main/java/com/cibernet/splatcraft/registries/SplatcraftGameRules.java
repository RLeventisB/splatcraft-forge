package com.cibernet.splatcraft.registries;

import com.cibernet.splatcraft.Splatcraft;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameRules.Category;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.TreeMap;

public class SplatcraftGameRules
{
	public static final TreeMap<Integer, Boolean> booleanRules = new TreeMap<>();
	public static final TreeMap<Integer, Integer> intRules = new TreeMap<>();
	public static final ArrayList<GameRules.RuleKey> ruleList = new ArrayList<>();
	
	public static GameRules.RuleKey<GameRules.BooleanValue> INK_DECAY;
	public static GameRules.RuleKey<GameRules.BooleanValue> COLORED_PLAYER_NAMES;
	public static GameRules.RuleKey<GameRules.BooleanValue> KEEP_MATCH_ITEMS;
	public static GameRules.RuleKey<GameRules.BooleanValue> UNIVERSAL_INK;
	public static GameRules.RuleKey<GameRules.BooleanValue> DROP_CRATE_LOOT;
	public static GameRules.RuleKey<GameRules.BooleanValue> WATER_DAMAGE;
	public static GameRules.RuleKey<GameRules.BooleanValue> REQUIRE_INK_TANK;
	public static GameRules.RuleKey<GameRules.IntegerValue> INK_MOB_DAMAGE_PERCENTAGE;
	public static GameRules.RuleKey<GameRules.BooleanValue> INK_FRIENDLY_FIRE;
	public static GameRules.RuleKey<GameRules.BooleanValue> INK_REGEN;

	public static void registerGamerules()
	{
		INK_DECAY = createBooleanRule("inkDecay", Category.UPDATES, true);
		COLORED_PLAYER_NAMES = createBooleanRule("coloredPlayerNames", Category.PLAYER, false);
		KEEP_MATCH_ITEMS = createBooleanRule("keepMatchItems", Category.PLAYER, false);
		UNIVERSAL_INK = createBooleanRule("universalInk", Category.PLAYER, false);
		DROP_CRATE_LOOT = createBooleanRule("dropCrateLoot", Category.DROPS, false);
		WATER_DAMAGE = createBooleanRule("waterDamage", Category.PLAYER, false);
		REQUIRE_INK_TANK = createBooleanRule("requireInkTank", Category.PLAYER, true);
		INK_FRIENDLY_FIRE = createBooleanRule("inkFriendlyFire", Category.PLAYER, false);
		INK_REGEN = createBooleanRule("inkRegeneration", Category.PLAYER, true);
		INK_MOB_DAMAGE_PERCENTAGE = createIntRule("inkMobDamagePercentage", Category.MOBS, 0);
	}
	
	public static GameRules.RuleKey<GameRules.BooleanValue> createBooleanRule(String name, GameRules.Category category, boolean defaultValue)
	{
			Method booleanValueCreate = ObfuscationReflectionHelper.findMethod(GameRules.BooleanValue.class, "func_223568_b", boolean.class);
			booleanValueCreate.setAccessible(true);
			
				try
				{
					Object booleanValue = booleanValueCreate.invoke(GameRules.BooleanValue.class, defaultValue);
					GameRules.RuleKey<GameRules.BooleanValue> ruleKey = GameRules.register(Splatcraft.MODID + "." + name, category, (GameRules.RuleType<GameRules.BooleanValue>) booleanValue);
					ruleList.add(ruleKey);
					booleanRules.put(getRuleIndex(ruleKey), defaultValue);
					return ruleKey;
					
				} catch(IllegalAccessException e)
				{
					e.printStackTrace();
				} catch(InvocationTargetException e)
				{
					e.printStackTrace();
				}
		return null;
	}

	public static GameRules.RuleKey<GameRules.IntegerValue> createIntRule(String name, GameRules.Category category, int defaultValue)
	{
			Method intValueCreate = ObfuscationReflectionHelper.findMethod(GameRules.IntegerValue.class, "func_223559_b", int.class);
		intValueCreate.setAccessible(true);

				try
				{
					Object intValue = intValueCreate.invoke(GameRules.IntegerValue.class, defaultValue);
					GameRules.RuleKey<GameRules.IntegerValue> ruleKey = GameRules.register(Splatcraft.MODID + "." + name, category, (GameRules.RuleType<GameRules.IntegerValue>) intValue);

					ruleList.add(ruleKey);
					intRules.put(getRuleIndex(ruleKey), defaultValue);
					return ruleKey;

				} catch(IllegalAccessException e)
				{
					e.printStackTrace();
				} catch(InvocationTargetException e)
				{
					e.printStackTrace();
				}
		return null;
	}
	
	public static int getRuleIndex(GameRules.RuleKey rule)
	{
		return ruleList.indexOf(rule);
	}
	
	public static GameRules.RuleKey getRuleFromIndex(int index)
	{
		return ruleList.get(index);
	}
	
	public static boolean getBooleanRuleValue(World world, GameRules.RuleKey<GameRules.BooleanValue> rule)
	{
		return world.isRemote ? getClientsideBooleanValue(rule) : world.getGameRules().getBoolean(rule);
	}

	public static int getIntRuleValue(World world, GameRules.RuleKey<GameRules.IntegerValue> rule)
	{
		return world.isRemote ? getClientsideIntValue(rule) : world.getGameRules().getInt(rule);
	}
	
	public static boolean getClientsideBooleanValue(GameRules.RuleKey<GameRules.BooleanValue> rule)
	{
		return booleanRules.get(getRuleIndex(rule));
	}

	public static int getClientsideIntValue(GameRules.RuleKey<GameRules.IntegerValue> rule)
	{
		return intRules.get(getRuleIndex(rule));
	}
}
