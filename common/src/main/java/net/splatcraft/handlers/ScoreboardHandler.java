package net.splatcraft.handlers;

import com.google.common.collect.Maps;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.stat.StatFormatter;
import net.minecraft.util.Identifier;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.util.InkColor;

import java.util.*;

public class ScoreboardHandler
{
	public static final DeferredRegister<Identifier> REGISTRY = Splatcraft.deferredRegistryOf(Registries.CUSTOM_STAT);
	public static final Identifier COLOR = register("ink_color", StatFormatter.DEFAULT);
	public static final Identifier TURF_WAR_SCORE = register("turf_war_score", StatFormatter.DEFAULT);
	protected static final Map<InkColor, CriteriaInkColor[]> COLOR_CRITERIA = Maps.newHashMap();
	private static Identifier register(String id, StatFormatter formatter)
	{
		Identifier identifier = Splatcraft.identifierOf(id);
		REGISTRY.register(id, () -> identifier);
//		Stats.CUSTOM.getOrCreateStat(identifier, formatter);
		return identifier;
	}
	//this method is WEIRD why is the third parameter called "color" which sets the score as the color value, which is fine, until you get to TurfScannerItem putting something that isnt a color here????
	public static void updatePlayerScore(ScoreboardCriterion criteria, PlayerEntity player, InkColor color)
	{
		player.getScoreboard().forEachScore(criteria, ScoreHolder.fromProfile(player.getGameProfile()), scoreAccess -> scoreAccess.setScore(color.getColor()));
	}
	public static void updatePlayerScore(ScoreboardCriterion criteria, PlayerEntity player, int score)
	{
		player.getScoreboard().forEachScore(criteria, ScoreHolder.fromProfile(player.getGameProfile()), scoreAccess -> scoreAccess.setScore(score));
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
	public static class CriteriaInkColor extends ScoreboardCriterion
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
			CRITERIA.remove(name);
		}
	}
}
