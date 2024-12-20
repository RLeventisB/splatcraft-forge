package net.splatcraft.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.splatcraft.handlers.DataHandler;
import net.splatcraft.items.weapons.settings.AbstractWeaponSettings;

import java.text.DecimalFormat;
import java.util.List;

@SuppressWarnings("unchecked")
public class WeaponTooltip<S extends AbstractWeaponSettings<S, ?>>
{
    public static final IStatRanker RANKER_ASCENDING = Float::compare;
    public static final IStatRanker RANKER_DESCENDING = (a, b) -> Float.compare(b, a);
    private final String name;
    private final IStatValueGetter<S> valueGetter;
    private final IStatRanker ranker;
    private final Metrics metric;

    public WeaponTooltip(String name, Metrics metric, IStatValueGetter<S> valueGetter, IStatRanker ranker)
    {
        this.name = name;
        this.valueGetter = valueGetter;
        this.ranker = ranker;
        this.metric = metric;
    }

    public float getStatValue(S settings)
    {
        return valueGetter.get(settings);
    }

    public int getStatRanking(S settings)
    {

        //this can be pooled if we need to micro-optimize
        List<Float> settingsList = DataHandler.WeaponStatsListener.SETTINGS.values().stream().filter(settings.getClass()::isInstance).filter(s -> !s.isSecret).map(settings.getClass()::cast)
            .sorted((setting, other) -> ranker.apply(valueGetter, (S) setting, (S) other)).map((setting) -> valueGetter.get((S) setting)).distinct().toList();

        float value = valueGetter.get(settings);
        if (!settings.isSecret)
            return settingsList.indexOf(value) == 0 ? 0 : (int) Math.ceil((float) (settingsList.indexOf(value) + 1) / settingsList.size() * 5f);

        for (float valueToCompare : settingsList)
        {
            if (ranker.apply(value, valueToCompare) <= 0)
                return settingsList.indexOf(value) == 0 ? 0 : (int) Math.ceil((float) (settingsList.indexOf(valueToCompare) + 1) / settingsList.size() * 5f);
        }

        return 6;
    }

    public MutableText getTextComponent(S settings, boolean advanced)
    {
        if (advanced)
            return Text.translatable("weaponStat.format", Text.translatable("weaponStat." + name),
                    Text.translatable("weaponStat.metric." + metric.localizedName, new DecimalFormat("0.#").format(getStatValue(settings))))
                .formatted(Formatting.DARK_GREEN);
        else
        {
            int ranking = getStatRanking(settings);

            Object[] args = new Object[5];
            for (int i = 0; i < 5; i++)
                args[i] = Text.translatable("weaponStat.gauge." + (ranking >= (i + 1) ? "full" : "empty"));

            return Text.translatable("weaponStat.format", Text.translatable("weaponStat." + name), Text.translatable("weaponStat.metric.gauge", args)).formatted(ranking > 5 ? Formatting.GOLD : Formatting.DARK_GREEN);
        }
    }

    @Override
    public String toString()
    {
        return name;
    }

    public enum Metrics
    {
        SECONDS("seconds"),
        TICKS("ticks"),
        BLOCKS("blocks"),
        BPS("bullets_per_second"),
        BPT("blocks_per_tick"),
        MULTIPLIER("multiplier"),
        UNITS("units"),
        HEALTH("health");
        public final String localizedName;

        Metrics(String localizedName)
        {
            this.localizedName = localizedName;
        }
    }

    public interface IStatValueGetter<S extends AbstractWeaponSettings<S, ?>>
    {
        float get(S settings);
    }

    public interface IStatRanker
    {
        int apply(float value, float other);

        default <S extends AbstractWeaponSettings<S, ?>> int apply(IStatValueGetter<S> statValueGetter, S settings, S other)
        {
            return apply(statValueGetter.get(settings), statValueGetter.get(other));
        }
    }
}
