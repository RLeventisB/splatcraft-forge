package net.splatcraft.criteriaTriggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;

import java.util.Optional;

public class ChangeInkColorTrigger extends SimpleCriterionTrigger<ChangeInkColorTrigger.Conditions>
{
    public void trigger(ServerPlayer player)
    {
        trigger(player, (instance) -> instance.matches(player));
    }

    @Override
    public Codec<Conditions> codec()
    {
        return Conditions.CODEC;
    }

    public record Conditions(Optional<ContextAwarePredicate> player,
                             Optional<InkColor> color) implements SimpleCriterionTrigger.SimpleInstance
    {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
            (instance) -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                InkColor.HEX_CODEC.optionalFieldOf("color").forGetter(Conditions::color)
            ).apply(instance, Conditions::new));

        public Conditions(Optional<ContextAwarePredicate> player, Optional<InkColor> color)
        {
            this.player = player;
            this.color = color;
        }

        public boolean matches(ServerPlayer player)
        {
            return color.isPresent() && ColorUtils.getEntityColor(player) == color.get();
        }
    }
}
