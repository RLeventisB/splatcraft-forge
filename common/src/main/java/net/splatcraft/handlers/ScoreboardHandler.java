package net.splatcraft.handlers;

import com.google.common.collect.Maps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.util.InkColor;

import java.util.*;

public class ScoreboardHandler
{
	public static final DeferredRegister<ResourceLocation> REGISTRY = Splatcraft.deferredRegistryOf(BuiltInRegistries.CUSTOM_STAT);
	public static final ResourceLocation COLOR = register("ink_color", StatFormatter.DEFAULT);
	public static final ResourceLocation TURF_WAR_SCORE = register("turf_war_score", StatFormatter.DEFAULT);
	protected static final Map<InkColor, CriteriaInkColor[]> COLOR_CRITERIA = Maps.newHashMap();
	private static ResourceLocation register(String id, StatFormatter formatter)
	{
		ResourceLocation identifier = Splatcraft.identifierOf(id);
		REGISTRY.register(id, () -> identifier);
//		Stats.CUSTOM.getOrCreateStat(identifier, formatter);
		return identifier;
	}
	//this method is WEIRD why is the third parameter called "color" which sets the score as the color value, which is fine, until you get to TurfScannerItem putting something that isnt a color here????
	public static void updatePlayerScore(ObjectiveCriteria criteria, Player player, InkColor color)
	{
		player.getScoreboard().forAllObjectives(criteria, ScoreHolder.fromGameProfile(player.getGameProfile()), scoreAccess -> scoreAccess.set(color.getColor()));
	}
	public static void updatePlayerScore(ObjectiveCriteria criteria, Player player, int score)
	{
		player.getScoreboard().forAllObjectives(criteria, ScoreHolder.fromGameProfile(player.getGameProfile()), scoreAccess -> scoreAccess.set(score));
	}
	public static void createColorCriterion(InkColor color)
	{
		COLOR_CRITERIA.put(color, new CriteriaInkColor[]
			{
				new CriteriaInkColor("colorKills", color),
				new CriteriaInkColor("deathsAsColor", color),
				new CriteriaInkColor("killsAsColor", color),
				new CriteriaInkColor("winsAsColor", color),
				new CriteriaInkColor("lossesAsColor", color),
			});
	}
	public static void clearColorCriteria()
	{
		for (InkColor color : COLOR_CRITERIA.keySet())
		{
			for (CriteriaInkColor c : COLOR_CRITERIA.get(color))
			{
				c.remove();
			}
		}
		COLOR_CRITERIA.clear();
	}
	public static void removeColorCriterion(InkColor color)
	{
		if (hasColorCriterion(color))
		{
			for (CriteriaInkColor c : COLOR_CRITERIA.get(color))
			{
				c.remove();
			}
			COLOR_CRITERIA.remove(color);
		}
	}
	public static boolean hasColorCriterion(InkColor color)
	{
		return COLOR_CRITERIA.containsKey(color);
	}
	public static Iterable<String> getCriteriaSuggestions()
	{
		List<String> suggestions = new ArrayList<>();
		
		COLOR_CRITERIA.keySet().forEach(key ->
		{
			suggestions.add(key.toString());
		});
		
		return suggestions;
	}
	public static Set<InkColor> getCriteriaKeySet()
	{
		return COLOR_CRITERIA.keySet();
	}
	public static CriteriaInkColor getColorKills(InkColor color)
	{
		return COLOR_CRITERIA.get(color)[0];
	}
	public static CriteriaInkColor getDeathsAsColor(InkColor color)
	{
		return COLOR_CRITERIA.get(color)[1];
	}
	public static CriteriaInkColor getKillsAsColor(InkColor color)
	{
		return COLOR_CRITERIA.get(color)[2];
	}
	public static CriteriaInkColor getColorWins(InkColor color)
	{
		return COLOR_CRITERIA.get(color)[3];
	}
	public static CriteriaInkColor getColorLosses(InkColor color)
	{
		return COLOR_CRITERIA.get(color)[4];
	}
	public static CriteriaInkColor[] getAllFromColor(InkColor color)
	{
		return COLOR_CRITERIA.get(color);
	}
	public static void register()
	{
	}
	public static String getColorIdentifier(InkColor color)
	{
		return Objects.requireNonNull(InkColorRegistry.getColorAlias(color)).getPath();
	}
	public static class CriteriaInkColor extends ObjectiveCriteria
	{
		private final String name;
		public CriteriaInkColor(String name, InkColor color)
		{
			super((Objects.requireNonNull(InkColorRegistry.getColorAlias(color)).getNamespace())
				+ "." + name + "." + getColorIdentifier(color));
			this.name = (Objects.requireNonNull(InkColorRegistry.getColorAlias(color)).getNamespace())
				+ "." + name + "." + getColorIdentifier(color);
		}
		public void remove()
		{
			CRITERIA_CACHE.remove(name);
		}
	}
}
